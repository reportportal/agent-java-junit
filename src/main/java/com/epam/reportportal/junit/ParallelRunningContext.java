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

/**
 * Parallel execution context and set of operations to interact with it
 */
public class ParallelRunningContext {
	/** Report Portal launch ID */
	private volatile String launchId = "";
	
	/** {@code ParentRunner} object => RP test item ID */
	private final Map<Object, String> itemIdOfTestRunner;
	
	/** {@link FrameworkMethod} of test method => RP test item ID */
	private final Map<FrameworkMethod, String> itemIdOfTestMethod;
	
	/** {@link FrameworkMethod} of test method => status */
	private final Map<FrameworkMethod, String> statusOfTestMethod;

	public ParallelRunningContext() {
		itemIdOfTestRunner = new ConcurrentHashMap<>();
		itemIdOfTestMethod = new ConcurrentHashMap<>();
		statusOfTestMethod = new ConcurrentHashMap<>();
	}

	/**
	 * Set the launch ID for the current launch.
	 * 
	 * @param launchId Report Portal launch ID
	 */
	public void setLaunchId(String launchId) {
		this.launchId = launchId;
	}

	/**
	 * Get the launch ID for the current launch.
	 * 
	 * @return Report Portal launch ID
	 */
	public String getLaunchId() {
		return launchId;
	}

	/**
	 * Set the test item ID for the indicated container object (test or suite).
	 * 
	 * @param runner JUnit test runner
	 * @param itemId Report Portal test item ID for container object
	 */
	public void setTestIdOfTestRunner(Object runner, String itemId) {
		itemIdOfTestRunner.put(runner, itemId);
	}

	/**
	 * Get the test item ID for the indicated container object (test or suite).
	 * 
	 * @param runner JUnit test runner
	 * @return Report Portal test item ID for container object
	 */
	public String getItemIdOfTestRunner(Object runner) {
		return itemIdOfTestRunner.get(runner);
	}

	/**
	 * Set the test item ID for the specified test method.
	 * 
	 * @param method {@link FrameworkMethod} object for test method
	 * @param itemId Report Portal test item ID for test method
	 */
	public void setItemIdOfTestMethod(FrameworkMethod method, String itemId) {
		itemIdOfTestMethod.put(method, itemId);
	}

	/**
	 * Get the test item ID for the specified test method.
	 * 
	 * @param method {@link FrameworkMethod} object for test method
	 * @return Report Portal test item ID for test method
	 */
	public String getItemIdOfTestMethod(FrameworkMethod method) {
		return itemIdOfTestMethod.get(method);
	}

	/**
	 * Set the status for the specified test method.
	 * 
	 * @param method {@link FrameworkMethod} object for test method
	 * @param status status for test method
	 */
	public void setStatusOfTestMethod(FrameworkMethod method, String status) {
		statusOfTestMethod.put(method, status);
	}

	/**
	 * Get the status for the specified test method.
	 * 
	 * @param method {@link FrameworkMethod} object for test method
	 * @return status for test method
	 */
	public String getStatusOfTestMethod(FrameworkMethod method) {
		return statusOfTestMethod.get(method);
	}
}
