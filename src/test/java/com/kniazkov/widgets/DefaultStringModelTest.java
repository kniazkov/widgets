package com.kniazkov.widgets;

import org.jetbrains.annotations.NotNull;
import org.junit.Assert;
import org.junit.Test;

/**
 * Testing default model containing a string.
 */
public class DefaultStringModelTest {
    @Test
    public void test() {
        final Model<String> model = new DefaultStringModel();
        Assert.assertNotNull(model.getData());
        Assert.assertTrue(model.isValid());
        final boolean[] changed = new boolean[1];
        final ModelListener<String> listener = data -> changed[0] = true;
        model.addListener(listener);
        model.setData("");
        Assert.assertFalse(changed[0]);
        model.setData("abc");
        Assert.assertTrue(changed[0]);
        Assert.assertEquals("abc", model.getData());
        model.removeListener(listener);
        changed[0] = false;
        model.setData("def");
        Assert.assertFalse(changed[0]);
    }
}
