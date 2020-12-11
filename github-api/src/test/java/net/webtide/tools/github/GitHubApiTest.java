package net.webtide.tools.github;

import org.junit.jupiter.api.Test;

import java.io.IOException;

public class GitHubApiTest
{
    @Test
    public void testRaw() throws IOException, InterruptedException
    {
        GitHubApi api = GitHubApi.connect();
        String path = "/repos/eclipse/jetty.project/pulls/5676/commits";
        // String path = "/repos/eclipse/jetty.project/issues/5675/events";
        String body = api.raw(path, (requestBuilder) ->
            requestBuilder.GET()
                .header("Accept", "application/vnd.github.v3+json")
                .build()
        );
        System.out.println(body);
    }
}
