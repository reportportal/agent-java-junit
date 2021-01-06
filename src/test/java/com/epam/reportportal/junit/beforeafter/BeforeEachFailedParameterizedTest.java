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
import com.epam.reportportal.junit.features.beforeafter.BeforeFailedParametrizedTest;
import com.epam.reportportal.junit.utils.TestUtils;
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
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.*;

public class BeforeEachFailedParameterizedTest {

	private final String classId = CommonUtils.namedId("class_");
	private final List<String> methodIds = Stream.generate(() -> CommonUtils.namedId("method_")).limit(4).collect(Collectors.toList());

	private final ReportPortalClient client = mock(ReportPortalClient.class);

	@BeforeEach
	public void setupMock() {
		TestUtils.mockLaunch(client, null, null, classId, methodIds);
		TestUtils.mockBatchLogging(client);
		ReportPortalListener.setReportPortal(ReportPortal.create(client, TestUtils.standardParameters(),
				Executors.newSingleThreadExecutor()));
	}

	@Test
	public void agent_should_report_skipped_parametrized_tests_in_case_of_failed_before_each() {
		TestUtils.runClasses(BeforeFailedParametrizedTest.class);

		ArgumentCaptor<StartTestItemRQ> startCaptor = ArgumentCaptor.forClass(StartTestItemRQ.class);
		ArgumentCaptor<FinishTestItemRQ> finishCaptor = ArgumentCaptor.forClass(FinishTestItemRQ.class);
		verify(client, times(4)).startTestItem(same(classId), startCaptor.capture());
		verify(client, times(1)).finishTestItem(same(methodIds.get(0)), finishCaptor.capture());
		verify(client, times(1)).finishTestItem(same(methodIds.get(1)), finishCaptor.capture());
		verify(client, times(1)).finishTestItem(same(methodIds.get(2)), finishCaptor.capture());
		verify(client, times(1)).finishTestItem(same(methodIds.get(3)), finishCaptor.capture());

		List<StartTestItemRQ> startItems = startCaptor.getAllValues();
		assertThat("There are 6 item created: parent suite, suite, 2 @BeforeEach, @Test 1 and @Test 2", startItems, hasSize(4));

		List<FinishTestItemRQ> finishItems = finishCaptor.getAllValues();
		FinishTestItemRQ beforeEachFinish = finishItems.get(0);

		assertThat("@Before failed", beforeEachFinish.getStatus(), equalTo("FAILED"));

		IntStream.rangeClosed(0, 1).boxed().forEach(i -> {
			StartTestItemRQ testStart = startItems.get(1 + (i * 2));
			assertThat(
					"@Test has correct code reference",
					testStart.getCodeRef(),
					equalTo(BeforeFailedParametrizedTest.class.getCanonicalName() + ".testBeforeEachFailed")
			);
			assertThat("@Test has correct name", testStart.getName(), equalTo("testBeforeEachFailed"));
			assertThat("@Test has correct type", testStart.getType(), equalTo(ItemType.STEP.name()));

			FinishTestItemRQ testFinish = finishItems.get(1 + (i * 2));
			assertThat("@Test reported as skipped", testFinish.getStatus(), equalTo("SKIPPED"));
			assertThat("@Test issue muted", testFinish.getIssue(), sameInstance(Launch.NOT_ISSUE));
		});
	}

}
