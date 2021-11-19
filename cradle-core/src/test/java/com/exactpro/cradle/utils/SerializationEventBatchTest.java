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

package com.exactpro.cradle.utils;

import com.exactpro.cradle.serialization.EventBatchDeserializer;
import com.exactpro.cradle.serialization.EventBatchSerializer;
import com.exactpro.cradle.serialization.SerializationException;
import com.exactpro.cradle.testevents.BatchedStoredTestEvent;
import com.exactpro.cradle.testevents.BatchedStoredTestEventBuilder;
import com.exactpro.cradle.testevents.BatchedStoredTestEventMetadata;
import com.exactpro.cradle.testevents.BatchedStoredTestEventMetadataBuilder;
import com.exactpro.cradle.testevents.StoredTestEventId;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

public class SerializationEventBatchTest {

	@Test
	public void checkSize1() throws SerializationException {
		BatchedStoredTestEvent build = createBatchedStoredTestEvent("Test even1234567890");
		EventBatchSerializer serializer = new EventBatchSerializer();
		ByteBuffer buffer = ByteBuffer.allocate(10_000);
		serializer.serializeEventRecord(build, buffer);
		Assert.assertEquals(buffer.position(), serializer.calculateEventRecordSize(build));
	}

	@Test
	public void checkSize2() throws SerializationException {
		BatchedStoredTestEventMetadata build = createBatchedStoredTestEventMetadata("Test even1234567890");
		EventBatchSerializer serializer = new EventBatchSerializer();
		ByteBuffer buffer = ByteBuffer.allocate(10_000);
		serializer.serializeEventMetadataRecord(build, buffer);
		Assert.assertEquals(buffer.position(), serializer.calculateEventMetadataSize(build));
	}

	@Test
	public void checkSize3() throws SerializationException {
		Collection<BatchedStoredTestEvent> build = createBatchEvents();
		EventBatchSerializer serializer = new EventBatchSerializer();
		ByteBuffer buffer = ByteBuffer.allocate(10_000);
		serializer.serializeEventBatch(build, buffer);
		Assert.assertEquals(buffer.position(), serializer.calculateBatchEventSize(build).total);
	}

	@Test
	public void checkSize4() throws SerializationException {
		Collection<BatchedStoredTestEventMetadata> build = createBatchMetadata();
		EventBatchSerializer serializer = new EventBatchSerializer();
		ByteBuffer buffer = ByteBuffer.allocate(10_000);
		serializer.serializeEventMetadataBatch(build, buffer);
		Assert.assertEquals(buffer.position(), serializer.calculateBatchEventMetadataSize(build).total);
	}
	

	@Test
	public void serializeDeserialize() throws SerializationException {
		BatchedStoredTestEvent build = createBatchedStoredTestEvent("Test even1234567890");
		EventBatchSerializer serializer = new EventBatchSerializer();
		byte[] serialize = serializer.serializeEventRecord(build);
		EventBatchDeserializer deserializer = new EventBatchDeserializer();
		BatchedStoredTestEvent deserialize = deserializer.deserializeBatchEntry(serialize);
		compare(deserialize, build);
	}

	
	@Test
	public void serializeDeserialize2() throws SerializationException {
		BatchedStoredTestEventMetadata build = createBatchedStoredTestEventMetadata("Test even1234567890");
		EventBatchSerializer serializer = new EventBatchSerializer();
		byte[] serialize = serializer.serializeEventMetadataRecord(build);
		EventBatchDeserializer deserializer = new EventBatchDeserializer();
		BatchedStoredTestEventMetadata deserialize = deserializer.deserializeBatchEntryMetadata(serialize);
		compare(deserialize, build);
	}

	@Test
	public void serializeDeserialize3() throws SerializationException {
		List<BatchedStoredTestEvent> build = createBatchEvents();
		EventBatchSerializer serializer = new EventBatchSerializer();
		byte[] serialize = serializer.serializeEventBatch(build);
		EventBatchDeserializer deserializer = new EventBatchDeserializer();
		List<BatchedStoredTestEvent> deserialize = deserializer.deserializeBatchEntries(serialize);
		for (int i = 0, to = Math.max(build.size(), deserialize.size()); i < to; ++i) {
			compare(build.get(0), deserialize.get(0));
		}
	}

