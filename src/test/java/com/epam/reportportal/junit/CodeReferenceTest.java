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
import com.epam.reportportal.junit.utils.TestUtils;
import com.epam.reportportal.listeners.ItemType;
import com.epam.reportportal.service.ReportPortal;
import com.epam.reportportal.service.ReportPortalClient;
import com.epam.reportportal.util.test.CommonUtils;
import com.epam.ta.reportportal.ws.model.StartTestItemRQ;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;

import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.List;

import static com.epam.reportportal.junit.utils.TestUtils.PROCESSING_TIMEOUT;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.*;

public class CodeReferenceTest {

	private final String classId = CommonUtils.namedId("class_");
	private final String methodId = CommonUtils.namedId("method_");

	private final ReportPortalClient client = mock(ReportPortalClient.class);
    private final ExecutorService executor = CommonUtils.testExecutor();

	@BeforeEach
	public void setupMock() {
		TestUtils.mockLaunch(client, null, null, classId, methodId);
		TestUtils.mockBatchLogging(client);
        ReportPortalListener.setReportPortal(ReportPortal.create(client, TestUtils.standardParameters(), executor));
	}

    @AfterEach
    public void tearDown() {
        CommonUtils.shutdownExecutorService(executor);
    }

	@Test
	public void verify_static_test_code_reference_generation() {
		TestUtils.runClasses(CodeRefTest.class);
		String methodName = Arrays.stream(CodeRefTest.class.getDeclaredMethods())
				.filter(m -> m.getAnnotation(org.junit.Test.class) != null)
				.findAny()
				.orElseThrow(() -> new IllegalArgumentException("No test method in test class"))
				.getName();

		ArgumentCaptor<StartTestItemRQ> captor = ArgumentCaptor.forClass(StartTestItemRQ.class);
		verify(client, timeout(PROCESSING_TIMEOUT)).startTestItem(ArgumentMatchers.startsWith("root_"), captor.capture());
		verify(client, timeout(PROCESSING_TIMEOUT)).startTestItem(same(classId), captor.capture());

		List<StartTestItemRQ> items = captor.getAllValues();
		assertThat(items, hasSize(2));

		StartTestItemRQ classRq = items.get(0);
		StartTestItemRQ testRq = items.get(1);

		assertThat(classRq.getCodeRef(), allOf(notNullValue(), equalTo(CodeRefTest.class.getCanonicalName())));
		assertThat(classRq.getType(), allOf(notNullValue(), equalTo(ItemType.TEST.name())));
		assertThat(testRq.getCodeRef(), allOf(notNullValue(), equalTo(CodeRefTest.class.getCanonicalName() + "." + methodName)));
	}

}
