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

package com.exactpro.cradle.cassandra.iterators;

import java.util.Iterator;

import com.datastax.oss.driver.api.core.MappedAsyncPagingIterable;
import com.exactpro.cradle.cassandra.dao.testevents.TestEventChildEntity;
import com.exactpro.cradle.cassandra.dao.testevents.converters.TestEventChildConverter;
import com.exactpro.cradle.cassandra.retries.RetrySupplies;
import com.exactpro.cradle.testevents.StoredTestEventMetadata;

public class TestEventChildrenMetadataIteratorAdapter implements Iterable<StoredTestEventMetadata>
{
	private final MappedAsyncPagingIterable<TestEventChildEntity> rows;
	private final RetrySupplies retrySupplies;
	private final TestEventChildConverter converter;
	
	public TestEventChildrenMetadataIteratorAdapter(MappedAsyncPagingIterable<TestEventChildEntity> rows,
			RetrySupplies retrySupplies, TestEventChildConverter converter)
	{
		this.rows = rows;
		this.retrySupplies = retrySupplies;
		this.converter = converter;
	}
	
	@Override
	public Iterator<StoredTestEventMetadata> iterator()
	{
		return new TestEventChildrenMetadataIterator(rows, retrySupplies, converter);
	}
}
