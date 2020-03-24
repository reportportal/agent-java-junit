package com.epam.reportportal.junit;

import com.epam.reportportal.annotations.TestCaseId;
import com.epam.reportportal.service.Launch;
import com.epam.ta.reportportal.ws.model.StartTestItemRQ;
import com.nordstrom.automation.junit.AtomicTest;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runners.model.FrameworkMethod;
import org.mockito.ArgumentCaptor;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;

/**
 * @author <a href="mailto:ivan_budayeu@epam.com">Ivan Budayeu</a>
 */
public class TestCaseIdTest {

	private final ParallelRunningContext parallelRunningContext = mock(ParallelRunningContext.class);
	private final Launch launch = mock(Launch.class);

	private FrameworkMethod frameworkMethod;
	private final ThreadLocal<Object> runner = new ThreadLocal<>();
	private final ThreadLocal<Object> target = new ThreadLocal<>();

	private final AtomicTest atomicTest = mock(AtomicTest.class);

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

		@Override
		protected Object getTargetForRunner(Object runner) {
			return target.get();
		}

		@Override
		protected AtomicTest getAtomicTest(Object runner) {
			return atomicTest;
		}
	}

	@Before
	public void init() throws NoSuchMethodException {
		frameworkMethod = new FrameworkMethod(this.getClass().getDeclaredMethod("methodForTesting"));
		parallelRunningHandler = new TestCaseIdParallelRunningHandler(parallelRunningContext);
	}

	@TestCaseId(value = "testId")
	public void methodForTesting() {

	}

	@Test
	public void shouldReturnProvidedIdWhenNotParametrized() {

		ArgumentCaptor<StartTestItemRQ> argumentCaptor = ArgumentCaptor.forClass(StartTestItemRQ.class);

		parallelRunningHandler.startTestMethod(frameworkMethod, runner.get());

		verify(launch, times(1)).startTestItem(any(), argumentCaptor.capture());

		StartTestItemRQ request = argumentCaptor.getValue();

		Assert.assertEquals("testId", request.getTestCaseId());
	}

	@Test
	public void retrieveParametrizedTestCaseIdTestWithKey() throws NoSuchMethodException {

		target.set(new ParallelRunningHandlerTest.DummyTest());

		ArgumentCaptor<StartTestItemRQ> argumentCaptor = ArgumentCaptor.forClass(StartTestItemRQ.class);

		FrameworkMethod parametrizedFrameworkMethod = new FrameworkMethod(ParallelRunningHandlerTest.DummyTest.class.getDeclaredMethod(
				"method"));

		parallelRunningHandler.startTestMethod(parametrizedFrameworkMethod, runner.get());

		verify(launch, times(1)).startTestItem(any(), argumentCaptor.capture());

		StartTestItemRQ request = argumentCaptor.getValue();

		Assert.assertEquals("I am test id", request.getTestCaseId());
	}

	@Test
	public void retrieveParametrizedTestCaseIdTestWithoutKey() throws NoSuchMethodException {

		target.set(new ParallelRunningHandlerTest.DummyTestWithoutKey());

		ArgumentCaptor<StartTestItemRQ> argumentCaptor = ArgumentCaptor.forClass(StartTestItemRQ.class);

		FrameworkMethod parametrizedFrameworkMethod = new FrameworkMethod(ParallelRunningHandlerTest.DummyTest.class.getDeclaredMethod(
				"method"));

		parallelRunningHandler.startTestMethod(parametrizedFrameworkMethod, runner.get());

		verify(launch, times(1)).startTestItem(any(), argumentCaptor.capture());

		StartTestItemRQ request = argumentCaptor.getValue();

		assertEquals("com.epam.reportportal.junit.ParallelRunningHandlerTest.DummyTest.method", request.getTestCaseId());
	}
}
