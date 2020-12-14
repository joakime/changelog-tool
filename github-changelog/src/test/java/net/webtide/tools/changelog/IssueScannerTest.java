//
// ========================================================================
// Copyright (c) Webtide LLC and others.
//
// This program and the accompanying materials are made available under the
// terms of the Apache License, Version 2.0 which is available at
// https://www.apache.org/licenses/LICENSE-2.0.
//
// SPDX-License-Identifier: Apache-2.0
// ========================================================================
//

package net.webtide.tools.changelog;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;

import org.eclipse.jetty.toolchain.test.MavenTestingUtils;
import org.junit.jupiter.api.Test;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class IssueScannerTest
{
    @Test
    public void testScanNoHits()
    {
        Set<Integer> hits = IssueScanner.scan("Nothing here");
        assertEquals(0, hits.size());
    }

    @Test
    public void testScanSimpleSingleStart()
    {
        Set<Integer> hits = IssueScanner.scan("#5555 - Example Issue");
        int[] actual = toSortedArray(hits);
        int[] expected = {5555};
        assertArrayEquals(expected, actual);
    }

    @Test
    public void testScanSimpleSingleEnd()
    {
        Set<Integer> hits = IssueScanner.scan("Example Issue (#4444)");
        int[] actual = toSortedArray(hits);
        int[] expected = {4444};
        assertArrayEquals(expected, actual);
    }

    @Test
    public void testScanSimpleTwoReferences()
    {
        Set<Integer> hits = IssueScanner.scan("#6666 - Example Issue (#4444)");
        int[] actual = toSortedArray(hits);
        int[] expected = {4444, 6666};
        assertArrayEquals(expected, actual);
    }

    @Test
    public void testScanOutsideReferenceHtml()
    {
        Set<Integer> hits = IssueScanner.scan("<a href=\"https://github-redirect.dependabot.com/spring-projects/spring-framework/issues/25769\">#25769</a>");
        int[] actual = toSortedArray(hits);
        int[] expected = {};
        assertArrayEquals(expected, actual);
    }

    @Test
    public void testScanOutsideReferenceMarkdown()
    {
        Set<Integer> hits = IssueScanner.scan("spring-projects/spring-framework#25769");
        int[] actual = toSortedArray(hits);
        int[] expected = {};
        assertArrayEquals(expected, actual);
    }

    @Test
    public void testScanMultiLineThreeReferences()
    {
        Set<Integer> hits = IssueScanner.scan("Example Issue\n" +
            "#5555 - Fixing problem\n" +
            "#5555 - Addressing missing copyright\n" +
            "#5555 - Oops, forgot this file too.\n");
        int[] actual = toSortedArray(hits);
        int[] expected = {5555};
        assertArrayEquals(expected, actual);
    }

    @Test
    public void testScanAwkwardReference()
    {
        Set<Integer> hits = IssueScanner.scan("Issue 7777 - Example Issue");
        int[] actual = toSortedArray(hits);
        int[] expected = {7777};
        assertArrayEquals(expected, actual);
    }

    @Test
    public void testScanDependabotBody() throws IOException
    {
        Path dependabotIssue = MavenTestingUtils.getTestResourcePathFile("github/dependabot-issue-body.txt");
        String body = Files.readString(dependabotIssue, UTF_8);
        // Should result in no hits, as this is a dependabot body
        Set<Integer> hits = IssueScanner.scan(body);
        int[] actual = toSortedArray(hits);
        int[] expected = {};
        assertArrayEquals(expected, actual);
    }

    private int[] toSortedArray(Set<Integer> hits)
    {
        return hits.stream().mapToInt(Integer::intValue).sorted().toArray();
    }
}
