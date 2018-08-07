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

import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.junit.runner.Description;

/**
 * 
 * This class is using for creating and supporting 'report portal' specified
 * test suite object model. 'Report portal' object model little bit different
 * from Junit model.
 */

public class SuitesKeeper {

	// Test suite keeper. Key - name of suite
	// set - tests
	private ConcurrentMap<String, Set<Class<?>>> suites;

	public SuitesKeeper() {
		suites = new ConcurrentHashMap<>();
	}

	public String getSuiteName(Class<?> test) {
		String suiteName = null;
		if (test == null) {
			return suiteName;
		}
		Set<Entry<String, Set<Class<?>>>> entries = suites.entrySet();
		for (Entry<String, Set<Class<?>>> entry : entries) {
			if (entry.getValue().contains(test)) {
				suiteName = entry.getKey();
				break;
			}
		}
		return suiteName;
	}

	/**
	 * Suite is passed if all tests from this suite passed
	 * 
	 * @param suiteName
	 * @return boolean
	 */
	public boolean isSuitePassed(String suiteName, Set<Class<?>> passedTests) {
		if (passedTests == null) {
			return false;
		}
		Set<Class<?>> tests = suites.get(suiteName);
		return tests == null || passedTests.containsAll(tests);
	}

	/**
	 * Add description to suite keeper. Description of single test will be added
	 * to 'default' suite
	 */
	public void addToSuiteKeeper(Description description) {
		if (description == null) {
			return;
		}
		if (isSuite(description)) {
			processSuite(description, description.getClassName());
		} else {
			addToSuite(description.getClassName(), description);
		}
	}

	/**
	 * Add test description to specified suite
	 * 
	 * @param suiteName
	 * @param description
	 */

	private void addToSuite(String suiteName, Description description) {
		Set<Class<?>> suiteContent = null;
		if (suites.containsKey(suiteName)) {
			suiteContent = suites.get(suiteName);
		} else {
			suiteContent = new HashSet<>();
		}
		suiteContent.add(description.getTestClass());
		suites.put(suiteName, suiteContent);

	}

	/**
	 * Check is input description it's description of test or test Suite
	 * 
	 * @param description
	 * @return isSuite
	 */
	private boolean isSuite(Description description) {
		List<Description> children = description.getChildren();
		return !children.isEmpty() && children.get(0).isSuite();
	}

	private void processSuite(Description suiteDescription, String rootSuiteName) {
		for (Description child : suiteDescription.getChildren()) {
			if (isSuite(child)) {
				processSuite(child, rootSuiteName);
			} else {
				addToSuite(rootSuiteName, child);
			}
		}
	}

}
