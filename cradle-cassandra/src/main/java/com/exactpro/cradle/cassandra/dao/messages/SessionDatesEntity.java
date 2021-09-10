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

import com.datastax.oss.driver.api.mapper.annotations.ClusteringColumn;
import com.datastax.oss.driver.api.mapper.annotations.CqlName;
import com.datastax.oss.driver.api.mapper.annotations.Entity;
import com.datastax.oss.driver.api.mapper.annotations.PartitionKey;
import com.exactpro.cradle.PageId;
import com.exactpro.cradle.cassandra.utils.CassandraTimeUtils;
import com.exactpro.cradle.messages.MessageBatch;
import com.exactpro.cradle.messages.StoredMessageId;
import com.exactpro.cradle.utils.TimeUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static com.exactpro.cradle.cassandra.StorageConstants.*;

@Entity
public class SessionDatesEntity
{
	@PartitionKey(0)
	@CqlName(PAGE)
	private String page;

	@ClusteringColumn(0)
	@CqlName(MESSAGE_DATE)
	private LocalDate messageDate;

	@ClusteringColumn(1)
	@CqlName(SESSION_ALIAS)
	private String sessionAlias;

	@ClusteringColumn(2)
	@CqlName(DIRECTION)
	private String direction;

	@ClusteringColumn(3)
	@CqlName(PART)
	private String part;

	public SessionDatesEntity()
	{
	}
	
	public SessionDatesEntity(StoredMessageId messageId, PageId pageId)
	{
		setPage(pageId.getName());
		LocalDateTime ldt = TimeUtils.toLocalTimestamp(messageId.getTimestamp());
		setMessageDate(ldt.toLocalDate());
		setSessionAlias(messageId.getSessionAlias());
		setDirection(messageId.getDirection().getLabel());
		setPart(CassandraTimeUtils.getPart(ldt));
	}
	
	public String getPage()
	{
		return page;
	}

	public void setPage(String page)
	{
		this.page = page;
	}

	public LocalDate getMessageDate()
	{
		return messageDate;
	}

	public void setMessageDate(LocalDate messageDate)
	{
		this.messageDate = messageDate;
	}

	public String getSessionAlias()
	{
		return sessionAlias;
	}

	public void setSessionAlias(String sessionAlias)
	{
		this.sessionAlias = sessionAlias;
	}

	public String getDirection()
	{
		return direction;
	}

	public void setDirection(String direction)
	{
		this.direction = direction;
	}

	public String getPart()
	{
		return part;
	}

	public void setPart(String part)
	{
		this.part = part;
	}
}
