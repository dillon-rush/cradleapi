/*
 * Copyright 2020-2021 Exactpro (Exactpro Systems Limited)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.exactpro.cradle;

import java.io.IOException;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

import com.exactpro.cradle.intervals.IntervalsWorker;
import com.exactpro.cradle.messages.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.exactpro.cradle.resultset.CradleResultSet;
import com.exactpro.cradle.resultset.EmptyResultSet;
import com.exactpro.cradle.testevents.StoredTestEvent;
import com.exactpro.cradle.testevents.TestEventFilter;
import com.exactpro.cradle.testevents.StoredTestEventId;
import com.exactpro.cradle.testevents.TestEventToStore;
import com.exactpro.cradle.utils.CradleStorageException;
import com.exactpro.cradle.utils.TestEventUtils;

/**
 * Storage which holds information about all data sent or received and test events.
 */
public abstract class CradleStorage
{
	private static final Logger logger = LoggerFactory.getLogger(CradleStorage.class);
	public static final ZoneOffset TIMEZONE_OFFSET = ZoneOffset.UTC;
	
	private final Map<BookId, BookInfo> books;
	protected final BookAndPageChecker bpc;
	private volatile boolean initialized = false,
			disposed = false;
	
	public CradleStorage() throws CradleStorageException
	{
		books = new ConcurrentHashMap<>();
		bpc = new BookAndPageChecker(books);
	}
	
	
	protected abstract void doInit(boolean prepareStorage) throws CradleStorageException;
	protected abstract void doDispose() throws CradleStorageException;
	
	protected abstract Collection<BookInfo> loadBooks() throws IOException;
	protected abstract void doAddBook(BookToAdd newBook, BookId bookId) throws IOException;
	protected abstract void doSwitchToNewPage(BookId bookId, String pageName, Instant timestamp, String comment, 
			PageInfo prevPage) throws CradleStorageException, IOException;
	protected abstract Collection<PageInfo> doLoadPages(BookId bookId) throws CradleStorageException, IOException;
	
	
	protected abstract void doStoreMessageBatch(MessageBatchToStore batch, PageInfo page) throws IOException;
	protected abstract CompletableFuture<Void> doStoreMessageBatchAsync(MessageBatchToStore batch,
			PageInfo page) throws IOException, CradleStorageException;
	
	
	protected abstract void doStoreTestEvent(TestEventToStore event, PageInfo page) throws IOException, CradleStorageException;
	protected abstract CompletableFuture<Void> doStoreTestEventAsync(TestEventToStore event, PageInfo page) throws IOException, CradleStorageException;
	protected abstract void doUpdateParentTestEvents(TestEventToStore event) throws IOException;
	protected abstract CompletableFuture<Void> doUpdateParentTestEventsAsync(TestEventToStore event);
	protected abstract void doUpdateEventStatus(StoredTestEvent event, boolean success) throws IOException;
	protected abstract CompletableFuture<Void> doUpdateEventStatusAsync(StoredTestEvent event, boolean success);
	
	
	protected abstract StoredMessage doGetMessage(StoredMessageId id, PageId pageId) throws IOException;
	protected abstract CompletableFuture<StoredMessage> doGetMessageAsync(StoredMessageId id, PageId pageId)
			throws CradleStorageException;
	protected abstract Collection<StoredMessage> doGetMessageBatch(StoredMessageId id, PageId pageId) throws IOException;
	protected abstract CompletableFuture<Collection<StoredMessage>> doGetMessageBatchAsync(StoredMessageId id, PageId pageId)
			throws CradleStorageException;
	
	protected abstract Iterable<StoredMessage> doGetMessages(StoredMessageFilter filter, BookInfo book) throws IOException;
	protected abstract CompletableFuture<Iterable<StoredMessage>> doGetMessagesAsync(StoredMessageFilter filter, BookInfo book)
			throws CradleStorageException;
	protected abstract CradleResultSet<StoredMessageBatch> doGetMessagesBatches(StoredMessageFilter filter, BookInfo book) throws IOException;
	protected abstract CompletableFuture<CradleResultSet<StoredMessageBatch>> doGetMessagesBatchesAsync(StoredMessageFilter filter,
			BookInfo book) throws CradleStorageException;
	
