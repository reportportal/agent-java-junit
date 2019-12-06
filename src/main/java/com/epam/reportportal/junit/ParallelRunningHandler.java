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

import com.epam.reportportal.annotations.Tags;
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
import io.reactivex.Maybe;
import org.junit.*;
import org.junit.experimental.categories.Category;
import org.junit.runners.Suite;
import org.junit.runners.model.Annotatable;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.TestClass;
import rp.com.google.common.annotations.VisibleForTesting;
import rp.com.google.common.base.Function;
import rp.com.google.common.base.Optional;
import rp.com.google.common.base.Supplier;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

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
	 * @param parallelRunningContext test execution context manager
	 * @param reportPortalService    Report Portal web service client
	 */
	@Inject
	public ParallelRunningHandler(final ParallelRunningContext parallelRunningContext, final ReportPortal reportPortalService) {

		context = parallelRunningContext;
		launch = new MemoizingSupplier<>(() -> {
			StartLaunchRQ rq = buildStartLaunchRq(reportPortalService.getParameters());
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
		launch.reset();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void startRunner(Object runner, boolean isSuite) {
		StartTestItemRQ rq;
		if (isSuite) {
			rq = buildStartSuiteRq(runner);
		} else {
			rq = buildStartTestItemRq(runner);
		}
		Maybe<String> containerId = getContainerId(runner);
		Maybe<String> itemId;
		if (containerId == null) {
			itemId = launch.get().startTestItem(rq);
		} else {
			itemId = launch.get().startTestItem(containerId, rq);
		}
		context.setTestIdOfTestRunner(runner, itemId);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void stopRunner(Object runner) {
		FinishTestItemRQ rq = buildFinishTestRq(null);
		launch.get().finishTestItem(context.getItemIdOfTestRunner(runner), rq);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void startTestMethod(FrameworkMethod method, Object runner) {
		StartTestItemRQ rq = buildStartStepRq(method);
		rq.setParameters(createStepParameters(method, runner));
		Maybe<String> itemId = launch.get().startTestItem(context.getItemIdOfTestRunner(runner), rq);
		context.setItemIdOfTestMethod(method, runner, itemId);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void stopTestMethod(FrameworkMethod method, Object runner) {
		String status = context.getStatusOfTestMethod(method, runner);
		FinishTestItemRQ rq = buildFinishStepRq(method, status);
		launch.get().finishTestItem(context.getItemIdOfTestMethod(method, runner), rq);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void markCurrentTestMethod(FrameworkMethod method, Object runner, String status) {
		context.setStatusOfTestMethod(method, runner, status);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void handleTestSkip(FrameworkMethod method, Object runner) {
		StartTestItemRQ startRQ = buildStartStepRq(method);
		Maybe<String> itemId = launch.get().startTestItem(context.getItemIdOfTestRunner(runner), startRQ);
		FinishTestItemRQ finishRQ = buildFinishStepRq(method, Statuses.SKIPPED);
		launch.get().finishTestItem(itemId, finishRQ);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void sendReportPortalMsg(final FrameworkMethod method, Object runner, final Throwable thrown) {
		Function<String, SaveLogRQ> function = itemId -> {
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
		};
		ReportPortal.emitLog(function);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean isReportable(FrameworkMethod method) {
		return !detectMethodType(method).isEmpty();
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
	 * @param runner JUnit suite context
	 * @return Request to ReportPortal
	 */
	protected StartTestItemRQ buildStartSuiteRq(Object runner) {
		StartTestItemRQ rq = new StartTestItemRQ();
		rq.setName(getName(runner));
		rq.setStartTime(Calendar.getInstance().getTime());
		rq.setType("SUITE");
		return rq;
	}

	/**
	 * Extension point to customize test creation event/request
	 *
	 * @param runner JUnit test context
	 * @return Request to ReportPortal
	 */
	protected StartTestItemRQ buildStartTestItemRq(Object runner) {
		StartTestItemRQ rq = new StartTestItemRQ();
		rq.setName(getName(runner));
		rq.setStartTime(Calendar.getInstance().getTime());
		rq.setType("TEST");
		rq.setTags(getAnnotationTags(LifecycleHooks.getTestClassOf(runner)));
		return rq;
	}

	/**
	 * Extension point to customize test step creation event/request
	 *
	 * @param method JUnit framework method context
	 * @return Request to ReportPortal
	 */
	protected StartTestItemRQ buildStartStepRq(FrameworkMethod method) {
		StartTestItemRQ rq = new StartTestItemRQ();
		rq.setName(method.getName());

		rq.setDescription(createStepDescription(method));
		rq.setUniqueId(extractUniqueID(method));
		rq.setStartTime(Calendar.getInstance().getTime());
		rq.setType(detectMethodType(method));
		rq.setTags(getAnnotationTags(method));

		rq.setRetry(isRetry(method));
		return rq;
	}

	/**
	 * Returns a set of tags of the given annotated context. This method is looking for the following annotations in
	 * this context:
	 *
	 * <ul>
	 * <li>{@link Tags}</li>
	 * <li>{@link Category}</li>
	 * </ul>
	 * <p>
	 * If annotations are not present - this method returns empty set.
	 *
	 * @param context Context to extract tags
	 * @return Set of tags of given annotated element
	 */
	@VisibleForTesting
	static Set<String> getAnnotationTags(Annotatable context) {
		Set<String> result = new HashSet<>();
		
		result.addAll(Optional.fromNullable(context.getAnnotation(Category.class))
				.transform(toCategoryNames()).or(Collections.emptySet()));

		result.addAll(Optional.fromNullable(context.getAnnotation(Tags.class))
				.transform(toTagNames()).or(Collections.emptySet()));

		return result;
	}
	
	private static Function<Category, Set<String>> toCategoryNames() {
		return new Function<Category, Set<String>>() {
			@Override
			public Set<String> apply(Category category) {
				return Arrays.stream(category.value()).map(Class::getSimpleName).collect(Collectors.toSet());
			}
		};
	}

	private static Function<Tags, Set<String>> toTagNames() {
		return new Function<Tags, Set<String>>() {
			@Override
			public Set<String> apply(Tags tags) {
				return Arrays.stream(tags.value()).collect(Collectors.toSet());
			}
		};
	}

	/**
	 * Extension point to customize test suite on it's finish
	 *
	 * @param testClass JUnit suite context
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
	 * @param testClass JUnit test context
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
	 * @param method JUnit framework method context
	 * @param status method completion status
	 * @return Request to ReportPortal
	 */
	protected FinishTestItemRQ buildFinishStepRq(FrameworkMethod method, String status) {
		FinishTestItemRQ rq = new FinishTestItemRQ();
		rq.setEndTime(Calendar.getInstance().getTime());
		rq.setStatus((status == null || status.equals("")) ? Statuses.PASSED : status);
		// Allows indicate that SKIPPED is not to investigate items for WS
		if (Statuses.SKIPPED.equals(status) && !Optional.fromNullable(launch.get().getParameters().getSkippedAnIssue()).or(false)) {
			Issue issue = new Issue();
			issue.setIssueType("NOT_ISSUE");
			rq.setIssue(issue);
		}
		return rq;
	}

	/**
	 * Extension point to customize Report Portal test parameters
	 *
	 * @param method JUnit framework method context
	 * @param runner JUnit test runner context
	 * @return Test/Step Parameters being sent to Report Portal
	 */
	protected List<ParameterResource> createStepParameters(FrameworkMethod method, Object runner) {
		List<ParameterResource> parameters = createMethodParameters(method, runner);
		return parameters.isEmpty() ? null : parameters;
	}

	/**
	 * Assemble execution parameters list for the specified framework method.
	 * <p>
	 * <b>NOTE</b>: To support publication of execution parameters, the client test class must implement the
	 * {@link com.nordstrom.automation.junit.ArtifactParams ArtifactParameters} interface.
	 *
	 * @param method JUnit framework method context
	 * @param runner JUnit test runner context
	 * @return Step Parameters being sent to ReportPortal
	 */
	@SuppressWarnings("squid:S3655")
	private List<ParameterResource> createMethodParameters(FrameworkMethod method, Object runner) {
		List<ParameterResource> result = new ArrayList<>();
		if (!(method.isStatic() || isIgnored(method))) {
			Object target = LifecycleHooks.getTargetForRunner(runner);
			if (target instanceof ArtifactParams) {
				com.google.common.base.Optional<Map<String, Object>> params = ((ArtifactParams) target).getParameters();
				if (params.isPresent()) {
					for (Entry<String, Object> param : params.get().entrySet()) {
						ParameterResource parameter = new ParameterResource();
						parameter.setKey(param.getKey());
						parameter.setValue(Objects.toString(param.getValue(), null));
						result.add(parameter);
					}
				}
			}
		}
		return result;
	}

	/**
	 * Extension point to customize test step description
	 *
	 * @param method JUnit framework method context
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
	 * Determine if the specified JUnit framework method is being ignored.
	 *
	 * @param method JUnit framework method context
	 * @return {@code true} if specified method is being ignored; otherwise {@code false}
	 */
	private boolean isIgnored(FrameworkMethod method) {
		return (null != method.getAnnotation(Ignore.class));
	}

	/**
	 * Determine if the specified JUnit framework method is being retried.
	 *
	 * @param method JUnit framework method context
	 * @return {@code true} if specified method is being retried; otherwise {@code false}
	 */
	private boolean isRetry(FrameworkMethod method) {
		return (null != method.getAnnotation(RetriedTest.class));
	}

	/**
	 * Get name associated with the specified JUnit runner.
	 *
	 * @param runner JUnit test runner
	 * @return name for runner
	 */
	private String getName(Object runner) {
		String name;
		TestClass testClass = LifecycleHooks.getTestClassOf(runner);
		Class<?> javaClass = testClass.getJavaClass();
		if (javaClass != null) {
			name = javaClass.getName();
		} else {
			String role = (null == LifecycleHooks.getParentOf(runner)) ? "Root " : "Context ";
			String type = (runner instanceof Suite) ? "Suite" : "Class";
			name = role + type + " Runner";
		}
		return name;
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
			if (!initialized) {
				synchronized (this) {
					if (!initialized) {
						T t = delegate.get();
						value = t;
						initialized = true;
						return t;
					}
				}
			}

			return value;
		}

		public synchronized void reset() {
			initialized = false;
		}

		public String toString() {
			return "Suppliers.memoize(" + delegate + ")";
		}
	}
}
