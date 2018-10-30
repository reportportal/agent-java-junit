/*
 * Copyright 2016 EPAM Systems
 *
 *
 * This file is part of EPAM Report Portal.
 * https://github.com/reportportal/agent-java-junit/
 *
 * Report Portal is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Report Portal is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Report Portal.  If not, see <http://www.gnu.org/licenses/>.
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
	
	/** {@link FrameworkMethod} of test method => RP test item ID */
	private final Map<FrameworkMethod, Maybe<String>> itemIdOfTestMethod;
	
	/** {@link FrameworkMethod} of test method => status */
	private final Map<FrameworkMethod, String> statusOfTestMethod;

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
	 * @param itemId Report Portal test item ID for test method
	 */
	public void setItemIdOfTestMethod(FrameworkMethod method, Maybe<String> itemId) {
		itemIdOfTestMethod.put(method, itemId);
	}

	/**
	 * Get the test item ID for the specified test method.
	 * 
	 * @param method {@link FrameworkMethod} object for test method
	 * @return Report Portal test item ID for test method
	 */
	public Maybe<String> getItemIdOfTestMethod(FrameworkMethod method) {
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