	protected abstract long doGetLastSequence(String sessionAlias, Direction direction, BookId bookId)
			throws IOException, CradleStorageException;
	protected abstract Collection<String> doGetSessionAliases(BookId bookId) throws IOException, CradleStorageException;
	
	
	protected abstract StoredTestEvent doGetTestEvent(StoredTestEventId id, PageId pageId) throws IOException, CradleStorageException;
	protected abstract CompletableFuture<StoredTestEvent> doGetTestEventAsync(StoredTestEventId ids, PageId pageId) throws CradleStorageException;
	
	protected abstract CradleResultSet<StoredTestEvent> doGetTestEvents(TestEventFilter filter, BookInfo book) 
			throws CradleStorageException, IOException;
	protected abstract CompletableFuture<CradleResultSet<StoredTestEvent>> doGetTestEventsAsync(TestEventFilter filter, BookInfo book) 
			throws CradleStorageException, IOException;
	
	protected abstract Collection<String> doGetScopes(BookId bookId) throws IOException, CradleStorageException;
	
	
	/**
	 * Initializes internal objects of storage and prepares it to access data, i.e. creates needed connections and facilities.
	 * @param prepareStorage if underlying physical storage should be created, if absent
	 * @throws CradleStorageException if storage initialization failed
	 * @throws IOException if data reading or creation of storage failed
	 */
	public void init(boolean prepareStorage) throws CradleStorageException, IOException
	{
		if (initialized)
			return;
		
		logger.info("Initializing storage");
		
		doInit(prepareStorage);
		
		Collection<BookInfo> loaded = loadBooks();
		if (loaded != null)
			loaded.forEach((b) -> books.put(b.getId(), b));
		
		initialized = true;
		logger.info("Storage initialized");
	}
	
	/**
	 * IntervalsWorker is used to work with Crawler intervals
	 * @param pageId page to get worker for
	 * @return instance of IntervalsWorker
	 */
	public abstract IntervalsWorker getIntervalsWorker(PageId pageId);
	
	
	/**
	 * Disposes resources occupied by storage which means closing of opened connections, flushing all buffers, etc.
	 * @throws CradleStorageException if there was error during storage disposal, which may mean issue with data flushing, unexpected connection break, etc.
	 */
	public final void dispose() throws CradleStorageException
	{
		if (disposed)
			return;
		
		logger.info("Disposing storage");
		doDispose();
		disposed = true;
		logger.info("Storage disposed");
	}
	
	/**
	 * @return true if storage is already disposed and false if it is not disposed, including the case when disposal failed with error
	 */
	public final boolean isDisposed()
	{
		return disposed;
	}
	
	
	/**
	 * Creates new book and adds it to storage, adding page with given name to newly created book
	 * @param book information about book to add and its first page
	 * @return {@link BookInfo} containing all information about created book
	 * @throws CradleStorageException if the book is already present
	 * @throws IOException if book data writing failed
	 */
	public BookInfo addBook(BookToAdd book) throws CradleStorageException, IOException
	{
		BookId id = new BookId(book.getName());
		logger.info("Adding book '{}' to storage", id);
		if (books.containsKey(id))
			throw new CradleStorageException("Book '"+id+"' is already present in storage");
		
		doAddBook(book, id);
		BookInfo newBook = new BookInfo(id, book.getFullName(), book.getDesc(), book.getCreated(), null);
		books.put(newBook.getId(), newBook);
		logger.info("Book '{}' has been added to storage", id);
		
		newBook = switchToNewPage(id, book.getFirstPageName(), book.getCreated(), book.getFirstPageComment());
		books.put(newBook.getId(), newBook);
		return newBook;
	}
	
	/**
	 * @return collection of books currently available in storage
	 */
	public Collection<BookInfo> getBooks()
	{
		return Collections.unmodifiableCollection(books.values());
	}
	
	/**
	 * Changes active page of given book to new one, started at current timestamp
	 * @param bookId ID of the book whose page to change
	 * @param pageName name of new page
	 * @param pageComment optional comment for new page
	 * @return updated book information
	 * @throws CradleStorageException if given bookId is unknown or page with given name already exists in this book
	 * @throws IOException if page data writing failed
	 */
	public BookInfo switchToNewPage(BookId bookId, String pageName, String pageComment) throws CradleStorageException, IOException
	{
		return switchToNewPage(bookId, pageName, Instant.now(), pageComment);
	}
	
