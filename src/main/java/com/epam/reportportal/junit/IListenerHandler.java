/*
 * Copyright 2016 EPAM Systems
 *
 *
 * This file is part of EPAM Report Portal.
 * https://github.com/epam/ReportPortal
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

import org.junit.runner.Description;
import org.junit.runner.notification.Failure;

import com.epam.reportportal.restclient.endpoint.exception.RestEndpointIOException;

/**
 * Describes all operations for junit RP listener handler
 * 
 */
public interface IListenerHandler {

	/**
	 * Start new launch in the report portal
	 * 
	 * @throws RestEndpointIOException
	 */

	void startLaunch() throws RestEndpointIOException;

	/**
	 * Finish current launch
	 * 
	 * @throws RestEndpointIOException
	 */

	void stopLaunch() throws RestEndpointIOException;

	/**
	 * Start test event handler
	 * 
	 * @param description
	 * @throws RestEndpointIOException
	 */
	void startTestMethod(Description description) throws RestEndpointIOException;

	/**
	 * Stop test event handler
	 * 
	 * @param description
	 * @throws RestEndpointIOException
	 */

	void stopTestMethod(Description description) throws RestEndpointIOException;

	/**
	 * Mark current test method with appropriative status. This method should be
	 * used in cases when test methods failed, skipped.
	 * 
	 * @param status
	 * @throws RestEndpointIOException
	 */
	void markCurrentTestMethod(Description description, String status) throws RestEndpointIOException;

	/**
	 * Start new suite if suite(for current running method) is not started
	 * 
	 * @param description
	 *            method
	 * @throws RestEndpointIOException
	 */
	void startSuiteIfRequired(Description description) throws RestEndpointIOException;

	/**
	 * Stop current suite if all tests finished
	 * 
	 * @param description
	 * @throws RestEndpointIOException
	 */
	void stopSuiteIfRequired(Description description) throws RestEndpointIOException;

	/**
	 * Stop current test if all test methods passed
	 * 
	 * @param description
	 * @throws RestEndpointIOException
	 */
	void stopTestIfRequired(Description description) throws RestEndpointIOException;

	/**
	 * Start new test
	 * 
	 * @param currentTest
	 * @throws RestEndpointIOException
	 */
	void starTestIfRequired(Description currentTest) throws RestEndpointIOException;

	/**
	 * Initialize suite processor i.e create data structure which describes all
	 * suites and tests relationships in the current launch
	 * 
	 * @param description
	 */
	void initSuiteProcessor(Description description);

	/**
	 * Handle test skip action
	 * 
	 * @param description
	 */
	void handleTestSkip(Description description) throws RestEndpointIOException;

	/**
	 * Mark current method as finished
	 * 
	 * @param description
	 * @throws RestEndpointIOException
	 */
	void addToFinishedMethods(Description description) throws RestEndpointIOException;

	/**
	 * Clear 'currently running item id' field in the context
	 */
	void clearRunningItemId();

	/**
	 * Send message to report portal about appeared failure
	 * 
	 * @param failure
	 */
	void sendReportPortalMsg(Failure failure);
}
