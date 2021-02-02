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

package com.epam.reportportal.junit.beforeafter;

import com.epam.reportportal.junit.ReportPortalListener;
import com.epam.reportportal.junit.features.beforeafter.BeforeEachFailedForSecondParameterTest;
import com.epam.reportportal.junit.utils.TestUtils;
import com.epam.reportportal.listeners.ItemStatus;
import com.epam.reportportal.listeners.ItemType;
import com.epam.reportportal.service.Launch;
import com.epam.reportportal.service.ReportPortal;
import com.epam.reportportal.service.ReportPortalClient;
import com.epam.reportportal.util.test.CommonUtils;
import com.epam.ta.reportportal.ws.model.FinishTestItemRQ;
import com.epam.ta.reportportal.ws.model.StartTestItemRQ;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.epam.reportportal.junit.utils.TestUtils.PROCESSING_TIMEOUT;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.sameInstance;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.*;

public class BeforeEachFailedInParameterizedTest {
	private static final int TEST_METHOD_NUMBER = 4;

	private final String classId = CommonUtils.namedId("class_");
	private final List<String> methodIds = Stream.generate(() -> CommonUtils.namedId("method_"))
			.limit(TEST_METHOD_NUMBER)
			.collect(Collectors.toList());

	private final ReportPortalClient client = mock(ReportPortalClient.class);

	@BeforeEach
	public void setupMock() {
		TestUtils.mockLaunch(client, null, null, classId, methodIds);
		TestUtils.mockBatchLogging(client);
		ReportPortalListener.setReportPortal(ReportPortal.create(client, TestUtils.standardParameters(),
				Executors.newSingleThreadExecutor()));
	}

	@Test
	public void verify_after_each_failure_in_parameterized_test() {
		TestUtils.runClasses(BeforeEachFailedForSecondParameterTest.class);

		ArgumentCaptor<StartTestItemRQ> startCapture = ArgumentCaptor.forClass(StartTestItemRQ.class);
		verify(client, timeout(PROCESSING_TIMEOUT).times(TEST_METHOD_NUMBER)).startTestItem(same(classId), startCapture.capture());

		ArgumentCaptor<FinishTestItemRQ> finishCaptor = ArgumentCaptor.forClass(FinishTestItemRQ.class);
		methodIds.forEach(id -> verify(client, timeout(PROCESSING_TIMEOUT)).finishTestItem(same(id), finishCaptor.capture()));

		StartTestItemRQ startRq = startCapture.getAllValues().get(3);
		assertThat(startRq.getType(), equalTo(ItemType.STEP.name()));

		List<FinishTestItemRQ> finishRqs = finishCaptor.getAllValues();

		finishRqs.subList(0, 2).forEach(rq -> assertThat(rq.getStatus(), equalTo(ItemStatus.PASSED.name())));
		assertThat(finishRqs.get(2).getStatus(), equalTo(ItemStatus.FAILED.name()));
		FinishTestItemRQ finishItem = finishRqs.get(3);
		assertThat(finishItem.getStatus(), equalTo(ItemStatus.SKIPPED.name()));
		assertThat(finishItem.getIssue(), sameInstance(Launch.NOT_ISSUE));
	}
}