	/**
	 * Refreshes pages information of given book, loading actual data from storage. 
	 * Use this method to refresh Cradle API internal book cache when page of the book was switched or removed outside of the application
	 * @param bookId ID of the book whose pages to refresh
	 * @return refreshed book information
	 * @throws CradleStorageException if given bookId is unknown
	 * @throws IOException if page data reading failed
	 */
	public BookInfo refreshPages(BookId bookId) throws CradleStorageException, IOException
	{
		logger.info("Refreshing pages of book '{}'", bookId);
		BookInfo book = bpc.getBook(bookId);
		Collection<PageInfo> pages = doLoadPages(bookId);
		book = new BookInfo(book.getId(), book.getFullName(), book.getDesc(), book.getCreated(), pages);
		books.put(book.getId(), book);
		return book;
	}
	
	
	/**
	 * Writes data about given message batch to current page
	 * @param batch data to write
	 * @throws IOException if data writing failed
	 * @throws CradleStorageException if given parameters are invalid
	 */
	public final void storeMessageBatch(MessageBatchToStore batch) throws IOException, CradleStorageException
	{
		StoredMessageId id = batch.getId();
		logger.debug("Storing message batch {}", id);
		PageInfo page = bpc.checkActivePage(id.getBookId(), id.getTimestamp());
		doStoreMessageBatch(batch, page);
		logger.debug("Message batch {} has been stored", id);
	}
	
	
	/**
	 * Asynchronously writes data about given message batch to current page
	 * @param batch data to write
	 * @return future to get know if storing was successful
	 * @throws CradleStorageException if given parameters are invalid
	 * @throws IOException if data writing failed
	 */
	public final CompletableFuture<Void> storeMessageBatchAsync(MessageBatchToStore batch)
			throws CradleStorageException, IOException
	{
		StoredMessageId id = batch.getId();
		logger.debug("Storing message batch {} asynchronously", id);
		PageInfo page = bpc.checkActivePage(id.getBookId(), id.getTimestamp());
		CompletableFuture<Void> result = doStoreMessageBatchAsync(batch, page);
		result.whenComplete((r, error) -> {
				if (error != null)
					logger.error("Error while storing message batch "+id+" asynchronously", error);
				else
					logger.debug("Message batch {} has been stored asynchronously", id);
			});
		return result;
	}
	
	
	/**
	 * Writes data about given test event to current page
	 * @param event data to write
	 * @throws IOException if data writing failed
	 * @throws CradleStorageException if given parameters are invalid
	 */
	public final void storeTestEvent(TestEventToStore event) throws IOException, CradleStorageException
	{
		StoredTestEventId id = event.getId();
		logger.debug("Storing test event {}", id);
		PageInfo page = bpc.checkActivePage(id.getBookId(), id.getStartTimestamp());
		
		TestEventUtils.validateTestEvent(event);
		
		doStoreTestEvent(event, page);
		logger.debug("Test event {} has been stored", id);
		if (event.getParentId() != null)
		{
			logger.debug("Updating parents of test event {}", id);
			doUpdateParentTestEvents(event);
			logger.debug("Parents of test event {} have been updated", id);
		}
	}
	
	/**
	 * Asynchronously writes data about given test event to current page
	 * @param event data to write
	 * @throws IOException if data is invalid
	 * @return future to get know if storing was successful
	 * @throws CradleStorageException if given parameters are invalid
	 */
	public final CompletableFuture<Void> storeTestEventAsync(TestEventToStore event) throws IOException, CradleStorageException
	{
		StoredTestEventId id = event.getId();
		logger.debug("Storing test event {} asynchronously", id);
		PageInfo page = bpc.checkActivePage(id.getBookId(), id.getStartTimestamp());
		
		TestEventUtils.validateTestEvent(event);
		
		CompletableFuture<Void> result = doStoreTestEventAsync(event, page);
		result.whenComplete((r, error) -> {
				if (error != null)
					logger.error("Error while storing test event "+id+" asynchronously", error);
				else
					logger.debug("Test event {} has been stored asynchronously", id);
			});
		
		if (event.getParentId() == null)
			return result;
		
		return result.thenComposeAsync(r -> {
			logger.debug("Updating parents of test event {} asynchronously", id);
			CompletableFuture<Void> result2 = doUpdateParentTestEventsAsync(event);
			result2.whenComplete((r2, error) -> {
					if (error != null)
						logger.error("Error while updating parents of test event "+id+" asynchronously", error);
					else
						logger.debug("Parents of test event {} have been updated asynchronously", event.getId());
				});
			return result2;
		});
	}
	
	
	/**
	 * Retrieves message data stored under given ID in given page
	 * @param id of stored message to retrieve
	 * @param pageId to get message from
	 * @return data of stored message
	 * @throws IOException if message data retrieval failed
	 * @throws CradleStorageException if given parameters are invalid
	 */
	protected final StoredMessage getMessage(StoredMessageId id, PageId pageId) throws IOException, CradleStorageException
	{
		logger.debug("Getting message {} from page {}", id, pageId);
		bpc.checkPage(pageId, id.getBookId());
		StoredMessage result = doGetMessage(id, pageId);
		logger.debug("Message {} from page {} got", id, pageId);
		return result;
	}
	
