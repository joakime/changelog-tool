package net.webtide.tools.github;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.google.gson.Gson;

import org.eclipse.jetty.toolchain.test.MavenTestingUtils;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class ChangeIssueTest
{
    @Test
    public void testIssue5675() throws IOException, InterruptedException
    {
        GitHubApi github = GitHubApi.connect();
        Issue issue = github.issue("eclipse", "jetty.project", 5675);

        assertNotNull(issue);
        assertEquals(5675, issue.number);
        assertEquals("janbartel", issue.user.login);
        assertEquals("open", issue.state);
        assertNull(issue.pullRequest);
    }

    @Test
    public void testPullRequest5676() throws IOException, InterruptedException
    {
        GitHubApi github = GitHubApi.connect();
        Issue issue = github.issue("eclipse", "jetty.project", 5676);

        assertNotNull(issue);
        assertEquals(5676, issue.number);
        assertEquals("janbartel", issue.user.login);
        assertEquals("closed", issue.state);
        assertNotNull(issue.pullRequest);
    }

    @Test
    public void testLoadIssueJson() throws IOException
    {
        Path json = MavenTestingUtils.getTestResourcePathFile("github/issue-eclipse-jetty.project-5675.json");
        Gson gson = GitHubApi.newGson();
        try (BufferedReader reader = Files.newBufferedReader(json))
        {
            Issue issue = gson.fromJson(reader, Issue.class);
            assertNotNull(issue);
            assertEquals(5675, issue.number);
            assertEquals("janbartel", issue.user.login);
            assertEquals("open", issue.state);
            assertNull(issue.pullRequest);
        }
    }

    @Test
    public void testLoadPRJson() throws IOException
    {
        Path json = MavenTestingUtils.getTestResourcePathFile("github/issue-eclipse-jetty.project-5676.json");
        Gson gson = GitHubApi.newGson();
        try (BufferedReader reader = Files.newBufferedReader(json))
        {
            Issue issue = gson.fromJson(reader, Issue.class);
            assertNotNull(issue);
            assertEquals(5676, issue.number);
            assertEquals("janbartel", issue.user.login);
            assertEquals("closed", issue.state);
            assertNotNull(issue.pullRequest);
        }
    }

}
