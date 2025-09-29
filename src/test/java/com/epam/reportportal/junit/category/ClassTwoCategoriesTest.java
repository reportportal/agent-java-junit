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
import com.epam.reportportal.junit.features.category.Stable;
import com.epam.reportportal.junit.utils.TestUtils;
import com.epam.reportportal.service.ReportPortal;
import com.epam.reportportal.service.ReportPortalClient;
import com.epam.reportportal.util.test.CommonUtils;
import com.epam.ta.reportportal.ws.model.StartTestItemRQ;
import com.epam.ta.reportportal.ws.model.attribute.ItemAttributesRQ;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.Set;
import java.util.stream.Collectors;

import static com.epam.reportportal.junit.utils.TestUtils.PROCESSING_TIMEOUT;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.*;

public class ClassTwoCategoriesTest {

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
	public void verify_class_categories_go_to_value_only_attributes() {
		TestUtils.runClasses(com.epam.reportportal.junit.features.category.ClassTwoCategoriesTest.class);

		ArgumentCaptor<StartTestItemRQ> captor = ArgumentCaptor.forClass(StartTestItemRQ.class);
		verify(client, timeout(PROCESSING_TIMEOUT)).startTestItem(ArgumentMatchers.startsWith("root_"), captor.capture());
		verify(client, timeout(PROCESSING_TIMEOUT)).startTestItem(same(classId), captor.capture());

		List<StartTestItemRQ> items = captor.getAllValues();
		assertThat(items.get(1).getAttributes(), anyOf(emptyCollectionOf(ItemAttributesRQ.class), nullValue()));

		StartTestItemRQ item = items.get(0);
		assertThat(item.getAttributes(), allOf(notNullValue(), hasSize(2)));
		Set<ItemAttributesRQ> attributes = item.getAttributes();
		assertThat(
				attributes.stream().filter(a -> Smoke.class.getSimpleName().equals(a.getValue())).collect(Collectors.toList()),
				hasSize(1)
		);
		assertThat(
				attributes.stream().filter(a -> Stable.class.getSimpleName().equals(a.getValue())).collect(Collectors.toList()),
				hasSize(1)
		);
	}
}
