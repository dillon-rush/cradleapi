/******************************************************************************
 * Copyright (c) 2009-2020, Exactpro Systems LLC
 * www.exactpro.com
 * Build Software to Test Software
 *
 * All rights reserved.
 * This is unpublished, licensed software, confidential and proprietary 
 * information which is the property of Exactpro Systems LLC or its licensors.
 ******************************************************************************/

package com.exactpro.cradle;

import java.io.IOException;
import java.util.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.exactpro.cradle.messages.StoredMessage;
import com.exactpro.cradle.messages.StoredMessageBatch;
import com.exactpro.cradle.messages.StoredMessageFilter;
import com.exactpro.cradle.messages.StoredMessageId;
import com.exactpro.cradle.testevents.StoredTestEvent;
import com.exactpro.cradle.testevents.StoredTestEventBatch;
import com.exactpro.cradle.testevents.StoredTestEventId;
import com.exactpro.cradle.testevents.TestEventsMessagesLinker;
import com.exactpro.cradle.testevents.TestEventsParentsLinker;
import com.exactpro.cradle.utils.CradleStorageException;

/**
 * Storage which holds information about all data sent or verified and generated reports.
 */
public abstract class CradleStorage
{
	private static final Logger logger = LoggerFactory.getLogger(CradleStorage.class);
	
	private String instanceId;
	
	private volatile boolean workingState = false;
	
	/**
	 * Initializes internal objects of storage, i.e. creates needed connections, tables and obtains ID of application instance with given name.
	 * If no ID of instance with that name is stored, makes new record in storage, returning ID of that instance
	 * @param instanceName name of current application instance. Will be used to mark written data
	 * @return ID of application instance as recorded in storage
	 * @throws CradleStorageException if storage initialization failed
	 */
	protected abstract String doInit(String instanceName) throws CradleStorageException;
	protected abstract void doDispose() throws CradleStorageException;
	
	
	protected abstract void doStoreMessageBatch(StoredMessageBatch batch) throws IOException;
	protected abstract void doStoreTestEventBatch(StoredTestEventBatch batch) throws IOException;
	protected abstract void doStoreTestEventMessagesLink(StoredTestEventId eventId, Set<StoredMessageId> messagesIds) throws IOException;
	protected abstract StoredMessage doGetMessage(StoredMessageId id) throws IOException;
	protected abstract long doGetLastMessageIndex(String streamName, Direction direction) throws IOException;
	protected abstract StoredTestEvent doGetTestEvent(StoredTestEventId id) throws IOException;
	
	
	/**
	 * TestEventsMessagesLinker is used to obtain links between test events and messages
	 * @return instance of TestEventsMessagesLinker
	 */
	public abstract TestEventsMessagesLinker getTestEventsMessagesLinker();
	
	/**
	 * TestEventsParentsLinker is used to obtain test events by their parent
	 * @return instance of TestEventsParentsLinker
	 */
	public abstract TestEventsParentsLinker getTestEventsParentsLinker();
	
	
	protected abstract Iterable<StoredMessage> doGetMessages(StoredMessageFilter filter) throws IOException;
	protected abstract Iterable<StoredTestEvent> doGetTestEvents(boolean onlyRootEvents) throws IOException;
	
	
	/**
	 * Initializes storage, i.e. creates needed streams and gets ready to write data marked with given instance name
	 * @param instanceName name of current application instance. Will be used to mark written data
	 * @throws CradleStorageException if storage initialization failed
	 */
	public final void init(String instanceName) throws CradleStorageException
	{
		if (workingState)
			throw new CradleStorageException("Already initialized");
		
		logger.info("Storage initialization started");
		instanceId = doInit(instanceName);
	}
	
	/**
	 * Switches storage from its initial state to working state. This affects storage operations.
	 */
	public final void initFinish()
	{
		workingState = true;
		logger.info("Storage initialization finished");
	}
	