	/**
	 * Retrieves message data stored under given ID in current page
	 * @param id of stored message to retrieve
	 * @return data of stored message
	 * @throws IOException if message data retrieval failed
	 * @throws CradleStorageException if given parameters are invalid
	 */
	public final StoredMessage getMessage(StoredMessageId id) throws IOException, CradleStorageException
	{
		return getMessage(id, bpc.findPage(id.getBookId(), id.getTimestamp()).getId());
	}
	
	/**
	 * Asynchronously retrieves message data stored under given ID in given page
	 * @param id of stored message to retrieve
	 * @param pageId to get message from
	 * @return future to obtain data of stored message
	 * @throws CradleStorageException if given parameters are invalid
	 */
	protected final CompletableFuture<StoredMessage> getMessageAsync(StoredMessageId id, PageId pageId) throws CradleStorageException
	{
		logger.debug("Getting message {} from page {} asynchronously", id, pageId);
		bpc.checkPage(pageId, id.getBookId());
		CompletableFuture<StoredMessage> result = doGetMessageAsync(id, pageId);
		result.whenComplete((r, error) -> {
				if (error != null)
					logger.error("Error while getting message "+id+" from page "+pageId+" asynchronously", error);
				else
					logger.debug("Message {} from page {} got asynchronously", id, pageId);
			});
		return result;
	}
	
	/**
	 * Asynchronously retrieves message data stored under given ID in current page
	 * @param id of stored message to retrieve
	 * @return future to obtain data of stored message
	 * @throws CradleStorageException if given parameters are invalid
	 */
	public final CompletableFuture<StoredMessage> getMessageAsync(StoredMessageId id) throws CradleStorageException
	{
		return getMessageAsync(id, bpc.findPage(id.getBookId(), id.getTimestamp()).getId());
	}
	
	
	/**
	 * Retrieves from given page the batch of messages where message with given ID is stored
	 * @param id of stored message whose batch to retrieve
	 * @param pageId to get batch from
	 * @return collection with messages stored in batch
	 * @throws IOException if batch data retrieval failed
	 * @throws CradleStorageException if given parameters are invalid
	 */
	protected final Collection<StoredMessage> getMessageBatch(StoredMessageId id, PageId pageId) throws IOException, CradleStorageException
	{
		logger.debug("Getting message batch by message ID {} from page {}", id, pageId);
		bpc.checkPage(pageId, id.getBookId());
		Collection<StoredMessage> result = doGetMessageBatch(id, pageId);
		logger.debug("Message batch by message ID {} from page {} got", id, pageId);
		return result;
	}
	
	/**
	 * Retrieves from current page the batch of messages where message with given ID is stored
	 * @param id of stored message whose batch to retrieve
	 * @return collection with messages stored in batch
	 * @throws IOException if batch data retrieval failed
	 * @throws CradleStorageException if given parameters are invalid
	 */
	public final Collection<StoredMessage> getMessageBatch(StoredMessageId id) throws IOException, CradleStorageException
	{
		return getMessageBatch(id, bpc.findPage(id.getBookId(), id.getTimestamp()).getId());
	}
	
	/**
	 * Asynchronously retrieves from given page the batch of messages where message with given ID is stored
	 * @param id of stored message whose batch to retrieve
	 * @param pageId to get batch from
	 * @return future to obtain collection with messages stored in batch
	 * @throws CradleStorageException if given parameters are invalid
	 */
	protected final CompletableFuture<Collection<StoredMessage>> getMessageBatchAsync(StoredMessageId id, PageId pageId) throws CradleStorageException
	{
		logger.debug("Getting message batch by message ID {} from page {} asynchronously", id, pageId);
		bpc.checkPage(pageId, id.getBookId());
		CompletableFuture<Collection<StoredMessage>> result = doGetMessageBatchAsync(id, pageId);
		result.whenComplete((r, error) -> {
				if (error != null)
					logger.error("Error while getting message batch by message ID "+id+" from page "+pageId+" asynchronously", error);
				else
					logger.debug("Message batch by message ID {} from page {} got asynchronously", id, pageId);
			});
		return result;
	}
	
