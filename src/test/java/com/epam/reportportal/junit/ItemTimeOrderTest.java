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

package com.epam.reportportal.junit;

import com.epam.reportportal.junit.features.suites.SuiteOfSuitesClass;
import com.epam.reportportal.junit.utils.TestUtils;
import com.epam.reportportal.service.ReportPortal;
import com.epam.reportportal.service.ReportPortalClient;
import com.epam.reportportal.util.test.CommonUtils;
import com.epam.ta.reportportal.ws.model.StartTestItemRQ;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;

import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static com.epam.reportportal.junit.utils.TestUtils.PROCESSING_TIMEOUT;
import static java.util.Optional.ofNullable;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.mockito.Mockito.*;

public class ItemTimeOrderTest {
	private static final int TEST_NUMBER = 2;

	private final String firstSuiteId = CommonUtils.namedId("suite1_");
	private final String secondSuiteId = CommonUtils.namedId("suite2_");
	private final List<String> classIds = Stream.generate(() -> CommonUtils.namedId("class_"))
			.limit(TEST_NUMBER)
			.collect(Collectors.toList());
	private final List<String> methodIds = Stream.generate(() -> CommonUtils.namedId("method_"))
			.limit(TEST_NUMBER)
			.collect(Collectors.toList());
	private final List<Pair<String, String>> tests = IntStream.range(0, TEST_NUMBER)
			.mapToObj(i -> Pair.of(classIds.get(i), methodIds.get(i)))
			.collect(Collectors.toList());

	private final ReportPortalClient client = mock(ReportPortalClient.class);

	@BeforeEach
	public void setupMock() {
		TestUtils.mockLaunch(client, null, firstSuiteId, secondSuiteId, classIds);
		TestUtils.mockNestedSteps(client, tests);
		TestUtils.mockBatchLogging(client);
		ReportPortalListener.setReportPortal(ReportPortal.create(client, TestUtils.standardParameters(), TestUtils.testExecutor()));
	}

	@Test
	public void verify_test_hierarchy_on_suite_of_suites() {
		TestUtils.runClasses(SuiteOfSuitesClass.class);

		ArgumentCaptor<StartTestItemRQ> suiteCaptor = ArgumentCaptor.forClass(StartTestItemRQ.class);
		verify(client, timeout(PROCESSING_TIMEOUT)).startTestItem(suiteCaptor.capture());
		verify(client, timeout(PROCESSING_TIMEOUT)).startTestItem(
				ArgumentMatchers.startsWith(TestUtils.ROOT_SUITE_PREFIX),
				suiteCaptor.capture()
		);
		ArgumentCaptor<StartTestItemRQ> testCaptor = ArgumentCaptor.forClass(StartTestItemRQ.class);
		verify(client, timeout(PROCESSING_TIMEOUT)).startTestItem(same(firstSuiteId), testCaptor.capture());
		verify(client, timeout(PROCESSING_TIMEOUT).times(2)).startTestItem(same(secondSuiteId), testCaptor.capture());
		ArgumentCaptor<StartTestItemRQ> stepCaptor = ArgumentCaptor.forClass(StartTestItemRQ.class);
		verify(client, timeout(PROCESSING_TIMEOUT)).startTestItem(same(classIds.get(0)), stepCaptor.capture());
		verify(client, timeout(PROCESSING_TIMEOUT)).startTestItem(same(classIds.get(1)), stepCaptor.capture());

		Date previousDate = null;
		List<StartTestItemRQ> testItems = testCaptor.getAllValues();
		List<StartTestItemRQ> suites = Stream.concat(suiteCaptor.getAllValues().stream(), testItems.stream()).collect(Collectors.toList());
		for (StartTestItemRQ item : suites) {
			Date parentDate = ofNullable(previousDate).orElseGet(() -> suites.get(0).getStartTime());
			Date itemDate = item.getStartTime();
			assertThat(item.getStartTime(), greaterThanOrEqualTo(parentDate));
			previousDate = itemDate;
		}
		List<StartTestItemRQ> steps = stepCaptor.getAllValues();
		IntStream.range(0, TEST_NUMBER)
				.forEach(i -> assertThat(steps.get(i).getStartTime(), greaterThanOrEqualTo(testItems.get(i).getStartTime())));
	}
}
