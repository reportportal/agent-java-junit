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

import com.epam.reportportal.listeners.ListenerParameters;
import com.epam.reportportal.listeners.ReportPortalListenerContext;
import com.epam.reportportal.listeners.Statuses;
import com.epam.reportportal.service.BatchedReportPortalService;
import com.epam.ta.reportportal.ws.model.EntryCreatedRS;
import com.epam.ta.reportportal.ws.model.FinishExecutionRQ;
import com.epam.ta.reportportal.ws.model.FinishTestItemRQ;
import com.epam.ta.reportportal.ws.model.StartTestItemRQ;
import com.epam.ta.reportportal.ws.model.launch.Mode;
import com.epam.ta.reportportal.ws.model.launch.StartLaunchRQ;
import com.epam.ta.reportportal.ws.model.log.SaveLogRQ;
import com.google.common.base.Strings;
import com.google.inject.Inject;
import com.nordstrom.automation.junit.LifecycleHooks;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.TestClass;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

import static com.epam.reportportal.listeners.ListenersUtils.handleException;

/**
 * MultyThread realization of IListenerHandler. This realization support
 * parallel running of tests and test methods. Main constraint: All test classes
 * in current launch should be unique. (User shouldn't run the same classes
 * twice/or more times in the one launch)
 *
 * @author Aliaksey_Makayed (modified by Andrei_Ramanchuk)
 */
public class ParallelRunningHandler implements IListenerHandler {
	
	public static final String API_BASE = "/reportportal-ws/api/v1";
	private static final Logger LOGGER = LoggerFactory.getLogger(ParallelRunningHandler.class);

	private ParallelRunningContext context;
	private BatchedReportPortalService reportPortalService;

	private String launchName = "test_launch";
	private Set<String> tags;
	private Mode launchRunningMode;

