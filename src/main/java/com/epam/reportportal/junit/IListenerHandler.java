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

import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.TestClass;

/**
 * Describes all operations for junit RP listener handler
 * 
 */
public interface IListenerHandler {

	/**
	 * Send a <b>start launch</b> request to Report Portal.
	 */

	void startLaunch();

	/**
	 * Send a "finish launch" request to Report Portal.
	 */

	void stopLaunch();

	/**
	 * Send a <b>start test item</b> request for the indicated container object (test or suite) to Report Portal.
	 * 
	 * @param runner JUnit test runner
	 * @param isSuite {@code true} if the specified test class is a suite; otherwise {@code false}
	 */
	void startRunner(Object runner, boolean isSuite);

	/**
	 * Send a <b>finish test item</b> request for the indicated container object (test or suite) to Report Portal.
	 * 
	 * @param runner JUnit test runner
	 */
	void stopRunner(Object runner);
	
	/**
	 * Send a <b>start test item</b> request for the indicated test method to Report Portal.
	 * 
	 * @param method {@link FrameworkMethod} object for test method
	 * @param testClass {@link TestClass} object for test method
	 */
	void startTestMethod(FrameworkMethod method, TestClass testClass);

	/**
	 * Send a <b>finish test item</b> request for the indicated test method to Report Portal.
	 * 
	 * @param method {@link FrameworkMethod} object for test method
	 */

	void stopTestMethod(FrameworkMethod method);

	/**
	 * Record the status of the specified test method.
	 * 
	 * @param method {@link FrameworkMethod} object for test method
	 * @param status test completion status
	 */
	void markCurrentTestMethod(FrameworkMethod method, String status);

	/**
	 * Handle test skip action
	 * 
	 * @param method {@link FrameworkMethod} object for test method
	 * @param testClass {@link TestClass} object for test method
	 */
	void handleTestSkip(FrameworkMethod method, TestClass testClass);

	/**
	 * Send message to report portal about appeared failure
	 * 
	 * @param method {@link FrameworkMethod} object for test method
	 * @param thrown {@link Throwable} object with details of the failure 
	 */
	void sendReportPortalMsg(FrameworkMethod method, Throwable thrown);
}
