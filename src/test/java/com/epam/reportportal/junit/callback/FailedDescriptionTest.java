package com.epam.reportportal.junit.callback;

import com.epam.reportportal.junit.ReportPortalListener;
import com.epam.reportportal.junit.features.callback.FailedDescriptionFeatureTest;
import com.epam.reportportal.junit.utils.TestUtils;
import com.epam.reportportal.listeners.ListenerParameters;
import com.epam.reportportal.service.ReportPortal;
import com.epam.reportportal.service.ReportPortalClient;
import com.epam.reportportal.util.test.CommonUtils;
import com.epam.ta.reportportal.ws.model.FinishTestItemRQ;
import com.epam.ta.reportportal.ws.model.StartTestItemRQ;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.epam.reportportal.junit.utils.TestUtils.PROCESSING_TIMEOUT;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.startsWith;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.*;

public class FailedDescriptionTest {
	private final String suiteId = CommonUtils.namedId("suite_");
	private final String classId = CommonUtils.namedId("class_");
	private final List<String> methodIds = Stream.generate(() -> CommonUtils.namedId("method_")).limit(2).collect(Collectors.toList());
    private final ReportPortalClient client = mock(ReportPortalClient.class);
    private final ExecutorService executor = CommonUtils.testExecutor();
	private final String testWithDescriptionAndAssertException = "testWithDescriptionAndAssertException";
	private final String testWithDescriptionAndStepError = "testWithDescriptionAndStepError";
	private final String testWithDescriptionAndException = "testWithDescriptionAndException";
	private final String testErrorMessagePattern = "%s(com.epam.reportportal.junit.features.callback.FailedDescriptionFeatureTest)\nError: \n%s";
	private final String assertErrorMessage = "java.lang.AssertionError: expected:<0> but was:<1>";
	private final String exceptionStepErrorMessage = "java.util.NoSuchElementException: Test error message";
	private final String exceptionErrorMessage = "java.lang.RuntimeException: Test error message";
	private final String failedStatus = "FAILED";
	private final String passedStatus = "PASSED";

	@BeforeEach
	public void setupMock() {
		TestUtils.mockLaunch(client, null, suiteId, classId, methodIds);
		TestUtils.mockBatchLogging(client);
        ListenerParameters params = TestUtils.standardParameters();
        ReportPortalListener.setReportPortal(ReportPortal.create(client, params, executor));
	}

    @AfterEach
    public void tearDown() {
        CommonUtils.shutdownExecutorService(executor);
    }

	@Test
	public void verify_retrieve_by_description() {
		TestUtils.runClasses(FailedDescriptionFeatureTest.class);

		ArgumentCaptor<StartTestItemRQ> startTestCaptor = ArgumentCaptor.forClass(StartTestItemRQ.class);
		verify(client, timeout(PROCESSING_TIMEOUT).atLeastOnce()).startTestItem(same(suiteId), startTestCaptor.capture());

		ArgumentCaptor<FinishTestItemRQ> finishTestCaptor = ArgumentCaptor.forClass(FinishTestItemRQ.class);
		verify(client, timeout(PROCESSING_TIMEOUT).atLeastOnce()).finishTestItem(same(classId), finishTestCaptor.capture());

		List<FinishTestItemRQ> finishTests = finishTestCaptor.getAllValues();
		FinishTestItemRQ testCaseWithDescriptionAndAssertException = finishTests.stream()
				.filter(f -> f.getDescription().startsWith(testWithDescriptionAndAssertException))
				.findFirst()
				.orElseThrow(NoSuchElementException::new);
		FinishTestItemRQ testCaseWithDescriptionAndStepError = finishTests.stream()
				.filter(f -> f.getDescription().startsWith(testWithDescriptionAndStepError))
				.findFirst()
				.orElseThrow(NoSuchElementException::new);
		FinishTestItemRQ tesCaseWithDescriptionAndException = finishTests.stream()
				.filter(f -> f.getDescription().startsWith(testWithDescriptionAndException))
				.findFirst()
				.orElseThrow(NoSuchElementException::new);
		FinishTestItemRQ tesCaseWithDescriptionAndPassed = finishTests.stream()
				.filter(f -> f.getDescription() == null)
				.findFirst()
				.orElseThrow(NoSuchElementException::new);

		assertThat(
				testCaseWithDescriptionAndAssertException.getDescription(),
				startsWith(String.format(testErrorMessagePattern, testWithDescriptionAndAssertException, assertErrorMessage))
		);
		assertThat(testCaseWithDescriptionAndAssertException.getStatus(), equalTo(failedStatus));

		assertThat(
				testCaseWithDescriptionAndStepError.getDescription(),
				startsWith(String.format(testErrorMessagePattern, testWithDescriptionAndStepError, exceptionStepErrorMessage))
		);
		assertThat(testCaseWithDescriptionAndStepError.getStatus(), equalTo(failedStatus));

		assertThat(
				tesCaseWithDescriptionAndException.getDescription(),
				startsWith(String.format(testErrorMessagePattern, testWithDescriptionAndException, exceptionErrorMessage))
		);
		assertThat(tesCaseWithDescriptionAndException.getStatus(), equalTo(failedStatus));

		assertThat(tesCaseWithDescriptionAndPassed.getStatus(), equalTo(passedStatus));
	}
}
