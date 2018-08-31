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
import org.junit.runners.model.TestClass;

/**
 * Parallel execution context and set of operations to interact with it
 */
public class ParallelRunningContext {
	/** Report Portal launch ID */
	private volatile String launchId = "";
	
	/** {@link TestClass} of test class => RP test item ID */
	private Map<TestClass, String> itemIdOfTestClass;
	
	/** {@link TestClass} of atomic test => RP test item ID */
	private Map<TestClass, String> itemIdOfAtomicTest;
	
	/** {@link FrameworkMethod} of test method => RP test item ID */
	private Map<FrameworkMethod, String> itemIdOfTestMethod;
	
	/** {@link FrameworkMethod} of test method => status */
	private Map<FrameworkMethod, String> statusOfTestMethod;

	public ParallelRunningContext() {
		itemIdOfTestClass = new ConcurrentHashMap<>();
		itemIdOfAtomicTest = new ConcurrentHashMap<>();
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
	 * @param testClass {@link TestClass} object for container object
	 * @param itemId Report Portal test item ID for container object
	 */
	public void setTestIdOfTestClass(TestClass testClass, String itemId) {
		itemIdOfTestClass.put(testClass, itemId);
	}

	/**
	 * Get the test item ID for the indicated container object (test or suite).
	 * 
	 * @param testClass {@link TestClass} object for container object
	 * @return Report Portal test item ID for container object
	 */
	public String getItemIdOfTestClass(TestClass testClass) {
		return itemIdOfTestClass.get(testClass);
	}

	/**
	 * Set the test item ID for the indicated "atomic" test.
	 * 
	 * @param testClass {@link TestClass} object for "atomic" test
	 * @param itemId Report Portal test item ID for "atomic" test
	 */
	public void setItemIdOfAtomicTest(TestClass testClass, String itemId) {
		itemIdOfAtomicTest.put(testClass, itemId);
	}
	
	/**
	 * Get the test item ID for the indicated "atomic" test.
	 * 
	 * @param testClass {@link TestClass} object for "atomic" test
	 * @return Report Portal test item ID for "atomic" test
	 */
	public String getItemIdOfAtomicTest(TestClass testClass) {
		return itemIdOfAtomicTest.get(testClass);
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
