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

package com.epam.reportportal.junit.nested;

import com.epam.reportportal.junit.ReportPortalListener;
import com.epam.reportportal.junit.features.nested.NestedStepMultiLevelTest;
import com.epam.reportportal.junit.utils.TestUtils;
import com.epam.reportportal.service.ReportPortal;
import com.epam.reportportal.service.ReportPortalClient;
import com.epam.reportportal.util.test.CommonUtils;
import com.epam.ta.reportportal.ws.model.FinishTestItemRQ;
import com.epam.ta.reportportal.ws.model.StartTestItemRQ;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Collections;
import java.util.List;

import static com.epam.reportportal.junit.utils.TestUtils.PROCESSING_TIMEOUT;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.*;

public class MultiLevelNestedStepTest {

	private final String classId = CommonUtils.namedId("class_");
	private final String methodId = CommonUtils.namedId("method_");
	private final String nestedId = CommonUtils.namedId("nested1_");
	private final String secondLevelNestedId = CommonUtils.namedId("nested2_");

	private final ReportPortalClient client = mock(ReportPortalClient.class);

	@BeforeEach
	public void setupMock() {
		TestUtils.mockLaunch(client, null, classId, methodId, nestedId);
		TestUtils.mockNestedSteps(client, Collections.singletonList(Pair.of(nestedId, secondLevelNestedId)));
		TestUtils.mockBatchLogging(client);
		ReportPortalListener.setReportPortal(ReportPortal.create(client, TestUtils.standardParameters(), TestUtils.testExecutor()));
	}

	@Test
	public void test_multi_layered_nested_steps() throws NoSuchMethodException {

		TestUtils.runClasses(NestedStepMultiLevelTest.class);

		ArgumentCaptor<StartTestItemRQ> nestedStepCaptor = ArgumentCaptor.forClass(StartTestItemRQ.class);
		ArgumentCaptor<FinishTestItemRQ> finishNestedCaptor = ArgumentCaptor.forClass(FinishTestItemRQ.class);
		verify(client, timeout(PROCESSING_TIMEOUT)).startTestItem(same(methodId), nestedStepCaptor.capture());
		verify(client, timeout(PROCESSING_TIMEOUT)).finishTestItem(same(nestedId), finishNestedCaptor.capture());
		verify(client, timeout(PROCESSING_TIMEOUT)).startTestItem(same(nestedId), nestedStepCaptor.capture());
		verify(client, timeout(PROCESSING_TIMEOUT)).finishTestItem(same(secondLevelNestedId), finishNestedCaptor.capture());

		List<StartTestItemRQ> nestedSteps = nestedStepCaptor.getAllValues();

		nestedSteps.forEach(step -> {
			assertNotNull(step);
			assertFalse(step.isHasStats());
		});

		StartTestItemRQ stepWithInnerStep = nestedSteps.get(0);
		assertEquals(NestedStepMultiLevelTest.METHOD_WITH_INNER_METHOD_NAME_TEMPLATE, stepWithInnerStep.getName());

		StartTestItemRQ innerStep = nestedSteps.get(1);
		assertEquals("I am - " + NestedStepMultiLevelTest.class.getDeclaredMethod("innerMethod").getName(), innerStep.getName());
	}

}