	/**
	 * Asynchronously retrieves from current page the batch of messages where message with given ID is stored
	 * @param id of stored message whose batch to retrieve
	 * @return future to obtain collection with messages stored in batch
	 * @throws CradleStorageException if given parameters are invalid
	 */
	public final CompletableFuture<Collection<StoredMessage>> getMessageBatchAsync(StoredMessageId id) throws CradleStorageException
	{
		return getMessageBatchAsync(id, bpc.getActivePageId(id.getBookId()));
	}
	
	
	/**
	 * Allows to enumerate stored messages filtering them by given conditions
	 * @param filter defines conditions to filter messages by
	 * @return iterable object to enumerate messages
	 * @throws IOException if data retrieval failed
	 * @throws CradleStorageException if given parameters are invalid
	 */
	public final Iterable<StoredMessage> getMessages(StoredMessageFilter filter) throws IOException, CradleStorageException
	{
		logger.debug("Filtering messages by {}", filter);
		BookInfo book = bpc.getBook(filter.getBookId().getValue());
		Iterable<StoredMessage> result = doGetMessages(filter, book);
		logger.debug("Prepared iterator for messages filtered by {}", filter);
		return result;
	}
	
	/**
	 * Allows to asynchronously obtain iterable object to enumerate stored messages filtering them by given conditions
	 * @param filter defines conditions to filter messages by
	 * @return future to obtain iterable object to enumerate messages
	 * @throws CradleStorageException if given parameters are invalid
	 */
	public final CompletableFuture<Iterable<StoredMessage>> getMessagesAsync(StoredMessageFilter filter) throws CradleStorageException
	{
		logger.debug("Asynchronously getting messages filtered by {}", filter);
		BookInfo book = bpc.getBook(filter.getBookId().getValue());
		CompletableFuture<Iterable<StoredMessage>> result = doGetMessagesAsync(filter, book);
		result.whenComplete((r, error) -> {
				if (error != null)
					logger.error("Error while getting messages filtered by "+filter+" asynchronously", error);
				else
					logger.debug("Iterator for messages filtered by {} got asynchronously", filter);
			});
		return result;
	}
	
	
	/**
	 * Allows to enumerate stored message batches filtering them by given conditions
	 * @param filter defines conditions to filter message batches by
	 * @return iterable object to enumerate message batches
	 * @throws IOException if data retrieval failed
	 * @throws CradleStorageException if given parameters are invalid
	 */
	public final CradleResultSet<StoredMessageBatch> getMessagesBatches(StoredMessageFilter filter) throws IOException, CradleStorageException
	{
		logger.debug("Filtering message batches by {}", filter);
		BookInfo book = bpc.getBook(filter.getBookId().getValue());
		CradleResultSet<StoredMessageBatch> result = doGetMessagesBatches(filter, book);
		logger.debug("Prepared iterator for message batches filtered by {}", filter);
		return result;
	}
	
