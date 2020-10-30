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
import com.nordstrom.automation.junit.AtomicTest;
import org.junit.runner.Description;
import org.junit.runners.model.FrameworkMethod;

import javax.annotation.Nullable;

/**
 * @author <a href="mailto:ivan_budayeu@epam.com">Ivan Budayeu</a>
 */
public class ItemTreeUtils {

	private ItemTreeUtils() {
		//static only
	}

	public static TestItemTree.ItemTreeKey createItemTreeKey(String name) {
		return TestItemTree.ItemTreeKey.of(name);
	}

	public static TestItemTree.ItemTreeKey createItemTreeKey(FrameworkMethod method) {
		return TestItemTree.ItemTreeKey.of(method.getName(), (method.getDeclaringClass().getName() + "." + method.getName()).hashCode());
	}

	public static <T> TestItemTree.ItemTreeKey createItemTreeKey(AtomicTest<T> test) {
		return TestItemTree.ItemTreeKey.of(test.getDescription().getDisplayName());
	}

	public static TestItemTree.ItemTreeKey createItemTreeKey(Description description) {
		return TestItemTree.ItemTreeKey.of(
				description.getMethodName(),
				(description.getTestClass().getName() + "." + description.getMethodName()).hashCode()
		);
	}

	@Nullable
	public static TestItemTree.TestItemLeaf retrieveLeaf(String name, TestItemTree testItemTree) {
		return testItemTree.getTestItems().get(createItemTreeKey(name));
	}

	@Nullable
	public static TestItemTree.TestItemLeaf retrieveLeaf(FrameworkMethod method, TestItemTree testItemTree) {
		return testItemTree.getTestItems().get(createItemTreeKey(method));
	}

	@Nullable
	public static TestItemTree.TestItemLeaf retrieveLeaf(Description description, TestItemTree testItemTree) {
		return testItemTree.getTestItems().get(createItemTreeKey(description));
	}
}
