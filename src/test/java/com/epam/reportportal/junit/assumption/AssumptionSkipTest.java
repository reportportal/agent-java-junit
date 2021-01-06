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

package com.epam.reportportal.junit.assumption;

import com.epam.reportportal.junit.ReportPortalListener;
import com.epam.reportportal.junit.features.assumption.AssumptionViolatedTest;
import com.epam.reportportal.junit.utils.TestUtils;
import com.epam.reportportal.listeners.ItemStatus;
import com.epam.reportportal.listeners.LogLevel;
import com.epam.reportportal.restendpoint.http.MultiPartRequest;
import com.epam.reportportal.service.ReportPortal;
import com.epam.reportportal.service.ReportPortalClient;
import com.epam.reportportal.util.test.CommonUtils;
import com.epam.ta.reportportal.ws.model.FinishTestItemRQ;
import com.epam.ta.reportportal.ws.model.log.SaveLogRQ;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.*;

public class AssumptionSkipTest {

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
	public void verify_assumption_violated_test_logs_message_and_marked_as_skipped() {
		TestUtils.runClasses(AssumptionViolatedTest.class);

		ArgumentCaptor<FinishTestItemRQ> captor = ArgumentCaptor.forClass(FinishTestItemRQ.class);
		verify(client, times(1)).finishTestItem(same(methodId), captor.capture());
		ArgumentCaptor<MultiPartRequest> logCaptor = ArgumentCaptor.forClass(MultiPartRequest.class);
		verify(client, atLeast(1)).log(logCaptor.capture());

		FinishTestItemRQ item = captor.getAllValues().get(0);
		assertThat(item.getStatus(), allOf(notNullValue(), equalTo(ItemStatus.SKIPPED.name())));

		List<SaveLogRQ> expectedErrorList = logCaptor.getAllValues()
				.stream()
				.flatMap(l -> l.getSerializedRQs().stream())
				.map(MultiPartRequest.MultiPartSerialized::getRequest)
				.filter(l -> l instanceof List)
				.flatMap(l -> ((List<?>) l).stream())
				.filter(l -> l instanceof SaveLogRQ)
				.map(l -> (SaveLogRQ) l)
				.filter(l -> LogLevel.WARN.name().equals(l.getLevel()))
				.filter(l -> l.getMessage() != null && l.getMessage().contains(AssumptionViolatedTest.VIOLATION_MESSAGE))
				.collect(Collectors.toList());
		assertThat(expectedErrorList, hasSize(1));
		SaveLogRQ expectedError = expectedErrorList.get(0);
		assertThat(expectedError.getItemUuid(), equalTo(methodId));
	}
}
