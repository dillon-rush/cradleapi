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

package com.exactpro.cradle.filters;

public class FilterForAnyBuilder<V extends Comparable<V>, R> extends FilterByFieldBuilder<V, R>
{
	public FilterForAnyBuilder(FilterByField<V> filter, R toReturn)
	{
		super(filter, toReturn);
	}
	
	
	public R isEqualTo(V value)
	{
		setFilter(ComparisonOperation.EQUALS, value);
		return toReturn;
	}
	
	public R isLessThan(V value)
	{
		setFilter(ComparisonOperation.LESS, value);
		return toReturn;
	}
	
	public R isLessThanOrEqualTo(V value)
	{
		setFilter(ComparisonOperation.LESS_OR_EQUALS, value);
		return toReturn;
	}
	
	public R isGreaterThan(V value)
	{
		setFilter(ComparisonOperation.GREATER, value);
		return toReturn;
	}
	
	public R isGreaterThanOrEqualTo(V value)
	{
		setFilter(ComparisonOperation.GREATER_OR_EQUALS, value);
		return toReturn;
	}
}
