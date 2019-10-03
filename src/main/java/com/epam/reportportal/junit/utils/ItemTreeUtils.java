package com.epam.reportportal.junit.utils;

import com.epam.reportportal.service.tree.TestItemTree;
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
}
