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

package com.epam.reportportal.junit.ignore;

import com.epam.reportportal.junit.ReportPortalListener;
import com.epam.reportportal.junit.features.ignore.IgnoredTest;
import com.epam.reportportal.junit.utils.TestUtils;
import com.epam.reportportal.listeners.ItemStatus;
import com.epam.reportportal.service.ReportPortal;
import com.epam.reportportal.service.ReportPortalClient;
import com.epam.reportportal.util.test.CommonUtils;
import com.epam.ta.reportportal.ws.model.FinishTestItemRQ;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.concurrent.Executors;

import static com.epam.reportportal.junit.utils.TestUtils.PROCESSING_TIMEOUT;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.*;

public class BasicIgnoreTest {

	private final String classId = CommonUtils.namedId("class_");
	private final String methodId = CommonUtils.namedId("method_");

	private final ReportPortalClient client = mock(ReportPortalClient.class);

	@BeforeEach
	public void setupMock() {
		TestUtils.mockLaunch(client, null, null, classId, methodId);
		TestUtils.mockBatchLogging(client);
		ReportPortalListener.setReportPortal(ReportPortal.create(client, TestUtils.standardParameters(),
				Executors.newSingleThreadExecutor()));
	}

	@Test
	public void verify_an_ignored_test_reporting() {
		TestUtils.runClasses(IgnoredTest.class);

		ArgumentCaptor<FinishTestItemRQ> finishCaptor = ArgumentCaptor.forClass(FinishTestItemRQ.class);
		verify(client, timeout(PROCESSING_TIMEOUT)).startTestItem(same(classId), any());
		verify(client, timeout(PROCESSING_TIMEOUT)).finishTestItem(same(methodId), finishCaptor.capture());

		FinishTestItemRQ finishRq = finishCaptor.getValue();

		assertThat(finishRq.getStatus(), allOf(notNullValue(), equalTo(ItemStatus.SKIPPED.name())));
		assertThat(finishRq.getIssue(), nullValue());
	}
}
