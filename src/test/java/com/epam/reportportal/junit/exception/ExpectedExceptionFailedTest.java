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

package com.epam.reportportal.junit.exception;

import com.epam.reportportal.junit.ReportPortalListener;
import com.epam.reportportal.junit.features.exception.ExpectedExceptionNotThrownTest;
import com.epam.reportportal.junit.utils.TestUtils;
import com.epam.reportportal.listeners.ItemStatus;
import com.epam.reportportal.service.ReportPortal;
import com.epam.reportportal.service.ReportPortalClient;
import com.epam.reportportal.util.test.CommonUtils;
import com.epam.ta.reportportal.ws.model.FinishTestItemRQ;
import com.epam.ta.reportportal.ws.model.log.SaveLogRQ;
import okhttp3.MultipartBody;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import static com.epam.reportportal.junit.utils.TestUtils.PROCESSING_TIMEOUT;
import static com.epam.reportportal.junit.utils.TestUtils.toSaveLogRQ;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.*;

public class ExpectedExceptionFailedTest {

	private static final String EXPECTED_ERROR = "java.lang.AssertionError: Expected test to throw (an instance of java.lang.IllegalArgumentException and exception with message a string containing \"My error message\")";
	private final String classId = CommonUtils.namedId("class_");
	private final String methodId = CommonUtils.namedId("method_");

	private final ReportPortalClient client = mock(ReportPortalClient.class);

	@BeforeEach
	public void setupMock() {
		TestUtils.mockLaunch(client, null, null, classId, methodId);
		TestUtils.mockBatchLogging(client);
		ReportPortalListener.setReportPortal(ReportPortal.create(client, TestUtils.standardParameters(),
				Executors.newSingleThreadExecutor()));
	}

	@Test
	public void verify_expected_exception_was_not_thrown() {
		TestUtils.runClasses(ExpectedExceptionNotThrownTest.class);

		verify(client).startTestItem(ArgumentMatchers.startsWith("root_"), any());
		verify(client).startTestItem(same(classId), any());
		ArgumentCaptor<FinishTestItemRQ> finishTestCaptor = ArgumentCaptor.forClass(FinishTestItemRQ.class);
		verify(client, timeout(PROCESSING_TIMEOUT).times(2)).finishTestItem(same(methodId), finishTestCaptor.capture());
		ArgumentCaptor<FinishTestItemRQ> finishSuiteCaptor = ArgumentCaptor.forClass(FinishTestItemRQ.class);
		verify(client).finishTestItem(same(classId), finishSuiteCaptor.capture());

		List<FinishTestItemRQ> items = finishTestCaptor.getAllValues();
		assertThat(items.get(0).getStatus(), equalTo(ItemStatus.PASSED.name()));
		assertThat(items.get(1).getStatus(), equalTo(ItemStatus.FAILED.name()));

		ArgumentCaptor<List<MultipartBody.Part>> logRqCaptor = ArgumentCaptor.forClass(List.class);
		verify(client, timeout(PROCESSING_TIMEOUT).atLeastOnce()).log(logRqCaptor.capture());

		List<SaveLogRQ> expectedErrorList = toSaveLogRQ(logRqCaptor.getAllValues()).stream()
				.filter(l -> l.getMessage() != null && l.getMessage().startsWith(EXPECTED_ERROR))
				.collect(Collectors.toList());
		assertThat(expectedErrorList, hasSize(1));
		SaveLogRQ expectedError = expectedErrorList.get(0);
		assertThat(expectedError.getItemUuid(), equalTo(methodId));
	}
}
