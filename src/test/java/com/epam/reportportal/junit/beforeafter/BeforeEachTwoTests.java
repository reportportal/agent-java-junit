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
import com.epam.reportportal.junit.features.beforeafter.BeforeTwoTests;
import com.epam.reportportal.junit.utils.TestUtils;
import com.epam.reportportal.listeners.ItemStatus;
import com.epam.reportportal.listeners.ItemType;
import com.epam.reportportal.service.ReportPortal;
import com.epam.reportportal.service.ReportPortalClient;
import com.epam.reportportal.util.test.CommonUtils;
import com.epam.ta.reportportal.ws.model.FinishTestItemRQ;
import com.epam.ta.reportportal.ws.model.StartTestItemRQ;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.epam.reportportal.junit.utils.TestUtils.PROCESSING_TIMEOUT;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.*;

public class BeforeEachTwoTests {

	private final String classId = CommonUtils.namedId("class_");
	private final List<String> methodIds = Stream.generate(() -> CommonUtils.namedId("method_")).limit(4).collect(Collectors.toList());

	private final ReportPortalClient client = mock(ReportPortalClient.class);

	@BeforeEach
	public void setupMock() {
		TestUtils.mockLaunch(client, null, null, classId, methodIds);
		TestUtils.mockBatchLogging(client);
		ReportPortalListener.setReportPortal(ReportPortal.create(client, TestUtils.standardParameters(), TestUtils.testExecutor()));
	}

	@Test
	public void agent_should_report_two_before_each_methods_in_one_test_correctly() {
		TestUtils.runClasses(BeforeTwoTests.class);

		ArgumentCaptor<StartTestItemRQ> startCaptor = ArgumentCaptor.forClass(StartTestItemRQ.class);
		ArgumentCaptor<FinishTestItemRQ> finishCaptor = ArgumentCaptor.forClass(FinishTestItemRQ.class);
		verify(client, timeout(PROCESSING_TIMEOUT).times(4)).startTestItem(same(classId), startCaptor.capture());
		verify(client, timeout(PROCESSING_TIMEOUT)).finishTestItem(same(methodIds.get(0)), finishCaptor.capture());
		verify(client, timeout(PROCESSING_TIMEOUT)).finishTestItem(same(methodIds.get(1)), finishCaptor.capture());
		verify(client, timeout(PROCESSING_TIMEOUT)).finishTestItem(same(methodIds.get(2)), finishCaptor.capture());
		verify(client, timeout(PROCESSING_TIMEOUT)).finishTestItem(same(methodIds.get(3)), finishCaptor.capture());

		List<StartTestItemRQ> startItems = startCaptor.getAllValues();
		finishCaptor.getAllValues().forEach(e -> assertThat(e.getStatus(), equalTo(ItemStatus.PASSED.name())));

		List<StartTestItemRQ> beforeStarts = Arrays.asList(startItems.get(0), startItems.get(2));

		beforeStarts.forEach(e -> {
			assertThat("@Before has correct code reference",
					e.getCodeRef(),
					equalTo(BeforeTwoTests.class.getCanonicalName() + ".beforeEach")
			);
			assertThat("@Before has correct item type", e.getType(), equalTo(ItemType.BEFORE_METHOD.name()));
		});
	}

}
