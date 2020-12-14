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

package net.webtide.tools.github;

import java.io.IOException;
import java.io.Reader;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.function.Function;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.webtide.tools.github.gson.ISO8601TypeAdapter;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.nio.charset.StandardCharsets.UTF_8;

public class GitHubApi
{
    private static final Logger LOG = LoggerFactory.getLogger(GitHubApi.class);
    private final URI apiURI;
    private final HttpClient client;
    private final HttpRequest.Builder baseRequest;
    private final Gson gson;
    private final Cache cache;

    private RateLeft rateLeft;

    private GitHubApi(String oauthToken)
    {
        this.apiURI = URI.create("https://api.github.com");

        this.client = HttpClient.newBuilder()
            .connectTimeout(Duration.of(2, ChronoUnit.SECONDS))
            .followRedirects(HttpClient.Redirect.NEVER)
            .build();
        this.baseRequest = HttpRequest.newBuilder()
            .header("Authorization", "Bearer " + oauthToken);
        this.gson = newGson();
        this.cache = new Cache();
    }

    public static GitHubApi connect()
    {
        String githubAppToken = System.getenv("GITHUB_TOKEN");
        if (StringUtils.isNotBlank(githubAppToken))
        {
            LOG.info("Connecting to GitHub with AppInstallation Token");
            return new GitHubApi(githubAppToken);
        }

        String[] configLocations = {
            ".github",
            ".github/oauth"
        };

        Path userHome = Paths.get(System.getProperty("user.home"));

        for (String configLocation : configLocations)
        {
            Path configPath = userHome.resolve(configLocation);
            if (Files.exists(configPath) && Files.isRegularFile(configPath))
            {
                try (Reader reader = Files.newBufferedReader(configPath, UTF_8))
                {
                    Properties props = new Properties();
                    props.load(reader);
                    String oauthToken = props.getProperty("oauth");
                    if (StringUtils.isNotBlank(oauthToken))
                    {
                        LOG.info("Connecting to GitHub with {} Token", configPath);
                        return new GitHubApi(oauthToken);
                    }
                }
                catch (IOException e)
                {
                    LOG.warn("Unable to read {}", configPath, e);
                }
            }
        }

        LOG.info("Connecting to GitHub with anonymous (no token)");
        return new GitHubApi(null);
    }

    /**
     * Create a Gson suitable for parsing Github JSON responses
     *
     * @return the Gson configured for Github JSON responses
     */
    public static Gson newGson()
    {
        return new GsonBuilder()
            .registerTypeAdapter(ZonedDateTime.class, new ISO8601TypeAdapter())
            .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
            .create();
    }

    public String raw(String path, Function<HttpRequest.Builder, HttpRequest> requestBuilder) throws IOException, InterruptedException
    {
        return getCachedBody(path, requestBuilder);
    }

    private String getCachedBody(String path, Function<HttpRequest.Builder, HttpRequest> requestBuilder) throws IOException, InterruptedException
    {
        try
        {
            String body = cache.getCached(path);
            LOG.debug("Returning Cached from {}", path);
            return body;
        }
        catch (GitHubResourceNotFoundException e)
        {
            throw e;
        }
        catch (IOException e)
        {
            RateLeft rateLeft = getRateLeft();
            int remainingRate = rateLeft.applyRequest("core");
            URI uri = apiURI.resolve(path);
            LOG.debug("Issuing API Request {} ({} remaining limit)", uri, remainingRate);
            HttpRequest request = requestBuilder.apply(baseRequest.copy().uri(uri));
            HttpResponse<String> response = client.send(request, responseInfo -> HttpResponse.BodySubscribers.ofString(UTF_8));
            switch (response.statusCode())
            {
                case 200:
                    cache.save(path, response.body());
                    return response.body();
                case 404:
                    cache.saveNotFound(path);
                    throw new GitHubResourceNotFoundException(path);
                default:
                    throw new GitHubApiException("Unable to get [" + path + "]: status code: " + response.statusCode());
            }
        }
    }

    private RateLeft getRateLeft() throws IOException, InterruptedException
    {
        if ((rateLeft == null) || (rateLeft.isExpired()))
        {
            RateLimits rateLimits = getRateLimits();
            rateLeft = new RateLeft(rateLimits);
        }
        return rateLeft;
    }

    public RateLimits getRateLimits() throws IOException, InterruptedException
    {
        URI endpointURI = apiURI.resolve("/rate_limit");
        HttpRequest request = baseRequest.copy()
            .GET()
            .uri(endpointURI)
            .header("Accept", "application/vnd.github.v3+json")
            .build();
        HttpResponse<String> response = client.send(request, responseInfo -> HttpResponse.BodySubscribers.ofString(UTF_8));
        if (response.statusCode() != 200)
            throw new GitHubApiException("Unable to get rate limits: status code: " + response.statusCode());
        return gson.fromJson(response.body(), RateLimits.class);
    }

