/*
 * Copyright (c) 2024 Ivan Kniazkov
 */
package com.kniazkov.widgets;

import java.util.Map;
import java.util.TreeMap;

/**
 * A virtual file system that contains the code that runs on the client browser
 * as well as some data. This file is generated, no need to edit it.
 */
final class VirtualFileSystem {
    private static final byte[] data0 = {
        60, 33, 45, 45, 10, 32, 32, 67, 111, 112, 121, 114, 105, 103, 104, 116, 32, 40, 99, 
        41, 32, 50, 48, 50, 52, 32, 73, 118, 97, 110, 32, 75, 110, 105, 97, 122, 107, 111, 
        118, 10, 45, 45, 62, 10, 60, 33, 68, 79, 67, 84, 89, 80, 69, 32, 104, 116, 109, 108, 
        62, 10, 60, 104, 116, 109, 108, 32, 108, 97, 110, 103, 61, 34, 101, 110, 34, 62, 10, 
        -4, 32, 60, 104, 101, 97, 100, 62, 10, -8, 32, 60, 109, 101, 116, 97, 32, 99, 104, 
        97, 114, 115, 101, 116, 61, 34, 85, 84, 70, 45, 56, 34, 62, 10, -8, 32, 60, 115, 99, 
        114, 105, 112, 116, 32, 115, 114, 99, 61, 34, 115, 99, 114, 105, 112, 116, 115, 47, 
        108, 105, 98, 46, 106, 115, 34, 62, 60, 47, 115, 99, 114, 105, 112, 116, 62, 10, -8, 
        32, 60, 115, 99, 114, 105, 112, 116, 32, 115, 114, 99, 61, 34, 115, 99, 114, 105, 112, 
        116, 115, 47, 119, 105, 100, 103, 101, 116, 115, 46, 106, 115, 34, 62, 60, 47, 115, 
        99, 114, 105, 112, 116, 62, 10, -8, 32, 60, 115, 99, 114, 105, 112, 116, 32, 115, 114, 
        99, 61, 34, 115, 99, 114, 105, 112, 116, 115, 47, 99, 108, 105, 101, 110, 116, 46, 
        106, 115, 34, 62, 60, 47, 115, 99, 114, 105, 112, 116, 62, 10, -4, 32, 60, 47, 104, 
        101, 97, 100, 62, 10, -4, 32, 60, 98, 111, 100, 121, 62, 10, -8, 32, 60, 115, 99, 114, 
        105, 112, 116, 62, 10, -12, 32, 115, 116, 97, 114, 116, 67, 108, 105, 101, 110, 116, 
        40, 41, 59, 10, -8, 32, 60, 47, 115, 99, 114, 105, 112, 116, 62, 10, -4, 32, 60, 47, 
        98, 111, 100, 121, 62, 10, 60, 47, 104, 116, 109, 108, 62, 10
    };
    private static final byte[] data1 = {
        47, 42, 10, 32, 42, 32, 67, 111, 112, 121, 114, 105, 103, 104, 116, 32, 40, 99, 41, 
        32, 50, 48, 50, 52, 32, 73, 118, 97, 110, 32, 75, 110, 105, 97, 122, 107, 111, 118, 
        10, 32, 42, 47, 10, 10, 118, 97, 114, 32, 99, 108, 105, 101, 110, 116, 73, 100, 32, 
        61, 32, 110, 117, 108, 108, 59, 10, 118, 97, 114, 32, 112, 101, 114, 105, 111, 100, 
        32, 61, 32, 50, 48, 59, 10, 10, 118, 97, 114, 32, 115, 116, 97, 114, 116, 67, 108, 
        105, 101, 110, 116, 32, 61, 32, 102, 117, 110, 99, 116, 105, 111, 110, 40, 41, 32, 
        123, 10, -4, 32, 115, 101, 110, 100, 82, 101, 113, 117, 101, 115, 116, 40, 10, -8, 
        32, 123, 10, -12, 32, 97, 99, 116, 105, 111, 110, 32, 58, 32, 34, 110, 101, 119, 32, 
        105, 110, 115, 116, 97, 110, 99, 101, 34, 10, -8, 32, 125, 44, 10, -8, 32, 102, 117, 
        110, 99, 116, 105, 111, 110, 40, 100, 97, 116, 97, 41, 32, 123, 10, -12, 32, 118, 97, 
        114, 32, 106, 115, 111, 110, 32, 61, 32, 74, 83, 79, 78, 46, 112, 97, 114, 115, 101, 
        40, 100, 97, 116, 97, 41, 59, 10, -12, 32, 99, 108, 105, 101, 110, 116, 73, 100, 32, 
        61, 32, 106, 115, 111, 110, 46, 105, 100, 59, 10, -12, 32, 99, 111, 110, 115, 111, 
        108, 101, 46, 108, 111, 103, 40, 34, 67, 108, 105, 101, 110, 116, 32, 99, 114, 101, 
        97, 116, 101, 100, 44, 32, 105, 100, 58, 32, 34, 32, 43, 32, 99, 108, 105, 101, 110, 
        116, 73, 100, 32, 43, 32, 39, 46, 39, 41, 59, 10, -12, 32, 115, 101, 116, 73, 110, 
        116, 101, 114, 118, 97, 108, 40, 109, 97, 105, 110, 67, 121, 99, 108, 101, 44, 32, 
        112, 101, 114, 105, 111, 100, 41, 59, 10, -12, 32, 119, 105, 110, 100, 111, 119, 46, 
        97, 100, 100, 69, 118, 101, 110, 116, 76, 105, 115, 116, 101, 110, 101, 114, 40, 34, 
        98, 101, 102, 111, 114, 101, 117, 110, 108, 111, 97, 100, 34, 44, 32, 102, 117, 110, 
        99, 116, 105, 111, 110, 40, 41, 32, 123, 10, -16, 32, 115, 101, 110, 100, 82, 101, 
        113, 117, 101, 115, 116, 40, 10, -20, 32, 123, 10, -24, 32, 97, 99, 116, 105, 111, 
        110, 32, 58, 32, 34, 107, 105, 108, 108, 34, 44, 10, -24, 32, 99, 108, 105, 101, 110, 
        116, 32, 58, 32, 99, 108, 105, 101, 110, 116, 73, 100, 10, -20, 32, 125, 10, -16, 32, 
        41, 59, 10, -12, 32, 125, 41, 59, 10, -8, 32, 125, 10, -4, 32, 41, 59, 10, -4, 32, 
        115, 101, 116, 84, 105, 109, 101, 111, 117, 116, 40, 102, 117, 110, 99, 116, 105, 111, 
        110, 40, 41, 32, 123, 10, -8, 32, 105, 102, 32, 40, 99, 108, 105, 101, 110, 116, 73, 
        100, 32, 61, 61, 32, 110, 117, 108, 108, 41, 32, 123, 10, -12, 32, 115, 116, 97, 114, 
        116, 67, 108, 105, 101, 110, 116, 40, 41, 59, 10, -8, 32, 125, 10, -4, 32, 125, 44, 
        32, 49, -3, 48, 41, 59, 10, 125, 10, 10, 118, 97, 114, 32, 109, 97, 105, 110, 67, 121, 
        99, 108, 101, 32, 61, 32, 102, 117, 110, 99, 116, 105, 111, 110, 40, 41, 32, 123, 10, 
        -4, 32, 115, 101, 110, 100, 82, 101, 113, 117, 101, 115, 116, 40, 10, -8, 32, 123, 
        10, -12, 32, 97, 99, 116, 105, 111, 110, 32, 58, 32, 34, 115, 121, 110, 99, 104, 114, 
        111, 110, 105, 122, 101, 34, 44, 10, -12, 32, 99, 108, 105, 101, 110, 116, 32, 58, 
        32, 99, 108, 105, 101, 110, 116, 73, 100, 10, -8, 32, 125, 44, 10, -8, 32, 102, 117, 
        110, 99, 116, 105, 111, 110, 40, 100, 97, 116, 97, 41, 32, 123, 10, -12, 32, 118, 97, 
        114, 32, 105, 59, 10, -12, 32, 118, 97, 114, 32, 106, 115, 111, 110, 32, 61, 32, 74, 
        83, 79, 78, 46, 112, 97, 114, 115, 101, 40, 100, 97, 116, 97, 41, 59, 10, -12, 32, 
        105, 102, 32, 40, 106, 115, 111, 110, 46, 117, 112, 100, 97, 116, 101, 115, 41, 32, 
        123, 10, -16, 32, 102, 111, 114, 32, 40, 118, 97, 114, 32, 105, 32, 61, 32, 48, 59, 
        32, 105, 32, 60, 32, 106, 115, 111, 110, 46, 117, 112, 100, 97, 116, 101, 115, 46, 
        108, 101, 110, 103, 116, 104, 59, 32, 105, 43, 43, 41, 32, 123, 10, -20, 32, 118, 97, 
        114, 32, 114, 101, 115, 117, 108, 116, 32, 61, 32, 102, 97, 108, 115, 101, 59, 10, 
        -20, 32, 118, 97, 114, 32, 117, 112, 100, 97, 116, 101, 32, 61, 32, 106, 115, 111, 
        110, 46, 117, 112, 100, 97, 116, 101, 115, 91, 105, 93, 59, 10, -20, 32, 118, 97, 114, 
        32, 104, 97, 110, 100, 108, 101, 114, 32, 61, 32, 97, 99, 116, 105, 111, 110, 72, 97, 
        110, 100, 108, 101, 114, 115, 91, 117, 112, 100, 97, 116, 101, 46, 97, 99, 116, 105, 
        111, 110, 93, 59, 10, -20, 32, 105, 102, 32, 40, 104, 97, 110, 100, 108, 101, 114, 
        41, 32, 123, 10, -24, 32, 114, 101, 115, 117, 108, 116, 32, 61, 32, 104, 97, 110, 100, 
        108, 101, 114, 40, 117, 112, 100, 97, 116, 101, 41, 59, 10, -20, 32, 125, 10, -16, 
        32, 125, 10, -12, 32, 125, 10, -8, 32, 125, 10, -4, 32, 41, 59, 10, 125, 59, 10, 10, 
        118, 97, 114, 32, 97, 99, 116, 105, 111, 110, 72, 97, 110, 100, 108, 101, 114, 115, 
        32, 61, 32, 123, 10, -4, 32, 34, 99, 114, 101, 97, 116, 101, 34, 32, 58, 32, 99, 114, 
        101, 97, 116, 101, 87, 105, 100, 103, 101, 116, 44, 10, -4, 32, 34, 97, 112, 112, 101, 
        110, 100, 32, 99, 104, 105, 108, 100, 34, 32, 58, 32, 97, 112, 112, 101, 110, 100, 
        67, 104, 105, 108, 100, 87, 105, 100, 103, 101, 116, 44, 10, -4, 32, 34, 115, 101, 
        116, 32, 116, 101, 120, 116, 34, 32, 58, 32, 115, 101, 116, 84, 101, 120, 116, 10, 
        125, 59, 10
    };
    private static final byte[] data2 = {
        47, 42, 10, 32, 42, 32, 67, 111, 112, 121, 114, 105, 103, 104, 116, 32, 40, 99, 41, 
        32, 50, 48, 50, 52, 32, 73, 118, 97, 110, 32, 75, 110, 105, 97, 122, 107, 111, 118, 
        10, 32, 42, 47, 10, 10, 118, 97, 114, 32, 120, 109, 108, 72, 116, 116, 112, 79, 98, 
        106, 101, 99, 116, 32, 61, 32, 110, 117, 108, 108, 59, 10, 118, 97, 114, 32, 115, 101, 
        114, 118, 101, 114, 32, 61, 32, 119, 105, 110, 100, 111, 119, 46, 108, 111, 99, 97, 
        116, 105, 111, 110, 46, 112, 114, 111, 116, 111, 99, 111, 108, 32, 43, 32, 39, 47, 
        47, 39, 32, 43, 32, 119, 105, 110, 100, 111, 119, 46, 108, 111, 99, 97, 116, 105, 111, 
        110, 46, 104, 111, 115, 116, 59, 10, 10, 118, 97, 114, 32, 103, 101, 116, 88, 109, 
        108, 72, 116, 116, 112, 32, 61, 32, 102, 117, 110, 99, 116, 105, 111, 110, 40, 41, 
        32, 123, 10, -4, 32, 105, 102, 32, 40, 120, 109, 108, 72, 116, 116, 112, 79, 98, 106, 
        101, 99, 116, 41, 32, 123, 10, -8, 32, 114, 101, 116, 117, 114, 110, 32, 120, 109, 
        108, 72, 116, 116, 112, 79, 98, 106, 101, 99, 116, 59, 10, -4, 32, 125, 10, -4, 32, 
        116, 114, 121, 32, 123, 10, -8, 32, 120, 109, 108, 72, 116, 116, 112, 79, 98, 106, 
        101, 99, 116, 32, 61, 32, 110, 101, 119, 32, 65, 99, 116, 105, 118, 101, 88, 79, 98, 
        106, 101, 99, 116, 40, 39, 77, 115, 120, 109, 108, 50, 46, 88, 77, 76, 72, 84, 84, 
        80, 39, 41, 59, 10, -4, 32, 125, 10, -4, 32, 99, 97, 116, 99, 104, 32, 40, 101, 48, 
        41, 32, 123, 10, -8, 32, 116, 114, 121, 32, 123, 10, -12, 32, 120, 109, 108, 72, 116, 
        116, 112, 79, 98, 106, 101, 99, 116, 32, 61, 32, 110, 101, 119, 32, 65, 99, 116, 105, 
        118, 101, 88, 79, 98, 106, 101, 99, 116, 40, 39, 77, 105, 99, 114, 111, 115, 111, 102, 
        116, 46, 88, 77, 76, 72, 84, 84, 80, 39, 41, 59, 10, -8, 32, 125, 10, -8, 32, 99, 97, 
        116, 99, 104, 32, 40, 101, 49, 41, 32, 123, 10, -12, 32, 120, 109, 108, 72, 116, 116, 
        112, 79, 98, 106, 101, 99, 116, 32, 61, 32, 102, 97, 108, 115, 101, 59, 10, -8, 32, 
        125, 10, -4, 32, 125, 10, -4, 32, 105, 102, 32, 40, 33, 120, 109, 108, 72, 116, 116, 
        112, 79, 98, 106, 101, 99, 116, 32, 38, 38, 32, 116, 121, 112, 101, 111, 102, 32, 88, 
        77, 76, 72, 116, 116, 112, 82, 101, 113, 117, 101, 115, 116, 33, 61, 39, 117, 110, 
        100, 101, 102, 105, 110, 101, 100, 39, 41, 32, 123, 10, -8, 32, 120, 109, 108, 72, 
        116, 116, 112, 79, 98, 106, 101, 99, 116, 32, 61, 32, 110, 101, 119, 32, 88, 77, 76, 
        72, 116, 116, 112, 82, 101, 113, 117, 101, 115, 116, 40, 41, 59, 10, -4, 32, 125, 10, 
        -4, 32, 114, 101, 116, 117, 114, 110, 32, 120, 109, 108, 72, 116, 116, 112, 79, 98, 
        106, 101, 99, 116, 59, 10, 125, 59, 10, 10, 118, 97, 114, 32, 115, 101, 110, 100, 82, 
        101, 113, 117, 101, 115, 116, 32, 61, 32, 102, 117, 110, 99, 116, 105, 111, 110, 40, 
        113, 117, 101, 114, 121, 44, 32, 99, 97, 108, 108, 98, 97, 99, 107, 44, 32, 109, 101, 
        116, 104, 111, 100, 41, 32, 123, 10, -4, 32, 118, 97, 114, 32, 114, 101, 113, 32, 61, 
        32, 103, 101, 116, 88, 109, 108, 72, 116, 116, 112, 40, 41, 59, 10, -4, 32, 118, 97, 
        114, 32, 102, 111, 114, 109, 32, 61, 32, 110, 117, 108, 108, 59, 10, -4, 32, 118, 97, 
        114, 32, 112, 111, 115, 116, 32, 61, 32, 40, 109, 101, 116, 104, 111, 100, 32, 61, 
        61, 32, 34, 112, 111, 115, 116, 34, 41, 59, 10, -4, 32, 105, 102, 32, 40, 112, 111, 
        115, 116, 41, 32, 123, 10, -8, 32, 102, 111, 114, 109, 32, 61, 32, 110, 101, 119, 32, 
        70, 111, 114, 109, 68, 97, 116, 97, 40, 41, 59, 10, -8, 32, 102, 111, 114, 32, 40, 
        118, 97, 114, 32, 107, 101, 121, 32, 105, 110, 32, 113, 117, 101, 114, 121, 41, 32, 
        123, 10, -12, 32, 118, 97, 114, 32, 118, 97, 108, 117, 101, 32, 61, 32, 113, 117, 101, 
        114, 121, 91, 107, 101, 121, 93, 59, 10, -12, 32, 102, 111, 114, 109, 46, 97, 112, 
        112, 101, 110, 100, 40, 107, 101, 121, 44, 32, 118, 97, 108, 117, 101, 41, 59, 10, 
        -8, 32, 125, 10, -8, 32, 114, 101, 113, 46, 111, 112, 101, 110, 40, 34, 80, 79, 83, 
        84, 34, 44, 32, 115, 101, 114, 118, 101, 114, 44, 32, 116, 114, 117, 101, 41, 59, 10, 
        -4, 32, 125, 32, 101, 108, 115, 101, 32, 123, 10, -8, 32, 118, 97, 114, 32, 113, 117, 
        101, 114, 121, 83, 116, 114, 105, 110, 103, 32, 61, 32, 34, 34, 59, 10, -8, 32, 118, 
        97, 114, 32, 99, 111, 117, 110, 116, 32, 61, 32, 48, 59, 10, -8, 32, 102, 111, 114, 
        32, 40, 118, 97, 114, 32, 107, 101, 121, 32, 105, 110, 32, 113, 117, 101, 114, 121, 
        41, 32, 123, 10, -12, 32, 118, 97, 114, 32, 118, 97, 108, 117, 101, 32, 61, 32, 113, 
        117, 101, 114, 121, 91, 107, 101, 121, 93, 59, 10, -12, 32, 105, 102, 32, 40, 99, 111, 
        117, 110, 116, 41, 32, 123, 10, -16, 32, 113, 117, 101, 114, 121, 83, 116, 114, 105, 
        110, 103, 32, 43, 61, 32, 39, 38, 39, 59, 10, -12, 32, 125, 10, -12, 32, 99, 111, 117, 
        110, 116, 43, 43, 59, 10, -12, 32, 113, 117, 101, 114, 121, 83, 116, 114, 105, 110, 
        103, 32, 43, 61, 32, 107, 101, 121, 32, 43, 32, 39, 61, 39, 32, 43, 32, 101, 110, 99, 
        111, 100, 101, 85, 82, 73, 67, 111, 109, 112, 111, 110, 101, 110, 116, 40, 118, 97, 
        108, 117, 101, 41, 59, 10, -8, 32, 125, 10, -8, 32, 114, 101, 113, 46, 111, 112, 101, 
        110, 40, 34, 71, 69, 84, 34, 44, 32, 115, 101, 114, 118, 101, 114, 32, 43, 32, 39, 
        63, 39, 32, 43, 32, 113, 117, 101, 114, 121, 83, 116, 114, 105, 110, 103, 44, 32, 116, 
        114, 117, 101, 41, 59, 10, -4, 32, 125, 10, -4, 32, 114, 101, 113, 46, 111, 110, 114, 
        101, 97, 100, 121, 115, 116, 97, 116, 101, 99, 104, 97, 110, 103, 101, 32, 61, 32, 
        102, 117, 110, 99, 116, 105, 111, 110, 40, 41, 32, 123, 10, -8, 32, 105, 102, 32, 40, 
        114, 101, 113, 46, 114, 101, 97, 100, 121, 83, 116, 97, 116, 101, 32, 61, 61, 32, 52, 
        41, 32, 123, 10, -12, 32, 105, 102, 40, 114, 101, 113, 46, 115, 116, 97, 116, 117, 
        115, 32, 61, 61, 32, 50, 48, 48, 41, 32, 123, 10, -16, 32, 105, 102, 32, 40, 99, 97, 
        108, 108, 98, 97, 99, 107, 41, 32, 123, 10, -20, 32, 99, 97, 108, 108, 98, 97, 99, 
        107, 40, 114, 101, 113, 46, 114, 101, 115, 112, 111, 110, 115, 101, 84, 101, 120, 116, 
        41, 59, 10, -16, 32, 125, 10, -12, 32, 125, 10, -8, 32, 125, 10, -4, 32, 125, 59, 10, 
        -4, 32, 114, 101, 113, 46, 115, 101, 110, 100, 40, 102, 111, 114, 109, 41, 59, 10, 
        125, 59, 10, 10, 118, 97, 114, 32, 97, 100, 100, 69, 118, 101, 110, 116, 32, 61, 32, 
        102, 117, 110, 99, 116, 105, 111, 110, 40, 111, 98, 106, 101, 99, 116, 44, 32, 116, 
        121, 112, 101, 44, 32, 99, 97, 108, 108, 98, 97, 99, 107, 41, 32, 123, 10, -4, 32, 
        105, 102, 32, 40, 116, 121, 112, 101, 111, 102, 40, 111, 98, 106, 101, 99, 116, 41, 
        32, 61, 61, 32, 34, 115, 116, 114, 105, 110, 103, 34, 41, 32, 123, 10, -8, 32, 111, 
        98, 106, 101, 99, 116, 32, 61, 32, 100, 111, 99, 117, 109, 101, 110, 116, 46, 103, 
        101, 116, 69, 108, 101, 109, 101, 110, 116, 66, 121, 73, 100, 40, 111, 98, 106, 101, 
        99, 116, 41, 59, 10, -4, 32, 125, 10, -4, 32, 105, 102, 32, 40, 111, 98, 106, 101, 
        99, 116, 32, 61, 61, 32, 110, 117, 108, 108, 32, 124, 124, 32, 116, 121, 112, 101, 
        111, 102, 40, 111, 98, 106, 101, 99, 116, 41, 32, 61, 61, 32, 34, 117, 110, 100, 101, 
        102, 105, 110, 101, 100, 34, 41, 32, 123, 10, -8, 32, 114, 101, 116, 117, 114, 110, 
        59, 10, -4, 32, 125, 10, 10, -4, 32, 105, 102, 32, 40, 111, 98, 106, 101, 99, 116, 
        46, 97, 100, 100, 69, 118, 101, 110, 116, 76, 105, 115, 116, 101, 110, 101, 114, 41, 
        32, 123, 10, -8, 32, 111, 98, 106, 101, 99, 116, 46, 97, 100, 100, 69, 118, 101, 110, 
        116, 76, 105, 115, 116, 101, 110, 101, 114, 40, 116, 121, 112, 101, 44, 32, 99, 97, 
        108, 108, 98, 97, 99, 107, 44, 32, 102, 97, 108, 115, 101, 41, 59, 10, -4, 32, 125, 
        10, -4, 32, 101, 108, 115, 101, 32, 105, 102, 32, 40, 111, 98, 106, 101, 99, 116, 46, 
        97, 116, 116, 97, 99, 104, 69, 118, 101, 110, 116, 41, 32, 123, 10, -8, 32, 111, 98, 
        106, 101, 99, 116, 46, 97, 116, 116, 97, 99, 104, 69, 118, 101, 110, 116, 40, 34, 111, 
        110, 34, 32, 43, 32, 116, 121, 112, 101, 44, 32, 99, 97, 108, 108, 98, 97, 99, 107, 
        41, 59, 10, -4, 32, 125, 10, -4, 32, 101, 108, 115, 101, 32, 123, 10, -8, 32, 111, 
        98, 106, 101, 99, 116, 91, 34, 111, 110, 34, 32, 43, 32, 116, 121, 112, 101, 93, 32, 
        61, 32, 99, 97, 108, 108, 98, 97, 99, 107, 59, 10, -4, 32, 125, 10, 125, 59, 10, 10, 
        118, 97, 114, 32, 101, 115, 99, 97, 112, 101, 72, 116, 109, 108, 32, 61, 32, 102, 117, 
        110, 99, 116, 105, 111, 110, 40, 117, 110, 115, 97, 102, 101, 41, 32, 123, 10, -4, 
        32, 114, 101, 116, 117, 114, 110, 32, 117, 110, 115, 97, 102, 101, 10, -8, 32, 46, 
        114, 101, 112, 108, 97, 99, 101, 40, 47, 38, 47, 103, 44, 32, 34, 38, 97, 109, 112, 
        59, 34, 41, 10, -8, 32, 46, 114, 101, 112, 108, 97, 99, 101, 40, 47, 60, 47, 103, 44, 
        32, 34, 38, 108, 116, 59, 34, 41, 10, -8, 32, 46, 114, 101, 112, 108, 97, 99, 101, 
        40, 47, 62, 47, 103, 44, 32, 34, 38, 103, 116, 59, 34, 41, 10, -8, 32, 46, 114, 101, 
        112, 108, 97, 99, 101, 40, 47, 34, 47, 103, 44, 32, 34, 38, 113, 117, 111, 116, 59, 
        34, 41, 10, -8, 32, 46, 114, 101, 112, 108, 97, 99, 101, 40, 47, 39, 47, 103, 44, 32, 
        34, 38, 35, 48, 51, 57, 59, 34, 41, 59, 10, 125, 59, 10
    };
    private static final byte[] data3 = {
        47, 42, 10, 32, 42, 32, 67, 111, 112, 121, 114, 105, 103, 104, 116, 32, 40, 99, 41, 
        32, 50, 48, 50, 52, 32, 73, 118, 97, 110, 32, 75, 110, 105, 97, 122, 107, 111, 118, 
        10, 32, 42, 47, 10, 10, 118, 97, 114, 32, 119, 105, 100, 103, 101, 116, 115, 32, 61, 
        32, 123, 32, 125, 59, 10, 10, 118, 97, 114, 32, 119, 105, 100, 103, 101, 116, 115, 
        76, 105, 98, 114, 97, 114, 121, 32, 61, 32, 123, 10, -4, 32, 34, 114, 111, 111, 116, 
        34, 32, 58, 32, 102, 117, 110, 99, 116, 105, 111, 110, 40, 41, 32, 123, 10, -8, 32, 
        114, 101, 116, 117, 114, 110, 32, 100, 111, 99, 117, 109, 101, 110, 116, 46, 98, 111, 
        100, 121, 59, 10, -4, 32, 125, 44, 10, -4, 32, 34, 112, 97, 114, 97, 103, 114, 97, 
        112, 104, 34, 32, 58, 32, 102, 117, 110, 99, 116, 105, 111, 110, 40, 41, 32, 123, 10, 
        -8, 32, 114, 101, 116, 117, 114, 110, 32, 100, 111, 99, 117, 109, 101, 110, 116, 46, 
        99, 114, 101, 97, 116, 101, 69, 108, 101, 109, 101, 110, 116, 40, 34, 112, 34, 41, 
        59, 10, -4, 32, 125, 44, 10, -4, 32, 34, 116, 101, 120, 116, 34, 32, 58, 32, 102, 117, 
        110, 99, 116, 105, 111, 110, 40, 41, 32, 123, 10, -8, 32, 114, 101, 116, 117, 114, 
        110, 32, 100, 111, 99, 117, 109, 101, 110, 116, 46, 99, 114, 101, 97, 116, 101, 69, 
        108, 101, 109, 101, 110, 116, 40, 34, 115, 112, 97, 110, 34, 41, 59, 10, -4, 32, 125, 
        44, 10, -4, 32, 34, 105, 110, 112, 117, 116, 32, 102, 105, 101, 108, 100, 34, 32, 58, 
        32, 102, 117, 110, 99, 116, 105, 111, 110, 40, 41, 32, 123, 10, -8, 32, 114, 101, 116, 
        117, 114, 110, 32, 100, 111, 99, 117, 109, 101, 110, 116, 46, 99, 114, 101, 97, 116, 
        101, 69, 108, 101, 109, 101, 110, 116, 40, 34, 105, 110, 112, 117, 116, 34, 41, 59, 
        10, -4, 32, 125, 44, 10, -4, 32, 34, 98, 117, 116, 116, 111, 110, 34, 32, 58, 32, 102, 
        117, 110, 99, 116, 105, 111, 110, 40, 41, 32, 123, 10, -8, 32, 114, 101, 116, 117, 
        114, 110, 32, 100, 111, 99, 117, 109, 101, 110, 116, 46, 99, 114, 101, 97, 116, 101, 
        69, 108, 101, 109, 101, 110, 116, 40, 34, 98, 117, 116, 116, 111, 110, 34, 41, 59, 
        10, -4, 32, 125, 10, 125, 59, 10, 10, 118, 97, 114, 32, 99, 114, 101, 97, 116, 101, 
        87, 105, 100, 103, 101, 116, 32, 61, 32, 102, 117, 110, 99, 116, 105, 111, 110, 40, 
        100, 97, 116, 97, 41, 32, 123, 10, -4, 32, 118, 97, 114, 32, 99, 116, 111, 114, 32, 
        61, 32, 119, 105, 100, 103, 101, 116, 115, 76, 105, 98, 114, 97, 114, 121, 91, 100, 
        97, 116, 97, 46, 116, 121, 112, 101, 93, 59, 10, -4, 32, 118, 97, 114, 32, 105, 100, 
        32, 61, 32, 100, 97, 116, 97, 46, 119, 105, 100, 103, 101, 116, 59, 10, -4, 32, 105, 
        102, 32, 40, 99, 116, 111, 114, 32, 38, 38, 32, 105, 100, 41, 32, 123, 10, -8, 32, 
        118, 97, 114, 32, 119, 105, 100, 103, 101, 116, 32, 61, 32, 99, 116, 111, 114, 40, 
        41, 59, 10, -8, 32, 119, 105, 100, 103, 101, 116, 115, 91, 105, 100, 93, 32, 61, 32, 
        119, 105, 100, 103, 101, 116, 59, 10, -8, 32, 99, 111, 110, 115, 111, 108, 101, 46, 
        108, 111, 103, 40, 34, 87, 105, 100, 103, 101, 116, 32, 39, 34, 32, 43, 32, 100, 97, 
        116, 97, 46, 116, 121, 112, 101, 32, 43, 32, 34, 39, 32, 99, 114, 101, 97, 116, 101, 
        100, 44, 32, 105, 100, 58, 32, 34, 32, 43, 32, 105, 100, 32, 43, 32, 39, 46, 39, 41, 
        59, 10, -8, 32, 114, 101, 116, 117, 114, 110, 32, 116, 114, 117, 101, 59, 10, -4, 32, 
        125, 10, -4, 32, 114, 101, 116, 117, 114, 110, 32, 102, 97, 108, 115, 101, 59, 10, 
        125, 59, 10, 10, 118, 97, 114, 32, 97, 112, 112, 101, 110, 100, 67, 104, 105, 108, 
        100, 87, 105, 100, 103, 101, 116, 32, 61, 32, 102, 117, 110, 99, 116, 105, 111, 110, 
        40, 100, 97, 116, 97, 41, 32, 123, 10, -4, 32, 118, 97, 114, 32, 119, 105, 100, 103, 
        101, 116, 32, 61, 32, 119, 105, 100, 103, 101, 116, 115, 91, 100, 97, 116, 97, 46, 
        119, 105, 100, 103, 101, 116, 93, 59, 10, -4, 32, 118, 97, 114, 32, 99, 104, 105, 108, 
        100, 32, 61, 32, 119, 105, 100, 103, 101, 116, 115, 91, 100, 97, 116, 97, 46, 99, 104, 
        105, 108, 100, 93, 59, 10, -4, 32, 105, 102, 32, 40, 119, 105, 100, 103, 101, 116, 
        32, 38, 38, 32, 99, 104, 105, 108, 100, 41, 32, 123, 10, -8, 32, 119, 105, 100, 103, 
        101, 116, 46, 97, 112, 112, 101, 110, 100, 67, 104, 105, 108, 100, 40, 99, 104, 105, 
        108, 100, 41, 59, 10, -8, 32, 99, 111, 110, 115, 111, 108, 101, 46, 108, 111, 103, 
        40, 34, 87, 105, 100, 103, 101, 116, 32, 34, 32, 43, 32, 100, 97, 116, 97, 46, 99, 
        104, 105, 108, 100, 32, 43, 32, 34, 32, 105, 115, 32, 97, 100, 100, 101, 100, 32, 97, 
        115, 32, 97, 32, 99, 104, 105, 108, 100, 32, 111, 102, 32, 119, 105, 100, 103, 101, 
        116, 32, 34, 32, 43, 32, 100, 97, 116, 97, 46, 119, 105, 100, 103, 101, 116, 32, 43, 
        32, 39, 46, 39, 41, 59, 10, -8, 32, 114, 101, 116, 117, 114, 110, 32, 116, 114, 117, 
        101, 59, 10, -4, 32, 125, 10, -4, 32, 114, 101, 116, 117, 114, 110, 32, 102, 97, 108, 
        115, 101, 59, 10, 125, 59, 10, 10, 118, 97, 114, 32, 115, 101, 116, 84, 101, 120, 116, 
        32, 61, 32, 102, 117, 110, 99, 116, 105, 111, 110, 40, 100, 97, 116, 97, 41, 32, 123, 
        10, -4, 32, 118, 97, 114, 32, 119, 105, 100, 103, 101, 116, 32, 61, 32, 119, 105, 100, 
        103, 101, 116, 115, 91, 100, 97, 116, 97, 46, 119, 105, 100, 103, 101, 116, 93, 59, 
        10, -4, 32, 105, 102, 32, 40, 119, 105, 100, 103, 101, 116, 32, 38, 38, 32, 116, 121, 
        112, 101, 111, 102, 32, 100, 97, 116, 97, 46, 116, 101, 120, 116, 32, 61, 61, 32, 34, 
        115, 116, 114, 105, 110, 103, 34, 41, 32, 123, 10, -8, 32, 119, 105, 100, 103, 101, 
        116, 46, 105, 110, 110, 101, 114, 72, 84, 77, 76, 32, 61, 32, 101, 115, 99, 97, 112, 
        101, 72, 116, 109, 108, 40, 100, 97, 116, 97, 46, 116, 101, 120, 116, 41, 59, 10, -8, 
        32, 99, 111, 110, 115, 111, 108, 101, 46, 108, 111, 103, 40, 34, 84, 104, 101, 32, 
        116, 101, 120, 116, 32, 92, 34, 34, 32, 43, 32, 100, 97, 116, 97, 46, 116, 101, 120, 
        116, 32, 43, 32, 34, 92, 34, 32, 104, 97, 115, 32, 98, 101, 101, 110, 32, 115, 101, 
        116, 32, 116, 111, 32, 116, 104, 101, 32, 119, 105, 100, 103, 101, 116, 32, 34, 32, 
        43, 32, 100, 97, 116, 97, 46, 119, 105, 100, 103, 101, 116, 32, 43, 32, 39, 46, 39, 
        41, 59, 10, -8, 32, 114, 101, 116, 117, 114, 110, 32, 116, 114, 117, 101, 59, 10, -4, 
        32, 125, 10, -4, 32, 114, 101, 116, 117, 114, 110, 32, 102, 97, 108, 115, 101, 59, 
        10, 125, 59, 10
    };
    private static final Map<String, byte[]> files = new TreeMap<String, byte[]>() {{
        put("/index.html", data0);
        put("/scripts/client.js", data1);
        put("/scripts/lib.js", data2);
        put("/scripts/widgets.js", data3);
    }};

    /**
     * Returns the contents of a file at its path.
     * @param path File path
     */
    public static byte[] get(final String path) {
        return files.get(path);
    }
}
