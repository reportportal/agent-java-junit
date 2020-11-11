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

package com.epam.reportportal.junit.utils;

import com.epam.reportportal.listeners.ListenerParameters;
import com.epam.reportportal.restendpoint.http.MultiPartRequest;
import com.epam.reportportal.service.ReportPortalClient;
import com.epam.reportportal.util.test.CommonUtils;
import com.epam.ta.reportportal.ws.model.BatchSaveOperatingRS;
import com.epam.ta.reportportal.ws.model.OperationCompletionRS;
import com.epam.ta.reportportal.ws.model.StartTestItemRQ;
import com.epam.ta.reportportal.ws.model.item.ItemCreatedRS;
import com.epam.ta.reportportal.ws.model.launch.StartLaunchRQ;
import com.epam.ta.reportportal.ws.model.launch.StartLaunchRS;
import io.reactivex.Maybe;
import junitparams.converters.Nullable;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.runner.JUnitCore;
import org.mockito.ArgumentCaptor;
import org.mockito.stubbing.Answer;

import javax.annotation.Nonnull;
import java.util.*;
import java.util.stream.Collectors;

import static com.epam.reportportal.util.test.CommonUtils.createMaybe;
import static com.epam.reportportal.util.test.CommonUtils.generateUniqueId;
import static java.util.Optional.ofNullable;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

public class TestUtils {

	public static final String ROOT_SUITE_PREFIX = "root_";

	private TestUtils() {
	}

	public static void runClasses(final Class<?>... testClasses) {
		JUnitCore.runClasses(testClasses);
	}

	public static StartTestItemRQ extractRequest(ArgumentCaptor<StartTestItemRQ> captor, String methodType) {
		return captor.getAllValues()
				.stream()
				.filter(it -> methodType.equalsIgnoreCase(it.getType()))
				.findAny()
				.orElseThrow(() -> new AssertionError(String.format("Method type '%s' should be present among requests", methodType)));
	}

	public static List<StartTestItemRQ> extractRequests(ArgumentCaptor<StartTestItemRQ> captor, String methodType) {
		return captor.getAllValues().stream().filter(it -> methodType.equalsIgnoreCase(it.getType())).collect(Collectors.toList());
	}

	public static StartTestItemRQ standardStartStepRequest() {
		StartTestItemRQ rq = new StartTestItemRQ();
		rq.setStartTime(Calendar.getInstance().getTime());
		String id = generateUniqueId();
		rq.setName("Step_" + id);
		rq.setDescription("Test step description");
		rq.setUniqueId(id);
		rq.setType("STEP");
		return rq;
	}

	public static void mockLaunch(@Nonnull ReportPortalClient client, @Nullable String launchUuid, @Nullable String suiteUuid,
			@Nonnull String testClassUuid, @Nonnull String testMethodUuid) {
		mockLaunch(client, launchUuid, suiteUuid, testClassUuid, Collections.singleton(testMethodUuid));
	}

	public static void mockLaunch(@Nonnull ReportPortalClient client, @Nullable String launchUuid, @Nullable String suiteUuid,
			@Nonnull String testClassUuid, @Nonnull Collection<String> testMethodUuidList) {
		mockLaunch(client, launchUuid, suiteUuid, Collections.singletonList(Pair.of(testClassUuid, testMethodUuidList)));
	}

