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

import com.epam.reportportal.junit.features.coderef.CodeRefTest;
import com.epam.reportportal.junit.features.parameters.JUnitParamsSimpleTest;
import com.epam.reportportal.junit.features.parameters.StandardParametersSimpleTest;
import com.epam.reportportal.junit.features.testcaseid.TestCaseIdFromAnnotationTest;
import com.epam.reportportal.junit.utils.TestUtils;
import com.epam.reportportal.service.ReportPortal;
import com.epam.reportportal.service.ReportPortalClient;
import com.epam.reportportal.util.test.CommonUtils;
import com.epam.ta.reportportal.ws.model.StartTestItemRQ;
import junitparams.Parameters;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.stream.IntStream;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.*;

public class TestCaseIdTest {

	private final String launchId = CommonUtils.namedId("launch_");
	private final String suiteId = CommonUtils.namedId("suite_");
	private final String classId = CommonUtils.namedId("class_");
	private final String methodId = CommonUtils.namedId("method_");

	private ReportPortalClient client;

	@BeforeEach
	public void setupMock() {
		client = mock(ReportPortalClient.class);
		TestUtils.mockLaunch(client, launchId, suiteId, classId, methodId);
		TestUtils.mockLogging(client);
		ParallelRunningHandler.setReportPortal(ReportPortal.create(client, TestUtils.standardParameters()));
	}

	@Test
	public void verify_static_test_case_id_generation() {
		TestUtils.runClasses(CodeRefTest.class);

		verify(client, times(1)).startTestItem(any());
		ArgumentCaptor<StartTestItemRQ> captor = ArgumentCaptor.forClass(StartTestItemRQ.class);
		verify(client, times(1)).startTestItem(same(suiteId), captor.capture());
		verify(client, times(1)).startTestItem(same(classId), captor.capture());

		List<StartTestItemRQ> items = captor.getAllValues();
		assertThat(items, hasSize(2));

		StartTestItemRQ testRq = items.get(1);

		assertThat(testRq.getTestCaseId(),
				allOf(notNullValue(),
						equalTo(CodeRefTest.class.getCanonicalName() + "." + CodeRefTest.class.getDeclaredMethods()[0].getName())
				)
		);
	}

	@Test
	public void verify_test_case_id_standard_parameters_generation() {
		Class<StandardParametersSimpleTest> testClass = StandardParametersSimpleTest.class;
		TestUtils.runClasses(testClass);

		ArgumentCaptor<StartTestItemRQ> captor = ArgumentCaptor.forClass(StartTestItemRQ.class);
		verify(client, times(1)).startTestItem(captor.capture());
		verify(client, times(1)).startTestItem(same(suiteId), captor.capture());
		verify(client, times(2)).startTestItem(same(classId), captor.capture());

		List<StartTestItemRQ> items = captor.getAllValues();
		assertThat(items, hasSize(4));

		List<StartTestItemRQ> testRqs = items.subList(2, 4);

		IntStream.range(0, testRqs.size()).forEach(i -> {
			StartTestItemRQ testRq = testRqs.get(i);
			String classStr = testClass.getCanonicalName();
			String methodStr = Arrays.stream(testClass.getDeclaredMethods())
					.filter(m -> m.getAnnotation(org.junit.Test.class) != null)
					.map(Method::getName)
					.findFirst()
					.orElse(null);
			Object params = StandardParametersSimpleTest.params()[i];
			assertThat(testRq.getTestCaseId(), allOf(notNullValue(), equalTo(classStr + "." + methodStr + "[" + params + "]")));
		});
	}

	@Test
	public void verify_test_case_id_junitparams_parameters_generation() {
		Class<JUnitParamsSimpleTest> testClass = JUnitParamsSimpleTest.class;
		TestUtils.runClasses(testClass);

		ArgumentCaptor<StartTestItemRQ> captor = ArgumentCaptor.forClass(StartTestItemRQ.class);
		verify(client, times(1)).startTestItem(captor.capture());
		verify(client, times(1)).startTestItem(same(suiteId), captor.capture());
		verify(client, times(2)).startTestItem(same(classId), captor.capture());

		List<StartTestItemRQ> items = captor.getAllValues();
		assertThat(items, hasSize(4));

		List<StartTestItemRQ> testRqs = items.subList(2, 4);

		IntStream.range(0, testRqs.size()).forEach(i -> {
			StartTestItemRQ testRq = testRqs.get(i);
			String classStr = testClass.getCanonicalName();
			String methodStr = Arrays.stream(testClass.getDeclaredMethods())
					.filter(m -> m.getAnnotation(org.junit.Test.class) != null)
					.map(Method::getName)
					.findFirst()
					.orElse(null);
			String params = Arrays.stream(testClass.getDeclaredMethods())
					.filter(m -> m.getAnnotation(Parameters.class) != null)
					.map(m -> m.getAnnotation(Parameters.class).value())
					.map(p -> p[i])
					.findAny()
					.orElse("").replace("\\", "");
			assertThat(testRq.getTestCaseId(), allOf(notNullValue(), equalTo(classStr + "." + methodStr + "[" + params + "]")));
		});
	}


	@Test
	public void verify_test_case_id_from_simple_annotation_generation() {
		Class<TestCaseIdFromAnnotationTest> testClass = TestCaseIdFromAnnotationTest.class;
		TestUtils.runClasses(testClass);

		ArgumentCaptor<StartTestItemRQ> captor = ArgumentCaptor.forClass(StartTestItemRQ.class);
		verify(client, times(1)).startTestItem(captor.capture());
		verify(client, times(1)).startTestItem(same(suiteId), captor.capture());
		verify(client, times(1)).startTestItem(same(classId), captor.capture());

		List<StartTestItemRQ> items = captor.getAllValues();
		assertThat(items, hasSize(3));

		StartTestItemRQ testRq = items.get(2);
		assertThat(testRq.getTestCaseId(), allOf(notNullValue(), equalTo(TestCaseIdFromAnnotationTest.TEST_CASE_ID_VALUE)));
	}
}
