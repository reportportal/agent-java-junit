package com.epam.reportportal.junit.junit;

import com.epam.reportportal.annotations.Tags;
import org.junit.Test;
import org.junit.experimental.categories.Category;

/**
 * @author Ilya_Koshaleu
 */
@Category({TagsTest.Tag1.class, TagsTest.Tag2.class})
@Tags({"Tag4", "Tag5"})
public class TagsTest {

    @Test
    @Category(Tag3.class)
    public void testTagCategory() {

    }

    @Test
    @Tags("Tag6")
    public void testTagTags() {

    }

    static class Tag1 {
    }

    static class Tag2 {
    }

    static class Tag3 {
    }
}
