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
import com.epam.reportportal.junit.features.nested.NestedStepFeatureFailedTest;
import com.epam.reportportal.junit.utils.TestUtils;
import com.epam.reportportal.service.ReportPortal;
import com.epam.reportportal.service.ReportPortalClient;
import com.epam.reportportal.util.test.CommonUtils;
import com.epam.ta.reportportal.ws.model.FinishTestItemRQ;
import com.epam.ta.reportportal.ws.model.StartTestItemRQ;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static com.epam.reportportal.junit.utils.TestUtils.PROCESSING_TIMEOUT;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.*;

public class NestedStepFailedTest {

	private final String classId = CommonUtils.namedId("class_");
	private final String methodId = CommonUtils.namedId("method_");
	private final String nestedId = CommonUtils.namedId("nested_");

	private final ReportPortalClient client = mock(ReportPortalClient.class);
	private final java.util.concurrent.ExecutorService executor = CommonUtils.testExecutor();

	@BeforeEach
	public void setupMock() {
		TestUtils.mockLaunch(client, null, classId, methodId, nestedId);
		TestUtils.mockBatchLogging(client);
		ReportPortalListener.setReportPortal(ReportPortal.create(client, TestUtils.standardParameters(), executor));
	}

	@AfterEach
	public void tearDown() {
		CommonUtils.shutdownExecutorService(executor);
	}

	@Test
	public void test_nested_step_failed_case() {
		TestUtils.runClasses(NestedStepFeatureFailedTest.class);

		ArgumentCaptor<StartTestItemRQ> nestedStepCaptor = ArgumentCaptor.forClass(StartTestItemRQ.class);
		ArgumentCaptor<FinishTestItemRQ> finishNestedCaptor = ArgumentCaptor.forClass(FinishTestItemRQ.class);
		verify(client, timeout(PROCESSING_TIMEOUT)).startTestItem(same(methodId), nestedStepCaptor.capture());
		verify(client, timeout(PROCESSING_TIMEOUT)).finishTestItem(same(nestedId), finishNestedCaptor.capture());

		StartTestItemRQ startTestItemRQ = nestedStepCaptor.getValue();

		assertNotNull(startTestItemRQ);
		assertFalse(startTestItemRQ.isHasStats());
		assertEquals("I am nested step with parameter - '" + NestedStepFeatureFailedTest.PARAM + "'", startTestItemRQ.getName());

		FinishTestItemRQ finishNestedRQ = finishNestedCaptor.getValue();
		assertNotNull(finishNestedRQ);
		assertEquals("FAILED", finishNestedRQ.getStatus());

	}
}
