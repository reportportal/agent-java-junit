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
package com.epam.reportportal.junit;

import com.epam.reportportal.listeners.ItemStatus;
import com.epam.reportportal.service.tree.TestItemTree;
import org.junit.runner.Description;
import org.junit.runners.model.FrameworkMethod;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/**
 * Parallel execution context and set of operations to interact with it
 */
public class ParallelRunningContext {

	private static final ThreadLocal<ParallelRunningContext> CONTEXT_THREAD_LOCAL = new ThreadLocal<>();

	/**
	 * Tree of test items tracking
	 */
	private final TestItemTree itemTree = new TestItemTree();

	private final Map<FrameworkMethod, Description> testMethodDescription = new ConcurrentHashMap<>();
	private final Map<TestItemTree.ItemTreeKey, ItemStatus> testStatus = new ConcurrentHashMap<>();
	private final Map<TestItemTree.ItemTreeKey, Throwable> testThrowable = new ConcurrentHashMap<>();

	public ParallelRunningContext() {
		CONTEXT_THREAD_LOCAL.set(this);
	}

	public ItemStatus getTestStatus(TestItemTree.ItemTreeKey test) {
		return testStatus.get(test);
	}

	public ItemStatus setTestStatus(TestItemTree.ItemTreeKey test, ItemStatus status) {
		return testStatus.put(test, status);
	}

	public ItemStatus computeTestStatusIfNo(TestItemTree.ItemTreeKey test, Function<TestItemTree.ItemTreeKey, ItemStatus> mappingFunction) {
		return testStatus.computeIfAbsent(test, mappingFunction);
	}

	public Throwable getTestThrowable(TestItemTree.ItemTreeKey test) {
		return testThrowable.get(test);
	}

	public void setTestThrowable(TestItemTree.ItemTreeKey test, Throwable throwable) {
		testThrowable.put(test, throwable);
	}

	public Description getTestMethodDescription(FrameworkMethod method) {
		return testMethodDescription.get(method);
	}

	public Description setTestMethodDescription(FrameworkMethod method, Description description) {
		CONTEXT_THREAD_LOCAL.set(this);
		return testMethodDescription.put(method, description);
	}

	/**
	 * Return current test launch context depending on the current thread
	 *
	 * @return a test launch context
	 */
	public static ParallelRunningContext getCurrent() {
		return CONTEXT_THREAD_LOCAL.get();
	}

	/**
	 * Returns current Test Item tree structure tracking object
	 *
	 * @return tree tracking object
	 */
	public TestItemTree getItemTree() {
		return itemTree;
	}
}
