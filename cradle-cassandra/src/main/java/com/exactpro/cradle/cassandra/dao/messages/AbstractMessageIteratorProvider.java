/*
 * Copyright 2021-2021 Exactpro (Exactpro Systems Limited)
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

package com.exactpro.cradle.cassandra.dao.messages;

import com.datastax.oss.driver.api.core.cql.BoundStatementBuilder;
import com.exactpro.cradle.BookInfo;
import com.exactpro.cradle.PageId;
import com.exactpro.cradle.PageInfo;
import com.exactpro.cradle.cassandra.dao.BookOperators;
import com.exactpro.cradle.cassandra.resultset.IteratorProvider;
import com.exactpro.cradle.cassandra.utils.FilterUtils;
import com.exactpro.cradle.filters.FilterForGreater;
import com.exactpro.cradle.filters.FilterForLess;
import com.exactpro.cradle.messages.MessageFilter;
import com.exactpro.cradle.utils.CradleStorageException;

import java.time.Instant;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

abstract public class AbstractMessageIteratorProvider<T> extends IteratorProvider<T>
{
	protected final MessageBatchOperator op;
	protected final BookInfo book;
	protected final ExecutorService composingService;
	protected final FilterForGreater<Instant> leftBoundFilter;
	protected final FilterForLess<Instant> rightBoundFilter;
	protected PageInfo firstPage, lastPage;
	protected final Function<BoundStatementBuilder, BoundStatementBuilder> readAttrs;
	protected final MessageFilter filter;
	protected final int limit;
	protected final AtomicInteger returned;
	protected CassandraStoredMessageFilter cassandraFilter;

	public AbstractMessageIteratorProvider(String requestInfo, MessageFilter filter, BookOperators ops, BookInfo book,
			ExecutorService composingService,
			Function<BoundStatementBuilder, BoundStatementBuilder> readAttrs) throws CradleStorageException
	{
		super(requestInfo);
		this.op = ops.getMessageBatchOperator();
		this.book = book;
		this.composingService = composingService;
		this.readAttrs = readAttrs;
		this.filter = filter;
		this.limit = filter.getLimit();
		this.returned = new AtomicInteger();
		this.leftBoundFilter = createLeftBoundFilter(filter);
		this.rightBoundFilter = createRightBoundFilter(filter);
		this.cassandraFilter = createInitialFilter(filter);
	}

	protected FilterForGreater<Instant> createLeftBoundFilter(MessageFilter filter) throws CradleStorageException
	{
		Instant leftBoundFromFilter = getLeftBoundFromFilter(filter, book);
		FilterForGreater<Instant> result = leftBoundFromFilter == null ? null : FilterForGreater.forGreaterOrEquals(leftBoundFromFilter);

		firstPage = FilterUtils.findFirstPage(filter.getPageId(), result, book);
		Instant leftBoundFromPage = firstPage.getStarted();
		if (result == null)
			return FilterForGreater.forGreaterOrEquals(leftBoundFromPage);

		result.setValue(leftBoundFromFilter.isAfter(leftBoundFromPage) ? leftBoundFromFilter : leftBoundFromPage);

		return result;
	}

	protected FilterForLess<Instant> createRightBoundFilter(MessageFilter filter)
	{
		Instant rightBoundFromFilter = getRightBoundFromFilter(filter, book);
		FilterForLess<Instant> result = FilterForLess.forLessOrEquals(rightBoundFromFilter);
		lastPage = FilterUtils.findLastPage(filter.getPageId(), result, book);
		Instant endOfPage = lastPage.getEnded() == null ? Instant.now() : lastPage.getEnded();

		return FilterForLess.forLessOrEquals(endOfPage.isBefore(rightBoundFromFilter) ? endOfPage : rightBoundFromFilter);
	}

	protected CassandraStoredMessageFilter createInitialFilter(MessageFilter filter)
	{
		return new CassandraStoredMessageFilter(firstPage.getId().getName(), filter.getSessionAlias(),
				filter.getDirection().getLabel(), leftBoundFilter, rightBoundFilter, filter.getSequence());
	}

	protected CassandraStoredMessageFilter createNextFilter(CassandraStoredMessageFilter prevFilter)
	{
		PageInfo prevPage = book.getPage(new PageId(book.getId(), prevFilter.getPage()));
		if (prevPage.equals(lastPage))
			return null;

		PageInfo nextPage = book.getNextPage(prevPage.getStarted());

		return new CassandraStoredMessageFilter(nextPage.getId().getName(), prevFilter.getSessionAlias(),
				prevFilter.getDirection(), leftBoundFilter, rightBoundFilter, prevFilter.getSequence());
	}

	public static Instant getLeftBoundFromFilter(MessageFilter filter, BookInfo book)
	{
		if (filter == null)
			return null;

		Instant result = null;
		FilterForGreater<Instant> filterFrom = filter.getTimestampFrom();
		if (filterFrom != null)
		{
			Instant value = filterFrom.getValue();
			result = (result == null || result.isBefore(value)) ? value : result;
		}

		PageId pageId = filter.getPageId();
		if (pageId != null)
		{
			PageInfo pageInfo = book.getPage(pageId);
			Instant value = pageInfo.getStarted();
			result = (result == null || result.isBefore(value)) ? value : result;
		}

		return result;
	}

	public static Instant getRightBoundFromFilter(MessageFilter filter, BookInfo book)
	{
		if (filter == null)
			return null;

		Instant result = null;
		FilterForLess<Instant> filterTo = filter.getTimestampTo();
		if (filterTo != null)
		{
			Instant value = filterTo.getValue();
			result = result == null || result.isAfter(value) ? value : result;
		}

		PageId pageId = filter.getPageId();
		if (pageId != null)
		{
			PageInfo pageInfo = book.getPage(pageId);
			Instant value = pageInfo.getEnded() == null ? Instant.now() : pageInfo.getEnded();
			result = result == null || result.isAfter(value) ? value : result;
		}

		return result == null ? Instant.now() : result;
	}

}
