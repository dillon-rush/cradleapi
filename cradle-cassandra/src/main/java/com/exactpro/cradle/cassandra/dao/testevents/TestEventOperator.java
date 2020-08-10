/*
 * Copyright 2020-2020 Exactpro (Exactpro Systems Limited)
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

package com.exactpro.cradle.cassandra.dao.testevents;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

import com.datastax.oss.driver.api.core.PagingIterable;
import com.datastax.oss.driver.api.core.cql.AsyncResultSet;
import com.datastax.oss.driver.api.core.cql.BoundStatementBuilder;
import com.datastax.oss.driver.api.core.cql.ResultSet;
import com.datastax.oss.driver.api.mapper.annotations.Dao;
import com.datastax.oss.driver.api.mapper.annotations.Insert;
import com.datastax.oss.driver.api.mapper.annotations.Query;

import static com.exactpro.cradle.cassandra.StorageConstants.*;

@Dao
public interface TestEventOperator
{
	@Query("SELECT * FROM ${qualifiedTableId} WHERE "+INSTANCE_ID+"=:instanceId AND "+ROOT+" IN (true, false) and "+ID+"=:id")
	TestEventEntity get(UUID instanceId, String id, 
			Function<BoundStatementBuilder, BoundStatementBuilder> attributes);
	
	@Query("SELECT * FROM ${qualifiedTableId} WHERE "+INSTANCE_ID+"=:instanceId AND "+ROOT+"=true")
	PagingIterable<TestEventEntity> getRootEvents(UUID instanceId, 
			Function<BoundStatementBuilder, BoundStatementBuilder> attributes);
	
	@Query("SELECT * FROM ${qualifiedTableId} WHERE "+INSTANCE_ID+"=:instanceId AND "+ROOT+" IN (true, false) and "+PARENT_ID+"=:parentId ALLOW FILTERING")
	PagingIterable<TestEventEntity> getChildren(UUID instanceId, String parentId, 
			Function<BoundStatementBuilder, BoundStatementBuilder> attributes);
	
	@Insert
	DetailedTestEventEntity write(DetailedTestEventEntity testEvent, Function<BoundStatementBuilder, BoundStatementBuilder> attributes);
	
	@Query("UPDATE ${qualifiedTableId} SET "+SUCCESS+"=:success WHERE "+INSTANCE_ID+"=:instanceId AND "+ROOT+"=:isRoot and "+ID+"=:eventId")
	ResultSet updateStatus(UUID instanceId, String eventId, boolean isRoot, boolean success,
			Function<BoundStatementBuilder, BoundStatementBuilder> attributes);
	
	@Query("UPDATE ${qualifiedTableId} SET "+SUCCESS+"=:success WHERE "+INSTANCE_ID+"=:instanceId AND "+ROOT+"=:isRoot and "+ID+"=:eventId")
	CompletableFuture<AsyncResultSet> updateStatusAsync(UUID instanceId, String eventId, boolean isRoot, boolean success,
			Function<BoundStatementBuilder, BoundStatementBuilder> attributes);
}
