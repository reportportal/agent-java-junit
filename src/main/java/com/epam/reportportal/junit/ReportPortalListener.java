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
import com.epam.reportportal.listeners.LogLevel;
import com.epam.reportportal.service.Launch;
import com.epam.reportportal.service.ReportPortal;
import com.epam.reportportal.service.item.TestCaseIdEntry;
import com.epam.reportportal.service.tree.TestItemTree;
import com.epam.reportportal.utils.*;
import com.epam.reportportal.utils.reflect.Accessible;
import com.epam.ta.reportportal.ws.model.*;
import com.epam.ta.reportportal.ws.model.attribute.ItemAttributesRQ;
import com.epam.ta.reportportal.ws.model.launch.StartLaunchRQ;
import com.epam.ta.reportportal.ws.model.log.SaveLogRQ;
import com.nordstrom.automation.junit.*;
import io.reactivex.Maybe;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.junit.*;
import org.junit.experimental.categories.Category;
import org.junit.experimental.theories.Theories;
import org.junit.experimental.theories.Theory;
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
import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.epam.reportportal.junit.utils.ItemTreeUtils.createItemTreeKey;
import static java.util.Optional.ofNullable;

/**
 * Report portal custom event listener. This listener support parallel running
 * of tests and test methods. Main constraint: All test classes in current
 * launch should be unique. (User shouldn't run the same classes twice/or more
 * times in the one launch)
 */
public class ReportPortalListener implements ShutdownListener, RunnerWatcher, RunWatcher, MethodWatcher<FrameworkMethod> {

	private static final Logger LOGGER = LoggerFactory.getLogger(ReportPortalListener.class);

	private static final String FINISH_REQUEST = "FINISH_REQUEST";
	private static final String START_TIME = "START_TIME";
	private static final String IS_RETRY = "IS_RETRY";
	private static final String IS_THEORY = "IS_THEORY";
	private static final Map<Class<? extends Annotation>, ItemType> TYPE_MAP = Collections.unmodifiableMap(new HashMap<Class<? extends Annotation>, ItemType>() {
		private static final long serialVersionUID = 5292344734560662610L;
	{
		put(Test.class, ItemType.STEP);
		put(Before.class, ItemType.BEFORE_METHOD);
		put(After.class, ItemType.AFTER_METHOD);
		put(BeforeClass.class, ItemType.BEFORE_CLASS);
		put(AfterClass.class, ItemType.AFTER_CLASS);
		put(Theory.class, ItemType.STEP);
	}});

	private static final Predicate<StackTraceElement> EXPECTED_EXCEPTION_ELEMENT = e -> "org.junit.rules.ExpectedException".equals(e.getClassName());
	private static final Predicate<StackTraceElement[]> IS_EXPECTED_EXCEPTION_RULE = eList -> Arrays.stream(eList)
			.anyMatch(EXPECTED_EXCEPTION_ELEMENT);

	private static volatile ReportPortal REPORT_PORTAL = ReportPortal.builder().build();

	private final ParallelRunningContext context = new ParallelRunningContext();
	private final MemoizingSupplier<Launch> launch = createLaunch();
	private final ConcurrentLinkedDeque<Object> runners = new ConcurrentLinkedDeque<>();

	/**
	 * Returns a supplier which initialize a launch on the first 'get'.
	 *
	 * @return a supplier with a lazy-initialized {@link Launch} instance
	 */
	protected MemoizingSupplier<Launch> createLaunch() {
		return new MemoizingSupplier<>(() -> {
			final ReportPortal reportPortal = getReportPortal();
			StartLaunchRQ rq = buildStartLaunchRq(reportPortal.getParameters());
			Launch l = reportPortal.newLaunch(rq);
			context.getItemTree().setLaunchId(l.start());
			return l;
		});
	}

	/**
	 * Returns current {@link ReportPortal} instance
	 *
	 * @return current instance
	 */
	public static ReportPortal getReportPortal() {
		return REPORT_PORTAL;
	}

	/**
	 * Set current {@link ReportPortal} instance
	 *
	 * @param reportPortal an instance to use
	 */
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

	@Nonnull
	private List<Object> getRunnerChain(@Nonnull final Object runner) {
		List<Object> chain = new ArrayList<>();
		chain.add(runner);
		Object current = runner;
		for (Object parent : runners) {
			if (!getRunnerName(current).equals(getRunnerName(parent))) {
				// skip duplicated runners in parameterized tests
				chain.add(parent);
			}
			current = parent;
		}
		Collections.reverse(chain);
		return chain;
	}

