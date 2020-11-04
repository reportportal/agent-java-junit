/*
 * Copyright 2020 EPAM Systems
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

package com.epam.reportportal.junit.features.retry;

import com.nordstrom.automation.junit.JUnitRetryAnalyzer;
import org.junit.runners.model.FrameworkMethod;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class RetryAnalyzer implements JUnitRetryAnalyzer {

	public static final int DEFAULT_MAX_RETRY = 1;
	private static final Map<FrameworkMethod, AtomicInteger> RETRY_COUNT = new ConcurrentHashMap<>();

	@Override
	public boolean retry(FrameworkMethod method, Throwable thrown) {
		if (method.getMethod().getDeclaringClass().getPackage().getName().contains("retry")) {
			return RETRY_COUNT.computeIfAbsent(method, (m) -> new AtomicInteger(0)).getAndIncrement() < DEFAULT_MAX_RETRY;
		}
		return false;
	}
}
