package org.eclipse.jetty.toolchain;

import java.util.Set;

import org.junit.jupiter.api.Test;

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

    private int[] toSortedArray(Set<Integer> hits)
    {
        return hits.stream().mapToInt(Integer::intValue).sorted().toArray();
    }
}
