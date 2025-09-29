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

package com.epam.reportportal.junit.callback;

import com.epam.reportportal.junit.ReportPortalListener;
import com.epam.reportportal.junit.features.callback.CallbackLogFeatureTest;
import com.epam.reportportal.junit.utils.TestUtils;
import com.epam.reportportal.listeners.ListenerParameters;
import com.epam.reportportal.service.ReportPortal;
import com.epam.reportportal.service.ReportPortalClient;
import com.epam.reportportal.util.test.CommonUtils;
import com.epam.ta.reportportal.ws.model.log.SaveLogRQ;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.epam.reportportal.junit.utils.TestUtils.PROCESSING_TIMEOUT;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.Mockito.*;

public class CallbackLogTest {

	private final String classId = CommonUtils.namedId("class_");
	private final List<String> methodIds = Stream.generate(() -> CommonUtils.namedId("method_")).limit(2).collect(Collectors.toList());
	private final ReportPortalClient client = mock(ReportPortalClient.class);
    private final ExecutorService executor = CommonUtils.testExecutor();

	@BeforeEach
	public void setupMock() {
		TestUtils.mockLaunch(client, null, null, classId, methodIds);
		TestUtils.mockBatchLogging(client);
		TestUtils.mockSingleLogging(client);
        ListenerParameters params = TestUtils.standardParameters();
        ReportPortalListener.setReportPortal(ReportPortal.create(client, params, executor));
	}

    @AfterEach
    public void tearDown() {
        CommonUtils.shutdownExecutorService(executor);
    }

	@Test
	public void verify_test_item_callback_log() {
		TestUtils.runClasses(CallbackLogFeatureTest.class);

		verify(client, timeout(PROCESSING_TIMEOUT).times(2)).startTestItem(same(classId), any());
		verify(client, timeout(PROCESSING_TIMEOUT)).finishTestItem(same(methodIds.get(0)), any());
		verify(client, timeout(PROCESSING_TIMEOUT)).finishTestItem(same(methodIds.get(1)), any());

		ArgumentCaptor<SaveLogRQ> logCapture = ArgumentCaptor.forClass(SaveLogRQ.class);
		verify(client).log(logCapture.capture());

		List<SaveLogRQ> logs = logCapture.getAllValues()
				.stream()
				.filter(r -> methodIds.get(0).equals(r.getItemUuid()))
				.collect(Collectors.toList());

		assertThat(
				logs.stream().filter(l -> CallbackLogFeatureTest.LOG_MESSAGE.equals(l.getMessage())).collect(Collectors.toList()),
				hasSize(1)
		);
	}
}
