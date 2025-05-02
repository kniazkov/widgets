/*
 * Copyright (c) 2025 Ivan Kniazkov
 */
package com.kniazkov.widgets;

import org.junit.Assert;
import org.junit.Test;

/**
 * Testing unique identifier class.
 */
public class UIdTest {
    @Test
    public void test() {
        final UId id0 = UId.create();
        final UId id1 = UId.create();
        Assert.assertNotEquals(id0, id1);
        Assert.assertEquals(1, id1.compareTo(id0));
        final String str = id0.toString();
        Assert.assertTrue(str.startsWith("#"));
        final UId id2 = UId.parse(str);
        Assert.assertEquals(id0, id2);
        Assert.assertEquals(0, id2.compareTo(id0));
    }
}
