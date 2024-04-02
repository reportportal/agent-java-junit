package com.epam.reportportal.junit.callback;

import static com.epam.reportportal.junit.utils.TestUtils.PROCESSING_TIMEOUT;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

import com.epam.reportportal.junit.ReportPortalListener;
import com.epam.reportportal.junit.features.callback.FailedDescriptionFeatureTest;
import com.epam.reportportal.junit.utils.TestUtils;
import com.epam.reportportal.listeners.ListenerParameters;
import com.epam.reportportal.service.ReportPortal;
import com.epam.reportportal.service.ReportPortalClient;
import com.epam.reportportal.util.test.CommonUtils;
import com.epam.ta.reportportal.ws.model.FinishTestItemRQ;
import com.epam.ta.reportportal.ws.model.StartTestItemRQ;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;

public class FailedDescriptionTest {
  private final String suiteId = CommonUtils.namedId("suite_");
  private final String classId = CommonUtils.namedId("class_");
  private final List<String> methodIds = Stream.generate(() -> CommonUtils.namedId("method_")).limit(2).collect(Collectors.toList());
  private final ReportPortalClient client = mock(ReportPortalClient.class);
  private final String testWithDescriptionAndAssertException = "testWithDescriptionAndAssertException";
  private final String testWithDescriptionAndStepError = "testWithDescriptionAndStepError";
  private final String testWithDescriptionAndException = "testWithDescriptionAndException";
  private final String testErrorMessagePattern = "%s(com.epam.reportportal.junit.features.callback.FailedDescriptionFeatureTest)\nError: \n%s";
  private final String suiteErrorMessagePattern = "Error: \n%s";
  private final String assertErrorMessage = "AssertionError: expected:<0> but was:<1>";
  private final String exceptionStepErrorMessage = "NoSuchElementException: Test error message";
  private final String exceptionErrorMessage = "RuntimeException: Test error message";
  private final String failedStatus = "FAILED";
  private final String passedStatus = "PASSED";

  @BeforeEach
  public void setupMock() {
    TestUtils.mockLaunch(client, null, suiteId, classId, methodIds);
    TestUtils.mockBatchLogging(client);
    ListenerParameters params = TestUtils.standardParameters();
    ReportPortalListener.setReportPortal(ReportPortal.create(client, params, TestUtils.testExecutor()));
  }

  @Test
  public void verify_retrieve_by_description() {
    TestUtils.runClasses(FailedDescriptionFeatureTest.class);


    ArgumentCaptor<StartTestItemRQ> startSuiteCaptor = ArgumentCaptor.forClass(StartTestItemRQ.class);
    verify(client, timeout(PROCESSING_TIMEOUT)).startTestItem(
        ArgumentMatchers.startsWith(TestUtils.ROOT_SUITE_PREFIX),
        startSuiteCaptor.capture()
    );

    ArgumentCaptor<StartTestItemRQ> startTestCaptor = ArgumentCaptor.forClass(StartTestItemRQ.class);
    verify(client, timeout(PROCESSING_TIMEOUT).atLeastOnce()).startTestItem(same(suiteId), startTestCaptor.capture());


    ArgumentCaptor<FinishTestItemRQ> finishSuiteCaptor = ArgumentCaptor.forClass(FinishTestItemRQ.class);
    verify(client, timeout(PROCESSING_TIMEOUT)).finishTestItem(
        ArgumentMatchers.startsWith(TestUtils.ROOT_SUITE_PREFIX),
        finishSuiteCaptor.capture()
    );

    ArgumentCaptor<FinishTestItemRQ> finishTestCaptor = ArgumentCaptor.forClass(FinishTestItemRQ.class);
    verify(client, timeout(PROCESSING_TIMEOUT).atLeastOnce()).finishTestItem(same(classId), finishTestCaptor.capture());

    FinishTestItemRQ finishSuite = finishSuiteCaptor.getAllValues().get(0);
    assertThat(finishSuite.getStatus(), equalTo(passedStatus));
    assertThat(finishSuite.getDescription(), equalTo(String.format(suiteErrorMessagePattern, assertErrorMessage)));

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

    assertThat(testCaseWithDescriptionAndAssertException.getDescription(), equalTo(String.format(testErrorMessagePattern, testWithDescriptionAndAssertException, assertErrorMessage)));
    assertThat(testCaseWithDescriptionAndAssertException.getStatus(), equalTo(failedStatus));

    assertThat(testCaseWithDescriptionAndStepError.getDescription(), equalTo(String.format(testErrorMessagePattern, testWithDescriptionAndStepError, exceptionStepErrorMessage)));
    assertThat(testCaseWithDescriptionAndStepError.getStatus(), equalTo(failedStatus));

    assertThat(tesCaseWithDescriptionAndException.getDescription(), equalTo(String.format(testErrorMessagePattern, testWithDescriptionAndException, exceptionErrorMessage)));
    assertThat(tesCaseWithDescriptionAndException.getStatus(), equalTo(failedStatus));

    assertThat(tesCaseWithDescriptionAndPassed.getStatus(), equalTo(passedStatus));
  }
}
