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

package com.epam.reportportal.junit.testcaseid;

import com.epam.reportportal.junit.ReportPortalListener;
import com.epam.reportportal.junit.features.parameters.StandardParametersSimpleTest;
import com.epam.reportportal.junit.utils.TestUtils;
import com.epam.reportportal.service.ReportPortal;
import com.epam.reportportal.service.ReportPortalClient;
import com.epam.reportportal.util.test.CommonUtils;
import com.epam.ta.reportportal.ws.model.StartTestItemRQ;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.stream.IntStream;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.*;

public class TestCaseIdStaticStandardParametersTest {

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
		ReportPortalListener.setReportPortal(ReportPortal.create(client, TestUtils.standardParameters()));
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
}