	/**
	 * Allows to asynchronously obtain iterable object to enumerate stored message batches filtering them by given conditions
	 * @param filter defines conditions to filter message batches by
	 * @return future to obtain iterable object to enumerate message batches
	 * @throws CradleStorageException if given parameters are invalid
	 */
	public final CompletableFuture<CradleResultSet<StoredMessageBatch>> getMessagesBatchesAsync(StoredMessageFilter filter) throws CradleStorageException
	{
		logger.debug("Asynchronously getting message batches filtered by {}", filter);
		BookInfo book = bpc.getBook(filter.getBookId().getValue());
		CompletableFuture<CradleResultSet<StoredMessageBatch>> result = doGetMessagesBatchesAsync(filter, book);
		result.whenComplete((r, error) -> {
				if (error != null)
					logger.error("Error while getting message batches filtered by "+filter+" asynchronously", error);
				else
					logger.debug("Iterator for message batches filtered by {} got asynchronously", filter);
			});
		return result;
	}
	
	
	/**
	 * Retrieves last stored sequence number for given session alias and direction within given page. 
	 * Use result of this method to continue writing messages.
	 * @param sessionAlias to get sequence number for 
	 * @param direction to get sequence number for
	 * @param bookId to get last sequence for
	 * @return last stored sequence number for given arguments, if it is present, -1 otherwise
	 * @throws IOException if retrieval failed
	 * @throws CradleStorageException if given parameters are invalid
	 */
	public final long getLastSequence(String sessionAlias, Direction direction, BookId bookId) throws IOException, CradleStorageException
	{
		logger.debug("Getting last stored sequence number for book '{}' and session alias '{}' and direction '{}'",
				bookId, sessionAlias, direction.getLabel());
		long result = doGetLastSequence(sessionAlias, direction, bookId);
		logger.debug("Sequence number {} got", result);
		return result;
	}
	
	
	/**
	 * Obtains collection of session aliases whose messages are saved in given page
	 * @param bookId to get scopes for
	 * @return collection of session aliases
	 * @throws IOException if data retrieval failed
	 * @throws CradleStorageException if given parameters are invalid
	 */
	public final Collection<String> getSessionAliases(BookId bookId) throws IOException, CradleStorageException
	{
		logger.debug("Getting session aliases for book '{}'", bookId);
		bpc.getBook(bookId);
		Collection<String> result = doGetSessionAliases(bookId);
		logger.debug("Session aliases for book '{}' got", bookId);
		return result;
	}
	
	
	/**
	 * Retrieves test event data stored under given ID
	 * @param id of stored test event to retrieve
	 * @return data of stored test event
	 * @throws IOException if test event data retrieval failed
	 * @throws CradleStorageException if given parameters are invalid
	 */
	public final StoredTestEvent getTestEvent(StoredTestEventId id) throws IOException, CradleStorageException
	{
		logger.debug("Getting test event {}", id);
		PageId pageId = bpc.findPage(id.getBookId(), id.getStartTimestamp()).getId();
		StoredTestEvent result = doGetTestEvent(id, pageId);
		logger.debug("Test event {} got from page {}", id, pageId);
		return result;
	}
	
	/**
	 * Asynchronously retrieves test event data stored under given ID
	 * @param id of stored test event to retrieve
	 * @return future to obtain data of stored test event
	 * @throws CradleStorageException if given parameters are invalid
	 */
	public final CompletableFuture<StoredTestEvent> getTestEventAsync(StoredTestEventId id) throws CradleStorageException
	{
		logger.debug("Getting test event {} asynchronously", id);
		PageId pageId = bpc.findPage(id.getBookId(), id.getStartTimestamp()).getId();
		CompletableFuture<StoredTestEvent> result = doGetTestEventAsync(id, pageId);
		result.whenComplete((r, error) -> {
				if (error != null)
					logger.error("Error while getting test event "+id+" from page "+pageId+" asynchronously", error);
				else
					logger.debug("Test event {} from page {} got asynchronously", id, pageId);
			});
		return result;
	}
	
	
	/**
	 * Allows to enumerate test events, filtering them by given conditions
	 * @param filter defines conditions to filter test events by
	 * @return result set to enumerate test events
	 * @throws CradleStorageException if filter is invalid
	 * @throws IOException if data retrieval failed
	 */
	public final CradleResultSet<StoredTestEvent> getTestEvents(TestEventFilter filter) throws CradleStorageException, IOException
	{
		logger.debug("Filtering test events by {}", filter);
		if (!checkFilter(filter))
			return new EmptyResultSet<>();
		
		BookInfo book = bpc.getBook(filter.getBookId());
		CradleResultSet<StoredTestEvent> result = doGetTestEvents(filter, book);
		logger.debug("Got result set with test events filtered by {}", filter);
		return result;
	}
	
	/**
	 * Allows to asynchronously obtain result set to enumerate test events, filtering them by given conditions
	 * @param filter defines conditions to filter test events by
	 * @return future to obtain result set to enumerate test events
	 * @throws CradleStorageException if filter is invalid
	 * @throws IOException if data retrieval failed
	 */
	public final CompletableFuture<CradleResultSet<StoredTestEvent>> getTestEventsAsync(TestEventFilter filter) throws CradleStorageException, IOException
	{
		logger.debug("Asynchronously getting test events filtered by {}", filter);
		if (!checkFilter(filter))
			return CompletableFuture.completedFuture(new EmptyResultSet<>());
		
		BookInfo book = bpc.getBook(filter.getBookId());
		CompletableFuture<CradleResultSet<StoredTestEvent>> result = doGetTestEventsAsync(filter, book);
		result.whenComplete((r, error) -> {
				if (error != null)
					logger.error("Error while getting test events filtered by "+filter+" asynchronously", error);
				else
					logger.debug("Result set with test events filtered by {} got asynchronously", filter);
			});
		return result;
	}
	
