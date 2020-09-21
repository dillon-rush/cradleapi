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

package com.exactpro.cradle.viewer;

public class Config
{
	private String dataCenter,
		host,
		keyspace,
		instanceName,
		username,
		password;
	private int port;
	
	public String getDataCenter()
	{
		return dataCenter;
	}
	
	public void setDataCenter(String dataCenter)
	{
		this.dataCenter = dataCenter;
	}
	
	
	public String getHost()
	{
		return host;
	}
	
	public void setHost(String host)
	{
		this.host = host;
	}
	
	
	public String getKeyspace()
	{
		return keyspace;
	}
	
	public void setKeyspace(String keyspace)
	{
		this.keyspace = keyspace;
	}
	
	
	public int getPort()
	{
		return port;
	}
	
	public void setPort(int port)
	{
		this.port = port;
	}
	
	
	public String getInstanceName()
	{
		return instanceName;
	}
	
	public void setInstanceName(String instanceName)
	{
		this.instanceName = instanceName;
	}
	
	
	public String getUsername()
	{
		return username;
	}
	
	public void setUsername(String username)
	{
		this.username = username;
	}
	
	
	public String getPassword()
	{
		return password;
	}
	
	public void setPassword(String password)
	{
		this.password = password;
	}
}
