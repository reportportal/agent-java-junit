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
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.junit.*;
import org.junit.Test.None;
import org.junit.experimental.categories.Category;
import org.junit.internal.AssumptionViolatedException;
import org.junit.internal.runners.model.ReflectiveCallable;
import org.junit.runner.Description;
import org.junit.runners.Suite;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.TestClass;
import org.junit.runners.parameterized.BlockJUnit4ClassRunnerWithParameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.*;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.epam.reportportal.junit.utils.ItemTreeUtils.createItemTreeKey;
import static java.util.Optional.ofNullable;

/**
 * Report portal custom event listener. This listener support parallel running
 * of tests and test methods. Main constraint: All test classes in current
 * launch should be unique. (User shouldn't run the same classes twice/or more
 * times in the one launch)
 *
 * @author Aliaksei_Makayed (modified by Andrei_Ramanchuk)
 */
public class ReportPortalListener implements ShutdownListener, RunnerWatcher, RunWatcher<FrameworkMethod>, MethodWatcher<FrameworkMethod> {

	public static final Issue NOT_ISSUE = new Issue();

	static {
		NOT_ISSUE.setIssueType("NOT_ISSUE");
	}

	private static final Logger LOGGER = LoggerFactory.getLogger(ReportPortalListener.class);

	private static final String FINISH_REQUEST = "FINISH_REQUEST";
	private static final String FINISH_RESPONSE = "FINISH_RESPONSE";
	private static final String START_TIME = "START_TIME";
	private static final String IS_RETRY = "IS_RETRY";
	private static final Map<Class<? extends Annotation>, ItemType> TYPE_MAP = Collections.unmodifiableMap(new HashMap<Class<? extends Annotation>, ItemType>() {{
		put(Test.class, ItemType.STEP);
		put(Before.class, ItemType.BEFORE_METHOD);
		put(After.class, ItemType.AFTER_METHOD);
		put(BeforeClass.class, ItemType.BEFORE_CLASS);
		put(AfterClass.class, ItemType.AFTER_CLASS);
	}});

	private static volatile ReportPortal REPORT_PORTAL = ReportPortal.builder().build();

	private final ParallelRunningContext context;
	private final MemorizingSupplier<Launch> launch;

	public ReportPortalListener() {
		context = new ParallelRunningContext();
		launch = createLaunch();
	}

	protected MemorizingSupplier<Launch> createLaunch() {
		return new MemorizingSupplier<>(() -> {
			final ReportPortal reportPortal = getReportPortal();
			StartLaunchRQ rq = buildStartLaunchRq(reportPortal.getParameters());
			Launch l = reportPortal.newLaunch(rq);
			context.getItemTree().setLaunchId(l.start());
			return l;
		});
	}

	public static ReportPortal getReportPortal() {
		return REPORT_PORTAL;
	}

	public static void setReportPortal(ReportPortal reportPortal) {
		REPORT_PORTAL = reportPortal;
	}

