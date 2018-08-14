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

	public void setLaunchId(String launchId) {
		this.launchId = launchId;
	}

	public String getLaunchId() {
		return launchId;
	}

	public void setTestIdOfTestClass(TestClass testClass, String itemId) {
		itemIdOfTestClass.put(testClass, itemId);
	}

	public String getItemIdOfTestClass(TestClass testClass) {
		return itemIdOfTestClass.get(testClass);
	}

	public void setItemIdOfAtomicTest(TestClass testClass, String itemId) {
		itemIdOfAtomicTest.put(testClass, itemId);
	}
	
	public String getItemIdOfAtomicTest(TestClass testClass) {
		return itemIdOfAtomicTest.get(testClass);
	}
	
	public void setItemIdOfTestMethod(FrameworkMethod method, String itemId) {
		itemIdOfTestMethod.put(method, itemId);
	}

	public String getItemIdOfTestMethod(FrameworkMethod method) {
		return itemIdOfTestMethod.get(method);
	}

	public void setStatusOfTestMethod(FrameworkMethod method, String status) {
		statusOfTestMethod.put(method, status);
	}

	public String getStatusOfTestMethod(FrameworkMethod method) {
		return statusOfTestMethod.get(method);
	}
}