    public User getSelf() throws IOException, InterruptedException
    {
        String body = getCachedBody("/user", (requestBuilder) ->
            requestBuilder.GET()
                .header("Accept", "application/vnd.github.v3+json")
                .build());
        return gson.fromJson(body, User.class);
    }

    public String graphql(String query) throws IOException, InterruptedException
    {
        Map<String, String> map = new HashMap<>();
        map.put("query", query);

        String jsonQuery = gson.toJson(map);

        URI endpointURI = apiURI.resolve("/graphql");
        HttpRequest request = baseRequest.copy()
            .POST(HttpRequest.BodyPublishers.ofString(jsonQuery))
            .header("Content-Type", "application/json")
            .uri(endpointURI)
            .header("Accept", "application/vnd.github.v3+json")
            .build();
        HttpResponse<String> response = client.send(request, responseInfo -> HttpResponse.BodySubscribers.ofString(UTF_8));
        if (response.statusCode() != 200)
            throw new GitHubApiException("Unable to post graphql: status code: " + response.statusCode());
        return response.body();
    }

    public Commit commit(String repoOwner, String repoName, String commitId) throws IOException, InterruptedException
    {
        String path = String.format("/repos/%s/%s/commits/%s", repoOwner, repoName, commitId);
        String body = getCachedBody(path, (requestBuilder) ->
            requestBuilder.GET()
                .header("Accept", "application/vnd.github.v3+json")
                .build());
        return gson.fromJson(body, Commit.class);
    }

    public Issue issue(String repoOwner, String repoName, int issueNum) throws IOException, InterruptedException
    {
        String path = String.format("/repos/%s/%s/issues/%d", repoOwner, repoName, issueNum);
        String body = getCachedBody(path, (requestBuilder) ->
            requestBuilder.GET()
                .header("Accept", "application/vnd.github.v3+json")
                .build());
        return gson.fromJson(body, Issue.class);
    }

    public IssueEvents issueEvents(String repoOwner, String repoName, int issueNum) throws IOException, InterruptedException
    {
        String path = String.format("/repos/%s/%s/issues/%d/events", repoOwner, repoName, issueNum);
        String body = getCachedBody(path, (requestBuilder) ->
            requestBuilder.GET()
                .header("Accept", "application/vnd.github.v3+json")
                .build());
        return gson.fromJson(body, IssueEvents.class);
    }

    public PullRequest pullRequest(String repoOwner, String repoName, int prNum) throws IOException, InterruptedException
    {
        String path = String.format("/repos/%s/%s/pulls/%d", repoOwner, repoName, prNum);
        String body = getCachedBody(path, (requestBuilder) ->
            requestBuilder.GET()
                .header("Accept", "application/vnd.github.v3+json")
                .build());
        return gson.fromJson(body, PullRequest.class);
    }

    public PullRequestCommits pullRequestCommits(String repoOwner, String repoName, int prNum) throws IOException, InterruptedException
    {
        String path = String.format("/repos/%s/%s/pulls/%d/commits", repoOwner, repoName, prNum);
        String body = getCachedBody(path, (requestBuilder) ->
            requestBuilder.GET()
                .header("Accept", "application/vnd.github.v3+json")
                .build());
        return gson.fromJson(body, PullRequestCommits.class);
    }

    public Releases listReleases(String repoOwner, String repoName, int resultsPerPage, int pageNum) throws IOException, InterruptedException
    {
        Query query = new Query();
        query.put("per_page", String.valueOf(resultsPerPage));
        query.put("page", String.valueOf(pageNum));

        String path = String.format("/repos/%s/%s/releases?%s", repoOwner, repoName, query.toEncodedQuery());

        String body = getCachedBody(path, (requestBuilder) ->
            requestBuilder.GET()
                .header("Accept", "application/vnd.github.v3+json")
                .build());
        return gson.fromJson(body, Releases.class);
    }

    public Stream<Release> streamReleases(String repoOwner, String repoName)
    {
        return streamReleases(repoOwner, repoName, 20);
    }

    public Stream<Release> streamReleases(String repoOwner, String repoName, int resultsPerPage)
    {
        return StreamSupport.stream(new ListReleasesSpliterator(this, repoOwner, repoName, resultsPerPage), false);
    }

    private void saveBody(String body, String format, Object... args)
    {
        Path targetDir = Paths.get("target");
        if (Files.exists(targetDir) && Files.isDirectory(targetDir))
        {
            Path outputPath = targetDir.resolve(String.format(format, args));
            try
            {
                LOG.debug("Writing body (size={}) to {}", body.length(), outputPath);
                Files.writeString(outputPath, body);
            }
            catch (IOException e)
            {
            }
        }
    }

    static class Query extends HashMap<String, String>
    {
        String toEncodedQuery()
        {
            StringBuilder ret = new StringBuilder();
            boolean delim = false;
            for (Entry<String, String> entry : entrySet())
            {
                if (delim)
                    ret.append('&');
                ret.append(entry.getKey()).append('=');
                ret.append(URLEncoder.encode(entry.getValue(), UTF_8));
                delim = true;
            }
            return ret.toString();
        }
    }
}
