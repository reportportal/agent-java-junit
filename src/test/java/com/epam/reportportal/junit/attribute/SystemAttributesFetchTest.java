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

package com.epam.reportportal.junit.attribute;

import com.epam.reportportal.junit.ReportPortalListener;
import com.epam.reportportal.junit.features.coderef.CodeRefTest;
import com.epam.reportportal.junit.utils.TestUtils;
import com.epam.reportportal.service.ReportPortal;
import com.epam.reportportal.service.ReportPortalClient;
import com.epam.reportportal.util.test.CommonUtils;
import com.epam.ta.reportportal.ws.model.attribute.ItemAttributeResource;
import com.epam.ta.reportportal.ws.model.attribute.ItemAttributesRQ;
import com.epam.ta.reportportal.ws.model.launch.StartLaunchRQ;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.Set;
import java.util.stream.Collectors;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class SystemAttributesFetchTest {

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
	public void verify_system_attributes_bypass_on_launch_start() {
		TestUtils.runClasses(CodeRefTest.class);

		ArgumentCaptor<StartLaunchRQ> launchCaptor = ArgumentCaptor.forClass(StartLaunchRQ.class);
		verify(client).startLaunch(launchCaptor.capture());

		StartLaunchRQ startRq = launchCaptor.getValue();
		Set<ItemAttributesRQ> attributes = startRq.getAttributes();
		assertThat(attributes, allOf(notNullValue(), hasSize(greaterThanOrEqualTo(4))));
		List<ItemAttributesRQ> systemAttributes = attributes.stream().filter(ItemAttributesRQ::isSystem).collect(Collectors.toList());
		assertThat(systemAttributes, hasSize(4));
		List<String> systemAttributesKeys = systemAttributes.stream().map(ItemAttributeResource::getKey).collect(Collectors.toList());
		assertThat(systemAttributesKeys, hasItems("agent", "skippedIssue", "jvm", "os"));
		List<ItemAttributesRQ> agentName = systemAttributes.stream().filter(a -> "agent".equals(a.getKey())).collect(Collectors.toList());
		assertThat(agentName, hasSize(1));
		assertThat(agentName.get(0).getValue(), equalTo("test-name|test-version"));
	}
}
