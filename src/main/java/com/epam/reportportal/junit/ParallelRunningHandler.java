/*
 * Copyright 2020 EPAM Systems
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

import com.epam.reportportal.annotations.TestCaseId;
import com.epam.reportportal.annotations.attribute.Attributes;
import com.epam.reportportal.junit.utils.ItemTreeUtils;
import com.epam.reportportal.junit.utils.SystemAttributesFetcher;
import com.epam.reportportal.listeners.ItemStatus;
import com.epam.reportportal.listeners.ItemType;
import com.epam.reportportal.listeners.ListenerParameters;
import com.epam.reportportal.service.Launch;
import com.epam.reportportal.service.ReportPortal;
import com.epam.reportportal.service.item.TestCaseIdEntry;
import com.epam.reportportal.service.tree.TestItemTree;
import com.epam.reportportal.utils.AttributeParser;
import com.epam.reportportal.utils.ParameterUtils;
import com.epam.reportportal.utils.TestCaseIdUtils;
import com.epam.reportportal.utils.reflect.Accessible;
import com.epam.ta.reportportal.ws.model.*;
import com.epam.ta.reportportal.ws.model.attribute.ItemAttributesRQ;
import com.epam.ta.reportportal.ws.model.issue.Issue;
import com.epam.ta.reportportal.ws.model.launch.StartLaunchRQ;
import com.epam.ta.reportportal.ws.model.log.SaveLogRQ;
import com.nordstrom.automation.junit.*;
import io.reactivex.Maybe;
import org.junit.*;
import org.junit.internal.runners.model.ReflectiveCallable;
import org.junit.runner.Description;
import org.junit.runners.Suite;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.TestClass;
import org.junit.runners.parameterized.BlockJUnit4ClassRunnerWithParameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rp.com.google.common.annotations.VisibleForTesting;
import rp.com.google.common.base.Function;
import rp.com.google.common.base.Supplier;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.*;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import static com.epam.reportportal.junit.utils.ItemTreeUtils.createItemTreeKey;
import static java.util.Optional.ofNullable;
import static rp.com.google.common.base.Strings.isNullOrEmpty;
import static rp.com.google.common.base.Throwables.getStackTraceAsString;

/**
 * MultiThread realization of IListenerHandler. This realization support
 * parallel running of tests and test methods. Main constraint: All test classes
 * in current launch should be unique. (User shouldn't run the same classes
 * twice/or more times in the one launch)
 *
 * @author Aliaksey_Makayed (modified by Andrei_Ramanchuk)
 */
public class ParallelRunningHandler implements IListenerHandler {

	private static final Logger LOGGER = LoggerFactory.getLogger(ParallelRunningHandler.class);

	private static final String FINISH_REQUEST = "FINISH_REQUEST";
	private static final String START_TIME = "START_TIME";
	private static final String IS_RETRY = "IS_RETRY";
	private static volatile ReportPortal REPORT_PORTAL = ReportPortal.builder().build();

	private final ParallelRunningContext context;
	private final MemoizingSupplier<Launch> launch;

	/**
	 * Constructor: Instantiate a parallel running handler
	 *
	 * @param parallelRunningContext test execution context manager
	 */
	public ParallelRunningHandler(final ParallelRunningContext parallelRunningContext) {
		context = parallelRunningContext;
		launch = createLaunch();
	}

	protected MemoizingSupplier<Launch> createLaunch() {
		return new MemoizingSupplier<>(() -> {
			final ReportPortal reportPortal = getReportPortal();
			StartLaunchRQ rq = buildStartLaunchRq(reportPortal.getParameters());
			return reportPortal.newLaunch(rq);
		});
	}

	public static ReportPortal getReportPortal() {
		return REPORT_PORTAL;
	}

