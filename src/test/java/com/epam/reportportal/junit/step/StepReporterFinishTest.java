/*
 * Copyright 2020 EPAM Systems
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.epam.reportportal.junit.step;

import com.epam.reportportal.junit.ReportPortalListener;
import com.epam.reportportal.junit.features.step.ManualStepReporterSimpleTest;
import com.epam.reportportal.junit.utils.TestUtils;
import com.epam.reportportal.service.ReportPortal;
import com.epam.reportportal.service.ReportPortalClient;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.epam.reportportal.junit.utils.TestUtils.PROCESSING_TIMEOUT;
import static com.epam.reportportal.util.test.CommonUtils.namedId;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class StepReporterFinishTest {

	private static final String testClassUuid = namedId("class");
	private static final String testMethodUuid = namedId("test");
	private static final List<String> stepUuidList = Stream.generate(() -> namedId("step")).limit(3).collect(Collectors.toList());
	private static final List<Pair<String, String>> testStepUuidOrder = stepUuidList.stream()
			.map(u -> Pair.of(testMethodUuid, u))
			.collect(Collectors.toList());

	private final ReportPortalClient client = mock(ReportPortalClient.class);

	@BeforeEach
	public void setupMock() {
		TestUtils.mockLaunch(client, null, null, testClassUuid, testMethodUuid);
		TestUtils.mockNestedSteps(client, testStepUuidOrder);
		TestUtils.mockBatchLogging(client);
		ReportPortalListener.setReportPortal(ReportPortal.create(client,
				TestUtils.standardParameters(),
				Executors.newSingleThreadExecutor()
		));
	}

	@Test
	public void verify_listener_finishes_unfinished_step() {
		TestUtils.runClasses(ManualStepReporterSimpleTest.class);

		verify(client, timeout(PROCESSING_TIMEOUT)).finishTestItem(same(stepUuidList.get(0)), any());
	}

}
