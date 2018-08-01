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
import com.google.common.base.Function;
import com.google.common.base.Strings;
import com.google.common.collect.Collections2;
import com.google.inject.Inject;
import org.junit.Test;
import org.junit.runner.Description;
import org.junit.runner.notification.Failure;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import javax.validation.constraints.NotNull;
import java.lang.reflect.Method;
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

	private final Logger logger = LoggerFactory.getLogger(ParallelRunningHandler.class);

	private SuitesKeeper processor;

	private ParallelRunningContext context;

	private BatchedReportPortalService reportPortalService;

	private String launchName = "test_launch";
	private Set<String> tags;
	private Mode launchRunningMode;

	private List<Class<?>> suites = new ArrayList<Class<?>>();
	private List<Class<?>> tests = new ArrayList<Class<?>>();

	@Inject
	public ParallelRunningHandler(ListenerParameters parameters, SuitesKeeper suitesKeeper, ParallelRunningContext parallelRunningContext,
			BatchedReportPortalService reportPortalService) {
		this.launchName = parameters.getLaunchName();
		this.tags = parameters.getTags();
		this.launchRunningMode = parameters.getMode();
		this.processor = suitesKeeper;
		this.context = parallelRunningContext;
		this.reportPortalService = reportPortalService;
	}

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
			handleException(e, logger, "Unable start the launch: '" + launchName + "'");
		}
	}

	@Override
	public void stopLaunch() {
		this.stopOverAll();
		if (!Strings.isNullOrEmpty(context.getLaunchId())) {
			FinishExecutionRQ finishExecutionRQ = new FinishExecutionRQ();
			finishExecutionRQ.setEndTime(Calendar.getInstance().getTime());
			try {
				reportPortalService.finishLaunch(context.getLaunchId(), finishExecutionRQ);
			} catch (Exception e) {
				handleException(e, logger, "Unable finish the launch: '" + launchName + "'");
			}
		}
	}

	@Override
	public void startTestMethod(Description description) {
		StartTestItemRQ rq = new StartTestItemRQ();
		String methodName = description.getMethodName();
		rq.setName(methodName.replaceAll("\\#.*\\#", ""));
		rq.setStartTime(Calendar.getInstance().getTime());
		rq.setType(detectMethodType(methodName));
		rq.setLaunchId(context.getLaunchId());
		String testId = context.getRunningTestId(description.getTestClass());
		EntryCreatedRS rs = null;
		try {
			rs = reportPortalService.startTestItem(testId, rq);
			String method = runningMethodName(getMethod(description));
			context.addRunningMethod(method, rs.getId());
			ReportPortalListenerContext.setRunningNowItemId(rs.getId());
		} catch (Exception e) {
			handleException(e, logger, "Unable start test method: '" + description.getMethodName() + "'");
		}
	}

	@Override
	public void stopTestMethod(Description description) {
		ReportPortalListenerContext.setRunningNowItemId(null);
		FinishTestItemRQ rq = new FinishTestItemRQ();
		rq.setEndTime(Calendar.getInstance().getTime());
		Method method = getMethod(description);
		String status = context.getStatus(method);
		rq.setStatus((status == null || status.equals("")) ? Statuses.PASSED : status);
		try {
			String methodName = runningMethodName(method);
			reportPortalService.finishTestItem(context.getRunningMethodId(methodName), rq);

			context.addFinishedMethod(description.getTestClass(), methodName);
		} catch (Exception e) {
			handleException(e, logger, "Unable finish test method: '" + context.getRunningMethodId(method.getName()) + "'");
		}
	}

	@Override
	public void markCurrentTestMethod(Description description, String status) {
		if (description.isSuite()) {
			return;
		}
		context.addStatus(getMethod(description), status);
	}

	@Override
	public void initSuiteProcessor(Description description) {
		if (null != description && null != description.getChildren()) {
			for (Description test : description.getChildren()) {
				processor.addToSuiteKeeper(test);
			}
		}
	}

	@Override
	public void startSuiteIfRequired(Description description) {
		String suiteName = processor.getSuiteName(description.getTestClass());
		String suiteId = context.getRunningSuiteId(suiteName);
		if (suiteId == null) {
			StartTestItemRQ rq = new StartTestItemRQ();
			rq.setLaunchId(context.getLaunchId());
			rq.setName(suiteName);
			rq.setType("SUITE");
			rq.setStartTime(Calendar.getInstance().getTime());
			EntryCreatedRS rs = null;
			try {
				suites.add(description.getTestClass());
				rs = reportPortalService.startRootTestItem(rq);
				context.setRunningSuiteId(suiteName, rs.getId());
			} catch (Exception e) {
				handleException(e, logger, "Unable start test suite: '" + suiteName + "'");
			}
		}
	}

	@Override
	public void stopSuiteIfRequired(Description description) {
		final String suiteName = processor.getSuiteName(description.getTestClass());
		final Class<?> test = description.getTestClass();
		final String runningId = context.getRunningTestId(test);
		if (runningId == null) {
			final String suiteId = context.getRunningSuiteId(suiteName);
			final FinishTestItemRQ rq = new FinishTestItemRQ();
			rq.setEndTime(Calendar.getInstance().getTime());
			try {
				reportPortalService.finishTestItem(suiteId, rq);
			} catch (Exception e) {
				handleException(e, logger, "Unable finish test suite: '" + suiteId + "'");
			}
		}
	}

	@Override
	public void stopTestIfRequired(Description description) {
		Class<?> test = description.getTestClass();
		Set<String> passedMethods = context.getFinishedMethods(test);
		String runningId = context.getRunningTestId(test);
		Set<Method> allTests = getTests(test.getMethods());
		Iterator<Method> iterator = allTests.iterator();
		Set<String> methodNames = new HashSet<String>();
		while (iterator.hasNext()) {
			methodNames.add(iterator.next().getName());
		}
		if (passedMethods.containsAll(methodNames)) {
			if (runningId == null) {
				FinishTestItemRQ rq = new FinishTestItemRQ();
				rq.setEndTime(Calendar.getInstance().getTime());
				try {
					reportPortalService.finishTestItem(context.getRunningTestId(test), rq);
					context.addFinishedTest(processor.getSuiteName(description.getTestClass()), description.getTestClass());
				} catch (Exception e) {
					handleException(e, logger, "Unable finish test: '" + context.getRunningTestId(test) + "'");
				}
			}
		}
	}

	@Override
	public void starTestIfRequired(Description currentTest) {
		if (context.getRunningTestId(currentTest.getTestClass()) == null) {
			StartTestItemRQ rq = new StartTestItemRQ();
			rq.setStartTime(Calendar.getInstance().getTime());
			rq.setName(currentTest.getClassName());
			rq.setType("TEST");
			rq.setLaunchId(context.getLaunchId());
			String suiteName = processor.getSuiteName(currentTest.getTestClass());
			EntryCreatedRS rs;
			try {
				tests.add(currentTest.getTestClass());
				rs = reportPortalService.startTestItem(context.getRunningSuiteId(suiteName), rq);
				context.setRunningTestId(currentTest.getTestClass(), rs.getId());
			} catch (Exception e) {
				handleException(e, logger, "Unable start test: '" + currentTest.getClassName() + "'");
			}
		}
	}

	@Override
	public void handleTestSkip(Description description) {
		if (description.isTest()) {
			return;
		}
		startSuiteIfRequired(description);
		StartTestItemRQ startRQ = new StartTestItemRQ();
		startRQ.setStartTime(Calendar.getInstance().getTime());
		startRQ.setName(description.getClassName());
		startRQ.setType("TEST");
		startRQ.setLaunchId(context.getLaunchId());
		String suiteName = processor.getSuiteName(description.getTestClass());
		try {
			EntryCreatedRS rs = reportPortalService.startTestItem(context.getRunningSuiteId(suiteName), startRQ);
			FinishTestItemRQ finishRQ = new FinishTestItemRQ();
			finishRQ.setStatus(Statuses.FAILED);
			finishRQ.setEndTime(Calendar.getInstance().getTime());
			reportPortalService.finishTestItem(rs.getId(), finishRQ);
			context.addFinishedTest(suiteName, description.getTestClass());
			stopSuiteIfRequired(description);
		} catch (Exception e) {
			handleException(e, logger, "Unable skip test: '" + description.getClassName() + "'");
		}
	}

	@Override
	public void addToFinishedMethods(Description description) {
		Method method = getMethod(description);
		if (method != null) {
			String currentMethod = runningMethodName(method);
			context.addFinishedMethod(description.getTestClass(), currentMethod);
		}
	}

	@Override
	public void clearRunningItemId() {
		ReportPortalListenerContext.setRunningNowItemId(null);
	}

	@Override
	public void sendReportPortalMsg(Failure result) {
		Method method = null;
		method = getMethod(result.getDescription());
		if (method == null) {
			return;
		}
		SaveLogRQ saveLogRQ = new SaveLogRQ();
		if (result.getException() != null) {
			saveLogRQ.setMessage(
					"Exception: " + result.getException().getMessage() + System.getProperty("line.separator") + this.getStackTraceString(
							result.getException()));
		} else {
			saveLogRQ.setMessage("Just exception (contact dev team)");
		}
		saveLogRQ.setLogTime(Calendar.getInstance().getTime());
		saveLogRQ.setTestItemId(context.getRunningMethodId(runningMethodName(method)));
		saveLogRQ.setLevel("ERROR");
		try {
			reportPortalService.log(saveLogRQ);
		} catch (Exception e1) {
			handleException(e1, logger, "Unnable to send message to Report Portal");
		}
	}

	private String getStackTraceString(Throwable e) {
		StringBuilder result = new StringBuilder();
		for (int i = 0; i < e.getStackTrace().length; i++) {
			result.append(e.getStackTrace()[i]);
			result.append(System.getProperty("line.separator"));
		}
		return result.toString();
	}

	/**
	 * get all methods annotated as test
	 *
	 * @param methods
	 * @return
	 */
	private Set<Method> getTests(Method[] methods) {
		Set<Method> tests = new HashSet<Method>();
		if (null == methods) {
			return tests;
		}

		for (Method method : methods) {
			if (method.isAnnotationPresent(Test.class)) {
				tests.add(method);
			}
		}
		return tests;
	}

	private Method getMethod(Description description) {
		Method method = null;
		try {
			List<Class<?>> listParent = this.getSuperClasses(description.getTestClass());
			method = this.getMethodFromClass(listParent, description.getMethodName().replaceAll("\\#.*\\#", ""));
		} catch (Exception e) {
			logger.error("Internal listener exception: ", e);
		}
		return method;
	}

	private List<Class<?>> getSuperClasses(Class<?> testClass) {
		ArrayList<Class<?>> results = new ArrayList<Class<?>>();
		Class<?> current = testClass;
		while (current != null) {
			results.add(current);
			current = current.getSuperclass();
		}
		return results;
	}

	private Method getMethodFromClass(List<Class<?>> supers, String method) {
		Method[] lstMethods;
		for (int i = supers.size() - 1; i >= 0; i--) {
			lstMethods = supers.get(i).getDeclaredMethods();
			for (int j = 0; j < lstMethods.length; j++) {
				if (lstMethods[j].getName().equalsIgnoreCase(method)) {
					return lstMethods[j];
				}
			}
		}
		return null;
	}

	/**
	 * Close all TEST-items and SUITE-items before LAUNCH-item finish
	 */
	private void stopOverAll() {
		// TESTS
		for (Class<?> cl : tests) {
			Set<String> finishedMethods = context.getFinishedMethods(cl);
			Collection<String> passedMethods;
			if (null != finishedMethods) {
				passedMethods = Collections2.transform(finishedMethods, new Function<String, String>() {
					@Nullable
					@Override
					public String apply(String input) {
						String[] split = input.split("#");
						return split.length == 2 ? split[0] : input;
					}
				});
			} else {
				passedMethods = Collections.emptyList();
			}

			Collection<String> allTests = Collections2.transform(getTests(cl.getMethods()), new Function<Method, String>() {
				@Nullable
				@Override
				public String apply(@NotNull Method input) {
					return input.getName();
				}
			});
			if (passedMethods.containsAll(allTests)) {
				FinishTestItemRQ rq = new FinishTestItemRQ();
				rq.setEndTime(Calendar.getInstance().getTime());
				try {
					reportPortalService.finishTestItem(context.getRunningTestId(cl), rq);
					context.addFinishedTest(processor.getSuiteName(cl), cl);
				} catch (Exception e) {
					handleException(e, logger, "Unable finish test: '" + context.getRunningTestId(cl) + "'");
				}
			}
		}
		// SUITES
		for (Class<?> s : suites) {
			String suiteName = processor.getSuiteName(s);
			String suiteId = context.getRunningSuiteId(suiteName);
			FinishTestItemRQ rq = new FinishTestItemRQ();
			rq.setEndTime(Calendar.getInstance().getTime());
			try {
				reportPortalService.finishTestItem(suiteId, rq);
			} catch (Exception e) {
				handleException(e, logger, "Unable finish test suite: '" + suiteId + "'");
			}
		}
	}

	private String detectMethodType(String methodName) {
		MethodType[] values = MethodType.values();
		MethodType methodType = null;
		for (MethodType value : values) {
			if (methodName.contains(value.getPrefix())) {
				methodType = value;
			}
		}
		return null == methodType ? "STEP" : methodType.name();
	}

	private String runningMethodName(Method method) {
		return method.getName() + "#" + Thread.currentThread().getName();
	}
}
