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
	 * @throws RestEndpointIOException if something goes wrong
	 */

	void startLaunch() throws RestEndpointIOException;

	/**
	 * Send a "finish launch" request to Report Portal.
	 * 
	 * @throws RestEndpointIOException if something goes wrong
	 */

	void stopLaunch() throws RestEndpointIOException;

	/**
	 * Send a <b>start test item</b> request for the indicated container object (test or suite) to Report Portal.
	 * 
	 * @param runner JUnit test runner
	 * @param isSuite {@code true} if the specified test class is a suite; otherwise {@code false}
	 * @throws RestEndpointIOException if something goes wrong
	 */
	void startRunner(Object runner, boolean isSuite) throws RestEndpointIOException;

	/**
	 * Send a <b>finish test item</b> request for the indicated container object (test or suite) to Report Portal.
	 * 
	 * @param runner JUnit test runner
	 * @throws RestEndpointIOException if something goes wrong
	 */
	void stopRunner(Object runner) throws RestEndpointIOException;
	
	/**
	 * Send a <b>start test item</b> request for the indicated test method to Report Portal.
	 * 
	 * @param method {@link FrameworkMethod} object for test method
	 * @param testClass {@link TestClass} object for test method
	 * @throws RestEndpointIOException if something goes wrong
	 */
	void startTestMethod(FrameworkMethod method, TestClass testClass) throws RestEndpointIOException;

	/**
	 * Send a <b>finish test item</b> request for the indicated test method to Report Portal.
	 * 
	 * @param method {@link FrameworkMethod} object for test method
	 * @throws RestEndpointIOException if something goes wrong
	 */

	void stopTestMethod(FrameworkMethod method) throws RestEndpointIOException;

	/**
	 * Record the status of the specified test method.
	 * 
	 * @param method {@link FrameworkMethod} object for test method
	 * @param status test completion status
	 * @throws RestEndpointIOException if something goes wrong
	 */
	void markCurrentTestMethod(FrameworkMethod method, String status) throws RestEndpointIOException;

	/**
	 * Handle test skip action
	 * 
	 * @param method {@link FrameworkMethod} object for test method
	 * @param testClass {@link TestClass} object for test method
	 * @throws RestEndpointIOException if something goes wrong
	 */
	void handleTestSkip(FrameworkMethod method, TestClass testClass) throws RestEndpointIOException;

	/**
	 * Clear 'currently running item id' field in the context
	 */
	void clearRunningItemId();

	/**
	 * Send message to report portal about appeared failure
	 * 
	 * @param method {@link FrameworkMethod} object for test method
	 * @param thrown {@link Throwable} object with details of the failure 
	 */
	void sendReportPortalMsg(FrameworkMethod method, Throwable thrown);
}
