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
import com.epam.reportportal.service.ReportPortalClient;
import com.epam.reportportal.util.test.CommonUtils;
import com.epam.reportportal.utils.http.HttpRequestUtils;
import com.epam.ta.reportportal.ws.model.BatchSaveOperatingRS;
import com.epam.ta.reportportal.ws.model.Constants;
import com.epam.ta.reportportal.ws.model.EntryCreatedAsyncRS;
import com.epam.ta.reportportal.ws.model.OperationCompletionRS;
import com.epam.ta.reportportal.ws.model.item.ItemCreatedRS;
import com.epam.ta.reportportal.ws.model.launch.StartLaunchRS;
import com.epam.ta.reportportal.ws.model.log.SaveLogRQ;
import com.fasterxml.jackson.core.type.TypeReference;
import io.reactivex.Maybe;
import okhttp3.MultipartBody;
import okio.Buffer;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.runner.JUnitCore;
import org.junit.runner.Result;
import org.mockito.stubbing.Answer;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.epam.reportportal.util.test.CommonUtils.createMaybe;
import static com.epam.reportportal.util.test.CommonUtils.generateUniqueId;
import static java.util.Optional.ofNullable;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

public class TestUtils {

	public static final String ROOT_SUITE_PREFIX = "root_";
	public static final long PROCESSING_TIMEOUT = TimeUnit.MINUTES.toMillis(1);

	private TestUtils() {
	}

	public static Result runClasses(final Class<?>... testClasses) {
		return JUnitCore.runClasses(testClasses);
	}

	public static void mockLaunch(@Nonnull final ReportPortalClient client, @Nullable final String launchUuid,
			@Nullable final String suiteUuid, @Nonnull final String testClassUuid, @Nonnull final String testMethodUuid) {
		mockLaunch(client, launchUuid, suiteUuid, testClassUuid, Collections.singleton(testMethodUuid));
	}

	public static void mockLaunch(@Nonnull final ReportPortalClient client, @Nullable final String launchUuid,
			@Nullable final String suiteUuid, @Nonnull final String testClassUuid, @Nonnull final Collection<String> testMethodUuidList) {
		mockLaunch(client, launchUuid, suiteUuid, Collections.singletonList(Pair.of(testClassUuid, testMethodUuidList)));
	}

	@SuppressWarnings("unchecked")
	public static <T extends Collection<String>> void mockLaunch(@Nonnull final ReportPortalClient client,
			@Nullable final String launchUuid, @Nullable final String suiteUuid, @Nonnull final Collection<Pair<String, T>> testSteps) {
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

	@SuppressWarnings("unchecked")
	public static void mockBatchLogging(final ReportPortalClient client) {
		when(client.log(any(List.class))).thenReturn(createMaybe(new BatchSaveOperatingRS()));
	}

	public static void mockSingleLogging(final ReportPortalClient client) {
		when(client.log(any(SaveLogRQ.class))).thenReturn(createMaybe(new EntryCreatedAsyncRS()));
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

	public static List<SaveLogRQ> toSaveLogRQ(List<List<MultipartBody.Part>> rqs) {
		return rqs.stream()
				.flatMap(List::stream)
				.filter(p -> ofNullable(p.headers()).map(headers -> headers.get("Content-Disposition"))
						.map(h -> h.contains(Constants.LOG_REQUEST_JSON_PART))
						.orElse(false))
				.map(MultipartBody.Part::body)
				.map(b -> {
					Buffer buf = new Buffer();
					try {
						b.writeTo(buf);
					} catch (IOException ignore) {
					}
					return buf.readByteArray();
				})
				.map(b -> {
					try {
						return HttpRequestUtils.MAPPER.readValue(b, new TypeReference<List<SaveLogRQ>>() {
						});
					} catch (IOException e) {
						return Collections.<SaveLogRQ>emptyList();
					}
				})
				.flatMap(Collection::stream)
				.collect(Collectors.toList());
	}

	public static ListenerParameters standardParameters() {
		ListenerParameters result = new ListenerParameters();
		result.setClientJoin(false);
		result.setBatchLogsSize(1);
		result.setLaunchName("My-test-launch" + generateUniqueId());
		result.setProjectName("test-project");
		result.setEnable(true);
		result.setCallbackReportingEnabled(true);
		result.setBaseUrl("http://localhost:8080");
		return result;
	}
}
