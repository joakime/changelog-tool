package net.webtide.tools.github;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

import java.io.IOException;

public class SelfTest
{
    @Test
    public void testSelf() throws IOException, InterruptedException
    {
        GitHubApi github = GitHubApi.connect();
        User self = github.getSelf();
        assertNotNull(self);
    }
}
