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

package com.epam.reportportal.junit.retry;

import com.epam.reportportal.junit.ReportPortalListener;
import com.epam.reportportal.junit.features.retry.BasicRetryPassedTest;
import com.epam.reportportal.junit.utils.TestUtils;
import com.epam.reportportal.listeners.ItemStatus;
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
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.epam.reportportal.junit.utils.TestUtils.PROCESSING_TIMEOUT;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.*;

public class BasicRetryTest {

	private final String classId = CommonUtils.namedId("class_");
	private final List<String> methodIds = Stream.generate(() -> CommonUtils.namedId("method_")).limit(2).collect(Collectors.toList());

	private final ReportPortalClient client = mock(ReportPortalClient.class);
	private final ExecutorService executor = CommonUtils.testExecutor();

	@BeforeEach
	public void setupMock() {
		TestUtils.mockLaunch(client, null, null, classId, methodIds);
		TestUtils.mockBatchLogging(client);
		ReportPortalListener.setReportPortal(ReportPortal.create(client, TestUtils.standardParameters(), executor));
	}

	@AfterEach
	public void tearDown() {
		CommonUtils.shutdownExecutorService(executor);
	}

	@Test
	public void verify_a_test_with_one_retry() {
		TestUtils.runClasses(BasicRetryPassedTest.class);

		ArgumentCaptor<StartTestItemRQ> startCaptor = ArgumentCaptor.forClass(StartTestItemRQ.class);
		ArgumentCaptor<FinishTestItemRQ> finishCaptor = ArgumentCaptor.forClass(FinishTestItemRQ.class);
		verify(client, timeout(PROCESSING_TIMEOUT).times(2)).startTestItem(same(classId), startCaptor.capture());
		verify(client, timeout(PROCESSING_TIMEOUT).times(2)).finishTestItem(same(methodIds.get(0)), finishCaptor.capture());
		verify(client, timeout(PROCESSING_TIMEOUT)).finishTestItem(same(methodIds.get(1)), finishCaptor.capture());

		List<StartTestItemRQ> startItems = startCaptor.getAllValues();
		assertThat(startItems.get(0).isRetry(), nullValue());
		assertThat(startItems.get(1).isRetry(), allOf(notNullValue(), equalTo(Boolean.TRUE)));

		List<FinishTestItemRQ> finishItems = finishCaptor.getAllValues();
		assertThat(finishItems.get(0).isRetry(), nullValue());
		assertThat(finishItems.get(1).isRetry(), allOf(notNullValue(), equalTo(Boolean.TRUE)));
		assertThat(finishItems.get(1).getStatus(), allOf(notNullValue(), equalTo(finishItems.get(0).getStatus())));
		assertThat(finishItems.get(2).isRetry(), nullValue());
		assertThat(finishItems.get(2).getStatus(), allOf(notNullValue(), equalTo(ItemStatus.PASSED.name())));
	}

}