	public static void setReportPortal(ReportPortal reportPortal) {
		REPORT_PORTAL = reportPortal;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void startLaunch() {
		Maybe<String> launchId = launch.get().start();
		context.getItemTree().setLaunchId(launchId);
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

	private List<Object> getRunnerChain(Object runner) {
		List<Object> chain = new ArrayList<>();
		chain.add(runner);
		Object parent;
		Object current = runner;
		while ((parent = LifecycleHooks.getParentOf(current)) != null) {
			if (!getRunnerName(current).equals(getRunnerName(parent))) {
				// skip duplicated runners in parameterized tests
				chain.add(parent);
			}
			current = parent;
		}
		Collections.reverse(chain);
		return chain;
	}

	@Nullable
	private TestItemTree.TestItemLeaf retrieveLeaf(Object runner) {
		List<Object> chain = getRunnerChain(runner);
		Launch myLaunch = launch.get();
		TestItemTree.TestItemLeaf leaf = null;
		Map<TestItemTree.ItemTreeKey, TestItemTree.TestItemLeaf> children = context.getItemTree().getTestItems();
		long currentDate = Calendar.getInstance().getTimeInMillis();
		for (Object r : chain) {
			StartTestItemRQ rq = buildStartSuiteRq(r, new Date(currentDate++));
			Maybe<String> parentId = ofNullable(leaf).map(TestItemTree.TestItemLeaf::getItemId).orElse(null);
			leaf = children.computeIfAbsent(TestItemTree.ItemTreeKey.of(rq.getName()), (k) -> {
				TestItemTree.TestItemLeaf l = ofNullable(parentId).map(p -> TestItemTree.createTestItemLeaf(p,
						myLaunch.startTestItem(p, rq)
				)).orElseGet(() -> TestItemTree.createTestItemLeaf(myLaunch.startTestItem(rq)));
				l.setType(ItemType.SUITE);
				l.setAttribute(START_TIME, rq.getStartTime());

				return l;
			});
			children = leaf.getChildItems();
		}
		return leaf;
	}

	@Nullable
	private TestItemTree.TestItemLeaf getLeaf(Object runner) {
		List<Object> chain = getRunnerChain(runner);
		TestItemTree.TestItemLeaf leaf = null;
		Map<TestItemTree.ItemTreeKey, TestItemTree.TestItemLeaf> children = context.getItemTree().getTestItems();
		for (Object r : chain) {
			leaf = children.get(TestItemTree.ItemTreeKey.of(getRunnerName(r)));
			if (leaf != null) {
				children = leaf.getChildItems();
			}
		}
		return leaf;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void startRunner(Object runner) {
	}

	@Nullable
	private ItemStatus evaluateStatus(@Nullable ItemStatus currentStatus, @Nullable ItemStatus childStatus) {
		if (childStatus == null) {
			return currentStatus;
		}
		ItemStatus status = ofNullable(currentStatus).orElse(ItemStatus.PASSED);
		switch (childStatus) {
			case PASSED:
			case SKIPPED:
			case STOPPED:
			case INFO:
			case WARN:
				return status;
			case CANCELLED:
				switch (status) {
					case PASSED:
					case SKIPPED:
					case STOPPED:
					case INFO:
					case WARN:
						return ItemStatus.CANCELLED;
					default:
						return currentStatus;
				}
			case INTERRUPTED:
				switch (status) {
					case PASSED:
					case SKIPPED:
					case STOPPED:
					case INFO:
					case WARN:
					case CANCELLED:
						return ItemStatus.INTERRUPTED;
					default:
						return currentStatus;
				}
			default:
				return childStatus;
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void stopRunner(Object runner) {
		FinishTestItemRQ rq = buildFinishSuiteRq(LifecycleHooks.getTestClassOf(runner));
		Launch myLaunch = launch.get();
		ofNullable(getLeaf(runner)).ifPresent(l -> {
			l.setAttribute(FINISH_REQUEST, rq);
			ItemStatus status = l.getStatus();
			for (Map.Entry<TestItemTree.ItemTreeKey, TestItemTree.TestItemLeaf> entry : l.getChildItems().entrySet()) {
				TestItemTree.TestItemLeaf value = entry.getValue();
				if (value.getType() != ItemType.SUITE) {
					continue;
				}
				ofNullable(value.getAttribute(FINISH_REQUEST)).ifPresent(r -> myLaunch.finishTestItem(value.getItemId(),
						(FinishTestItemRQ) value.clearAttribute(FINISH_REQUEST)
				));
				status = evaluateStatus(status, value.getStatus());
			}
			l.setStatus(status);
			if (l.getParentId() == null) {
				rq.setStatus(ofNullable(status).map(Enum::name).orElse(null));
				myLaunch.finishTestItem(l.getItemId(), rq);
			}
		});
	}

	@Nonnull
	protected Date getDateForChild(@Nullable TestItemTree.TestItemLeaf leaf) {
		return ofNullable(leaf).map(l -> l.<Date>getAttribute(START_TIME)).map(d -> {
			Date currentDate = Calendar.getInstance().getTime();
			if (currentDate.compareTo(d) > 0) {
				return currentDate;
			} else {
				return new Date(d.getTime() + 1);
			}
		}).orElseGet(() -> Calendar.getInstance().getTime());
	}

	@Override
	public void startTest(AtomicTest<FrameworkMethod> testContext) {
		context.setTestMethodDescription(testContext.getIdentity(), testContext.getDescription());
	}

	@Override
	public void finishTest(AtomicTest<FrameworkMethod> testContext) {
	}

	protected void startTestStepItem(Object runner, FrameworkMethod method) {
		TestItemTree.ItemTreeKey myParentKey = createItemTreeKey(getRunnerName(runner));
		TestItemTree.TestItemLeaf testLeaf = ofNullable(retrieveLeaf(runner)).orElseGet(() -> context.getItemTree()
				.getTestItems()
				.get(myParentKey));
		ofNullable(testLeaf).ifPresent(l -> {
			Maybe<String> parentId = l.getItemId();
			StartTestItemRQ rq = buildStartStepRq(runner, context.getTestMethodDescription(method), method, getDateForChild(l));
			TestItemTree.ItemTreeKey myKey = createItemTreeKey(method, rq.getParameters());
			ofNullable(l.getChildItems().remove(myKey)).map(ol -> ol.<Boolean>getAttribute(IS_RETRY)).ifPresent(r -> {
				if (r) {
					rq.setRetry(true);
				}
			});
			Maybe<String> itemId = launch.get().startTestItem(parentId, rq);
			TestItemTree.TestItemLeaf myLeaf = TestItemTree.createTestItemLeaf(parentId, itemId);
			myLeaf.setType(ItemType.STEP);
			l.getChildItems().put(myKey, myLeaf);
			if (getReportPortal().getParameters().isCallbackReportingEnabled()) {
				context.getItemTree().getTestItems().put(myKey, myLeaf);
			}
		});
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void startTestMethod(Object runner, FrameworkMethod method, ReflectiveCallable callable) {
		startTestStepItem(runner, method);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void stopTestMethod(Object runner, FrameworkMethod method, ReflectiveCallable callable) {
		ItemStatus status = context.getStatusOfTestMethod(callable);
		FinishTestItemRQ rq = buildFinishStepRq(method, ofNullable(status).orElse(ItemStatus.PASSED));
		stopTestMethod(runner, method, rq);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void stopTestMethod(Object runner, FrameworkMethod method, FinishTestItemRQ rq) {
		TestItemTree.ItemTreeKey myParentKey = createItemTreeKey(getRunnerName(runner));
		TestItemTree.TestItemLeaf testLeaf = ofNullable(getLeaf(runner)).orElseGet(() -> context.getItemTree()
				.getTestItems()
				.get(myParentKey));
		List<ParameterResource> parameters = createStepParameters(method, runner);
		TestItemTree.ItemTreeKey myKey = createItemTreeKey(method, createStepParameters(method, runner));
		ofNullable(testLeaf).map(l -> l.getChildItems().get(myKey)).ifPresent(l -> {
			Maybe<String> itemId = l.getItemId();
			l.setStatus(ItemStatus.valueOf(rq.getStatus()));
			Maybe<OperationCompletionRS> finishResponse = launch.get().finishTestItem(itemId, rq);
			if (getReportPortal().getParameters().isCallbackReportingEnabled()) {
				updateTestItemTree(method, parameters, finishResponse);
			}
		});
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	@Deprecated
	public void markCurrentTestMethod(ReflectiveCallable callable, String status) {
		markCurrentTestMethod(callable, ItemStatus.valueOf(status));
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void markCurrentTestMethod(ReflectiveCallable callable, ItemStatus status) {
		context.setStatusOfTestMethod(callable, status);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void handleTestSkip(AtomicTest<FrameworkMethod> testContext) {
		Object runner = testContext.getRunner();
		FrameworkMethod method = testContext.getIdentity();
		TestItemTree.ItemTreeKey myParentKey = createItemTreeKey(getRunnerName(runner));
		TestItemTree.TestItemLeaf testLeaf = ofNullable(retrieveLeaf(runner)).orElseGet(() -> context.getItemTree()
				.getTestItems()
				.get(myParentKey));
		List<ParameterResource> parameters = createStepParameters(testContext.getIdentity(), testContext.getRunner());
		TestItemTree.ItemTreeKey myKey = createItemTreeKey(method, parameters);

		ofNullable(testLeaf).ifPresent(p -> {
			TestItemTree.TestItemLeaf myLeaf = ofNullable(p.getChildItems().get(myKey)).orElse(null);
			if (myLeaf == null) {
				// a test method wasn't started, most likely an ignored test: start and stop a test item with 'skipped' status
				startTest(testContext);
				Object target = getTargetForRunner(runner);
				startTestStepItem(runner, method);
				ReflectiveCallable callable = LifecycleHooks.encloseCallable(method.getMethod(), target);
				markCurrentTestMethod(callable, ItemStatus.SKIPPED);
				stopTestMethod(runner, method, callable);
			} else {
				// a test method started
				FinishTestItemRQ rq;
				if (testContext.getDescription().getAnnotation(RetriedTest.class) != null) {
					// a retry, send an item update with retry flag
					rq = buildFinishStepRq(method, myLeaf.getStatus());
					rq.setRetry(true);
					myLeaf.setAttribute(IS_RETRY, true);
				} else {
					rq = buildFinishStepRq(method, ItemStatus.SKIPPED);
					myLeaf.setStatus(ItemStatus.SKIPPED);
				}
				stopTestMethod(runner, method, rq);
			}
		});
	}

	private void updateTestItemTree(FrameworkMethod method, List<ParameterResource> parameters,
			Maybe<OperationCompletionRS> finishResponse) {
		TestItemTree.TestItemLeaf testItemLeaf = ItemTreeUtils.retrieveLeaf(method, parameters, context.getItemTree());
		if (testItemLeaf != null) {
			testItemLeaf.setFinishResponse(finishResponse);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void sendReportPortalMsg(ReflectiveCallable callable, final Throwable thrown) {
		Function<String, SaveLogRQ> function = itemUuid -> {
			SaveLogRQ rq = new SaveLogRQ();
			rq.setItemUuid(itemUuid);
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
		return MethodType.detect(method) != null;
	}

	private enum MethodType {
		STEP(Test.class),
		BEFORE_METHOD(Before.class),
		AFTER_METHOD(After.class),
		BEFORE_CLASS(BeforeClass.class),
		AFTER_CLASS(AfterClass.class);

		private final Class<? extends Annotation> clazz;

		MethodType(Class<? extends Annotation> markerAnnotation) {
			clazz = markerAnnotation;
		}

		/**
		 * Detect the type of the specified JUnit method.
		 *
		 * @param method {@link FrameworkMethod} object
		 * @return an instance of this or null
		 */
		static MethodType detect(FrameworkMethod method) {
			for (MethodType type : values()) {
				if (null != method.getAnnotation(type.clazz)) {
					return type;
				}
			}
			return null;
		}
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
		rq.setAttributes(parameters.getAttributes());
		rq.setMode(parameters.getLaunchRunningMode());
		rq.getAttributes().addAll(SystemAttributesFetcher.collectSystemAttributes(parameters.getSkippedAnIssue()));

		rq.setRerun(parameters.isRerun());
		if (!isNullOrEmpty(parameters.getRerunOf())) {
			rq.setRerunOf(parameters.getRerunOf());
		}

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
	@SuppressWarnings("unused")
	protected StartTestItemRQ buildStartSuiteRq(Object runner) {
		return buildStartSuiteRq(runner, Calendar.getInstance().getTime());
	}

	/**
	 * Extension point to customize suite creation event/request
	 *
	 * @param runner    JUnit suite context
	 * @param startTime a suite start time which will be passed to RP
	 * @return Request to ReportPortal
	 */
	protected StartTestItemRQ buildStartSuiteRq(Object runner, Date startTime) {
		StartTestItemRQ rq = new StartTestItemRQ();
		rq.setName(getRunnerName(runner));
		rq.setCodeRef(getCodeRef(runner));
		rq.setStartTime(startTime);
		rq.setType("SUITE");
		return rq;
	}

	/**
	 * Extension point to customize test creation event/request
	 *
	 * @param test JUnit test context
	 * @return Request to ReportPortal
	 */
	@SuppressWarnings("unused")
	protected StartTestItemRQ buildStartTestItemRq(AtomicTest<FrameworkMethod> test) {
		return buildStartTestItemRq(test, Calendar.getInstance().getTime());
	}

	/**
	 * Extension point to customize test creation event/request
	 *
	 * @param test      JUnit test context
	 * @param startTime a suite start time which will be passed to RP
	 * @return Request to ReportPortal
	 */
	protected StartTestItemRQ buildStartTestItemRq(AtomicTest<FrameworkMethod> test, Date startTime) {
		StartTestItemRQ rq = new StartTestItemRQ();
		rq.setName(test.getDescription().getDisplayName());
		rq.setCodeRef(getCodeRef(test.getRunner()));
		rq.setStartTime(startTime);
		rq.setType("TEST");
		return rq;
	}

	/**
	 * Extension point to customize test step creation event/request
	 *
	 * @param runner      JUnit test runner context
	 * @param description JUnit framework test description object
	 * @param method      JUnit framework method context
	 * @param startTime   A test step start date and time
	 * @return Request to ReportPortal
	 */
	protected StartTestItemRQ buildStartStepRq(Object runner, Description description, FrameworkMethod method, Date startTime) {
		StartTestItemRQ rq = new StartTestItemRQ();
		rq.setName(method.getName());
		rq.setCodeRef(getCodeRef(method));
		rq.setAttributes(getAttributes(method));
		rq.setDescription(createStepDescription(description, method));
		rq.setParameters(createStepParameters(method, runner));
		rq.setTestCaseId(ofNullable(getTestCaseId(
				method,
				rq.getCodeRef(),
				ofNullable(rq.getParameters()).map(p -> p.stream().map(ParameterResource::getValue).collect(Collectors.toList()))
						.orElse(null)
		)).map(TestCaseIdEntry::getId).orElse(null));
		rq.setStartTime(startTime);
		MethodType type = MethodType.detect(method);
		rq.setType(type == null ? "" : type.name());
		return rq;
	}

	/**
	 * Extension point to customize test suite on it's finish
	 *
	 * @param testClass JUnit suite context
	 * @return Request to ReportPortal
	 */
	@SuppressWarnings("unused")
	protected FinishTestItemRQ buildFinishSuiteRq(TestClass testClass) {
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
	 * @deprecated use {@link #buildFinishStepRq(FrameworkMethod, ItemStatus)}
	 */
	@Deprecated
	protected FinishTestItemRQ buildFinishStepRq(@Nullable FrameworkMethod method, @Nonnull String status) {
		return buildFinishStepRq(method, ItemStatus.valueOf(status));
	}

	/**
	 * Extension point to customize test method on it's finish
	 *
	 * @param method JUnit framework method context
	 * @param status method completion status
	 * @return Request to ReportPortal
	 */
	@SuppressWarnings("unused")
	protected FinishTestItemRQ buildFinishStepRq(@Nullable FrameworkMethod method, @Nonnull ItemStatus status) {
		FinishTestItemRQ rq = new FinishTestItemRQ();
		rq.setEndTime(Calendar.getInstance().getTime());
		rq.setStatus(status.name());
		// Allows indicate that SKIPPED is not to investigate items for WS
		if (ItemStatus.SKIPPED == status && !ofNullable(launch.get().getParameters().getSkippedAnIssue()).orElse(false)) {
			Issue issue = new Issue();
			issue.setIssueType("NOT_ISSUE");
			rq.setIssue(issue);
		}
		return rq;
	}

	protected <T> TestCaseIdEntry getTestCaseId(FrameworkMethod frameworkMethod, String codeRef, List<T> params) {
		Method method = frameworkMethod.getMethod();
		return TestCaseIdUtils.getTestCaseId(method.getAnnotation(TestCaseId.class), method, codeRef, params);
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
		if (!(method.isStatic())) {
			Object target = getTargetForRunner(runner);
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
			} else if (runner instanceof BlockJUnit4ClassRunnerWithParameters) {
				try {
					Optional<Constructor<?>> constructor = Arrays.stream(method.getDeclaringClass().getConstructors()).findFirst();
					if (constructor.isPresent()) {
						result.addAll(ParameterUtils.getParameters(constructor.get(),
								Arrays.asList((Object[]) Accessible.on(runner).field("parameters").getValue())
						));
					}
				} catch (NoSuchFieldException e) {
					LOGGER.warn("Unable to get parameters for parameterized runner", e);
				}

			}
		}
		return result;
	}

	/**
	 * Get the JUnit test class instance for the specified class runner.
	 * <p>
	 * <b>NOTE</b>: This shim enables subclasses of this handler to supply custom instances.
	 *
	 * @param runner JUnit class runner
	 * @return JUnit test class instance for specified runner
	 */
	protected Object getTargetForRunner(Object runner) {
		return LifecycleHooks.getTargetForRunner(runner);
	}

	/**
	 * Extension point to customize test step description
	 *
	 * @param description JUnit framework test description object
	 * @param method      JUnit framework method context
	 * @return Test/Step Description being sent to ReportPortal
	 */
	protected String createStepDescription(Description description, FrameworkMethod method) {
		DisplayName itemDisplayName = method.getAnnotation(DisplayName.class);
		return (itemDisplayName != null) ? itemDisplayName.value() : description.getDisplayName();
	}

	/**
	 * Get name associated with the specified JUnit runner.
	 *
	 * @param runner JUnit test runner
	 * @return name for runner
	 */
	private static String getRunnerName(Object runner) {
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

	/**
	 * Get code reference associated with the specified JUnit runner.
	 *
	 * @param runner JUnit test runner
	 * @return code reference to the runner
	 */
	@Nullable
	private String getCodeRef(Object runner) {
		TestClass testClass = LifecycleHooks.getTestClassOf(runner);
		Class<?> javaClass = testClass.getJavaClass();
		if (javaClass != null) {
			return javaClass.getCanonicalName();
		}
		return null;
	}

	/**
	 * Get code reference associated with the specified JUnit test method.
	 *
	 * @param frameworkMethod JUnit test method
	 * @return code reference to the test method
	 */
	private String getCodeRef(FrameworkMethod frameworkMethod) {
		return TestCaseIdUtils.getCodeRef(frameworkMethod.getMethod());
	}

	private Set<ItemAttributesRQ> getAttributes(FrameworkMethod frameworkMethod) {
		return ofNullable(frameworkMethod.getMethod()).map(m -> ofNullable(m.getAnnotation(Attributes.class)).map(AttributeParser::retrieveAttributes)
				.orElseGet(Collections::emptySet)).orElseGet(Collections::emptySet);
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