	/**
	 * @return ID of current application instance as recorded in storage
	 */
	public String getInstanceId()
	{
		return instanceId;
	}
	
	
	/**
	 * Disposes resources occupied by storage which means closing of opened connections, flushing all buffers, etc.
	 * @throws CradleStorageException if there was error during storage disposal, which may mean issue with data flushing, unexpected connection break, etc.
	 */
	public final void dispose() throws CradleStorageException
	{
		doDispose();
		logger.info("Storage disposed");
	}
	
	
	/**
	 * Writes data about given message batch to storage. Messages from batch are linked with corresponding streams
	 * @param batch data to write
	 * @throws IOException if data writing failed
	 */
	public final void storeMessageBatch(StoredMessageBatch batch) throws IOException
	{
		logger.debug("Storing message batch {}", batch.getId());
		doStoreMessageBatch(batch);
		logger.debug("Message batch {} has been stored", batch.getId());
	}
	
	/**
	 * Writes data about given test event batch to storage.
	 * @param batch data to write.
	 * @throws IOException if data writing failed
	 */
	public final void storeTestEventBatch(StoredTestEventBatch batch) throws IOException
	{
		logger.debug("Storing test event batch {}", batch.getId());
		doStoreTestEventBatch(batch);
		logger.debug("Test event batch {} has been stored", batch.getId());
	}
	
	/**
	 * Writes to storage the links between given test event and messages.
	 * @param eventId ID of stored test event
	 * @param messagesIds list of stored message IDs
	 * @throws IOException if data writing failed
	 */
	public final void storeTestEventMessagesLink(StoredTestEventId eventId, Set<StoredMessageId> messagesIds) throws IOException
	{
		logger.debug("Storing link between test event {} and {} message(s)", eventId, messagesIds.size());
		doStoreTestEventMessagesLink(eventId, messagesIds);
		logger.debug("Link between test event {} and {} message(s) has been stored", eventId, messagesIds.size());
	}
	
	
	/**
	 * Retrieves message data stored under given ID
	 * @param id of stored message to retrieve
	 * @return data of stored messages
	 * @throws IOException if message data retrieval failed
	 */
	public final StoredMessage getMessage(StoredMessageId id) throws IOException
	{
		logger.debug("Getting message {}", id);
		StoredMessage result = doGetMessage(id);
		logger.debug("Message {} got", id);
		return result;
	}
	
	/**
	 * Retrieves last stored message index for given stream and direction. Use result of this method to continue sequence of message indices.
	 * Indices are scoped by stream and direction, so both arguments are required 
	 * @param streamName to get message index for 
	 * @param direction to get message index for
	 * @return last stored message index for given arguments
	 * @throws IOException if index retrieval failed
	 */
	public final long getLastMessageIndex(String streamName, Direction direction) throws IOException
	{
		logger.debug("Getting last stored message index for stream '{}' and direction '{}'", streamName, direction.getLabel());
		long result = doGetLastMessageIndex(streamName, direction);
		logger.debug("Message index {} got", result);
		return result;
	}
	
	
	/**
	 * Retrieves test event data stored under given ID
	 * @param id of stored test event to retrieve
	 * @return data of stored test event
	 * @throws IOException if test event data retrieval failed
	 */
	public final StoredTestEvent getTestEvent(StoredTestEventId id) throws IOException
	{
		logger.debug("Getting test event {}", id);
		StoredTestEvent result = doGetTestEvent(id);
		logger.debug("Test event {} got", id);
		return result;
	}
	
	
	/**
	 * Allows to enumerate stored messages, optionally filtering them by given conditions
	 * @param filter defines conditions to filter messages by. Use null is no filtering is needed
	 * @return iterable object to enumerate messages
	 * @throws IOException if data retrieval failed
	 */
	public final Iterable<StoredMessage> getMessages(StoredMessageFilter filter) throws IOException
	{
		logger.debug("Filtering messages by {}", filter);
		Iterable<StoredMessage> result = doGetMessages(filter);
		logger.debug("Prepared iterator for messages filtered by {}", filter);
		return result;
	}
	
	/**
	 * Allows to enumerate test events
	 * @param onlyRootEvents set to true if you need to obtain only root test events, i.e. events with no parent 
	 * @return iterable object to enumerate test events
	 * @throws IOException if data retrieval failed
	 */
	public final Iterable<StoredTestEvent> getTestEvents(boolean onlyRootEvents) throws IOException
	{
		String events = onlyRootEvents ? "root" : "all";
		logger.debug("Getting {} test events ", events);
		Iterable<StoredTestEvent> result = doGetTestEvents(onlyRootEvents);
		logger.debug("Prepared iterator for {} test events", events);
		return result;
	}
}