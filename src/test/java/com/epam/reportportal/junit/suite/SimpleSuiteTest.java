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

package com.epam.reportportal.junit.suite;

import com.epam.reportportal.junit.ReportPortalListener;
import com.epam.reportportal.junit.features.suites.SimpleSuiteClass;
import com.epam.reportportal.junit.features.suites.tests.FirstSuiteTest;
import com.epam.reportportal.junit.features.suites.tests.SecondSuiteTest;
import com.epam.reportportal.junit.utils.TestUtils;
import com.epam.reportportal.listeners.ItemType;
import com.epam.reportportal.service.ReportPortal;
import com.epam.reportportal.service.ReportPortalClient;
import com.epam.reportportal.util.test.CommonUtils;
import com.epam.ta.reportportal.ws.model.StartTestItemRQ;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static com.epam.reportportal.junit.utils.TestUtils.PROCESSING_TIMEOUT;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.Mockito.*;

public class SimpleSuiteTest {
	private static final int TEST_NUMBER = 2;

	private final String suiteId = CommonUtils.namedId("suite_");
	private final List<String> classIds = Stream.generate(() -> CommonUtils.namedId("class_"))
			.limit(TEST_NUMBER)
			.collect(Collectors.toList());
	private final List<String> methodIds = Stream.generate(() -> CommonUtils.namedId("method_"))
			.limit(TEST_NUMBER)
			.collect(Collectors.toList());
	private final List<Pair<String, Set<String>>> tests = IntStream.range(0, TEST_NUMBER)
			.mapToObj(i -> Pair.of(classIds.get(i), Collections.singleton(methodIds.get(i))))
			.collect(Collectors.toList());

	private final ReportPortalClient client = mock(ReportPortalClient.class);

	@BeforeEach
	public void setupMock() {
		TestUtils.mockLaunch(client, null, suiteId, tests);
		TestUtils.mockBatchLogging(client);
		ReportPortalListener.setReportPortal(ReportPortal.create(client, TestUtils.standardParameters(),
				Executors.newSingleThreadExecutor()));
	}

	private static String getMethodName(Class<?> clazz) {
		return Arrays.stream(clazz.getMethods())
				.filter(m -> m.getAnnotation(org.junit.Test.class) != null)
				.map(Method::getName)
				.findAny()
				.orElse("");
	}

	private static final List<Class<?>> TEST_NAMES = Arrays.asList(FirstSuiteTest.class, SecondSuiteTest.class);
	private static final List<String> STEP_NAMES = Arrays.asList(getMethodName(TEST_NAMES.get(0)), getMethodName(TEST_NAMES.get(1)));

	@Test
	public void verify_test_hierarchy_on_simple_suite() {
		TestUtils.runClasses(SimpleSuiteClass.class);

		ArgumentCaptor<StartTestItemRQ> suiteCaptor = ArgumentCaptor.forClass(StartTestItemRQ.class);
		verify(client, timeout(PROCESSING_TIMEOUT)).startTestItem(ArgumentMatchers.startsWith(TestUtils.ROOT_SUITE_PREFIX), suiteCaptor.capture());
		ArgumentCaptor<StartTestItemRQ> testCaptor = ArgumentCaptor.forClass(StartTestItemRQ.class);
		verify(client, timeout(PROCESSING_TIMEOUT).times(2)).startTestItem(same(suiteId), testCaptor.capture());
		ArgumentCaptor<StartTestItemRQ> stepCaptor = ArgumentCaptor.forClass(StartTestItemRQ.class);
		verify(client, timeout(PROCESSING_TIMEOUT)).startTestItem(same(classIds.get(0)), stepCaptor.capture());
		verify(client, timeout(PROCESSING_TIMEOUT)).startTestItem(same(classIds.get(1)), stepCaptor.capture());

		StartTestItemRQ suite = suiteCaptor.getValue();
		String suiteCodeRef = SimpleSuiteClass.class.getCanonicalName();
		assertThat(suite.getName(), equalTo(suiteCodeRef));
		assertThat(suite.getCodeRef(), equalTo(suiteCodeRef));
		assertThat(suite.getType(), equalTo(ItemType.SUITE.name()));

		List<StartTestItemRQ> tests = testCaptor.getAllValues();
		IntStream.range(0, tests.size()).forEach(i -> {
			StartTestItemRQ test = tests.get(i);
			assertThat(test.getName(), equalTo(TEST_NAMES.get(i).getCanonicalName()));
			assertThat(test.getType(), equalTo(ItemType.TEST.name()));
		});

		List<StartTestItemRQ> steps = stepCaptor.getAllValues();
		IntStream.range(0, steps.size()).forEach(i -> {
			StartTestItemRQ step = steps.get(i);
			assertThat(step.getName(), equalTo(STEP_NAMES.get(i)));
			assertThat(step.getType(), equalTo(ItemType.STEP.name()));
		});
	}

}
