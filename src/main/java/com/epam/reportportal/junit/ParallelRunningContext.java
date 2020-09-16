/*
 * Copyright 2018 EPAM Systems
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
import com.nordstrom.automation.junit.AtomicTest;
import io.reactivex.Maybe;
import org.junit.internal.runners.model.ReflectiveCallable;
import org.junit.runners.model.FrameworkMethod;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Parallel execution context and set of operations to interact with it
 */
public class ParallelRunningContext {

	public static final TestItemTree ITEM_TREE = new TestItemTree();

	/**
	 * {@code ParentRunner} object => RP test item ID
	 */
	private final Map<Object, Maybe<String>> itemIdOfTestRunner;

	/**
	 * {@code AtomicTest} object => RP test item ID
	 */
	private final Map<AtomicTest<FrameworkMethod>, Maybe<String>> itemIdOfTests;

	/**
	 * hash of runner/method pair => RP test item ID
	 */
	private final Map<ReflectiveCallable, Maybe<String>> itemIdOfTestMethod;

	/**
	 * hash of runner/method pair => status
	 */
	private final Map<ReflectiveCallable, ItemStatus> statusOfTestMethod;

	public ParallelRunningContext() {
		itemIdOfTestRunner = new ConcurrentHashMap<>();
		itemIdOfTests = new ConcurrentHashMap<>();
		itemIdOfTestMethod = new ConcurrentHashMap<>();
		statusOfTestMethod = new ConcurrentHashMap<>();
	}

	/**
	 * Set the test item ID for the indicated container object (test or suite).
	 *
	 * @param runner JUnit test runner
	 * @param itemId Report Portal test item ID for container object
	 */
	public void setTestIdOfTestRunner(Object runner, Maybe<String> itemId) {
		itemIdOfTestRunner.put(runner, itemId);
	}

	/**
	 * Get the test item ID for the indicated container object (test or suite).
	 *
	 * @param runner JUnit test runner
	 * @return Report Portal test item ID for container object
	 */
	public Maybe<String> getItemIdOfTestRunner(Object runner) {
		return itemIdOfTestRunner.get(runner);
	}

	/**
	 * Set the test item ID for the indicated container object (test or suite).
	 *
	 * @param test   JUnit test
	 * @param itemId Report Portal test item ID for container object
	 */
	public void setTestIdOfTest(AtomicTest<FrameworkMethod> test, Maybe<String> itemId) {
		itemIdOfTests.put(test, itemId);
	}

	/**
	 * Get the test item ID for the indicated container object (test or suite).
	 *
	 * @param test JUnit test
	 * @return Report Portal test item ID for container object
	 */
	public Maybe<String> getItemIdOfTest(AtomicTest<FrameworkMethod> test) {
		return itemIdOfTests.get(test);
	}

	/**
	 * Set the test item ID for the specified test method.
	 *
	 * @param callable {@link ReflectiveCallable} object being intercepted
	 * @param itemId   Report Portal test item ID for test method
	 */
	public void setItemIdOfTestMethod(ReflectiveCallable callable, Maybe<String> itemId) {
		itemIdOfTestMethod.put(callable, itemId);
	}

	/**
	 * Get the test item ID for the specified test method.
	 *
	 * @param callable {@link ReflectiveCallable} object being intercepted
	 * @return Report Portal test item ID for test method
	 */
	public Maybe<String> getItemIdOfTestMethod(ReflectiveCallable callable) {
		return itemIdOfTestMethod.get(callable);
	}

	/**
	 * Set the status for the specified test method.
	 *
	 * @param callable {@link ReflectiveCallable} object being intercepted
	 * @param status   status for test method
	 */
	public void setStatusOfTestMethod(ReflectiveCallable callable, ItemStatus status) {
		statusOfTestMethod.put(callable, status);
	}

	/**
	 * Get the status for the specified test method.
	 *
	 * @param callable {@link ReflectiveCallable} object being intercepted
	 * @return status for test method
	 */
	public ItemStatus getStatusOfTestMethod(ReflectiveCallable callable) {
		return statusOfTestMethod.get(callable);
	}
}
