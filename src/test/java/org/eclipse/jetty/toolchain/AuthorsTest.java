package org.eclipse.jetty.toolchain;

import java.io.IOException;
import java.nio.file.Path;

import org.eclipse.jetty.toolchain.test.FS;
import org.eclipse.jetty.toolchain.test.MavenTestingUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class AuthorsTest
{
    @Test
    public void testLoad() throws IOException
    {
        Authors authors = Authors.load();
        assertEquals(9, authors.size());
    }

    @ParameterizedTest
    @ValueSource(strings = {"joakim@erdfelt.com", "gregw@webtide.com"})
    public void testIsCommitter(String email) throws IOException
    {
        Authors authors = Authors.load();
        assertTrue(authors.isCommitter(email), "Email should be a committer: " + email);
    }

    @ParameterizedTest
    @ValueSource(strings = {"bogus@webtide.com", "nobody@github.com"})
    public void testIsNotCommitter(String email) throws IOException
    {
        Authors authors = Authors.load();
        assertFalse(authors.isCommitter(email), "Email should NOT be a committer: " + email);
    }

    public void testSave(TestInfo testInfo) throws IOException
    {
        Authors authors = new Authors();
        Path outputDir = MavenTestingUtils.getTargetTestingPath(testInfo.getDisplayName());
        FS.ensureEmpty(outputDir);
        Path outputJson = outputDir.resolve("authors-out.json");
        authors.save(outputJson);
    }
}