	/**
	 * Constructor: Instantiate a parallel running handler
	 * 
	 * @param parameters listener parameters
	 * @param suitesKeeper test collection hierarchy processor
	 * @param parallelRunningContext test execution context manager
	 * @param reportPortalService Report Portal web service client
	 */
	@Inject
	public ParallelRunningHandler(ListenerParameters parameters, ParallelRunningContext parallelRunningContext,
			BatchedReportPortalService reportPortalService) {
		this.launchName = parameters.getLaunchName();
		this.tags = parameters.getTags();
		this.launchRunningMode = parameters.getMode();
		this.context = parallelRunningContext;
		this.reportPortalService = reportPortalService;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void startLaunch() {
		StartLaunchRQ startLaunchRQ = new StartLaunchRQ();
		startLaunchRQ.setName(launchName);
		startLaunchRQ.setStartTime(Calendar.getInstance().getTime());
		startLaunchRQ.setTags(tags);
		startLaunchRQ.setMode(launchRunningMode);
		EntryCreatedRS rs = null;
		try {
			rs = reportPortalService.startLaunch(startLaunchRQ);
			context.setLaunchId(rs.getId());
		} catch (Exception e) {
			handleException(e, LOGGER, "Unable start the launch: '" + launchName + "'");
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void stopLaunch() {
		if (!Strings.isNullOrEmpty(context.getLaunchId())) {
			FinishExecutionRQ finishExecutionRQ = new FinishExecutionRQ();
			finishExecutionRQ.setEndTime(Calendar.getInstance().getTime());
			try {
				reportPortalService.finishLaunch(context.getLaunchId(), finishExecutionRQ);
			} catch (Exception e) {
				handleException(e, LOGGER, "Unable finish the launch: '" + launchName + "'");
			}
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void startRunner(Object runner, boolean isSuite) {
		StartTestItemRQ rq = new StartTestItemRQ();
		rq.setStartTime(Calendar.getInstance().getTime());
		TestClass testClass = LifecycleHooks.getTestClassOf(runner);
		rq.setName(testClass.getName());
		rq.setType(isSuite ? "SUITE" : "TEST");
		rq.setLaunchId(context.getLaunchId());
		String containerId = getContainerId(runner);
		EntryCreatedRS rs;
		try {
			if (containerId == null) {
				rs = reportPortalService.startRootTestItem(rq);
			} else {
				rs = reportPortalService.startTestItem(containerId, rq);
			}
			context.setTestIdOfTestRunner(runner, rs.getId());
		} catch (Exception e) {
			handleException(e, LOGGER, "Unable start test class: '" + testClass.getName() + "'");
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void stopRunner(Object runner) {
		FinishTestItemRQ rq = new FinishTestItemRQ();
		rq.setEndTime(Calendar.getInstance().getTime());
		try {
			reportPortalService.finishTestItem(context.getItemIdOfTestRunner(runner), rq);
		} catch (Exception e) {
			TestClass testClass = LifecycleHooks.getTestClassOf(runner);
			handleException(e, LOGGER, "Unable finish test class: '" + testClass.getName() + "'");
		}
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public void startTestMethod(FrameworkMethod method, TestClass testClass) {
		StartTestItemRQ rq = new StartTestItemRQ();
		rq.setName(method.getName());
		rq.setStartTime(Calendar.getInstance().getTime());
		rq.setType(detectMethodType(method));
		rq.setLaunchId(context.getLaunchId());
		EntryCreatedRS rs = null;
		try {
			Object runner = LifecycleHooks.getRunnerFor(testClass);
			rs = reportPortalService.startTestItem(context.getItemIdOfTestRunner(runner), rq);
			context.setItemIdOfTestMethod(method, rs.getId());
			ReportPortalListenerContext.setRunningNowItemId(rs.getId());
		} catch (Exception e) {
			handleException(e, LOGGER, "Unable start test method: '" + method.getName() + "'");
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void stopTestMethod(FrameworkMethod method) {
		ReportPortalListenerContext.setRunningNowItemId(null);
		FinishTestItemRQ rq = new FinishTestItemRQ();
		rq.setEndTime(Calendar.getInstance().getTime());
		String status = context.getStatusOfTestMethod(method);
		rq.setStatus((status == null || status.equals("")) ? Statuses.PASSED : status);
		try {
			reportPortalService.finishTestItem(context.getItemIdOfTestMethod(method), rq);
		} catch (Exception e) {
			handleException(e, LOGGER, "Unable finish test method: '" + method.getName() + "'");
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void markCurrentTestMethod(FrameworkMethod method, String status) {
		context.setStatusOfTestMethod(method, status);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void handleTestSkip(FrameworkMethod method, TestClass testClass) {
		StartTestItemRQ startRQ = new StartTestItemRQ();
		startRQ.setStartTime(Calendar.getInstance().getTime());
		startRQ.setName(method.getName());
		startRQ.setType("TEST");
		startRQ.setLaunchId(context.getLaunchId());
		try {
			Object runner = LifecycleHooks.getRunnerFor(testClass);
			EntryCreatedRS rs = reportPortalService.startTestItem(context.getItemIdOfTestRunner(runner), startRQ);
			FinishTestItemRQ finishRQ = new FinishTestItemRQ();
			finishRQ.setStatus(Statuses.SKIPPED);
			finishRQ.setEndTime(Calendar.getInstance().getTime());
			reportPortalService.finishTestItem(rs.getId(), finishRQ);
		} catch (Exception e) {
			handleException(e, LOGGER, "Unable skip test: '" + method.getName() + "'");
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void clearRunningItemId() {
		ReportPortalListenerContext.setRunningNowItemId(null);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void sendReportPortalMsg(FrameworkMethod method, Throwable thrown) {
		SaveLogRQ saveLogRQ = new SaveLogRQ();
		if (thrown != null) {
			saveLogRQ.setMessage("Exception: " + thrown.getMessage() + System.getProperty("line.separator")
					+ this.getStackTraceString(thrown));
		} else {
			saveLogRQ.setMessage("Just exception (contact dev team)");
		}
		saveLogRQ.setLogTime(Calendar.getInstance().getTime());
		saveLogRQ.setTestItemId(context.getItemIdOfTestMethod(method));
		saveLogRQ.setLevel("ERROR");
		try {
			reportPortalService.log(saveLogRQ);
		} catch (Exception e) {
			handleException(e, LOGGER, "Unable to send message to Report Portal");
		}
	}

	/**
	 * Get the stack trace of the specified exception as a string.
	 * 
	 * @param e exception
	 * @return stack trace of the specified exception as a string
	 */
	private String getStackTraceString(Throwable e) {
		StringBuilder result = new StringBuilder();
		for (int i = 0; i < e.getStackTrace().length; i++) {
			result.append(e.getStackTrace()[i]);
			result.append(System.getProperty("line.separator"));
		}
		return result.toString();
	}

	/**
	 * Detect the type of the specified JUnit method.
	 * 
	 * @param method {@FrameworkMethod} object
	 * @return method type string; empty string for unsupported types
	 */
	private String detectMethodType(FrameworkMethod method) {
		if (null != method.getAnnotation(Test.class)) {
			return "STEP";
		} else if (null != method.getAnnotation(Before.class)) {
			return "BEFORE_METHOD";
		} else if (null != method.getAnnotation(After.class)) {
			return "AFTER_METHOD";
		} else if (null != method.getAnnotation(BeforeClass.class)) {
			return "BEFORE_CLASS";
		} else if (null != method.getAnnotation(AfterClass.class)) {
			return "AFTER_CLASS";
		}
		return "";
	}

	/**
	 * Get the test item ID for the container of the indicated test item.
	 * 
	 * @param runner JUnit test runner
	 * @return container ID for the indicated test item; {@code null} for root test items
	 */
	private String getContainerId(Object runner) {
		Object parent = LifecycleHooks.getParentOf(runner);
		// if not root object
		if (parent != null) {
			return context.getItemIdOfTestRunner(parent);
		}
		return null;
	}
}