	@Test
	public void serializeDeserialize4() throws SerializationException {
		List<BatchedStoredTestEventMetadata> build = createBatchMetadata();
		EventBatchSerializer serializer = new EventBatchSerializer();
		byte[] serialize = serializer.serializeEventMetadataBatch(build);
		EventBatchDeserializer deserializer = new EventBatchDeserializer();
		List<BatchedStoredTestEventMetadata> deserialize = deserializer.deserializeBatchEntriesMetadata(serialize);
		for (int i = 0, to = Math.max(build.size(), deserialize.size()); i < to; ++i) {
			compare(build.get(0), deserialize.get(0));
		}
	}	

	private BatchedStoredTestEventMetadata createBatchedStoredTestEventMetadata(String name) {
		BatchedStoredTestEventMetadataBuilder builder = new BatchedStoredTestEventMetadataBuilder();
		builder.setSuccess(true);
		builder.setStartTimestamp(Instant.parse("2007-12-03T10:15:30.00Z"));
		builder.setEndTimestamp(Instant.parse("2007-12-03T10:15:31.00Z"));
		builder.setId(new StoredTestEventId(UUID.randomUUID().toString()));
		builder.setParentId(new StoredTestEventId(UUID.randomUUID().toString()));
		builder.setName("Test even1234567890");
		builder.setType("Test even1234567890 ----");
		return builder.build();
	}

	private BatchedStoredTestEvent createBatchedStoredTestEvent(String name) {
		BatchedStoredTestEventBuilder builder = new BatchedStoredTestEventBuilder();
		builder.setSuccess(true);
		builder.setStartTimestamp(Instant.parse("2007-12-03T10:15:30.00Z"));
		builder.setEndTimestamp(Instant.parse("2007-12-03T10:15:31.00Z"));
		builder.setId(new StoredTestEventId(UUID.randomUUID().toString()));
		builder.setParentId(new StoredTestEventId(UUID.randomUUID().toString()));
		builder.setName(name);
		builder.setType(name + " ----");
		builder.setContent("Message".repeat(10).getBytes(StandardCharsets.UTF_8));
		return builder.build();
	}

	private List<BatchedStoredTestEvent> createBatchEvents() {
		ArrayList<BatchedStoredTestEvent> objects = new ArrayList<>(3);
		objects.add(createBatchedStoredTestEvent("batch1"));
		objects.add(createBatchedStoredTestEvent("batch2"));
		objects.add(createBatchedStoredTestEvent("batch3"));
		return objects;
	}

	private List<BatchedStoredTestEventMetadata> createBatchMetadata() {
		ArrayList<BatchedStoredTestEventMetadata> objects = new ArrayList<>(3);
		objects.add(createBatchedStoredTestEventMetadata("batch1"));
		objects.add(createBatchedStoredTestEventMetadata("batch2"));
		objects.add(createBatchedStoredTestEventMetadata("batch3"));
		return objects;
	}
	
	private static void compare(BatchedStoredTestEvent o1, BatchedStoredTestEvent o2) {
		Assert.assertEquals(o1.getContent(), o2.getContent());
		Assert.assertEquals(o1.getParentId(), o2.getParentId());
		Assert.assertEquals(o1.getId(), o2.getId());
		Assert.assertEquals(o1.getMessageIds(), o2.getMessageIds());
		Assert.assertEquals(o1.getStartTimestamp(), o2.getStartTimestamp());
		Assert.assertEquals(o1.getEndTimestamp(), o2.getEndTimestamp());
		Assert.assertEquals(o1.getName(), o2.getName());
		Assert.assertEquals(o1.getType(), o2.getType());
	}

	private static void compare(BatchedStoredTestEventMetadata o1, BatchedStoredTestEventMetadata o2) {
		Assert.assertEquals(o1.getParentId(), o2.getParentId());
		Assert.assertEquals(o1.getId(), o2.getId());
		Assert.assertEquals(o1.getStartTimestamp(), o2.getStartTimestamp());
		Assert.assertEquals(o1.getEndTimestamp(), o2.getEndTimestamp());
		Assert.assertEquals(o1.getName(), o2.getName());
		Assert.assertEquals(o1.getType(), o2.getType());
	}
}