	/**
	 * Obtains collection of scope names whose test events are saved in Cradle
	 * @param bookId to get scopes from
	 * @return collection of scope names
	 * @throws IOException if data retrieval failed
	 * @throws CradleStorageException if given book ID is invalid
	 */
	public final Collection<String> getScopes(BookId bookId) throws IOException, CradleStorageException
	{
		logger.debug("Getting scopes for book '{}'", bookId);
		bpc.getBook(bookId);
		Collection<String> result = doGetScopes(bookId);
		logger.debug("Scopes for book '{}' got", bookId);
		return result;
	}
	
	
	public final void updateEventStatus(StoredTestEvent event, boolean success) throws IOException
	{
		logger.debug("Updating status of event {}", event.getId());
		doUpdateEventStatus(event, success);
		logger.debug("Status of event {} has been updated", event.getId());
	}

	public final CompletableFuture<Void> updateEventStatusAsync(StoredTestEvent event, boolean success)
	{
		logger.debug("Asynchronously updating status of event {}", event.getId());
		CompletableFuture<Void> result = doUpdateEventStatusAsync(event, success);
		result.whenComplete((r, error) -> {
				if (error != null)
					logger.error("Error while asynchronously updating status of event "+event.getId());
				else
					logger.debug("Status of event {} updated asynchronously", event.getId());
			});
		return result;
	}
	
	
	private BookInfo switchToNewPage(BookId bookId, String pageName, Instant pageStart, String pageComment) throws CradleStorageException, IOException
	{
		logger.info("Switching to page '{}' of book '{}'", pageName, bookId);
		BookInfo book = refreshPages(bookId);
		if (book.getPage(new PageId(bookId, pageName)) != null)
			throw new CradleStorageException("Page '"+pageName+"' is already present in book '"+bookId+"'");
		PageInfo activePage = book.getActivePage();
		if (activePage != null && !pageStart.isAfter(activePage.getStarted()))
			throw new CradleStorageException("Timestamp of new page start must be after active page start ("+activePage.getStarted()+")");
		
		try
		{
			doSwitchToNewPage(bookId, pageName, pageStart, pageComment, PageInfo.ended(activePage, pageStart));
		}
		catch (IOException e)
		{
			//Need to refresh book's pages to make user able see what was the reason of failure, e.g. new page was actually present
			refreshPages(bookId);
			throw e;
		}
		book.nextPage(pageName, pageStart, pageComment);
		return book;
	}
	
	private boolean checkFilter(TestEventFilter filter) throws CradleStorageException
	{
		if (filter.getBookId() == null)
			throw new CradleStorageException("bookId is mandatory");
		
		BookInfo book = bpc.getBook(filter.getBookId());
		
		if (filter.getPageId() != null)
			bpc.checkPage(filter.getPageId(), book.getId());
		
		if (filter.getScope() == null)
			throw new CradleStorageException("scope is mandatory");
		
		if (filter.getParentId() != null && !book.getId().equals(filter.getParentId().getBookId()))
			throw new CradleStorageException("Requested book ("+book.getId()+") doesn't match book of requested parent ("+filter.getParentId()+")");
		
		Instant timeFrom = filter.getStartTimestampFrom() != null ? filter.getStartTimestampFrom().getValue() : null,
				timeTo = filter.getStartTimestampTo() != null ? filter.getStartTimestampTo().getValue() : null;
		if (timeFrom != null && timeTo != null 
				&& timeFrom.isAfter(timeTo))
			throw new CradleStorageException("Left bound for start timestamp ("+timeFrom+") "
					+ "is after the right bound ("+timeTo+")");
		
		if (timeTo != null && timeTo.isBefore(book.getCreated()))
			return false;
		
		if (filter.getPageId() != null)
		{
			PageInfo page = book.getPage(filter.getPageId());
			Instant pageStarted = page.getStarted(),
					pageEnded = page.getEnded();
			
			if (timeFrom != null && pageEnded != null && timeFrom.isAfter(pageEnded))
				return false;
			if (timeTo != null && timeTo.isBefore(pageStarted))
				return false;
		}
		
		return true;
	}
}