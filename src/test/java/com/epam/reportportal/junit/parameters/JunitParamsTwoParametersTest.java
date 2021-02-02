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

package com.epam.reportportal.junit.parameters;

import com.epam.reportportal.junit.ReportPortalListener;
import com.epam.reportportal.junit.features.parameters.JUnitParamsTwoParamsTest;
import com.epam.reportportal.junit.utils.TestUtils;
import com.epam.reportportal.service.ReportPortal;
import com.epam.reportportal.service.ReportPortalClient;
import com.epam.reportportal.util.test.CommonUtils;
import com.epam.ta.reportportal.ws.model.ParameterResource;
import com.epam.ta.reportportal.ws.model.StartTestItemRQ;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.stream.IntStream;

import static com.epam.reportportal.junit.utils.TestUtils.PROCESSING_TIMEOUT;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.*;

public class JunitParamsTwoParametersTest {

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

	private static final List<List<Pair<String, Object>>> PARAMETERS = Arrays.asList(Arrays.asList(Pair.of("param1", "one"),
			Pair.of("param2", "1")
	), Arrays.asList(Pair.of("param1", "two"), Pair.of("param2", "2")));

	@Test
	public void verify_two_parameters_junitparams_implementation() {
		TestUtils.runClasses(JUnitParamsTwoParamsTest.class);

		ArgumentCaptor<StartTestItemRQ> captor = ArgumentCaptor.forClass(StartTestItemRQ.class);
		verify(client, timeout(PROCESSING_TIMEOUT).times(2)).startTestItem(same(classId), captor.capture());

		List<StartTestItemRQ> items = captor.getAllValues();
		assertThat(items, hasSize(2));

		IntStream.range(0, items.size()).forEach(i -> {
			StartTestItemRQ item = items.get(i);
			assertThat(item.getParameters(), allOf(notNullValue(), hasSize(2)));
			List<ParameterResource> params = item.getParameters();
			IntStream.range(0, params.size()).forEach(j -> {
				ParameterResource param = params.get(j);
				assertThat(param.getKey(), equalTo(PARAMETERS.get(i).get(j).getKey()));
				assertThat(param.getValue(), equalTo(PARAMETERS.get(i).get(j).getValue()));
			});
		});
	}
}
