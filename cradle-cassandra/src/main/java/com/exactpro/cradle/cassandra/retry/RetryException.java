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

package com.exactpro.cradle.cassandra.retry;

/**
 * Exception to indicate that request to Cassandra has failed and cannot be retried.
 */
public class RetryException extends Exception
{
	private static final long serialVersionUID = 8629258280897290696L;

	public RetryException(String message)
	{
		super(message);
	}
	
	public RetryException(Throwable cause)
	{
		super(cause);
	}
	
	public RetryException(String message, Throwable cause)
	{
		super(message, cause);
	}
}