	@Nonnull
	private TestItemTree.TestItemLeaf retrieveLeaf(@Nonnull final Map<TestItemTree.ItemTreeKey, TestItemTree.TestItemLeaf> children,
			@Nonnull final Object runner, @Nonnull final Date previousDate, @Nonnull final ItemType itemType,
			@Nullable final Maybe<String> parentId) {
		return children.computeIfAbsent(TestItemTree.ItemTreeKey.of(getRunnerName(runner)), (k) -> {
			Launch myLaunch = launch.get();
			Date currentDate = Calendar.getInstance().getTime();
			Date itemDate;
			if (previousDate.compareTo(currentDate) <= 0) {
				itemDate = currentDate;
			} else {
				itemDate = previousDate;
			}
			StartTestItemRQ rq = itemType == ItemType.TEST ? buildStartTestItemRq(runner, itemDate) : buildStartSuiteRq(runner, itemDate);
			TestItemTree.TestItemLeaf l = ofNullable(parentId).map(p -> TestItemTree.createTestItemLeaf(p, myLaunch.startTestItem(p, rq)))
					.orElseGet(() -> TestItemTree.createTestItemLeaf(myLaunch.startTestItem(rq)));
			l.setType(ItemType.SUITE);
			l.setAttribute(START_TIME, rq.getStartTime());
			return l;
		});
	}

	/**
	 * Returns current test item leaf in Test Tree. Creates Test Item Tree branches and leaves if no such items found.
	 *
	 * @param testRunner JUnit test runner
	 * @return a leaf of an item inside ItemTree or null if not found
	 */
	@Nonnull
	protected TestItemTree.TestItemLeaf retrieveLeaf(@Nonnull final Object testRunner) {
		List<Object> runnerChain = getRunnerChain(testRunner);
		int chainSize = runnerChain.size();
		List<TestItemTree.TestItemLeaf> leafChain = new ArrayList<>(chainSize);
		for (int i = 0; i < chainSize; i++) {
			Object runner = runnerChain.get(i);
			ItemType type = i + 1 < chainSize ? ItemType.SUITE : ItemType.TEST;
			if (i <= 0) {
				leafChain.add(retrieveLeaf(context.getItemTree().getTestItems(), runner, Calendar.getInstance().getTime(), type, null));
			} else {
				TestItemTree.TestItemLeaf parentLeaf = leafChain.get(i - 1);
				leafChain.add(retrieveLeaf(parentLeaf.getChildItems(), runner,
						// should not be null, since 'retrieveLeaf' always set this attribute
						Objects.requireNonNull(parentLeaf.getAttribute(START_TIME)), type, parentLeaf.getItemId()
				));
			}
		}
		return leafChain.get(chainSize - 1);
	}

