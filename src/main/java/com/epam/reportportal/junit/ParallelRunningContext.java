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

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.junit.runners.model.FrameworkMethod;

import io.reactivex.Maybe;

/**
 * Parallel execution context and set of operations to interact with it
 */
public class ParallelRunningContext {

	/** {@code ParentRunner} object => RP test item ID */
	private final Map<Object, Maybe<String>> itemIdOfTestRunner;
	
	/** hash of runner/method pair => RP test item ID */
	private final Map<Integer, Maybe<String>> itemIdOfTestMethod;
	
	/** hash of runner/method pair => status */
	private final Map<Integer, String> statusOfTestMethod;

	public ParallelRunningContext() {
		itemIdOfTestRunner = new ConcurrentHashMap<>();
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
	 * Set the test item ID for the specified test method.
	 * 
	 * @param method {@link FrameworkMethod} object for test method
	 * @param runner JUnit test runner
	 * @param itemId Report Portal test item ID for test method
	 */
	public void setItemIdOfTestMethod(FrameworkMethod method, Object runner, Maybe<String> itemId) {
		itemIdOfTestMethod.put(methodHash(runner, method), itemId);
	}

	/**
	 * Get the test item ID for the specified test method.
	 * 
	 * @param method {@link FrameworkMethod} object for test method
	 * @param runner JUnit test runner
	 * @return Report Portal test item ID for test method
	 */
	public Maybe<String> getItemIdOfTestMethod(FrameworkMethod method, Object runner) {
		return itemIdOfTestMethod.get(methodHash(runner, method));
	}

	/**
	 * Set the status for the specified test method.
	 * 
	 * @param method {@link FrameworkMethod} object for test method
	 * @param runner JUnit test runner
	 * @param status status for test method
	 */
	public void setStatusOfTestMethod(FrameworkMethod method, Object runner, String status) {
		statusOfTestMethod.put(methodHash(runner, method), status);
	}

	/**
	 * Get the status for the specified test method.
	 * 
	 * @param method {@link FrameworkMethod} object for test method
	 * @param runner JUnit test runner
	 * @return status for test method
	 */
	public String getStatusOfTestMethod(FrameworkMethod method, Object runner) {
		return statusOfTestMethod.get(methodHash(runner, method));
	}
	
	/**
	 * Get hash code for the specified runner/method pair.
	 * 
	 * @param runner JUnit test runner
	 * @param method {@link FrameworkMethod} object for test method
	 * @return hash code for runner/method pair
	 */
	private static Integer methodHash(Object runner, FrameworkMethod method) {
		return runner.hashCode() * 31 + method.hashCode();
	}
}
