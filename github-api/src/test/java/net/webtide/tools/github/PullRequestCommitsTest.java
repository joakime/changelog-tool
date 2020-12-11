package net.webtide.tools.github;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.google.gson.Gson;

import org.eclipse.jetty.toolchain.test.MavenTestingUtils;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class PullRequestCommitsTest
{
    @Test
    public void testPullRequest5676Commits() throws IOException, InterruptedException
    {
        GitHubApi github = GitHubApi.connect();
        PullRequestCommits commits = github.pullRequestCommits("eclipse", "jetty.project", 5676);

        assertNotNull(commits);
        assertEquals(8, commits.size());
        PullRequestCommits.Commit commit = commits.get(0);
        assertEquals("janb@webtide.com", commit.commit.author.email);
        assertEquals("bdb4dd435e18336c61f67d270004e17696481bc3", commit.sha);
    }

    @Test
    public void testLoadPullRequestCommitsJson() throws IOException
    {
        Path json = MavenTestingUtils.getTestResourcePathFile("github/pull-request-5676-commits.json");
        Gson gson = GitHubApi.newGson();
        try (BufferedReader reader = Files.newBufferedReader(json))
        {
            PullRequestCommits commits = gson.fromJson(reader, PullRequestCommits.class);
            assertNotNull(commits);
            assertEquals(8, commits.size());
            PullRequestCommits.Commit commit = commits.get(0);
            assertEquals("janb@webtide.com", commit.commit.author.email);
            assertEquals("bdb4dd435e18336c61f67d270004e17696481bc3", commit.sha);
        }
    }
}
