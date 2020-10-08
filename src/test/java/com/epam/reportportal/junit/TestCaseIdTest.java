/*
 * Copyright (C) 2020 Epic Games, Inc. All Rights Reserved.
 */

package com.epam.reportportal.junit;

import com.epam.reportportal.annotations.TestCaseId;
import com.epam.reportportal.listeners.ListenerParameters;
import com.epam.reportportal.service.Launch;
import com.epam.ta.reportportal.ws.model.StartTestItemRQ;
import com.nordstrom.automation.junit.AtomicTest;
import com.nordstrom.automation.junit.LifecycleHooks;
import com.nordstrom.automation.junit.RunAnnouncer;
import com.nordstrom.common.base.UncheckedThrow;
import org.apache.commons.lang3.reflect.MethodUtils;
import org.junit.internal.runners.model.ReflectiveCallable;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.runners.BlockJUnit4ClassRunner;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.InitializationError;
import org.mockito.ArgumentCaptor;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;

/**
 * @author <a href="mailto:ivan_budayeu@epam.com">Ivan Budayeu</a>
 */
public class TestCaseIdTest {

	private final ParallelRunningContext parallelRunningContext = mock(ParallelRunningContext.class);
	private final Launch launch = mock(Launch.class);

	private final ThreadLocal<Object> runner = new ThreadLocal<>();
	private Object target;
	private FrameworkMethod frameworkMethod;
	private ReflectiveCallable callable;
	private AtomicTest<FrameworkMethod> atomicTest;

	private TestCaseIdParallelRunningHandler parallelRunningHandler;

	private class TestCaseIdParallelRunningHandler extends ParallelRunningHandler {

		/**
		 * Constructor: Instantiate a parallel running handler
		 *
		 * @param parallelRunningContext test execution context manager
		 */
		public TestCaseIdParallelRunningHandler(ParallelRunningContext parallelRunningContext) {
			super(parallelRunningContext);
		}

		@Override
		protected ParallelRunningHandler.MemoizingSupplier<Launch> createLaunch() {
			return new MemoizingSupplier<>(() -> launch);
		}
	}

	@BeforeEach
	public void init() {
		parallelRunningHandler = new TestCaseIdParallelRunningHandler(parallelRunningContext);
		when(launch.getParameters()).thenReturn(new ListenerParameters());
	}

	@SuppressWarnings("unchecked")
	private void setupFor(Class<?> testClass) {
		try {
			BlockJUnit4ClassRunner classRunner = new BlockJUnit4ClassRunner(testClass);
			runner.set(classRunner);

			List<FrameworkMethod> methods = invoke(classRunner, "getChildren");
			frameworkMethod = methods.get(0);

			Method newAtomicTest = RunAnnouncer.class.getDeclaredMethod("newAtomicTest", Object.class, Object.class);
			newAtomicTest.setAccessible(true);
			atomicTest = (AtomicTest<FrameworkMethod>) newAtomicTest.invoke(null, classRunner, frameworkMethod);

			target = invoke(classRunner,"createTest");
			callable = LifecycleHooks.encloseCallable(frameworkMethod.getMethod(), target);
		} catch (InitializationError | NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
			throw UncheckedThrow.throwUnchecked(e);
		}
	}

	public static class ClassWithTestCaseIdMethod {
		@org.junit.Test
		@TestCaseId(value = "testId")
		public void methodForTesting() {

		}
	}

	@Test
	public void shouldReturnProvidedIdWhenNotParametrized() {

		setupFor(ClassWithTestCaseIdMethod.class);

		ArgumentCaptor<StartTestItemRQ> argumentCaptor = ArgumentCaptor.forClass(StartTestItemRQ.class);

		parallelRunningHandler.startTest(atomicTest);
		parallelRunningHandler.startTestMethod(runner.get(), frameworkMethod, callable);

		verify(launch, times(1)).startTestItem(any(), argumentCaptor.capture());

		StartTestItemRQ request = argumentCaptor.getValue();

		Assertions.assertEquals("testId", request.getTestCaseId());
	}

//	@Test
	public void retrieveParametrizedTestCaseIdTestWithKey() {

		setupFor(ParallelRunningHandlerTest.DummyTest.class);

		ArgumentCaptor<StartTestItemRQ> argumentCaptor = ArgumentCaptor.forClass(StartTestItemRQ.class);

		parallelRunningHandler.startTest(atomicTest);
		parallelRunningHandler.startTestMethod(runner.get(), frameworkMethod, callable);

		verify(launch, times(1)).startTestItem(any(), argumentCaptor.capture());

		StartTestItemRQ request = argumentCaptor.getValue();

		Assertions.assertEquals("I am test id", request.getTestCaseId());
	}

	@Test
	public void retrieveParametrizedTestCaseIdTestWithoutKey() {

		setupFor(ParallelRunningHandlerTest.DummyTestWithoutKey.class);

		ArgumentCaptor<StartTestItemRQ> argumentCaptor = ArgumentCaptor.forClass(StartTestItemRQ.class);

		parallelRunningHandler.startTest(atomicTest);
		parallelRunningHandler.startTestMethod(runner.get(), frameworkMethod, callable);

		verify(launch, times(1)).startTestItem(any(), argumentCaptor.capture());

		StartTestItemRQ request = argumentCaptor.getValue();

		assertEquals("com.epam.reportportal.junit.ParallelRunningHandlerTest.DummyTestWithoutKey.method", request.getTestCaseId());
	}

	@Test
	public void shouldReturnProvidedIdWhenNotParametrizedSkipped() {

		setupFor(ClassWithTestCaseIdMethod.class);

		ArgumentCaptor<StartTestItemRQ> argumentCaptor = ArgumentCaptor.forClass(StartTestItemRQ.class);

		parallelRunningHandler.handleTestSkip(atomicTest);

		verify(launch, times(1)).startTestItem(any(), argumentCaptor.capture());

		StartTestItemRQ request = argumentCaptor.getValue();

		Assertions.assertEquals("testId", request.getTestCaseId());
	}

//	@Test
	public void retrieveParametrizedTestCaseIdTestWithKeySkipped() {

		setupFor(ParallelRunningHandlerTest.DummyTest.class);

		ArgumentCaptor<StartTestItemRQ> argumentCaptor = ArgumentCaptor.forClass(StartTestItemRQ.class);

		parallelRunningHandler.handleTestSkip(atomicTest);

		verify(launch, times(1)).startTestItem(any(), argumentCaptor.capture());

		StartTestItemRQ request = argumentCaptor.getValue();

		Assertions.assertEquals("I am test id", request.getTestCaseId());
	}

	@Test
	public void retrieveParametrizedTestCaseIdTestWithoutKeySkipped() {

		setupFor(ParallelRunningHandlerTest.DummyTestWithoutKey.class);

		ArgumentCaptor<StartTestItemRQ> argumentCaptor = ArgumentCaptor.forClass(StartTestItemRQ.class);

		parallelRunningHandler.handleTestSkip(atomicTest);

		verify(launch, times(1)).startTestItem(any(), argumentCaptor.capture());

		StartTestItemRQ request = argumentCaptor.getValue();

		assertEquals("com.epam.reportportal.junit.ParallelRunningHandlerTest.DummyTestWithoutKey.method", request.getTestCaseId());
	}

	@SuppressWarnings("unchecked")
	private static <T> T invoke(Object target, String methodName, Object... parameters) {
		try {
			return (T) MethodUtils.invokeMethod(target, true, methodName, parameters);
		} catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
			throw UncheckedThrow.throwUnchecked(e);
		}
	}

}