	/**
	 * Returns current test item leaf in Test Tree.
	 *
	 * @param runner JUnit test runner
	 * @return a leaf of an item inside ItemTree or null if not found
	 */
	@Nullable
	protected TestItemTree.TestItemLeaf getLeaf(@Nonnull final Object runner) {
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
	 * Returns current test item leaf in Test Tree.
	 *
	 * @param runner   JUnit test runner
	 * @param method   {@link FrameworkMethod} object for test
	 * @param callable an object being intercepted
	 * @return a leaf of an item inside ItemTree or null if not found
	 */
	@Nullable
	protected TestItemTree.TestItemLeaf getLeaf(@Nonnull final Object runner, @Nonnull final FrameworkMethod method,
			@Nonnull final ReflectiveCallable callable) {
		TestItemTree.ItemTreeKey myKey = ofNullable(method.getAnnotation(Theory.class)).map(T -> createItemTreeKey(method))
				.orElseGet(() -> createItemTreeKey(method, getStepParameters(method, runner, callable)));
		TestItemTree.ItemTreeKey myParentKey = createItemTreeKey(getRunnerName(runner));
		TestItemTree.TestItemLeaf testLeaf = ofNullable(getLeaf(runner)).orElseGet(() -> context.getItemTree()
				.getTestItems()
				.get(myParentKey));

		return ofNullable(testLeaf).map(l -> l.getChildItems().get(myKey)).orElse(null);
	}

	/**
	 * Calculate an Item status according to its child item status and current status. E.G.: SUITE-TEST or TEST-STEP.
	 * <p>
	 * Example 1:
	 * - Current status: {@link ItemStatus#FAILED}
	 * - Child item status: {@link ItemStatus#SKIPPED}
	 * Result: {@link ItemStatus#FAILED}
	 * <p>
	 * Example 2:
	 * - Current status: {@link ItemStatus#PASSED}
	 * - Child item status: {@link ItemStatus#SKIPPED}
	 * Result: {@link ItemStatus#PASSED}
	 * <p>
	 * Example 3:
	 * - Current status: {@link ItemStatus#PASSED}
	 * - Child item status: {@link ItemStatus#FAILED}
	 * Result: {@link ItemStatus#FAILED}
	 * <p>
	 * Example 4:
	 * - Current status: {@link ItemStatus#SKIPPED}
	 * - Child item status: {@link ItemStatus#FAILED}
	 * Result: {@link ItemStatus#FAILED}
	 *
	 * @param currentStatus an Item status
	 * @param childStatus   a status of its child element
	 * @return new status
	 */
	@Nullable
	protected ItemStatus evaluateStatus(@Nullable final ItemStatus currentStatus, @Nullable final ItemStatus childStatus) {
		return StatusEvaluation.evaluateStatus(currentStatus, childStatus);
	}

	/**
	 * Send a <b>start test item</b> request for the indicated container object (category or suite) to Report Portal.
	 *
	 * @param runner JUnit test runner
	 */
	protected void startRunner(@Nonnull final Object runner) {
		runners.addFirst(runner);
	}

	/**
	 * Send a <b>finish test item</b> request for the indicated container object (test or suite) to Report Portal.
	 *
	 * @param runner JUnit test runner
	 */
	protected void stopRunner(@Nonnull final Object runner) {
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
		runners.removeFirstOccurrence(runner);
	}

	/**
	 * Calculates a RP-safe date for a child item in Item Tree, which will be greater or equal to the parent timestamp
	 *
	 * @param parentLeaf a parent leaf of a new item inside ItemTree
	 * @return a safe child item timestamp
	 */
	@Nonnull
	protected Date getDateForChild(@Nullable final TestItemTree.TestItemLeaf parentLeaf) {
		Date currentDate = Calendar.getInstance().getTime();
		return ofNullable(parentLeaf).map(l -> l.<Date>getAttribute(START_TIME)).map(d -> {
			if (currentDate.compareTo(d) >= 0) {
				return currentDate;
			} else {
				return d;
			}
		}).orElse(currentDate);
	}

	/**
	 * Send a <b>start test item</b> request for the indicated test to Report Portal.
	 *
	 * @param testContext {@link AtomicTest} object for test method
	 */
	protected void startTest(@Nonnull final AtomicTest testContext) {
		FrameworkMethod method = testContext.getIdentity();
		if (!ofNullable(method.getAnnotation(Theory.class)).isPresent()) {
			context.setTestStatus(createItemTreeKey(method, getStepParameters(testContext)), ItemStatus.PASSED);
		}
		context.setTestMethodDescription(method, testContext.getDescription());
	}

	/**
	 * Send a <b>finish test item</b> request for the indicated test to Report Portal.
	 *
	 * @param testContext {@link AtomicTest} object for test method
	 */
	protected void finishTest(@Nonnull final AtomicTest testContext) {
		FrameworkMethod method = testContext.getIdentity();
		TestItemTree.ItemTreeKey key = ofNullable(method.getAnnotation(Theory.class)).map(T -> createItemTreeKey(method))
				.orElseGet(() -> createItemTreeKey(method, getStepParameters(testContext)));
		ItemStatus status = context.getTestStatus(key);
		Throwable throwable = context.getTestThrowable(key);
		TestItemTree.TestItemLeaf testLeaf = getLeaf(testContext.getRunner());
		if (testLeaf != null) {
			ofNullable(testLeaf.getChildItems().get(key)).ifPresent(l -> {
				ItemStatus itemStatus = l.getStatus();
				boolean isTheory = ofNullable(l.<Boolean>getAttribute(IS_THEORY)).orElse(Boolean.FALSE);
				if (itemStatus != status && !isTheory) {
					Boolean isRetry = l.<Boolean>getAttribute(IS_RETRY);
					if (!(ItemStatus.SKIPPED == status && isRetry != null && isRetry)) {
						ofNullable(throwable).ifPresent(t -> {
							if (status == ItemStatus.SKIPPED) {
								sendReportPortalMsg(l.getItemId(), LogLevel.WARN, throwable);
							} else {
								sendReportPortalMsg(l.getItemId(), LogLevel.ERROR, throwable);
							}
						});
						if (status == ItemStatus.PASSED || (throwable != null
								&& IS_EXPECTED_EXCEPTION_RULE.test(throwable.getStackTrace()))) {
							stopTestMethod(l, method, buildFinishStepRq(method, status));
						}
					}
				} else if (isTheory && testContext.getRunner().getClass() == Theories.class) {
					// fail theory test if no theories passed
					ItemStatus theoryStatus = context.getTestStatus(key);
					if (ItemStatus.FAILED == theoryStatus) {
						sendReportPortalMsg(l.getItemId(), LogLevel.ERROR, context.getTestThrowable(key));
					}
					stopTestMethod(l, method, buildFinishStepRq(method, theoryStatus == null ? ItemStatus.PASSED : theoryStatus));
				}
			});
			testLeaf.setStatus(status);
		}
	}

	/**
	 * Start a test item on RP, save a child item into `parentLeaf` property.
	 *
	 * @param method     {@link FrameworkMethod} object for test
	 * @param parentLeaf a parent leaf of a new item inside ItemTree
	 * @param rq         a request to send to RP
	 */
	protected void startTestItem(@Nonnull final FrameworkMethod method, @Nonnull final TestItemTree.TestItemLeaf parentLeaf,
			@Nonnull final StartTestItemRQ rq) {
		TestItemTree.ItemTreeKey myKey = ofNullable(method.getAnnotation(Theory.class)).map(a -> createItemTreeKey(method))
				.orElseGet(() -> createItemTreeKey(method, rq.getParameters()));
		Map<TestItemTree.ItemTreeKey, TestItemTree.TestItemLeaf> children = parentLeaf.getChildItems();
		TestItemTree.TestItemLeaf child = children.get(myKey);
		if (child != null) {
			Boolean t = child.getAttribute(IS_THEORY);
			if (t != null && t) {
				return; // the test already started
			}
			Boolean r = child.getAttribute(IS_RETRY);
			if (r != null && r) {
				parentLeaf.getChildItems().remove(myKey);
				rq.setRetry(true);
			}
		}
		Maybe<String> itemId = launch.get().startTestItem(parentLeaf.getItemId(), rq);
		TestItemTree.TestItemLeaf myLeaf = TestItemTree.createTestItemLeaf(parentLeaf.getItemId(), itemId);
		myLeaf.setType(ItemType.STEP);
		ofNullable(method.getAnnotation(Theory.class)).ifPresent(a -> myLeaf.setAttribute(IS_THEORY, Boolean.TRUE));
		parentLeaf.getChildItems().put(myKey, myLeaf);
		if (getReportPortal().getParameters().isCallbackReportingEnabled()) {
			context.getItemTree().getTestItems().put(createItemTreeKey(method), myLeaf);
		}
	}

	/**
	 * Send a <b>start test item</b> request for the indicated test method to Report Portal.
	 *
	 * @param runner   JUnit test runner
	 * @param method   {@link FrameworkMethod} object for test
	 * @param callable {@link ReflectiveCallable} object being intercepted
	 */
	protected void startTestMethod(@Nonnull final Object runner, @Nonnull final FrameworkMethod method,
			@Nonnull final ReflectiveCallable callable) {
		TestItemTree.TestItemLeaf testLeaf = retrieveLeaf(runner);
		StartTestItemRQ rq = buildStartStepRq(runner,
				context.getTestMethodDescription(method),
				method,
				callable,
				getDateForChild(testLeaf)
		);
		startTestItem(method, testLeaf, rq);
	}

	/**
	 * Detect RP item type by annotation
	 *
	 * @param method JUnit framework method context
	 * @return an item type or null if no such mapping (unknown annotation)
	 */
	@Nullable
	protected ItemType detectMethodType(@Nonnull final FrameworkMethod method) {
		return Arrays.stream(method.getAnnotations())
				.map(a -> TYPE_MAP.get(a.annotationType()))
				.filter(Objects::nonNull)
				.findFirst()
				.orElse(null);
	}

	private void updateTestItemTree(@Nonnull final FrameworkMethod method, @Nullable final Maybe<OperationCompletionRS> finishResponse) {
		TestItemTree.TestItemLeaf testItemLeaf = ItemTreeUtils.retrieveLeaf(method, context.getItemTree());
		if (testItemLeaf != null) {
			testItemLeaf.setFinishResponse(finishResponse);
		}
	}

	/**
	 * Extension point to customize test steps skipped in case of a <code>@Before</code> method failed.
	 *
	 * @param runner       JUnit class runner
	 * @param failedMethod a method which caused the skip
	 * @param callable     an object being intercepted
	 * @param eventTime    <code>@Before</code> start time
	 * @param throwable    An exception which caused the skip
	 */
	@SuppressWarnings("unused")
	protected void reportSkippedStep(@Nonnull final Object runner, @Nonnull final FrameworkMethod failedMethod,
			@Nonnull final ReflectiveCallable callable, @Nonnull final Date eventTime, @Nullable final Throwable throwable) {
		Date currentTime = Calendar.getInstance().getTime();
		Date skipStartTime = currentTime.after(eventTime) ? new Date(currentTime.getTime() - 1) : currentTime;
		TestItemTree.ItemTreeKey myParentKey = createItemTreeKey(getRunnerName(runner));
		TestItemTree.TestItemLeaf testLeaf = ofNullable(getLeaf(runner)).orElseGet(() -> context.getItemTree()
				.getTestItems()
				.get(myParentKey));
		AtomicTest testContext = LifecycleHooks.getAtomicTestOf(runner);
		Description description = testContext.getDescription();
		FrameworkMethod method = testContext.getIdentity();
		ofNullable(testLeaf).ifPresent(l -> {
			StartTestItemRQ startRq = buildStartStepRq(runner, description, method, callable, skipStartTime);
			Maybe<String> id = launch.get().startTestItem(l.getItemId(), startRq);
			ofNullable(throwable).ifPresent(t -> sendReportPortalMsg(id, LogLevel.WARN, throwable));
			FinishTestItemRQ finishRq = buildFinishStepRq(method, ItemStatus.SKIPPED);
			finishRq.setIssue(Launch.NOT_ISSUE);
			launch.get().finishTestItem(id, finishRq);
		});
	}

	/**
	 * Extension point to customize test steps skipped in case of a <code>@BeforeClass</code> method failed.
	 *
	 * @param runner       JUnit class runner
	 * @param failedMethod a method which caused the skip
	 * @param callable     an object being intercepted
	 * @param eventTime    <code>@BeforeClass</code> start time
	 * @param throwable    An exception which caused the skip
	 */
	@SuppressWarnings("unused")
	protected void reportSkippedClassTests(@Nonnull final Object runner, @Nonnull final FrameworkMethod failedMethod,
			@Nonnull final ReflectiveCallable callable, @Nonnull final Date eventTime, @Nullable final Throwable throwable) {
	}

	private void stopTestMethod(@Nonnull final TestItemTree.TestItemLeaf myLeaf, @Nonnull final FrameworkMethod method,
			@Nonnull final FinishTestItemRQ rq) {
		Maybe<String> itemId = myLeaf.getItemId();
		// update existing item or just send new
		Maybe<OperationCompletionRS> finishResponse = ofNullable(myLeaf.getFinishResponse()).map(rs -> {
			/*
			 * ensure previous request passed to maintain the order
			 * FIXME: rework it to be more RX-friendly, make client wait for all Maybe features
			 */
			// noinspection BlockingMethodInNonBlockingContext, ResultOfMethodCallIgnored
			rs.blockingGet();
			return launch.get().finishTestItem(itemId, rq);
		}).orElseGet(() -> launch.get().finishTestItem(itemId, rq));
		myLeaf.setFinishResponse(finishResponse);
		if (getReportPortal().getParameters().isCallbackReportingEnabled()) {
			updateTestItemTree(method, finishResponse);
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
	protected void stopTestMethod(@Nonnull final Object runner, @Nonnull final FrameworkMethod method,
			@Nonnull final ReflectiveCallable callable, @Nonnull final FinishTestItemRQ rq) {
		ofNullable(getLeaf(runner, method, callable)).ifPresent(l -> {
			ItemStatus status = ItemStatus.valueOf(rq.getStatus());
			if (!ofNullable(method.getAnnotation(Theory.class)).isPresent()) {
				l.setStatus(status);
				stopTestMethod(l, method, rq);
			}
		});
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
	protected void stopTestMethod(@Nonnull final Object runner, @Nonnull final FrameworkMethod method,
			@Nonnull final ReflectiveCallable callable, @Nonnull final ItemStatus status, @Nullable final Throwable throwable) {
		FinishTestItemRQ rq = buildFinishStepRq(method, status);
		stopTestMethod(runner, method, callable, rq);

		ItemType methodType = detectMethodType(method);
		boolean reportSkippedMethod =
				(ItemType.BEFORE_METHOD == methodType && ItemStatus.FAILED == status) || (ItemType.BEFORE_METHOD == methodType
						&& ItemStatus.SKIPPED == status && throwable instanceof AssumptionViolatedException);
		if (reportSkippedMethod) {
			reportSkippedStep(runner, method, callable, rq.getEndTime(), throwable);
		}

		boolean reportSkippedClass =
				(ItemType.BEFORE_CLASS == methodType && ItemStatus.FAILED == status) || (ItemType.BEFORE_CLASS == methodType
						&& ItemStatus.SKIPPED == status && throwable instanceof AssumptionViolatedException);
		if (reportSkippedClass) {
			reportSkippedClassTests(runner, method, callable, rq.getEndTime(), throwable);
		}
	}

	/**
	 * Handle test failure action
	 *
	 * @param testContext {@link AtomicTest} object for test method
	 * @param thrown      a <code>Throwable</code> thrown by method
	 */
	protected void setTestFailure(@Nonnull final AtomicTest testContext, @Nullable final Throwable thrown) {
		FrameworkMethod method = testContext.getIdentity();
		TestItemTree.ItemTreeKey key = ofNullable(method.getAnnotation(Theory.class)).map(a -> createItemTreeKey(testContext.getIdentity()))
				.orElseGet(() -> createItemTreeKey(testContext.getIdentity(), getStepParameters(testContext)));
		context.setTestStatus(key, ItemStatus.FAILED);
		context.setTestThrowable(key, thrown);
	}

	/**
	 * Handle test skip action
	 *
	 * @param testContext {@link AtomicTest} object for test method
	 */
	protected void handleTestSkip(@Nonnull final AtomicTest testContext) {
		List<ParameterResource> parameters = getStepParameters(testContext);
		TestItemTree.ItemTreeKey key = createItemTreeKey(testContext.getIdentity(), parameters);
		context.setTestStatus(key, ItemStatus.SKIPPED);
		Object runner = testContext.getRunner();
		FrameworkMethod method = testContext.getIdentity();
		Object target = getTargetFor(runner, method);
		ReflectiveCallable callable = LifecycleHooks.encloseCallable(method.getMethod(), target);
		TestItemTree.ItemTreeKey myKey = createItemTreeKey(method, parameters);
		TestItemTree.TestItemLeaf myLeaf = ofNullable(retrieveLeaf(runner).getChildItems().get(myKey)).orElse(null);
		if (myLeaf == null) {
			// a test method wasn't started, most likely an ignored test: start and stop a test item with 'skipped' status
			startTest(testContext);
			startTestMethod(runner, method, callable);
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
	}

	/**
	 * Handle test skip (AssumptionViolatedException) action
	 *
	 * @param testContext {@link AtomicTest} object for test method
	 * @param thrown      a <code>Throwable</code> thrown by method
	 */
	protected void setTestSkip(@Nonnull final AtomicTest testContext, @Nullable final Throwable thrown) {
		TestItemTree.ItemTreeKey key = createItemTreeKey(testContext.getIdentity(), getStepParameters(testContext));
		context.setTestStatus(key, ItemStatus.SKIPPED);
		context.setTestThrowable(key, thrown);
	}

	/**
	 * Prepare a function which creates a {@link SaveLogRQ} from a {@link Throwable}
	 *
	 * @param level  a log level
	 * @param thrown a {@code Throwable} which was thrown
	 * @return a {@link SaveLogRQ} supplier {@link Function}
	 */
	@Nonnull
	protected Function<String, SaveLogRQ> getLogSupplier(@Nonnull final LogLevel level, @Nullable final Throwable thrown) {
		return itemUuid -> {
			SaveLogRQ rq = new SaveLogRQ();
			rq.setItemUuid(itemUuid);
			rq.setLevel(level.name());
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
	 * Send a message to report portal about appeared failure, attach it to the specific item
	 *
	 * @param itemUud an item Item UUID promise
	 * @param level   {@link LogLevel} level of a log entry
	 * @param thrown  {@link Throwable} object with details of the failure
	 */
	@SuppressWarnings("SameParameterValue")
	protected void sendReportPortalMsg(@Nonnull final Maybe<String> itemUud, @Nonnull final LogLevel level,
			@Nullable final Throwable thrown) {
		ReportPortal.emitLog(itemUud, getLogSupplier(level, thrown));
	}

	/**
	 * Send message to report portal about appeared failure
	 *
	 * @param level  {@link LogLevel} level of a log entry
	 * @param thrown {@link Throwable} object with details of the failure
	 */
	protected void sendReportPortalMsg(@Nonnull final LogLevel level, @Nullable final Throwable thrown) {
		ReportPortal.emitLog(getLogSupplier(level, thrown));
	}

	/**
	 * Determine if the specified method is reportable.
	 *
	 * @param method {@link FrameworkMethod} object
	 * @return {@code true} if method is reportable; otherwise {@code false}
	 */
	protected boolean isReportable(@Nonnull final FrameworkMethod method) {
		return detectMethodType(method) != null;
	}

	/**
	 * Extension point to customize launch creation event/request
	 *
	 * @param parameters Launch Configuration parameters
	 * @return Request to ReportPortal
	 */
	@Nonnull
	protected StartLaunchRQ buildStartLaunchRq(@Nonnull final ListenerParameters parameters) {
		StartLaunchRQ rq = new StartLaunchRQ();
		rq.setName(parameters.getLaunchName());
		rq.setStartTime(Calendar.getInstance().getTime());
		Set<ItemAttributesRQ> attributes = new HashSet<>();
		attributes.addAll(parameters.getAttributes());
		attributes.addAll(SystemAttributesFetcher.collectSystemAttributes(parameters.getSkippedAnIssue()));
		rq.setAttributes(attributes);
		rq.setMode(parameters.getLaunchRunningMode());

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
	 * @param runner    JUnit suite context
	 * @param startTime a suite start time which will be passed to RP
	 * @return Request to ReportPortal
	 */
	@Nonnull
	protected StartTestItemRQ buildStartSuiteRq(@Nonnull final Object runner, @Nullable final Date startTime) {
		StartTestItemRQ rq = new StartTestItemRQ();
		rq.setName(getRunnerName(runner));
		rq.setCodeRef(getCodeRef(runner));
		rq.setStartTime(startTime);
		rq.setType(ItemType.SUITE.name());
		rq.setAttributes(getAttributes(LifecycleHooks.getTestClassOf(runner)));
		return rq;
	}

	/**
	 * Extension point to customize test creation event/request
	 *
	 * @param runner    JUnit test runner context
	 * @param startTime a suite start time which will be passed to RP
	 * @return Request to ReportPortal
	 */
	@Nonnull
	protected StartTestItemRQ buildStartTestItemRq(@Nonnull final Object runner, @Nullable final Date startTime) {
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
	@Nonnull
	protected StartTestItemRQ buildStartStepRq(@Nonnull final Object runner, @Nullable final Description description,
			@Nonnull final FrameworkMethod method, @Nonnull final ReflectiveCallable callable, @Nullable final Date startTime) {
		StartTestItemRQ rq = new StartTestItemRQ();
		rq.setName(method.getName());
		rq.setCodeRef(getCodeRef(method));
		rq.setAttributes(getAttributes(method));
		rq.setDescription(createStepDescription(description, method));
		rq.setParameters(getStepParameters(method, runner, callable));
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
	@Nonnull
	protected FinishTestItemRQ buildFinishSuiteRq(@Nullable final TestClass testClass) {
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
	@Nonnull
	protected FinishTestItemRQ buildFinishStepRq(@Nullable final FrameworkMethod method, @Nonnull final ItemStatus status) {
		FinishTestItemRQ rq = new FinishTestItemRQ();
		rq.setEndTime(Calendar.getInstance().getTime());
		rq.setStatus(status.name());
		return rq;
	}

	/**
	 * Calculates a test case ID based on code reference and parameters
	 *
	 * @param runner          JUnit test runner context
	 * @param frameworkMethod JUnit framework method context
	 * @param codeRef         a code reference which will be used for the calculation
	 * @param params          a list of test arguments
	 * @param <T>             arguments type
	 * @return a test case ID
	 */
	@Nullable
	protected <T> TestCaseIdEntry getTestCaseId(@Nullable final Object runner, @Nonnull final FrameworkMethod frameworkMethod,
			@Nullable final String codeRef, @Nullable final List<T> params) {
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
	 * @param test JUnit test context
	 * @return Test/Step Parameters being sent to Report Portal or 'null' if empty or not exist
	 */
	@Nullable
	protected List<ParameterResource> getStepParameters(@Nonnull final AtomicTest test) {
		Object runner = test.getRunner();
		FrameworkMethod identity = test.getIdentity();
		return getStepParameters(identity, runner, LifecycleHooks.encloseCallable(identity.getMethod(), getTargetFor(runner, identity)));
	}

	/**
	 * Extension point to customize Report Portal test parameters
	 *
	 * @param method   JUnit framework method context
	 * @param runner   JUnit test runner context
	 * @param callable {@link ReflectiveCallable} object being intercepted
	 * @return Test/Step Parameters being sent to Report Portal or 'null' if empty or not exist
	 */
	@Nullable
	protected List<ParameterResource> getStepParameters(@Nonnull final FrameworkMethod method, @Nonnull final Object runner,
			@Nonnull final ReflectiveCallable callable) {
		List<ParameterResource> parameters = getMethodParameters(method, runner, callable);
		return parameters.isEmpty() ? null : parameters;
	}

	/**
	 * Assemble execution parameters list for the specified framework method.
	 *
	 * @param method   JUnit framework method context
	 * @param runner   JUnit test runner context
	 * @param callable {@link ReflectiveCallable} object being intercepted
	 * @return Step Parameters being sent to ReportPortal
	 */
	@Nonnull
	protected List<ParameterResource> getMethodParameters(@Nonnull final FrameworkMethod method, @Nonnull final Object runner,
			@Nonnull final ReflectiveCallable callable) {
		List<ParameterResource> result = new ArrayList<>();
		if (!(method.isStatic())) {
			Object target = getTargetFor(runner, method);
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
	 * @param method JUnit framework method context
	 * @return JUnit test class instance for specified runner
	 */
	@Nullable
	protected Object getTargetFor(@Nonnull final Object runner, @Nonnull final FrameworkMethod method) {
		Description description = LifecycleHooks.describeChild(runner, method);
		return LifecycleHooks.getTargetOf(description);
	}

	/**
	 * Extension point to customize test step description
	 *
	 * @param description JUnit framework test description object
	 * @param method      JUnit framework method context
	 * @return Test/Step Description being sent to ReportPortal
	 */
	@Nullable
	protected String createStepDescription(@Nullable final Description description, @Nonnull final FrameworkMethod method) {
		DisplayName itemDisplayName = method.getAnnotation(DisplayName.class);
		return (itemDisplayName != null) ? itemDisplayName.value() : ofNullable(description).map(Description::getDisplayName).orElse(null);
	}

	/**
	 * Get name associated with the specified JUnit runner.
	 *
	 * @param runner JUnit test runner
	 * @return name for runner
	 */
	@Nonnull
	protected static String getRunnerName(@Nonnull final Object runner) {
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
	protected String getCodeRef(@Nonnull final Object runner) {
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
	@Nonnull
	protected String getCodeRef(@Nonnull final FrameworkMethod frameworkMethod) {
		return TestCaseIdUtils.getCodeRef(frameworkMethod.getMethod());
	}

	/**
	 * Extract and returns static attributes of a test class (set with {@link Category} annotation)
	 *
	 * @param testClass JUnit suite context
	 * @return a set of attributes
	 */
	@Nonnull
	protected Set<ItemAttributesRQ> getAttributes(@Nonnull final TestClass testClass) {
		return ofNullable(testClass.getAnnotation(Category.class)).map(a -> Arrays.stream(a.value())
				.map(c -> new ItemAttributesRQ(null, c.getSimpleName()))).orElse(Stream.empty()).collect(Collectors.toSet());
	}

	/**
	 * Extract and returns static attributes of a test method (set with {@link Attributes} and {@link Category} annotations)
	 *
	 * @param frameworkMethod JUnit test method
	 * @return a set of attributes
	 */
	@Nonnull
	protected Set<ItemAttributesRQ> getAttributes(@Nonnull final FrameworkMethod frameworkMethod) {
		Stream<ItemAttributesRQ> categories = ofNullable(frameworkMethod.getAnnotation(Category.class)).map(a -> Arrays.stream(a.value())
				.map(c -> new ItemAttributesRQ(null, c.getSimpleName()))).orElse(Stream.empty());
		Stream<ItemAttributesRQ> attributes = ofNullable(frameworkMethod.getMethod()).flatMap(m -> ofNullable(m.getAnnotation(Attributes.class))
				.map(a -> AttributeParser.retrieveAttributes(a).stream())).orElse(Stream.empty());
		return Stream.concat(categories, attributes).collect(Collectors.toSet());
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onShutdown() {
		stopLaunch();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void runStarted(Object runner) {
		startRunner(runner);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void runFinished(Object runner) {
		stopRunner(runner);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void testStarted(AtomicTest atomicTest) {
		startTest(atomicTest);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void testFinished(AtomicTest atomicTest) {
		finishTest(atomicTest);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void testFailure(AtomicTest atomicTest, Throwable thrown) {
		setTestFailure(atomicTest, thrown);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void testAssumptionFailure(AtomicTest atomicTest, AssumptionViolatedException thrown) {
		setTestSkip(atomicTest, thrown);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void testIgnored(AtomicTest atomicTest) {
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
		if (isReportable(method)) {
			launch.get().getStepReporter().finishPreviousStep();
			ItemStatus status = ofNullable(thrown).map(t -> {
				boolean isAssumption = t instanceof AssumptionViolatedException;
				ItemStatus myStatus = ofNullable(method.getAnnotation(Test.class)).filter(a -> a.expected().isInstance(thrown))
						.map(a -> ItemStatus.PASSED)
						.orElse(isAssumption ? ItemStatus.SKIPPED : ItemStatus.FAILED);
				LogLevel level = ItemStatus.PASSED == myStatus ? LogLevel.INFO : isAssumption ? LogLevel.WARN : LogLevel.ERROR;
				sendReportPortalMsg(level, thrown);
				return myStatus;
			}).orElse(ItemStatus.PASSED);
			stopTestMethod(runner, method, callable, status, thrown);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Class<FrameworkMethod> supportedType() {
		return FrameworkMethod.class;
	}
}
