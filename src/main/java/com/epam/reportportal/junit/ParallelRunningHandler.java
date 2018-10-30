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

import com.epam.reportportal.annotations.UniqueID;
import com.epam.reportportal.listeners.ListenerParameters;
import com.epam.reportportal.listeners.Statuses;
import com.epam.reportportal.service.Launch;
import com.epam.reportportal.service.ReportPortal;
import com.epam.ta.reportportal.ws.model.FinishExecutionRQ;
import com.epam.ta.reportportal.ws.model.FinishTestItemRQ;
import com.epam.ta.reportportal.ws.model.ParameterResource;
import com.epam.ta.reportportal.ws.model.StartTestItemRQ;
import com.epam.ta.reportportal.ws.model.issue.Issue;
import com.epam.ta.reportportal.ws.model.launch.StartLaunchRQ;
import com.epam.ta.reportportal.ws.model.log.SaveLogRQ;
import com.google.inject.Inject;
import com.nordstrom.automation.junit.ArtifactParams;
import com.nordstrom.automation.junit.LifecycleHooks;
import com.nordstrom.automation.junit.RetriedTest;
import com.nordstrom.automation.junit.RunReflectiveCall;

import io.reactivex.Maybe;
import rp.com.google.common.annotations.VisibleForTesting;
import rp.com.google.common.base.Function;
import rp.com.google.common.base.Supplier;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.TestClass;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Objects;