	@SuppressWarnings("unchecked")
	public static <T extends Collection<String>> void mockLaunch(@Nonnull ReportPortalClient client, @Nullable String launchUuid,
			@Nullable String suiteUuid, @Nonnull Collection<Pair<String, T>> testSteps) {
		String launch = ofNullable(launchUuid).orElse(CommonUtils.namedId("launch_"));
		when(client.startLaunch(any())).thenReturn(createMaybe(new StartLaunchRS(launch, 1L)));

		String rootItemId = CommonUtils.namedId(ROOT_SUITE_PREFIX);
		Maybe<ItemCreatedRS> rootMaybe = createMaybe(new ItemCreatedRS(rootItemId, rootItemId));
		when(client.startTestItem(any())).thenReturn(rootMaybe);

		String parentId = ofNullable(suiteUuid).map(s -> {
			Maybe<ItemCreatedRS> suiteMaybe = createMaybe(new ItemCreatedRS(s, s));
			when(client.startTestItem(same(rootItemId), any())).thenReturn(suiteMaybe);
			return s;
		}).orElse(rootItemId);

		List<Maybe<ItemCreatedRS>> testResponses = testSteps.stream()
				.map(Pair::getKey)
				.map(uuid -> createMaybe(new ItemCreatedRS(uuid, uuid)))
				.collect(Collectors.toList());

		Maybe<ItemCreatedRS> first = testResponses.get(0);
		Maybe<ItemCreatedRS>[] other = testResponses.subList(1, testResponses.size()).toArray(new Maybe[0]);
		when(client.startTestItem(same(parentId), any())).thenReturn(first, other);

		testSteps.forEach(test -> {
			String testClassUuid = test.getKey();
			List<Maybe<ItemCreatedRS>> stepResponses = test.getValue()
					.stream()
					.map(uuid -> createMaybe(new ItemCreatedRS(uuid, uuid)))
					.collect(Collectors.toList());

			Maybe<ItemCreatedRS> myFirst = stepResponses.get(0);
			Maybe<ItemCreatedRS>[] myOther = stepResponses.subList(1, stepResponses.size()).toArray(new Maybe[0]);
			when(client.startTestItem(same(testClassUuid), any())).thenReturn(myFirst, myOther);
			new HashSet<>(test.getValue()).forEach(testMethodUuid -> when(client.finishTestItem(same(testMethodUuid), any())).thenReturn(
					createMaybe(new OperationCompletionRS())));
			when(client.finishTestItem(same(testClassUuid), any())).thenReturn(createMaybe(new OperationCompletionRS()));
		});

		ofNullable(suiteUuid).ifPresent(s -> {
			Maybe<OperationCompletionRS> suiteFinishMaybe = createMaybe(new OperationCompletionRS());
			when(client.finishTestItem(same(s), any())).thenReturn(suiteFinishMaybe);
		});

		Maybe<OperationCompletionRS> rootFinishMaybe = createMaybe(new OperationCompletionRS());
		when(client.finishTestItem(eq(rootItemId), any())).thenReturn(rootFinishMaybe);

		when(client.finishLaunch(eq(launch), any())).thenReturn(createMaybe(new OperationCompletionRS()));
	}

	public static void mockLogging(ReportPortalClient client) {
		when(client.log(any(MultiPartRequest.class))).thenReturn(createMaybe(new BatchSaveOperatingRS()));
	}

	public static void mockNestedSteps(ReportPortalClient client, Pair<String, String> parentNestedPair) {
		mockNestedSteps(client, Collections.singletonList(parentNestedPair));
	}

	@SuppressWarnings("unchecked")
	public static void mockNestedSteps(final ReportPortalClient client, final List<Pair<String, String>> parentNestedPairs) {
		Map<String, List<String>> responseOrders = parentNestedPairs.stream()
				.collect(Collectors.groupingBy(Pair::getKey, Collectors.mapping(Pair::getValue, Collectors.toList())));
		responseOrders.forEach((k, v) -> {
			List<Maybe<ItemCreatedRS>> responses = v.stream()
					.map(uuid -> createMaybe(new ItemCreatedRS(uuid, uuid)))
					.collect(Collectors.toList());

			Maybe<ItemCreatedRS> first = responses.get(0);
			Maybe<ItemCreatedRS>[] other = responses.subList(1, responses.size()).toArray(new Maybe[0]);
			when(client.startTestItem(eq(k), any())).thenReturn(first, other);
		});
		parentNestedPairs.forEach(p -> when(client.finishTestItem(
				same(p.getValue()),
				any()
		)).thenAnswer((Answer<Maybe<OperationCompletionRS>>) invocation -> createMaybe(new OperationCompletionRS())));
	}

	public static ListenerParameters standardParameters() {
		ListenerParameters result = new ListenerParameters();
		result.setClientJoin(false);
		result.setBatchLogsSize(1);
		result.setLaunchName("My-test-launch" + generateUniqueId());
		result.setProjectName("test-project");
		result.setEnable(true);
		result.setCallbackReportingEnabled(true);
		return result;
	}

	public static StartLaunchRQ launchRQ(ListenerParameters parameters) {
		StartLaunchRQ result = new StartLaunchRQ();
		result.setName(parameters.getLaunchName());
		result.setStartTime(Calendar.getInstance().getTime());
		return result;
	}
}
