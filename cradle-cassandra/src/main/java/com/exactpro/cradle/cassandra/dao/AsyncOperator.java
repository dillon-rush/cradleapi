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

package com.exactpro.cradle.cassandra.dao;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.function.Supplier;

import com.exactpro.cradle.cassandra.CassandraSemaphore;

public class AsyncOperator<T>
{
	private final CassandraSemaphore semaphore;

	public AsyncOperator(CassandraSemaphore semaphore)
	{
		this.semaphore = semaphore;
	}
	
	public CompletableFuture<T> getFuture(Supplier<CompletableFuture<T>> worker)
	{
		return CompletableFuture.runAsync(() -> {
				try
				{
					semaphore.acquireSemaphore();
				}
				catch (InterruptedException e)
				{
					throw new CompletionException("Could not acquire semaphore permit", e);
				}
			}).thenCompose((v) -> worker.get())
				.whenComplete((t, error) -> {
						if (error == null || !(error.getCause() instanceof InterruptedException))  //I.e. if semaphore was acquired
							semaphore.releaseSemaphore(); 
						if (error != null)
						{
							if (error instanceof CompletionException)
								throw (CompletionException)error;
							throw new CompletionException(error);
						}
					});
	}
}