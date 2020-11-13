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
import com.epam.reportportal.junit.features.attribute.MultiValueAttributeTest;
import com.epam.reportportal.junit.utils.TestUtils;
import com.epam.reportportal.service.ReportPortal;
import com.epam.reportportal.service.ReportPortalClient;
import com.epam.reportportal.util.test.CommonUtils;
import com.epam.ta.reportportal.ws.model.StartTestItemRQ;
import com.epam.ta.reportportal.ws.model.attribute.ItemAttributesRQ;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Set;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.*;

public class MultipleVAttributeTest {

	private final String classId = CommonUtils.namedId("class_");
	private final String methodId = CommonUtils.namedId("method_");

	private final ReportPortalClient client = mock(ReportPortalClient.class);

	@BeforeEach
	public void setupMock() {
		TestUtils.mockLaunch(client, null, null, classId, methodId);
		TestUtils.mockBatchLogging(client);
		ReportPortalListener.setReportPortal(ReportPortal.create(client, TestUtils.standardParameters()));
	}

	@Test
	public void verify_static_multiple_attribute_bypass() {
		TestUtils.runClasses(MultiValueAttributeTest.class);

		ArgumentCaptor<StartTestItemRQ> captor = ArgumentCaptor.forClass(StartTestItemRQ.class);
		verify(client, times(1)).startTestItem(same(classId), captor.capture());

		StartTestItemRQ testRq = captor.getValue();
		assertThat(testRq.getAttributes(), allOf(notNullValue(), hasSize(MultiValueAttributeTest.ATTRIBUTES.size())));
		Set<ItemAttributesRQ> attributes = testRq.getAttributes();
		MultiValueAttributeTest.ATTRIBUTES.forEach(expected -> {
			ItemAttributesRQ actual = attributes.stream().filter(a -> a.getValue().equals(expected.getValue())).findAny().orElse(null);
			assertThat(actual, notNullValue());
			assertThat(actual.getKey(), equalTo(expected.getKey()));
			assertThat(actual.getValue(), equalTo(expected.getValue()));
		});
	}

}
