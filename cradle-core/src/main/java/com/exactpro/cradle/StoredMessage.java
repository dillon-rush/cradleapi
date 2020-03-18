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

import java.io.Serializable;
import java.time.Instant;
import java.util.Arrays;

import com.exactpro.cradle.utils.CradleUtils;

public class StoredMessage implements Serializable
{
	private static final long serialVersionUID = 200983136307497672L;
	
	private StoredMessageId id;
	private byte[] content;
	private Direction direction;
	private String streamName;
	private Instant timestamp;
	
	public StoredMessage()
	{
	}
	
	public StoredMessage(StoredMessage copyFrom)
	{
		this.id = copyFrom.getId();
		this.content = copyFrom.getContent();
		this.direction = copyFrom.getDirection();
		this.streamName = copyFrom.getStreamName();
		this.timestamp = copyFrom.getTimestamp();
	}
	
	
	public StoredMessageId getId()
	{
		return id;
	}
	
	public void setId(StoredMessageId id)
	{
		this.id = id;
	}
	
	
	public byte[] getContent()
	{
		return content;
	}
	
	public void setContent(byte[] message)
	{
		this.content = message;
	}
	
	
	public Direction getDirection()
	{
		return direction;
	}
	
	public void setDirection(Direction direction)
	{
		this.direction = direction;
	}
	
	
	public String getStreamName()
	{
		return streamName;
	}
	
	public void setStreamName(String streamName)
	{
		this.streamName = streamName;
	}
	
	
	public Instant getTimestamp()
	{
		return timestamp;
	}
	
	public void setTimestamp(Instant timestamp)
	{
		this.timestamp = timestamp;
	}

	@Override
	public String toString()
	{
		return new StringBuilder()
				.append("StoredMessage{").append(CradleUtils.EOL)
				.append("id=").append(id).append(",").append(CradleUtils.EOL)
				.append("content=").append(Arrays.toString(content)).append(",").append(CradleUtils.EOL)
				.append("streamName='").append(streamName).append("',").append(CradleUtils.EOL)
				.append("direction='").append(direction.toString().toLowerCase()).append("',").append(CradleUtils.EOL)
				.append("timestamp='").append(timestamp).append("',").append(CradleUtils.EOL)
				.append("}").toString();
	}
}
