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

import com.nordstrom.automation.junit.AtomicTest;
import org.junit.internal.runners.model.ReflectiveCallable;
import org.junit.runners.model.FrameworkMethod;

/**
 * Describes all operations for junit RP listener handler
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
	 * Send a <b>start test item</b> request for the indicated container object (category or suite) to Report Portal.
	 *
	 * @param runner JUnit test runner
	 */
	void startRunner(Object runner);

	/**
	 * Send a <b>finish test item</b> request for the indicated container object (test or suite) to Report Portal.
	 *
	 * @param runner JUnit test runner
	 */
	void stopRunner(Object runner);

	/**
	 * Send a <b>start test item</b> request for the indicated test to Report Portal.
	 *
	 * @param test {@link AtomicTest} object for test method
	 */
	void startTest(AtomicTest<FrameworkMethod> test);

	/**
	 * Send a <b>finish test item</b> request for the indicated test to Report Portal.
	 *
	 * @param test {@link AtomicTest} object for test method
	 */
	void finishTest(AtomicTest<FrameworkMethod> test);

	/**
	 * Send a <b>start test item</b> request for the indicated test method to Report Portal.
	 *
	 * @param runner JUnit test runner
	 * @param method {@link FrameworkMethod} object for test
	 * @param callable {@link ReflectiveCallable} object being intercepted
	 */
	void startTestMethod(Object runner, FrameworkMethod method, ReflectiveCallable callable);

	/**
	 * Send a <b>finish test item</b> request for the indicated test method to Report Portal.
	 *
	 * @param runner JUnit test runner
	 * @param method {@link FrameworkMethod} object for test
	 * @param callable {@link ReflectiveCallable} object being intercepted
	 */
	void stopTestMethod(Object runner, FrameworkMethod method, ReflectiveCallable callable);

	/**
	 * Record the status of the specified test method.
	 *
	 * @param callable {@link ReflectiveCallable} object being intercepted
	 * @param status test completion status
	 */
	void markCurrentTestMethod(ReflectiveCallable callable, String status);

	/**
	 * Handle test skip action
	 *
	 * @param test {@link AtomicTest} object for test method
	 */
	void handleTestSkip(AtomicTest<FrameworkMethod> test);

	/**
	 * Send message to report portal about appeared failure
	 *
	 * @param callable {@link ReflectiveCallable} object being intercepted
	 * @param thrown {@link Throwable} object with details of the failure
	 */
	void sendReportPortalMsg(ReflectiveCallable callable, Throwable thrown);

	/**
	 * Determine if the specified method is reportable.
	 *
	 * @param method {@link FrameworkMethod} object
	 * @return {@code true} if method is reportable; otherwise {@code false}
	 */
	boolean isReportable(FrameworkMethod method);
}