	/**
	 * Send a "finish launch" request to Report Portal.
	 */
	protected void stopLaunch() {
		if (launch.isInitialized()) {
			FinishExecutionRQ finishExecutionRQ = new FinishExecutionRQ();
			finishExecutionRQ.setEndTime(Calendar.getInstance().getTime());
			launch.get().finish(finishExecutionRQ);
			launch.reset();
		}
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
		TestItemTree.TestItemLeaf leaf = null;
		Map<TestItemTree.ItemTreeKey, TestItemTree.TestItemLeaf> children = context.getItemTree().getTestItems();
		int chainSize = chain.size();
		for (int i = 0; i < chainSize; i++) {
			Object r = chain.get(i);
			Maybe<String> parentId = ofNullable(leaf).map(TestItemTree.TestItemLeaf::getItemId).orElse(null);
			Date previousDate = ofNullable(leaf).map(l -> (Date) l.getAttribute(START_TIME))
					.orElseGet(() -> Calendar.getInstance().getTime());
			int itemNumber = i + 1;
			leaf = children.computeIfAbsent(TestItemTree.ItemTreeKey.of(getRunnerName(r)), (k) -> {
				Launch myLaunch = launch.get();
				long currentDate = Calendar.getInstance().getTimeInMillis();
				Date itemDate;
				if (currentDate >= previousDate.getTime()) {
					itemDate = new Date(currentDate);
				} else {
					itemDate = new Date(previousDate.getTime() + 1);
				}
				StartTestItemRQ rq;
				if (itemNumber < chainSize) {
					rq = buildStartSuiteRq(r, itemDate);
				} else {
					rq = buildStartTestItemRq(r, itemDate);
				}
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

	@Nullable
	private TestItemTree.TestItemLeaf getLeaf(Object runner, FrameworkMethod method, ReflectiveCallable callable) {
		TestItemTree.ItemTreeKey myParentKey = createItemTreeKey(getRunnerName(runner));
		TestItemTree.TestItemLeaf testLeaf = ofNullable(getLeaf(runner)).orElseGet(() -> context.getItemTree()
				.getTestItems()
				.get(myParentKey));
		List<ParameterResource> parameters = createStepParameters(method, runner, callable);
		TestItemTree.ItemTreeKey myKey = createItemTreeKey(method, parameters);
		return ofNullable(testLeaf).map(l -> l.getChildItems().get(myKey)).orElse(null);
	}

	@Nullable
	protected ItemStatus evaluateStatus(@Nullable ItemStatus currentStatus, @Nullable ItemStatus childStatus) {
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
	 * Send a <b>start test item</b> request for the indicated container object (category or suite) to Report Portal.
	 *
	 * @param runner JUnit test runner
	 */
	@SuppressWarnings("unused")
	protected void startRunner(Object runner) {
		// do nothing, we will construct runner chain on a real test start
	}

	/**
	 * Send a <b>finish test item</b> request for the indicated container object (test or suite) to Report Portal.
	 *
	 * @param runner JUnit test runner
	 */
	protected void stopRunner(Object runner) {
		FinishTestItemRQ rq = buildFinishSuiteRq(LifecycleHooks.getTestClassOf(runner));
		ofNullable(getLeaf(runner)).ifPresent(l -> {
			l.setAttribute(FINISH_REQUEST, rq);
			ItemStatus status = l.getStatus();
			for (Map.Entry<TestItemTree.ItemTreeKey, TestItemTree.TestItemLeaf> entry : l.getChildItems().entrySet()) {
				TestItemTree.TestItemLeaf value = entry.getValue();
				if (value.getType() != ItemType.SUITE) {
					continue;
				}
				ofNullable(value.getAttribute(FINISH_REQUEST)).ifPresent(r -> launch.get()
						.finishTestItem(value.getItemId(), (FinishTestItemRQ) value.clearAttribute(FINISH_REQUEST)));
				status = evaluateStatus(status, value.getStatus());
			}
			l.setStatus(status);
			if (l.getParentId() == null) {
				rq.setStatus(ofNullable(status).map(Enum::name).orElse(null));
				launch.get().finishTestItem(l.getItemId(), rq);
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

	/**
	 * Send a <b>start test item</b> request for the indicated test to Report Portal.
	 *
	 * @param testContext {@link AtomicTest} object for test method
	 */
	protected void startTest(AtomicTest<FrameworkMethod> testContext) {
		context.setTestStatus(testContext, ItemStatus.PASSED);
		context.setTestMethodDescription(testContext.getIdentity(), testContext.getDescription());
	}

	/**
	 * Send a <b>finish test item</b> request for the indicated test to Report Portal.
	 *
	 * @param testContext {@link AtomicTest} object for test method
	 */
	protected void finishTest(AtomicTest<FrameworkMethod> testContext) {
		ItemStatus status = context.getTestStatus(testContext);
		Throwable throwable = context.getTestThrowable(testContext);
		TestItemTree.TestItemLeaf testLeaf = getLeaf(testContext.getRunner());
		if (testLeaf != null) {
			testLeaf.getChildItems()
					.values()
					.stream()
					.filter(i -> i.getType() == ItemType.STEP)
					.filter(i -> i.getStatus() != status)
					.filter(i -> {
						Boolean isRetry = i.<Boolean>getAttribute(IS_RETRY);
						return !(ItemStatus.SKIPPED == status && isRetry != null && isRetry);
					})
					.forEach(i -> {
						FrameworkMethod frameworkMethod = testContext.getIdentity();
						if (status == ItemStatus.FAILED && throwable != null) {
							reportTestFailure(i, throwable);
						}
						stopTestMethod(i, frameworkMethod, buildFinishStepRq(frameworkMethod, status));
					});
			testLeaf.setStatus(status);
		}
	}

	protected void startTestStepItem(Object runner, FrameworkMethod method, ReflectiveCallable callable) {
		TestItemTree.ItemTreeKey myParentKey = createItemTreeKey(getRunnerName(runner));
		TestItemTree.TestItemLeaf testLeaf = ofNullable(retrieveLeaf(runner)).orElseGet(() -> context.getItemTree()
				.getTestItems()
				.get(myParentKey));
		ofNullable(testLeaf).ifPresent(l -> {
			StartTestItemRQ rq = buildStartStepRq(runner, context.getTestMethodDescription(method), method, callable, getDateForChild(l));
			startTestStepItem(method, l, rq);
		});
	}


	protected Maybe<String> startTestStepItem(TestItemTree.TestItemLeaf parentLeaf, StartTestItemRQ rq) {
		Maybe<String> parentId = parentLeaf.getItemId();
		return launch.get().startTestItem(parentId, rq);
	}

	protected void startTestStepItem(FrameworkMethod method, TestItemTree.TestItemLeaf parentLeaf, StartTestItemRQ rq) {
		TestItemTree.ItemTreeKey myKey = createItemTreeKey(method, rq.getParameters());
		ofNullable(parentLeaf.getChildItems().remove(myKey)).map(ol -> ol.<Boolean>getAttribute(IS_RETRY)).ifPresent(r -> {
			if (r) {
				rq.setRetry(true);
			}
		});
		Maybe<String> itemId = startTestStepItem(parentLeaf, rq);
		TestItemTree.TestItemLeaf myLeaf = TestItemTree.createTestItemLeaf(parentLeaf.getItemId(), itemId);
		myLeaf.setType(ItemType.STEP);
		parentLeaf.getChildItems().put(myKey, myLeaf);
		if (getReportPortal().getParameters().isCallbackReportingEnabled()) {
			context.getItemTree().getTestItems().put(createItemTreeKey(method), myLeaf);
		}
	}

	/**
	 * Detect RP item type by annotation
	 *
	 * @param method JUnit framework method context
	 * @return an item type or null if no such mapping (unknown annotation)
	 */
	@Nullable
	protected ItemType detectMethodType(@Nonnull FrameworkMethod method) {
		return Arrays.stream(method.getAnnotations())
				.map(a -> TYPE_MAP.get(a.annotationType()))
				.filter(Objects::nonNull)
				.findFirst()
				.orElse(null);
	}

	/**
	 * Extension point to customize test steps skipped in case of a <code>@Before</code> method failed.
	 *
	 * @param runner      JUnit class runner
	 * @param testContext {@link AtomicTest} object for test method
	 * @param callable    an object being intercepted
	 * @param throwable   An exception which caused the skip
	 * @param eventTime   <code>@Before</code> start time
	 */
	protected void reportSkippedStep(Object runner, AtomicTest<FrameworkMethod> testContext, ReflectiveCallable callable,
			Throwable throwable, Date eventTime) {
		Date currentTime = Calendar.getInstance().getTime();
		Date skipStartTime = currentTime.after(eventTime) ? new Date(currentTime.getTime() - 1) : currentTime;
		TestItemTree.ItemTreeKey myParentKey = createItemTreeKey(getRunnerName(runner));
		TestItemTree.TestItemLeaf testLeaf = ofNullable(getLeaf(runner)).orElseGet(() -> context.getItemTree()
				.getTestItems()
				.get(myParentKey));
		ofNullable(testLeaf).ifPresent(l -> {
			FrameworkMethod method = testContext.getIdentity();
			StartTestItemRQ startRq = buildStartStepRq(runner, testContext.getDescription(), method, callable, skipStartTime);
			Maybe<String> id = startTestStepItem(l, startRq);
			ofNullable(throwable).ifPresent(t->sendReportPortalMsg(id, throwable));
			FinishTestItemRQ finishRq = buildFinishStepRq(method, ItemStatus.SKIPPED);
			finishRq.setIssue(NOT_ISSUE);
			launch.get().finishTestItem(id, finishRq);
		});
	}

	/**
	 * Send a <b>start test item</b> request for the indicated test method to Report Portal.
	 *
	 * @param runner   JUnit test runner
	 * @param method   {@link FrameworkMethod} object for test
	 * @param callable {@link ReflectiveCallable} object being intercepted
	 */
	@SuppressWarnings("unused")
	protected void startTestMethod(Object runner, FrameworkMethod method, ReflectiveCallable callable) {
		startTestStepItem(runner, method, callable);
	}

	/**
	 * Send a <b>finish test item</b> request for the indicated test method to Report Portal.
	 *
	 * @param runner    JUnit test runner
	 * @param method    {@link FrameworkMethod} object for test
	 * @param callable  {@link ReflectiveCallable} object being intercepted
	 * @param status    a test method execution result
	 * @param throwable a throwable result of the method (a failure cause or expected error)
	 */
	protected void stopTestMethod(Object runner, FrameworkMethod method, ReflectiveCallable callable, @Nonnull ItemStatus status,
			@Nullable Throwable throwable) {
		FinishTestItemRQ rq = buildFinishStepRq(method, status);
		stopTestMethod(runner, method, callable, rq);

		ItemType methodType = detectMethodType(method);
		if (ItemType.BEFORE_METHOD == methodType && ItemStatus.FAILED == status) {
			reportSkippedStep(runner, LifecycleHooks.getAtomicTestOf(runner), callable, throwable, rq.getEndTime());
		}
	}

	/**
	 * Send a <b>finish test item</b> request for the indicated test method to Report Portal.
	 *
	 * @param runner   JUnit test runner
	 * @param method   {@link FrameworkMethod} object for test
	 * @param callable {@link ReflectiveCallable} object being intercepted
	 * @param rq       {@link FinishTestItemRQ} a finish request to send
	 */
	public void stopTestMethod(Object runner, FrameworkMethod method, ReflectiveCallable callable, FinishTestItemRQ rq) {
		ofNullable(getLeaf(runner, method, callable)).ifPresent(l -> stopTestMethod(l, method, rq));
	}

	private void stopTestMethod(@Nonnull TestItemTree.TestItemLeaf myLeaf, FrameworkMethod method, FinishTestItemRQ rq) {
		Maybe<String> itemId = myLeaf.getItemId();
		myLeaf.setStatus(ItemStatus.valueOf(rq.getStatus()));
		// update existing item or just send new
		Maybe<OperationCompletionRS> finishResponse = ofNullable(myLeaf.<Maybe<OperationCompletionRS>>getAttribute(FINISH_RESPONSE)).map(rs -> {
			// ensure previous request passed to maintain the order
			// FIXME: rework it to be more RX-friendly, make client wait for all Maybe features
			//noinspection BlockingMethodInNonBlockingContext, ResultOfMethodCallIgnored
			rs.blockingGet();
			return launch.get().finishTestItem(itemId, rq);
		}).orElseGet(() -> launch.get().finishTestItem(itemId, rq));
		myLeaf.setAttribute(FINISH_RESPONSE, finishResponse);
		if (getReportPortal().getParameters().isCallbackReportingEnabled()) {
			updateTestItemTree(method, finishResponse);
		}
	}

	/**
	 * Handle test failure action
	 *
	 * @param testContext {@link AtomicTest} object for test method
	 * @param thrown      a <code>Throwable</code> thrown by method
	 */
	protected void handleFailureTest(AtomicTest<FrameworkMethod> testContext, Throwable thrown) {
		context.setTestStatus(testContext, ItemStatus.FAILED);
		context.setTestThrowable(testContext, thrown);
	}

	/**
	 * Handle test skip action
	 *
	 * @param testContext {@link AtomicTest} object for test method
	 */
	protected void handleTestSkip(AtomicTest<FrameworkMethod> testContext) {
		context.setTestStatus(testContext, ItemStatus.SKIPPED);
		Object runner = testContext.getRunner();
		FrameworkMethod method = testContext.getIdentity();
		TestItemTree.ItemTreeKey myParentKey = createItemTreeKey(getRunnerName(runner));
		TestItemTree.TestItemLeaf testLeaf = ofNullable(retrieveLeaf(runner)).orElseGet(() -> context.getItemTree()
				.getTestItems()
				.get(myParentKey));
		Object target = getTargetForRunner(runner);
		ReflectiveCallable callable = LifecycleHooks.encloseCallable(method.getMethod(), target);
		List<ParameterResource> parameters = createStepParameters(testContext.getIdentity(), testContext.getRunner(), callable);
		TestItemTree.ItemTreeKey myKey = createItemTreeKey(method, parameters);

		ofNullable(testLeaf).ifPresent(p -> {
			TestItemTree.TestItemLeaf myLeaf = ofNullable(p.getChildItems().get(myKey)).orElse(null);
			if (myLeaf == null) {
				// a test method wasn't started, most likely an ignored test: start and stop a test item with 'skipped' status
				startTest(testContext);
				startTestStepItem(runner, method, callable);
				stopTestMethod(runner, method, callable, ItemStatus.SKIPPED, null);
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
				stopTestMethod(runner, method, callable, rq);
			}
		});
	}

	private void updateTestItemTree(FrameworkMethod method, Maybe<OperationCompletionRS> finishResponse) {
		TestItemTree.TestItemLeaf testItemLeaf = ItemTreeUtils.retrieveLeaf(method, context.getItemTree());
		if (testItemLeaf != null) {
			testItemLeaf.setFinishResponse(finishResponse);
		}
	}

	private Function<String, SaveLogRQ> getLogSupplier(final Throwable thrown) {
		return itemUuid -> {
			SaveLogRQ rq = new SaveLogRQ();
			rq.setItemUuid(itemUuid);
			rq.setLevel("ERROR");
			rq.setLogTime(Calendar.getInstance().getTime());
			if (thrown != null) {
				rq.setMessage(ExceptionUtils.getStackTrace(thrown));
			} else {
				rq.setMessage("Test has failed without exception");
			}
			rq.setLogTime(Calendar.getInstance().getTime());

			return rq;
		};
	}

	/**
	 * Send message to report portal about appeared failure
	 *
	 * @param itemUud item Item UUID promise
	 * @param thrown  {@link Throwable} object with details of the failure
	 */
	@SuppressWarnings("unused")
	protected void sendReportPortalMsg(Maybe<String> itemUud, final Throwable thrown) {
		ReportPortal.emitLog(itemUud, getLogSupplier(thrown));
	}

	/**
	 * Send message to report portal about appeared failure
	 *
	 * @param callable {@link ReflectiveCallable} object being intercepted
	 * @param thrown   {@link Throwable} object with details of the failure
	 */
	@SuppressWarnings("unused")
	protected void sendReportPortalMsg(ReflectiveCallable callable, final Throwable thrown) {
		ReportPortal.emitLog(getLogSupplier(thrown));
	}

	/**
	 * Determine if the specified method is reportable.
	 *
	 * @param method {@link FrameworkMethod} object
	 * @return {@code true} if method is reportable; otherwise {@code false}
	 */
	public boolean isReportable(FrameworkMethod method) {
		return detectMethodType(method) != null;
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
		if (StringUtils.isNotBlank(parameters.getRerunOf())) {
			rq.setRerunOf(parameters.getRerunOf());
		}

		if (StringUtils.isNotBlank(parameters.getDescription())) {
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
		rq.setAttributes(getAttributes(LifecycleHooks.getTestClassOf(runner)));
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
	 * @param runner    JUnit test runner context
	 * @param startTime a suite start time which will be passed to RP
	 * @return Request to ReportPortal
	 */
	protected StartTestItemRQ buildStartTestItemRq(Object runner, Date startTime) {
		StartTestItemRQ rq = new StartTestItemRQ();
		rq.setName(getRunnerName(runner));
		rq.setCodeRef(getCodeRef(runner));
		rq.setStartTime(startTime);
		rq.setType(ItemType.TEST.name());
		rq.setAttributes(getAttributes(LifecycleHooks.getTestClassOf(runner)));
		return rq;
	}

	/**
	 * Extension point to customize test step creation event/request
	 *
	 * @param runner      JUnit test runner context
	 * @param description JUnit framework test description object
	 * @param method      JUnit framework method context
	 * @param callable    {@link ReflectiveCallable} object being intercepted
	 * @param startTime   A test step start date and time
	 * @return Request to ReportPortal
	 */
	protected StartTestItemRQ buildStartStepRq(Object runner, Description description, FrameworkMethod method, ReflectiveCallable callable,
			Date startTime) {
		StartTestItemRQ rq = new StartTestItemRQ();
		rq.setName(method.getName());
		rq.setCodeRef(getCodeRef(method));
		rq.setAttributes(getAttributes(method));
		rq.setDescription(createStepDescription(description, method));
		rq.setParameters(createStepParameters(method, runner, callable));
		rq.setTestCaseId(ofNullable(getTestCaseId(runner,
				method,
				rq.getCodeRef(),
				ofNullable(rq.getParameters()).map(p -> p.stream().map(ParameterResource::getValue).collect(Collectors.toList()))
						.orElse(null)
		)).map(TestCaseIdEntry::getId).orElse(null));
		rq.setStartTime(startTime);
		rq.setType(ofNullable(detectMethodType(method)).map(Enum::name).orElse(""));
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
	 */
	@SuppressWarnings("unused")
	protected FinishTestItemRQ buildFinishStepRq(@Nullable FrameworkMethod method, @Nonnull ItemStatus status) {
		FinishTestItemRQ rq = new FinishTestItemRQ();
		rq.setEndTime(Calendar.getInstance().getTime());
		rq.setStatus(status.name());
		return rq;
	}

	protected <T> TestCaseIdEntry getTestCaseId(Object runner, FrameworkMethod frameworkMethod, String codeRef, List<T> params) {
		Method method = frameworkMethod.getMethod();
		if (runner instanceof BlockJUnit4ClassRunnerWithParameters) {
			return TestCaseIdUtils.getTestCaseId(method.getAnnotation(TestCaseId.class),
					Arrays.stream(frameworkMethod.getDeclaringClass().getConstructors()).findAny().orElse(null),
					codeRef,
					params
			);
		}
		return TestCaseIdUtils.getTestCaseId(method.getAnnotation(TestCaseId.class), method, codeRef, params);
	}

	/**
	 * Extension point to customize Report Portal test parameters
	 *
	 * @param method   JUnit framework method context
	 * @param runner   JUnit test runner context
	 * @param callable {@link ReflectiveCallable} object being intercepted
	 * @return Test/Step Parameters being sent to Report Portal
	 */
	protected List<ParameterResource> createStepParameters(FrameworkMethod method, Object runner, ReflectiveCallable callable) {
		List<ParameterResource> parameters = createMethodParameters(method, runner, callable);
		return parameters.isEmpty() ? null : parameters;
	}

	/**
	 * Assemble execution parameters list for the specified framework method.
	 * <p>
	 * <b>NOTE</b>: To support publication of execution parameters, the client test class must implement the
	 * {@link com.nordstrom.automation.junit.ArtifactParams ArtifactParameters} interface.
	 *
	 * @param method   JUnit framework method context
	 * @param runner   JUnit test runner context
	 * @param callable {@link ReflectiveCallable} object being intercepted
	 * @return Step Parameters being sent to ReportPortal
	 */
	private List<ParameterResource> createMethodParameters(FrameworkMethod method, Object runner, ReflectiveCallable callable) {
		List<ParameterResource> result = new ArrayList<>();
		if (!(method.isStatic())) {
			Object target = getTargetForRunner(runner);
			if (target instanceof ArtifactParams) {
				//noinspection Guava
				com.google.common.base.Optional<Map<String, Object>> params = ((ArtifactParams) target).getParameters();
				if (params.isPresent()) {
					for (Map.Entry<String, Object> param : params.get().entrySet()) {
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
			} else {
				try {
					Object[] params = (Object[]) Accessible.on(callable).field("val$params").getValue();
					result.addAll(ParameterUtils.getParameters(method.getMethod(), Arrays.asList(params)));
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
	@Nullable
	protected String createStepDescription(@Nullable Description description, FrameworkMethod method) {
		DisplayName itemDisplayName = method.getAnnotation(DisplayName.class);
		return (itemDisplayName != null) ? itemDisplayName.value() : ofNullable(description).map(Description::getDisplayName).orElse(null);
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
	private String getCodeRef(@Nonnull Object runner) {
		return ofNullable(LifecycleHooks.getTestClassOf(runner)).flatMap(tc -> ofNullable(tc.getJavaClass()))
				.map(Class::getCanonicalName)
				.orElse(null);
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

	@Nonnull
	private Set<ItemAttributesRQ> getAttributes(@Nonnull TestClass testClass) {
		return ofNullable(testClass.getAnnotation(Category.class)).map(a -> Arrays.stream(a.value())
				.map(c -> new ItemAttributesRQ(null, c.getSimpleName()))).orElse(Stream.empty()).collect(Collectors.toSet());
	}

	@Nonnull
	private Set<ItemAttributesRQ> getAttributes(@Nonnull FrameworkMethod frameworkMethod) {
		Stream<ItemAttributesRQ> categories = ofNullable(frameworkMethod.getAnnotation(Category.class)).map(a -> Arrays.stream(a.value())
				.map(c -> new ItemAttributesRQ(null, c.getSimpleName()))).orElse(Stream.empty());
		Stream<ItemAttributesRQ> attributes = ofNullable(frameworkMethod.getMethod()).flatMap(m -> ofNullable(m.getAnnotation(Attributes.class))
				.map(a -> AttributeParser.retrieveAttributes(a).stream())).orElse(Stream.empty());
		return Stream.concat(categories, attributes).collect(Collectors.toSet());
	}

	private static class MemorizingSupplier<T> implements Supplier<T>, Serializable {
		private final Supplier<T> delegate;
		private transient volatile boolean initialized;
		private transient volatile T value;
		private static final long serialVersionUID = 0L;

		MemorizingSupplier(Supplier<T> delegate) {
			this.delegate = delegate;
		}

		public T get() {
			if (!initialized) {
				synchronized (this) {
					if (!initialized) {
						value = delegate.get();
						initialized = true;
						return value;
					}
				}
			}
			return value;
		}

		public boolean isInitialized() {
			return initialized;
		}

		public synchronized void reset() {
			initialized = false;
		}

		public String toString() {
			return "Suppliers.memoize(" + delegate + ")";
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onShutdown() {
		stopLaunch();
	}

	@Override
	public void runStarted(Object runner) {
		startRunner(runner);
	}

	@Override
	public void runFinished(Object runner) {
		stopRunner(runner);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void testStarted(AtomicTest<FrameworkMethod> atomicTest) {
		startTest(atomicTest);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void testFinished(AtomicTest<FrameworkMethod> atomicTest) {
		finishTest(atomicTest);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void testFailure(AtomicTest<FrameworkMethod> atomicTest, Throwable thrown) {
		handleFailureTest(atomicTest, thrown);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void testAssumptionFailure(AtomicTest<FrameworkMethod> atomicTest, AssumptionViolatedException thrown) {
		finishTest(atomicTest);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void testIgnored(AtomicTest<FrameworkMethod> atomicTest) {
		handleTestSkip(atomicTest);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void beforeInvocation(Object runner, FrameworkMethod method, ReflectiveCallable callable) {
		// if this is a JUnit configuration method
		if (isReportable(method)) {
			startTestMethod(runner, method, callable);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void afterInvocation(Object runner, FrameworkMethod method, ReflectiveCallable callable, Throwable thrown) {
		// if this is a JUnit configuration method
		if (isReportable(method)) {
			ItemStatus status = ItemStatus.PASSED;
			// if has exception
			if (thrown != null) {
				Class<? extends Throwable> expected = None.class;

				// if this is not a class-level configuration method
				if ((null == method.getAnnotation(BeforeClass.class)) && (null == method.getAnnotation(AfterClass.class))) {

					AtomicTest<FrameworkMethod> atomicTest = LifecycleHooks.getAtomicTestOf(runner);
					FrameworkMethod identity = atomicTest.getIdentity();
					Test annotation = identity.getAnnotation(Test.class);
					if (annotation != null) {
						expected = annotation.expected();
					}
				}

				if (!expected.isInstance(thrown)) {
					reportTestFailure(callable, thrown);
					status = ItemStatus.FAILED;
				}
			}
			stopTestMethod(runner, method, callable, status, thrown);
		}
	}

	/**
	 * Report failure of the indicated "particle" method.
	 *
	 * @param callable {@link ReflectiveCallable} object being intercepted
	 * @param thrown   exception thrown by method
	 */
	public void reportTestFailure(ReflectiveCallable callable, Throwable thrown) {
		sendReportPortalMsg(callable, thrown);
	}

	/**
	 * Report failure of the indicated "particle" method.
	 *
	 * @param testLeaf {@link TestItemTree.TestItemLeaf} object, the source of item ID
	 * @param thrown   exception thrown by method
	 */
	private void reportTestFailure(TestItemTree.TestItemLeaf testLeaf, Throwable thrown) {
		sendReportPortalMsg(testLeaf.getItemId(), thrown);
	}

	@Override
	public Class<FrameworkMethod> supportedType() {
		return FrameworkMethod.class;
	}
}
