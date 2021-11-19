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

package com.exactpro.cradle.utils;

import java.io.*;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.zip.DataFormatException;

import com.exactpro.cradle.Direction;
import com.exactpro.cradle.messages.StoredMessageId;
import com.exactpro.cradle.serialization.EventMessageIdDeserializer;
import com.exactpro.cradle.serialization.EventMessageIdSerializer;
import com.exactpro.cradle.testevents.*;
import org.apache.commons.lang3.SerializationUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

public class TestEventUtils
{
	
	/**
	 * Checks that test event has all necessary fields set
	 * @param event to validate
	 * @param checkName indicates whether event name should be validated. For some events name is optional and thus shouldn't be checked
	 * @throws CradleStorageException if validation failed
	 */
	public static void validateTestEvent(StoredTestEvent event, boolean checkName) throws CradleStorageException
	{
		if (event.getId() == null)
			throw new CradleStorageException("Test event ID cannot be null");
		if (event.getId().equals(event.getParentId()))
			throw new CradleStorageException("Test event cannot reference itself");
		if (checkName && StringUtils.isEmpty(event.getName()))
			throw new CradleStorageException("Test event must have a name");
		if (event.getStartTimestamp() == null)
			throw new CradleStorageException("Test event must have a start timestamp");
	}
	
	/**
	 * Serializes test events, skipping non-meaningful or calculatable fields
	 * @param testEvents to serialize
	 * @return array of bytes, containing serialized events
	 * @throws IOException if serialization failed
	 */
	public static byte[] serializeTestEvents(Collection<BatchedStoredTestEvent> testEvents) throws IOException
	{
		byte[] batchContent;
		try (ByteArrayOutputStream out = new ByteArrayOutputStream();
				DataOutputStream dos = new DataOutputStream(out))
		{
			for (BatchedStoredTestEvent te : testEvents)
				serialize(te, dos);
			dos.flush();
			batchContent = out.toByteArray();
		}
		return batchContent;
	}

	/**
	 * Serializes test events metadata, skipping non-meaningful or calculatable fields
	 * @param testEventsMetadata to serialize
	 * @return array of bytes, containing serialized and compressed metadata of events
	 * @throws IOException if serialization failed
	 */
	public static byte[] serializeTestEventsMetadata(Collection<BatchedStoredTestEventMetadata> testEventsMetadata) throws IOException
	{
		byte[] batchContent;
		try (ByteArrayOutputStream out = new ByteArrayOutputStream();
				DataOutputStream dos = new DataOutputStream(out))
		{
			for (BatchedStoredTestEventMetadata te : testEventsMetadata)
				serialize(te, dos);
			dos.flush();
			batchContent = CompressionUtils.compressData(out.toByteArray());
		}
		return batchContent;
	}
	
	/**
	 * Deserializes all test events, adding them to given batch
	 * @param contentBytes to deserialize events from
	 * @param batch to add events to
	 * @param ids Map of Collection of messages' id's related with added events
	 * @throws IOException if deserialization failed
	 * @throws CradleStorageException if deserialized event doesn't match batch conditions
	 */
	public static void deserializeTestEvents(byte[] contentBytes, StoredTestEventBatch batch,
			Map<StoredTestEventId, Collection<StoredMessageId>> ids)
			throws IOException, CradleStorageException
	{
		try (DataInputStream dis = new DataInputStream(new ByteArrayInputStream(contentBytes)))
		{
			while (dis.available() != 0)
			{
				byte[] teBytes = readNextData(dis);
				BatchedStoredTestEvent tempTe = deserializeTestEvent(teBytes);
				
				if (ids == null)
					StoredTestEventBatch.addTestEvent(tempTe, batch);
				else
					StoredTestEventBatch.addTestEvent(tempTe, batch, ids.get(tempTe.getId()));
			}
		}
	}

