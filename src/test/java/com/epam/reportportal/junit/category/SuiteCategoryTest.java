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

package com.epam.reportportal.junit.category;

import com.epam.reportportal.junit.ReportPortalListener;
import com.epam.reportportal.junit.features.category.Smoke;
import com.epam.reportportal.junit.features.category.SuiteCategory;
import com.epam.reportportal.junit.utils.TestUtils;
import com.epam.reportportal.service.ReportPortal;
import com.epam.reportportal.service.ReportPortalClient;
import com.epam.reportportal.util.test.CommonUtils;
import com.epam.ta.reportportal.ws.model.StartTestItemRQ;
import com.epam.ta.reportportal.ws.model.attribute.ItemAttributesRQ;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;

import java.util.concurrent.Executors;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.*;

public class SuiteCategoryTest {
	private final String suiteId = CommonUtils.namedId("suite_");
	private final String classId = CommonUtils.namedId("class_");
	private final String methodId = CommonUtils.namedId("method_");

	private final ReportPortalClient client = mock(ReportPortalClient.class);

	@BeforeEach
	public void setupMock() {
		TestUtils.mockLaunch(client, null, suiteId, classId, methodId);
		TestUtils.mockBatchLogging(client);
		ReportPortalListener.setReportPortal(ReportPortal.create(client, TestUtils.standardParameters(),
				Executors.newSingleThreadExecutor()));
	}

	@Test
	public void verify_suite_categories_go_to_value_only_attributes() {
		TestUtils.runClasses(SuiteCategory.class);

		ArgumentCaptor<StartTestItemRQ> suiteCaptor = ArgumentCaptor.forClass(StartTestItemRQ.class);
		verify(client, times(1)).startTestItem(ArgumentMatchers.startsWith(TestUtils.ROOT_SUITE_PREFIX), suiteCaptor.capture());
		ArgumentCaptor<StartTestItemRQ> testCaptor = ArgumentCaptor.forClass(StartTestItemRQ.class);
		verify(client, times(1)).startTestItem(same(suiteId), testCaptor.capture());
		ArgumentCaptor<StartTestItemRQ> stepCaptor = ArgumentCaptor.forClass(StartTestItemRQ.class);
		verify(client, times(1)).startTestItem(same(classId), stepCaptor.capture());

		StartTestItemRQ suite = suiteCaptor.getValue();
		assertThat(suite.getAttributes(), allOf(notNullValue(), hasSize(1)));
		ItemAttributesRQ attribute = suite.getAttributes().iterator().next();
		assertThat(attribute.getKey(), nullValue());
		assertThat(attribute.getValue(), equalTo(Smoke.class.getSimpleName()));

		StartTestItemRQ test = testCaptor.getValue();
		assertThat(test.getAttributes(), anyOf(emptyCollectionOf(ItemAttributesRQ.class), nullValue()));

		StartTestItemRQ step = stepCaptor.getValue();
		assertThat(step.getAttributes(), anyOf(emptyCollectionOf(ItemAttributesRQ.class), nullValue()));
	}

}
