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

package com.epam.reportportal.junit.ignore;

import com.epam.reportportal.junit.ReportPortalListener;
import com.epam.reportportal.junit.features.ignore.ParameterizedIgnoredTest;
import com.epam.reportportal.junit.utils.TestUtils;
import com.epam.reportportal.listeners.ItemStatus;
import com.epam.reportportal.service.ReportPortal;
import com.epam.reportportal.service.ReportPortalClient;
import com.epam.reportportal.util.test.CommonUtils;
import com.epam.ta.reportportal.ws.model.FinishTestItemRQ;
import com.epam.ta.reportportal.ws.model.StartTestItemRQ;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.*;

public class IgnoreParameterizedTest {

	private final String classId = CommonUtils.namedId("class_");
	private final List<String> methodIds = Stream.generate(() -> CommonUtils.namedId("method_")).limit(2).collect(Collectors.toList());

	private final ReportPortalClient client = mock(ReportPortalClient.class);

	@BeforeEach
	public void setupMock() {
		TestUtils.mockLaunch(client, null, null, classId, methodIds);
		TestUtils.mockLogging(client);
		ReportPortalListener.setReportPortal(ReportPortal.create(client, TestUtils.standardParameters()));
	}

	@Test
	public void verify_a_test_with_one_retry() {
		TestUtils.runClasses(ParameterizedIgnoredTest.class);

		ArgumentCaptor<StartTestItemRQ> startCaptor = ArgumentCaptor.forClass(StartTestItemRQ.class);
		ArgumentCaptor<FinishTestItemRQ> finishCaptor = ArgumentCaptor.forClass(FinishTestItemRQ.class);
		verify(client, times(2)).startTestItem(same(classId), startCaptor.capture());
		verify(client, times(1)).finishTestItem(same(methodIds.get(0)), finishCaptor.capture());
		verify(client, times(1)).finishTestItem(same(methodIds.get(1)), finishCaptor.capture());

		List<StartTestItemRQ> startRqs = startCaptor.getAllValues();
		assertThat(startRqs.get(0).getTestCaseId(), not(equalTo(startRqs.get(1).getTestCaseId()))); // test case ID should be parameterized
		assertThat(startRqs.get(0).getParameters(), allOf(notNullValue(), hasSize(1)));
		assertThat(startRqs.get(1).getParameters(), allOf(notNullValue(), hasSize(1)));
		assertThat(startRqs.get(0).getCodeRef(), equalTo(startRqs.get(1).getCodeRef()));

		finishCaptor.getAllValues().forEach(i -> {
			assertThat(i.getIssue(), nullValue());
			assertThat(i.getStatus(), equalTo(ItemStatus.SKIPPED.name()));
		});
	}

}
