/******************************************************************************
 * Copyright (c) 2009-2020, Exactpro Systems LLC
 * www.exactpro.com
 * Build Software to Test Software
 *
 * All rights reserved.
 * This is unpublished, licensed software, confidential and proprietary 
 * information which is the property of Exactpro Systems LLC or its licensors.
 ******************************************************************************/

package com.exactpro.cradle.cassandra.dao.messages;

import static com.exactpro.cradle.cassandra.StorageConstants.COMPRESSED;
import static com.exactpro.cradle.cassandra.StorageConstants.DIRECTION;
import static com.exactpro.cradle.cassandra.StorageConstants.INSTANCE_ID;
import static com.exactpro.cradle.cassandra.StorageConstants.MESSAGE_INDEX;
import static com.exactpro.cradle.cassandra.StorageConstants.STREAM_NAME;

import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.datastax.oss.driver.api.mapper.annotations.ClusteringColumn;
import com.datastax.oss.driver.api.mapper.annotations.CqlName;
import com.datastax.oss.driver.api.mapper.annotations.Entity;
import com.datastax.oss.driver.api.mapper.annotations.PartitionKey;
import com.exactpro.cradle.Direction;
import com.exactpro.cradle.messages.StoredMessageBatch;
import com.exactpro.cradle.messages.StoredMessageBatchId;

/**
 * Contains meta-data fields of message batch to obtain from Cassandra
 */
@Entity
public class MessageBatchMetadataEntity
{
	private static final Logger logger = LoggerFactory.getLogger(MessageBatchMetadataEntity.class);
	
	@PartitionKey(0)
	@CqlName(INSTANCE_ID)
	private UUID instanceId;
	
	@PartitionKey(1)
	@CqlName(STREAM_NAME)
	private String streamName;
	
	@ClusteringColumn(0)
	@CqlName(DIRECTION)
	private String direction;
	
	@ClusteringColumn(1)
	@CqlName(MESSAGE_INDEX)
	private long messageIndex;
	
	@CqlName(COMPRESSED)
	private boolean compressed;
	
	
	public MessageBatchMetadataEntity()
	{
	}
	
	public MessageBatchMetadataEntity(StoredMessageBatch batch, UUID instanceId)
	{
		logger.debug("Creating meta-data from message batch");
		this.setInstanceId(instanceId);
		
		StoredMessageBatchId id = batch.getId();
		this.setStreamName(id.getStreamName());
		this.setDirection(id.getDirection().getLabel());
		this.setMessageIndex(id.getIndex());
	}
	
	
	public UUID getInstanceId()
	{
		return instanceId;
	}
	
	public void setInstanceId(UUID instanceId)
	{
		this.instanceId = instanceId;
	}
	
	
	public String getStreamName()
	{
		return streamName;
	}
	
	public void setStreamName(String streamName)
	{
		this.streamName = streamName;
	}
	
	
	public String getDirection()
	{
		return direction;
	}
	
	public void setDirection(String direction)
	{
		this.direction = direction;
	}
	
	
	public long getMessageIndex()
	{
		return messageIndex;
	}
	
	public void setMessageIndex(long messageIndex)
	{
		this.messageIndex = messageIndex;
	}
	
	
	public boolean isCompressed()
	{
		return compressed;
	}
	
	public void setCompressed(boolean compressed)
	{
		this.compressed = compressed;
	}
	
	
	public StoredMessageBatchId createBatchId()
	{
		return new StoredMessageBatchId(getStreamName(), Direction.byLabel(getDirection()), getMessageIndex());
	}
}