	/**
	 * Deserializes all test events metadata, adding them to given batch for metadata
	 * @param contentBytes to deserialize events metadata from
	 * @param batch to add events to
	 * @throws IOException if deserialization failed
	 */
	public static void deserializeTestEventsMetadata(byte[] contentBytes, StoredTestEventBatchMetadata batch) 
			throws IOException
	{
		try
		{
			contentBytes = CompressionUtils.decompressData(contentBytes);
		}
		catch (IOException e)
		{
			throw new IOException("Could not decompress metadata of test events from batch with ID '"+batch.getId()+"'", e);
		}
		catch (DataFormatException e)
		{
			//Data seems to be not compressed, i.e written by Cradle API prior to 2.9.0, let's try to deserialize events from bytes as they are
		}
		
		try (DataInputStream dis = new DataInputStream(new ByteArrayInputStream(contentBytes)))
		{
			while (dis.available() != 0)
			{
				byte[] teBytes = readNextData(dis);
				BatchedStoredTestEventMetadata tempTe = deserializeTestEventMetadata(teBytes);
				StoredTestEventBatchMetadata.addTestEventMetadata(tempTe, batch);
			}
		}
	}
	
	
	/**
	 * Decompresses given ByteBuffer and deserializes all test events, adding them to given batch
	 * @param content to deserialize events from
	 * @param compressed flag that indicates if content needs to be decompressed first
	 * @param batch to add events to
	 * @param ids Map of Collections of message IDs related to added events
	 * @throws IOException if deserialization failed
	 * @throws CradleStorageException if deserialized event doesn't match batch conditions
	 */
	public static void 	bytesToTestEvents(ByteBuffer content, boolean compressed, StoredTestEventBatch batch,
			Map<StoredTestEventId, Collection<StoredMessageId>> ids)
			throws IOException, CradleStorageException
	{
		byte[] contentBytes = getTestEventContentBytes(content, compressed, batch.getId());
		deserializeTestEvents(contentBytes, batch, ids);
	}
	
	public static byte[] getTestEventContentBytes(ByteBuffer content, boolean compressed, StoredTestEventId eventId) throws IOException
	{
		byte[] contentBytes = content.array();
		if (!compressed)
			return contentBytes;
		
		try
		{
			return CompressionUtils.decompressData(contentBytes);
		}
		catch (IOException | DataFormatException e)
		{
			throw new IOException(String.format("Could not decompress content of test event (ID: '%s') from Cradle", eventId), e);
		}
	}
	
	
	private static void serialize(Serializable data, DataOutputStream target) throws IOException
	{
		byte[] serializedData = SerializationUtils.serialize(data);
		target.writeInt(serializedData.length);
		target.write(serializedData);
	}
	
	private static byte[] readNextData(DataInputStream source) throws IOException
	{
		int size = source.readInt();
		byte[] result = new byte[size];
		source.read(result);
		return result;
	}
	
	private static BatchedStoredTestEvent deserializeTestEvent(byte[] bytes)
	{
		return (BatchedStoredTestEvent)SerializationUtils.deserialize(bytes);
	}
	
	private static BatchedStoredTestEventMetadata deserializeTestEventMetadata(byte[] bytes)
	{
		return (BatchedStoredTestEventMetadata)SerializationUtils.deserialize(bytes);
	}

	public static Collection<StoredMessageId> deserializeLinkedMessageIds(byte[] array) throws IOException {
		return EventMessageIdDeserializer.deserializeLinkedMessageIds(array);
	}

	public static byte[] serializeLinkedMessageIds(Collection<StoredMessageId> messageIds) throws IOException {
		return EventMessageIdSerializer.serializeLinkedMessageIds(messageIds);
	}

	public static Map<StoredTestEventId, Collection<StoredMessageId>> deserializeBatchLinkedMessageIds(byte[] array) throws IOException {
		return EventMessageIdDeserializer.deserializeBatchLinkedMessageIds(array);
	}

	public static byte[] serializeBatchLinkedMessageIds(Map<StoredTestEventId, Collection<StoredMessageId>> messageIdsMap) throws IOException {
		return EventMessageIdSerializer.serializeBatchLinkedMessageIds(messageIdsMap);
	}
}
