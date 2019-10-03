package com.epam.reportportal.junit.utils;

import com.epam.reportportal.service.tree.TestItemTree;
import io.reactivex.annotations.Nullable;
import org.junit.runner.Description;
import org.junit.runners.model.FrameworkMethod;

/**
 * @author <a href="mailto:ivan_budayeu@epam.com">Ivan Budayeu</a>
 */
public class ItemTreeUtils {

	private ItemTreeUtils() {
		//static only
	}

	public static TestItemTree.ItemTreeKey createItemTreeKey(FrameworkMethod method) {
		return TestItemTree.ItemTreeKey.of(method.getName(), method.getDeclaringClass().getName().hashCode());
	}

	public static TestItemTree.ItemTreeKey createItemTreeKey(Description description) {
		return TestItemTree.ItemTreeKey.of(description.getMethodName(), description.getTestClass().getName().hashCode());
	}

	@Nullable
	public static TestItemTree.TestItemLeaf retrieveLeaf(Description description, TestItemTree testItemTree) {
		return testItemTree.getTestItems().get(createItemTreeKey(description));
	}
}
