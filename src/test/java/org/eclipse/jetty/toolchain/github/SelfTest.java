package org.eclipse.jetty.toolchain.github;

import java.io.IOException;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

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
