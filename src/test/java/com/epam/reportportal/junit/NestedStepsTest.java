package com.epam.reportportal.junit;

import com.epam.reportportal.annotations.Step;
import com.epam.reportportal.aspect.StepAspect;
import com.epam.reportportal.listeners.ListenerParameters;
import com.epam.reportportal.service.Launch;
import com.epam.ta.reportportal.ws.model.FinishTestItemRQ;
import com.epam.ta.reportportal.ws.model.StartTestItemRQ;
import com.nordstrom.automation.junit.AtomicTest;
import io.reactivex.Maybe;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.internal.runners.model.ReflectiveCallable;
import org.junit.runners.model.FrameworkMethod;
import org.mockito.ArgumentCaptor;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * @author <a href="mailto:ivan_budayeu@epam.com">Ivan Budayeu</a>
 */
public class NestedStepsTest {

	private final ParallelRunningContext parallelRunningContext = mock(ParallelRunningContext.class);
	private final Launch launch = mock(Launch.class);

	private FrameworkMethod frameworkMethod;
	private ReflectiveCallable callable;
	private final ThreadLocal<Object> runner = new ThreadLocal<>();
	private final ThreadLocal<Object> target = new ThreadLocal<>();

	private NestedStepsTest.NestedStepsParallelRunningHandler parallelRunningHandler;

	private class NestedStepsParallelRunningHandler extends ParallelRunningHandler {

		/**
		 * Constructor: Instantiate a parallel running handler
		 *
		 * @param parallelRunningContext test execution context manager
		 */
		public NestedStepsParallelRunningHandler(ParallelRunningContext parallelRunningContext) {
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
	}

	@Before
	public void init() throws NoSuchMethodException {
		frameworkMethod = new FrameworkMethod(this.getClass().getDeclaredMethod("shouldSendNestedStepRequest"));
		callable = new ReflectiveCallable() {
			@Override
			protected Object runReflectiveCall() throws Throwable {
				return target.get();
			}
		};
		parallelRunningHandler = new NestedStepsParallelRunningHandler(parallelRunningContext);

		when(launch.getParameters()).thenReturn(new ListenerParameters());
		StepAspect.addLaunch("default", launch);
	}

	@Test
	public void shouldSendNestedStepRequest() {
		Maybe<String> testMethodId = createIdMaybe("testMethodId");
		when(launch.startTestItem(any(), any())).thenReturn(testMethodId);
		ArgumentCaptor<StartTestItemRQ> nestedStepCaptor = ArgumentCaptor.forClass(StartTestItemRQ.class);

		parallelRunningHandler.startTestMethod(runner, frameworkMethod, callable);
		step();

		verify(launch, times(1)).startTestItem(eq(testMethodId), nestedStepCaptor.capture());

		StartTestItemRQ nestedStepRequest = nestedStepCaptor.getValue();
		Assert.assertEquals("step", nestedStepRequest.getName());
		Assert.assertFalse(nestedStepRequest.isHasStats());
		Assert.assertEquals("STEP", nestedStepRequest.getType());
	}

	@Test
	public void failedNestedStep() {
		Maybe<String> testMethodId = createIdMaybe("testMethodId");
		when(launch.startTestItem(any(), any())).thenReturn(testMethodId);
		Maybe<String> nestedStepId = createIdMaybe("nestedStepId");
		when(launch.startTestItem(eq(testMethodId), any())).thenReturn(nestedStepId);

		ArgumentCaptor<StartTestItemRQ> nestedStepStartCaptor = ArgumentCaptor.forClass(StartTestItemRQ.class);
		ArgumentCaptor<FinishTestItemRQ> nestedStepFinishCaptor = ArgumentCaptor.forClass(FinishTestItemRQ.class);

		parallelRunningHandler.startTestMethod(runner, frameworkMethod, callable);
		try {
			failedStep();
		} catch (Exception ex) {
			//do nothing
		}

		verify(launch, times(1)).startTestItem(eq(testMethodId), nestedStepStartCaptor.capture());
		verify(launch, times(1)).finishTestItem(eq(nestedStepId), nestedStepFinishCaptor.capture());

		StartTestItemRQ nestedStepStartRequest = nestedStepStartCaptor.getValue();
		Assert.assertEquals("failedStep", nestedStepStartRequest.getName());
		Assert.assertFalse(nestedStepStartRequest.isHasStats());

		FinishTestItemRQ nestedStepFinishRequest = nestedStepFinishCaptor.getValue();
		Assert.assertEquals("FAILED", nestedStepFinishRequest.getStatus());
	}

	@Test
	public void multiLevelNestedStep() {
		Maybe<String> testMethodId = createIdMaybe("testMethodId");
		when(launch.startTestItem(any(), any())).thenReturn(testMethodId);
		Maybe<String> stepWithStepInsideId = createIdMaybe("stepWithStepInsideId");
		when(launch.startTestItem(eq(testMethodId), any())).thenReturn(stepWithStepInsideId);
		ArgumentCaptor<StartTestItemRQ> nestedStepCaptor = ArgumentCaptor.forClass(StartTestItemRQ.class);

		parallelRunningHandler.startTestMethod(runner, frameworkMethod, callable);
		stepWithStepInside();

		verify(launch, times(1)).startTestItem(eq(testMethodId), nestedStepCaptor.capture());
		verify(launch, times(1)).startTestItem(eq(stepWithStepInsideId), nestedStepCaptor.capture());

		List<StartTestItemRQ> nestedSteps = nestedStepCaptor.getAllValues();
		Assert.assertEquals(2, nestedSteps.size());

		StartTestItemRQ outerStepRequest = nestedSteps.get(0);
		StartTestItemRQ innerStepRequest = nestedSteps.get(1);

		Assert.assertEquals("stepWithStepInside", outerStepRequest.getName());
		Assert.assertFalse(outerStepRequest.isHasStats());
		Assert.assertEquals("STEP", outerStepRequest.getType());

		Assert.assertEquals("step", innerStepRequest.getName());
		Assert.assertFalse(innerStepRequest.isHasStats());
		Assert.assertEquals("STEP", innerStepRequest.getType());
	}

	@Step
	void stepWithStepInside() {
		step();
	}

	@Step
	public void step() {
		System.out.println("hi there");
	}

	@Step
	public void failedStep() {
		throw new RuntimeException();
	}

	public static Maybe<String> createIdMaybe(String id) {
		return Maybe.create(emitter -> {
			emitter.onSuccess(id);
			emitter.onComplete();
		});
	}
}