import static rp.com.google.common.base.Optional.fromNullable;
import static rp.com.google.common.base.Strings.isNullOrEmpty;
import static rp.com.google.common.base.Throwables.getStackTraceAsString;

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

	private ParallelRunningContext context;
	private MemoizingSupplier<Launch> launch;

	/**
	 * Constructor: Instantiate a parallel running handler
	 * 
	 * @param suitesKeeper test collection hierarchy processor
	 * @param parallelRunningContext test execution context manager
	 * @param reportPortalService Report Portal web service client
	 */
	@Inject
	public ParallelRunningHandler(ParallelRunningContext parallelRunningContext, ReportPortal reportPortalService) {
		this.context = parallelRunningContext;
		this.launch = new MemoizingSupplier<>(() -> {
			StartLaunchRQ rq = buildStartLaunchRq(reportPortalService.getParameters());
			rq.setStartTime(Calendar.getInstance().getTime());
			return reportPortalService.newLaunch(rq);
		});
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void startLaunch() {
		launch.get().start();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void stopLaunch() {
		FinishExecutionRQ finishExecutionRQ = new FinishExecutionRQ();
		finishExecutionRQ.setEndTime(Calendar.getInstance().getTime());
		launch.get().finish(finishExecutionRQ);
		this.launch.reset();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void startRunner(Object runner, boolean isSuite) {
		TestClass testClass = LifecycleHooks.getTestClassOf(runner);
		
		StartTestItemRQ rq;
		if (isSuite) {
			rq = buildStartSuiteRq(testClass);
		} else {
			rq = buildStartTestItemRq(testClass);
		}
		Maybe<String> containerId = getContainerId(runner);
		Maybe<String> itemId;
		if (containerId == null) {
			itemId = this.launch.get().startTestItem(rq);
		} else {
			itemId = this.launch.get().startTestItem(containerId, rq);
		}
		context.setTestIdOfTestRunner(runner, itemId);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void stopRunner(Object runner) {
		FinishTestItemRQ rq = buildFinishTestRq(null);
		rq.setEndTime(Calendar.getInstance().getTime());
		launch.get().finishTestItem(context.getItemIdOfTestRunner(runner), rq);
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public void startTestMethod(FrameworkMethod method, TestClass testClass) {
		StartTestItemRQ rq = buildStartStepRq(method);
		rq.setName(method.getName());
		rq.setStartTime(Calendar.getInstance().getTime());
		rq.setType(detectMethodType(method));
		Object runner = LifecycleHooks.getRunnerFor(testClass);
		Maybe<String> itemId = launch.get().startTestItem(context.getItemIdOfTestRunner(runner), rq);
		context.setItemIdOfTestMethod(method, itemId);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void stopTestMethod(FrameworkMethod method) {
		String status = context.getStatusOfTestMethod(method);
		FinishTestItemRQ rq = buildFinishStepRq(status, method);
		rq.setEndTime(Calendar.getInstance().getTime());
		rq.setStatus((status == null || status.equals("")) ? Statuses.PASSED : status);
		launch.get().finishTestItem(context.getItemIdOfTestMethod(method), rq);
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
		StartTestItemRQ startRQ = buildStartStepRq(method);
		
		Object runner = LifecycleHooks.getRunnerFor(testClass);
		Maybe<String> itemId = launch.get().startTestItem(context.getItemIdOfTestRunner(runner), startRQ);
		
		FinishTestItemRQ finishRQ = buildFinishStepRq(Statuses.SKIPPED, method);
		launch.get().finishTestItem(itemId, finishRQ);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	@SuppressWarnings("squid:S1604")
	public void sendReportPortalMsg(FrameworkMethod method, Throwable thrown) {
		ReportPortal.emitLog(new Function<String, SaveLogRQ>() {
			@Override
			public SaveLogRQ apply(final String itemId) {
				SaveLogRQ rq = new SaveLogRQ();
				rq.setTestItemId(itemId);
				rq.setLevel("ERROR");
				rq.setLogTime(Calendar.getInstance().getTime());
				if (thrown != null) {
					rq.setMessage(getStackTraceAsString(thrown));
				} else {
					rq.setMessage("Test has failed without exception");
				}
				rq.setLogTime(Calendar.getInstance().getTime());

				return rq;
			}
		});
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
	private Maybe<String> getContainerId(Object runner) {
		Object parent = LifecycleHooks.getParentOf(runner);
		// if not root object
		if (parent != null) {
			return context.getItemIdOfTestRunner(parent);
		}
		return null;
	}

	/**
	 * Extension point to customize launch creation event/request
	 *
	 * @param parameters Launch Configuration parameters
	 * @return Request to ReportPortal
	 */
	protected StartLaunchRQ buildStartLaunchRq(ListenerParameters parameters) {
		StartLaunchRQ rq = new StartLaunchRQ();
		rq.setName(parameters.getLaunchName());
		rq.setStartTime(Calendar.getInstance().getTime());
		rq.setTags(parameters.getTags());
		rq.setMode(parameters.getLaunchRunningMode());
		if (!isNullOrEmpty(parameters.getDescription())) {
			rq.setDescription(parameters.getDescription());
		}
		return rq;
	}

	/**
	 * Extension point to customize suite creation event/request
	 *
	 * @param testClass TestNG suite
	 * @return Request to ReportPortal
	 */
	protected StartTestItemRQ buildStartSuiteRq(TestClass testClass) {
		StartTestItemRQ rq = new StartTestItemRQ();
		rq.setName(testClass.getName());
		rq.setStartTime(Calendar.getInstance().getTime());
		rq.setType("SUITE");
		return rq;
	}

	/**
	 * Extension point to customize test creation event/request
	 *
	 * @param testClass TestNG test context
	 * @return Request to ReportPortal
	 */
	protected StartTestItemRQ buildStartTestItemRq(TestClass testClass) {
		StartTestItemRQ rq = new StartTestItemRQ();
		rq.setName(testClass.getName());
		rq.setStartTime(Calendar.getInstance().getTime());
		rq.setType("TEST");
		return rq;
	}

	/**
	 * Extension point to customize test step creation event/request
	 *
	 * @param method TestNG's testResult context
	 * @return Request to ReportPortal
	 */
	protected StartTestItemRQ buildStartStepRq(FrameworkMethod method) {
		StartTestItemRQ rq = new StartTestItemRQ();
		rq.setName(method.getName());

		rq.setDescription(createStepDescription(method));
		rq.setParameters(createStepParameters(method));
		rq.setUniqueId(extractUniqueID(method));
		rq.setStartTime(Calendar.getInstance().getTime());
		rq.setType(detectMethodType(method));

		rq.setRetry(isRetry(method));
		return rq;
	}

	/**
	 * Extension point to customize test suite on it's finish
	 *
	 * @param testClass TestNG's suite context
	 * @return Request to ReportPortal
	 */
	protected FinishTestItemRQ buildFinishSuiteRq(TestClass testClass) {
		FinishTestItemRQ rq = new FinishTestItemRQ();
		rq.setEndTime(Calendar.getInstance().getTime());
		return rq;
	}

	/**
	 * Extension point to customize test on it's finish
	 *
	 * @param testClass TestNG test context
	 * @return Request to ReportPortal
	 */
	@SuppressWarnings("squid:S4144")
	protected FinishTestItemRQ buildFinishTestRq(TestClass testClass) {
		FinishTestItemRQ rq = new FinishTestItemRQ();
		rq.setEndTime(Calendar.getInstance().getTime());
		return rq;
	}

	/**
	 * Extension point to customize test method on it's finish
	 *
	 * @param method TestNG's testResult context
	 * @return Request to ReportPortal
	 */
	protected FinishTestItemRQ buildFinishStepRq(String status, FrameworkMethod method) {
		FinishTestItemRQ rq = new FinishTestItemRQ();
		rq.setEndTime(Calendar.getInstance().getTime());
		rq.setStatus(status);
		// Allows indicate that SKIPPED is not to investigate items for WS
		if (Statuses.SKIPPED.equals(status) && !fromNullable(launch.get().getParameters().getSkippedAnIssue()).or(false)) {
			Issue issue = new Issue();
			issue.setIssueType("NOT_ISSUE");
			rq.setIssue(issue);
		}
		return rq;
	}
	
	/**
	 * Extension point to customize Report Portal test parameters
	 *
	 * @param method TestNG's testResult context
	 * @return Test/Step Parameters being sent to Report Portal
	 */
	protected List<ParameterResource> createStepParameters(FrameworkMethod method) {
		List<ParameterResource> parameters = createMethodParameters(method);
		return parameters.isEmpty() ? null : parameters;
	}

	/**
	 * Processes testResult to create parameters provided
	 * by {@link org.testng.annotations.DataProvider} If parameter key isn't provided
	 * by {@link ParameterKey} annotation then it will be 'arg[index]'
	 *
	 * @param testResult TestNG's testResult context
	 * @return Step Parameters being sent to ReportPortal
	 */

	private List<ParameterResource> createMethodParameters(FrameworkMethod method) {
		List<ParameterResource> result = new ArrayList<>();
		Object target = RunReflectiveCall.getTargetFor(method);
		if (target instanceof ArtifactParams) {
			Object[] values = ((ArtifactParams) target).getParameters();
			for (int i = 0; i < values.length; i++) {
				ParameterResource parameter = new ParameterResource();
				parameter.setKey("arg" + i);
				parameter.setValue(Objects.toString(values[i], null));
				result.add(parameter);
			}
		}
		return result;
	}

	/**
	 * Extension point to customize test step description
	 *
	 * @param method TestNG's testResult context
	 * @return Test/Step Description being sent to ReportPortal
	 */
	protected String createStepDescription(FrameworkMethod method) {
		return method.getName();
	}

	/**
	 * Returns test item ID from annotation if it provided.
	 *
	 * @param method Where to find
	 * @return test item ID or null
	 */
	private String extractUniqueID(FrameworkMethod method) {
		UniqueID itemUniqueID = method.getAnnotation(UniqueID.class);
		return itemUniqueID != null ? itemUniqueID.value() : null;
	}

	/**
	 * Calculate parent id for configuration
	 */
	@VisibleForTesting
	Maybe<String> getConfigParent(FrameworkMethod method) {
		TestClass testClass = LifecycleHooks.getTestClassWith(method);
		Object runner = LifecycleHooks.getRunnerFor(testClass);
		return context.getItemIdOfTestRunner(runner);
	}

	private boolean isRetry(FrameworkMethod method) {
		return (null != method.getAnnotation(RetriedTest.class));
	}

	@VisibleForTesting
	static class MemoizingSupplier<T> implements Supplier<T>, Serializable {
		final Supplier<T> delegate;
		transient volatile boolean initialized;
		transient T value;
		private static final long serialVersionUID = 0L;

		MemoizingSupplier(Supplier<T> delegate) {
			this.delegate = delegate;
		}

		public T get() {
			if (!this.initialized) {
				synchronized (this) {
					if (!this.initialized) {
						T t = this.delegate.get();
						this.value = t;
						this.initialized = true;
						return t;
					}
				}
			}

			return this.value;
		}

		public synchronized void reset() {
			this.initialized = false;
		}

		public String toString() {
			return "Suppliers.memoize(" + this.delegate + ")";
		}
	}
}
