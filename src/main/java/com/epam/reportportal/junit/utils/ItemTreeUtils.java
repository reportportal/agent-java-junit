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

import com.epam.reportportal.service.tree.TestItemTree;
import com.epam.ta.reportportal.ws.model.ParameterResource;
import com.nordstrom.automation.junit.AtomicTest;
import org.junit.runner.Description;
import org.junit.runners.model.FrameworkMethod;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import static java.util.Optional.ofNullable;

/**
 * @author <a href="mailto:ivan_budayeu@epam.com">Ivan Budayeu</a>
 */
public class ItemTreeUtils {

	private ItemTreeUtils() {
		//static only
	}

	@Nonnull
	public static TestItemTree.ItemTreeKey createItemTreeKey(@Nullable String name) {
		return TestItemTree.ItemTreeKey.of(name);
	}

	public static final Function<List<ParameterResource>, String> PARAMETER_FORMAT = list -> ofNullable(list).map(l -> l.stream()
			.map(ParameterResource::getValue)
			.collect(Collectors.joining(",", "[", "]"))).orElse("");

	@Nonnull
	public static TestItemTree.ItemTreeKey createItemTreeKey(@Nonnull FrameworkMethod method) {
		String strKey = method.getDeclaringClass().getName() + "." + method.getName();
		return TestItemTree.ItemTreeKey.of(strKey);
	}

	@Nonnull
	public static TestItemTree.ItemTreeKey createItemTreeKey(@Nonnull FrameworkMethod method,
			@Nullable List<ParameterResource> parameters) {
		String paramStr = PARAMETER_FORMAT.apply(parameters);
		String strKey = method.getDeclaringClass().getName() + "." + method.getName() + paramStr;
		return TestItemTree.ItemTreeKey.of(strKey);
	}

	@Nonnull
	public static <T> TestItemTree.ItemTreeKey createItemTreeKey(@Nonnull AtomicTest<T> test) {
		return TestItemTree.ItemTreeKey.of(test.getDescription().getDisplayName());
	}

	@Nonnull
	public static TestItemTree.ItemTreeKey createItemTreeKey(@Nonnull Description description) {
		return TestItemTree.ItemTreeKey.of(description.getMethodName(),
				(description.getTestClass().getName() + "." + description.getMethodName()).hashCode()
		);
	}

	@Nullable
	@SuppressWarnings("unused")
	public static TestItemTree.TestItemLeaf retrieveLeaf(@Nullable String name, @Nonnull TestItemTree testItemTree) {
		return testItemTree.getTestItems().get(createItemTreeKey(name));
	}

	@Nullable
	public static TestItemTree.TestItemLeaf retrieveLeaf(@Nonnull FrameworkMethod method, @Nonnull TestItemTree testItemTree) {
		return testItemTree.getTestItems().get(createItemTreeKey(method));
	}

	@Nullable
	@SuppressWarnings("unused")
	public static TestItemTree.TestItemLeaf retrieveLeaf(@Nonnull Description description, @Nonnull TestItemTree testItemTree) {
		return testItemTree.getTestItems().get(createItemTreeKey(description));
	}
}
