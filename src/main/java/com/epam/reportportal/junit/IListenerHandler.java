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

import com.epam.reportportal.restclient.endpoint.exception.RestEndpointIOException;

/**
 * Describes all operations for junit RP listener handler
 * 
 */
public interface IListenerHandler {

	/**
	 * Send a <b>start launch</b> request to Report Portal.
	 * 
	 * @throws RestEndpointIOException
	 */

	void startLaunch() throws RestEndpointIOException;

	/**
	 * Send a "finish launch" request to Report Portal.
	 * 
	 * @throws RestEndpointIOException
	 */

	void stopLaunch() throws RestEndpointIOException;

	/**
	 * Send a <b>start test item</b> request for the indicated test class to Report Portal.
	 * 
	 * @param testClass {@link TestClass} object for test class
	 * @param isSuite {@code true} if the specified test class is a suite; otherwise {@code false}
	 * @throws RestEndpointIOException
	 */
	void startTestClass(TestClass testClass, boolean isSuite) throws RestEndpointIOException;

	/**
	 * Send a <b>finish test item</b> request for the indicated test class to Report Portal.
	 * 
	 * @param testClass {@link TestClass} object for test class
	 * @throws RestEndpointIOException
	 */
	void stopTestClass(TestClass testClass) throws RestEndpointIOException;
	
	/**
	 * 
	 * @param method
	 * @param testClass
	 * @throws RestEndpointIOException
	 */
	void startAtomicTest(FrameworkMethod method, TestClass testClass) throws RestEndpointIOException;

	/**
	 * 
	 * @param method
	 * @param testClass
	 * @throws RestEndpointIOException
	 */
	void stopAtomicTest(FrameworkMethod method, TestClass testClass) throws RestEndpointIOException;
	
	/**
	 * Send a <b>start test item</b> request for the indicated test method to Report Portal.
	 * 
	 * @param method
	 * @param testClass
	 * @throws RestEndpointIOException
	 */
	void startTestMethod(FrameworkMethod method, TestClass testClass) throws RestEndpointIOException;

	/**
	 * Send a <b>finish test item</b> request for the indicated test method to Report Portal.
	 * 
	 * @param method the method of the test that just ran
	 * @throws RestEndpointIOException
	 */

	void stopTestMethod(FrameworkMethod method) throws RestEndpointIOException;

	/**
	 * Record the status of the specified test.
	 * 
	 * @param method the method of the test that just ran
	 * @param status test completion status
	 * @throws RestEndpointIOException
	 */
	void markCurrentTestMethod(FrameworkMethod method, String status) throws RestEndpointIOException;

	/**
	 * Handle test skip action
	 * 
	 * @param method
	 * @param testClass
	 */
	void handleTestSkip(FrameworkMethod method, TestClass testClass) throws RestEndpointIOException;

	/**
	 * Clear 'currently running item id' field in the context
	 */
	void clearRunningItemId();

	/**
	 * Send message to report portal about appeared failure
	 * 
	 * @param method
	 * @param thrown TODO
	 */
	void sendReportPortalMsg(FrameworkMethod method, Throwable thrown);
}
