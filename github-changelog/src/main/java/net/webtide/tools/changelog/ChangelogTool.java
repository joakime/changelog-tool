package net.webtide.tools.changelog;

import static java.nio.charset.StandardCharsets.UTF_8;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.stream.JsonWriter;

import net.webtide.tools.changelog.gson.ISO8601TypeAdapter;
import net.webtide.tools.github.Commit;
import net.webtide.tools.github.GitHubApi;
import net.webtide.tools.github.GitHubResourceNotFoundException;
import net.webtide.tools.github.IssueEvents;
import net.webtide.tools.github.Label;
import net.webtide.tools.github.PullRequestCommits;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.LogCommand;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class ChangelogTool implements AutoCloseable
{
    private static final Logger LOG = LoggerFactory.getLogger(ChangelogTool.class);

    private final Git git;
    private final Repository repository;
    private final GitCache gitCache;
    private final Authors authors = Authors.load();
    private String githubOwner;
    private String githubRepoName;
    private GitHubApi github;
    private String branch;
    private String tagOldVersion;
    private String tagNewVersion;
    private Map<Integer, ChangeIssue> issueMap = new HashMap<>();
    private Map<String, ChangeCommit> commitMap = new HashMap<>();
    private List<Predicate<String>> branchExclusion = new ArrayList<>();
    private List<Predicate<String>> commitPathExclusionFilters = new ArrayList<>();
    private Set<String> excludedLabels = new HashSet<>();
    private List<Change> changes = new ArrayList<>();

    public ChangelogTool(Path localGitRepo) throws IOException
    {
        git = Git.open(localGitRepo.toFile());
        repository = git.getRepository();
        gitCache = new GitCache(git);
        System.out.println("Repository: " + repository);
    }

    @Override
    public void close()
    {
        this.gitCache.close();
        this.repository.close();
        this.git.close();
    }

    public Collection<ChangeCommit> getCommits()
    {
        return this.commitMap.values();
    }

    public ChangeIssue getIssue(int num)
    {
        return this.issueMap.get(num);
    }

    public Collection<ChangeIssue> getIssues()
    {
        return this.issueMap.values();
    }

    public void save(Path outputDir) throws IOException
    {
        Gson gson = new GsonBuilder().setPrettyPrinting()
            .registerTypeAdapter(ZonedDateTime.class, new ISO8601TypeAdapter())
            .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
            .create();

        Path authorsLog = outputDir.resolve("authors-scan.json");
        try (BufferedWriter writer = Files.newBufferedWriter(authorsLog, UTF_8);
             JsonWriter jsonWriter = gson.newJsonWriter(writer))
        {
            gson.toJson(authors, Authors.class, jsonWriter);
        }

        Path issueLog = outputDir.resolve("change-issues.json");
        try (BufferedWriter writer = Files.newBufferedWriter(issueLog, UTF_8);
             JsonWriter jsonWriter = gson.newJsonWriter(writer))
        {
            gson.toJson(issueMap.values(), Set.class, jsonWriter);
        }

        Path commitsLog = outputDir.resolve("change-commits.json");
        try (BufferedWriter writer = Files.newBufferedWriter(commitsLog, UTF_8);
             JsonWriter jsonWriter = gson.newJsonWriter(writer))
        {
            gson.toJson(commitMap.values(), Set.class, jsonWriter);
        }

        Path changeLog = outputDir.resolve("change-groups.json");
        try (BufferedWriter writer = Files.newBufferedWriter(changeLog, UTF_8);
             JsonWriter jsonWriter = gson.newJsonWriter(writer))
        {
            gson.toJson(changes, List.class, jsonWriter);
        }

        Path changePaths = outputDir.resolve("change-paths.log");
        try (BufferedWriter writer = Files.newBufferedWriter(changePaths))
        {
            Set<String> changedFiles = new HashSet<>();
            for (ChangeCommit commit : commitMap.values())
            {
                if (commit.isSkipped())
                    continue;
                if (commit.getFiles() != null)
                    changedFiles.addAll(commit.getFiles());
            }
            for (String filename : changedFiles.stream().sorted().collect(Collectors.toList()))
            {
                writer.write(filename);
                writer.write("\n");
            }
            System.out.printf("Found %,d Files changed in the various commits%n", changedFiles.size());
        }

        Path markdownOutput = outputDir.resolve("changelog.md");
        writeMarkdown(markdownOutput);
    }

    public void setVersionRange(String tagOldVersion, String tagNewVersion)
    {
        this.tagOldVersion = tagOldVersion;
        this.tagNewVersion = tagNewVersion;
    }

    public void setGithubRepo(String owner, String repoName)
    {
        this.githubOwner = owner;
        this.githubRepoName = repoName;
    }

    public void setBranch(String branch)
    {
        this.branch = branch;
    }

    public void addLabelExclusion(String label)
    {
        this.excludedLabels.add(label);
    }

    /**
     * Exclude paths on commit with this predicate.
     * If the resulting commit is devoid of paths as a result, it is flagged as skipped.
     */
    public void addCommitPathExclusionFilter(Predicate<String> predicate)
    {
        Objects.requireNonNull(predicate, "predicate");
        this.commitPathExclusionFilters.add(predicate);
    }

    /**
     * If commit has specific branch, do not include it in the results.
     */
    public void addBranchExclusion(Predicate<String> predicate)
    {
        this.branchExclusion.add(predicate);
    }

    public ChangeIssue findRelevantIssue(Set<Integer> nums)
    {
        for (int num : nums)
        {
            ChangeIssue issue = this.issueMap.get(num);
            if (issue == null)
                continue;
            if (issue.isSkipped())
                continue;
            return issue;
        }
        return null;
    }

    public void resolveCommits() throws IOException, GitAPIException
    {
        RevCommit commitOld = findCommitForTag(tagOldVersion);
        RevCommit commitNew = findCommitForTag(tagNewVersion);
        LOG.debug("commit log: {} .. {}", commitOld, commitNew);

        int count = 0;

        LogCommand logCommand = git.log().addRange(commitOld, commitNew);

        for (RevCommit commit : logCommand.call())
        {
            Author author = getAuthor(authors, commit);

            ChangeCommit changeCommit = getCommit(commit.getId().getName());

            changeCommit.setCommitTime(ZonedDateTime.ofInstant(Instant.ofEpochMilli(commit.getCommitTime()), ZoneId.systemDefault()));
            changeCommit.setAuthor(author);
            changeCommit.setTitle(commit.getShortMessage());
            changeCommit.setBody(commit.getFullMessage());
            if (isMergeCommit(commit))
                changeCommit.addSkipReason(Skip.IS_MERGE_COMMIT);

            count++;
        }

        LOG.debug("Found {} commits", count);
    }

    private ChangeCommit getCommit(String sha)
    {
        String lowerSha = sha.toLowerCase(Locale.US);
        ChangeCommit commit = commitMap.get(lowerSha);
        if (commit == null)
        {
            commit = new ChangeCommit();
            commit.setSha(lowerSha);
            commitMap.put(lowerSha, commit);
        }
        return commit;
    }

    private Predicate<String> getStringPredicate(Collection<Predicate<String>> filters)
    {
        Predicate<String> predicate = str -> true;
        for (Predicate<String> logPredicate : filters)
        {
            predicate = predicate.and(logPredicate);
        }
        return predicate;
    }

    private void collectIssueReferences(ChangeCommit commit)
    {
        Set<Integer> issueNums = new HashSet<>();
        issueNums.addAll(IssueScanner.scan(commit.getTitle()));
        issueNums.addAll(IssueScanner.scanResolutions(commit.getBody()));
        for (int issueNum : issueNums)
        {
            ChangeIssue issue = issueMap.get(issueNum);
            if (issue == null)
            {
                issue = new ChangeIssue(issueNum);
                issueMap.put(issueNum, issue);
            }
            issue.addCommit(commit.getSha());
        }
    }

    public void resolveIssues()
    {
        LOG.debug("Discover issue references");

        for (ChangeCommit changeCommit : commitMap.values())
        {
            collectIssueReferences(changeCommit);
        }

        LOG.debug("Resolving issue details ...");
        boolean done = false;
        while (!done)
        {
            List<ChangeIssue> unknownIssues = issueMap.values().stream()
                .filter((issue) -> issue.getType() == IssueType.UNKNOWN)
                .collect(Collectors.toList());

            if (unknownIssues.isEmpty())
                done = true;
            else
            {
                int issuesLeft = unknownIssues.size();
                for (ChangeIssue unknownIssue : unknownIssues)
                {
                    LOG.info("Need to resolve {} more issues ...", issuesLeft--);
                    resolveUnknownIssue(unknownIssue);
                }
            }
        }

        LOG.debug("Tracking {} issues", issueMap.size());
    }

    private void resolveUnknownIssue(ChangeIssue issue)
    {
        try
        {
            net.webtide.tools.github.Issue ghIssue = getGitHubApi().issue(githubOwner, githubRepoName, issue.getNum());
            issue.addLabels(ghIssue.getLabels().stream().map(Label::getName).collect(Collectors.toSet()));

            if (ghIssue.getPullRequest() != null)
            {
                net.webtide.tools.github.PullRequest ghPullRequest = getGitHubApi().pullRequest(githubOwner, githubRepoName, issue.getNum());
                issue.addLabels(ghPullRequest.getLabels().stream().map(Label::getName).collect(Collectors.toSet()));
                issue.setBaseRef(ghPullRequest.getBase().getRef());
                issue.setTitle(ghPullRequest.getTitle());
                issue.setBody(ghPullRequest.getBody());
                issue.setState(ghPullRequest.getState());
                issue.setType(IssueType.PULL_REQUEST);
            }
            else
            {
                issue.setTitle(ghIssue.getTitle());
                issue.setBody(ghIssue.getBody());
                issue.setState(ghIssue.getState());
                issue.setType(IssueType.ISSUE);
            }

            Set<Integer> issueRefs = new HashSet<>();
            issueRefs.addAll(IssueScanner.scan(issue.getTitle()));
            issueRefs.addAll(IssueScanner.scanResolutions(issue.getBody()));
            issueRefs.remove(issue.getNum());
            issue.addReferencedIssues(issueRefs);

            // Discover any newly referenced issue for later resolve
            for (int issueNum : issueRefs)
            {
                ChangeIssue ref = issueMap.get(issueNum);
                if (ref == null)
                {
                    ref = new ChangeIssue(issueNum);
                    issueMap.put(issueNum, ref);
                }
            }

            // Test labels
            for (String excludedLabel : excludedLabels)
            {
                if (issue.hasLabel(excludedLabel))
                {
                    issue.addSkipReason(Skip.EXCLUDED_LABEL);
                    return;
                }
            }

            if (issue.getType() == IssueType.ISSUE)
            {
                net.webtide.tools.github.IssueEvents ghIssueEvents = getGitHubApi().issueEvents(githubOwner, githubRepoName, issue.getNum());
                for (IssueEvents.IssueEvent event : ghIssueEvents)
                {
                    if (StringUtils.isNotBlank(event.getCommitId()))
                    {
                        issue.addCommit(event.getCommitId());
                    }
                }
            }
            else if (issue.getType() == IssueType.PULL_REQUEST)
            {
                net.webtide.tools.github.PullRequestCommits ghPullRequestCommits = getGitHubApi().pullRequestCommits(githubOwner, githubRepoName, issue.getNum());
                for (PullRequestCommits.Commit commit : ghPullRequestCommits)
                {
                    issue.addCommit(commit.getSha());
                }
            }
        }
        catch (GitHubResourceNotFoundException e)
        {
            issue.setType(IssueType.INVALID);
            issue.addSkipReason(Skip.INVALID_ISSUE_REF);
        }
        catch (IOException | InterruptedException e)
        {
            e.printStackTrace();
        }
    }

    public void resolveIssueCommits()
    {
        List<ChangeIssue> relevantIssues = getRelevantKnownIssues();

        int issuesTotal = relevantIssues.size();
        int issuesLeft = issuesTotal;
        System.out.printf("Resolving commit branches and paths on %,d issues ...%n", issuesLeft);

        Predicate<String> branchesExclusionPredicate = getStringPredicate(branchExclusion);

        for (ChangeIssue issue : relevantIssues)
        {
            System.out.printf("\r%,d issues left (out of %,d) ...      ", issuesLeft--, issuesTotal);

            for (String commitSha : issue.getCommits())
            {
                String sha = Sha.toLowercase(commitSha);
                ChangeCommit commit = commitMap.get(sha);
                if ((commit != null) && (!commit.isSkipped()))
                {
                    Set<String> diffPaths = gitCache.getPaths(sha)
                        .stream()
                        .filter(Predicate.not(this::isExcludedPath))
                        .collect(Collectors.toSet());
                    commit.setFiles(diffPaths);
                    if (diffPaths.isEmpty())
                    {
                        commit.addSkipReason(Skip.NO_INTERESTING_PATHS_LEFT);
                    }

                    // Note: this lookup (all branches that commit exists in) is VERY time consuming.
                    Set<String> branchesWithCommit = gitCache.getBranchesContaining(sha);
                    commit.setBranches(branchesWithCommit);
                    if (branchesWithCommit.stream().anyMatch(branchesExclusionPredicate))
                    {
                        commit.addSkipReason(Skip.EXCLUDED_BRANCH);
                    }
                }
            }
        }
    }

    public void linkActivity()
    {
        LOG.debug("Connecting up {} issues to {} commits", issueMap.size(), commitMap.size());

        int connectedIssues = 0;
        int connectedPullRequests = 0;
        Set<String> uniqueCommits = new HashSet<>();

        // Back reference the issues into the commits
        for (ChangeIssue issue : issueMap.values())
        {
            // Does issue have relevance?
            boolean hasRelevance = false;
            for (String commitSha : issue.getCommits())
            {
                // Only pull in commits found via log, don't create new ones.
                // This is done to avoid referencing commits outside of the log range.
                String sha = commitSha.toLowerCase(Locale.US);
                ChangeCommit changeCommit = commitMap.get(sha);
                if (changeCommit != null)
                {
                    if (issue.getType() == IssueType.ISSUE)
                    {
                        connectedIssues++;
                        uniqueCommits.add(sha);
                        changeCommit.addIssueRef(issue.getNum());
                    }
                    else if (issue.getType() == IssueType.PULL_REQUEST)
                    {
                        connectedPullRequests++;
                        uniqueCommits.add(sha);
                        changeCommit.addPullRequestRef(issue.getNum());
                    }

                    if (!changeCommit.isSkipped())
                    {
                        hasRelevance = true;
                    }
                }
            }
            if (!hasRelevance)
            {
                issue.addSkipReason(Skip.NO_RELEVANT_COMMITS);
            }

            // special handling for pull requests
            if (issue.getType() == IssueType.PULL_REQUEST)
            {
                if (!this.branch.equals(issue.getBaseRef()))
                {
                    issue.addSkipReason(Skip.NOT_CORRECT_BASE_REF);
                }
            }
        }

        LOG.debug("Connected {} commits to {} issues and {} pull requests", uniqueCommits.size(), connectedIssues, connectedPullRequests);

        // Create Change list
        int changeId = 0;
        for (ChangeIssue issue : getRelevantKnownIssues())
        {
            if (issue.isSkipped() || issue.hasChangeRef())
                continue;

            Change change = new Change(changeId++);
            // add commits
            issue.getCommits().forEach((commitSha) -> updateChangeCommit(change, commitSha));
            // add issues & pull requests
            issue.getReferencedIssues().forEach((issueNum) -> updateChangeIssues(change, issueNum));
            this.changes.add(change);
        }

        this.changes.forEach((change) -> change.normalize(IssueType.ISSUE));
    }

    private void updateChangeCommit(Change change, String commitSha)
    {
        String sha = Sha.toLowercase(commitSha);
        ChangeCommit commit = this.commitMap.get(sha);
        if (commit != null)
        {
            if (commit.hasChangeRef())
                return; // ignore it, already referenced

            commit.setChangeRef(change);

            change.addCommit(commit);
            change.addAuthor(commit.getAuthor());

            if (commit.getIssueRefs() != null)
                commit.getIssueRefs().forEach((ref) -> updateChangeIssues(change, ref));
            if (commit.getPullRequestRefs() != null)
                commit.getPullRequestRefs().forEach((ref) -> updateChangeIssues(change, ref));
        }
    }

    private void updateChangeIssues(Change change, int issueNum)
    {
        ChangeIssue issue = this.issueMap.get(issueNum);
        if (issue != null)
        {
            if (issue.hasChangeRef())
                return; // ignore it, already referenced

            issue.setChangeRef(change);

            switch (issue.getType())
            {
                case ISSUE:
                    change.addIssue(issue);
                    break;
                case PULL_REQUEST:
                    change.addPullRequest(issue);
                    break;
                default:
                    break;
            }

            issue.getReferencedIssues().forEach((ref) -> updateChangeIssues(change, ref));
            issue.getCommits().forEach((sha) -> updateChangeCommit(change, sha));
        }
    }

    public List<ChangeIssue> getRelevantPullRequests()
    {
        return issueMap.values().stream()
            .filter((issue) -> !issue.isSkipped())
            .filter((issue) -> issue.getType() == IssueType.PULL_REQUEST)
            .filter((issue) -> branch.equals(issue.getBaseRef()))
            .sorted(Comparator.comparing(ChangeIssue::getNum).reversed())
            .collect(Collectors.toList());
    }

    public List<ChangeIssue> getRelevantKnownIssues()
    {
        return issueMap.values().stream()
            .filter((issue) -> !issue.isSkipped())
            .filter((issue) -> issue.getType() != IssueType.UNKNOWN)
            .filter((issue) -> branch.equals(issue.getBaseRef()))
            .sorted(Comparator.comparing(ChangeIssue::getNum).reversed())
            .collect(Collectors.toList());
    }

    public void writeMarkdown(Path markdownOutput) throws IOException
    {
        try (BufferedWriter writer = Files.newBufferedWriter(markdownOutput, UTF_8);
             PrintWriter out = new PrintWriter(writer))
        {
            List<Change> relevantChanges = changes.stream()
                .filter(Predicate.not(Change::isSkip))
                .sorted(Comparator.comparingInt(Change::getRefNumber).reversed())
                .collect(Collectors.toList());

            // Collect list of community member participation
            Set<String> community = new HashSet<>();
            for (Change change : relevantChanges)
            {
                for (Author author : change.getAuthors())
                {
                    if (!author.committer())
                    {
                        community.add(String.format("%s (%s)", author.toNiceName(), author.name()));
                    }
                }
            }

            if (!community.isEmpty())
            {
                out.println("# Special Thanks to the following Eclipse Jetty community members");
                out.println();
                community.forEach((author) -> out.printf("* %s%n", author));
                out.println();
            }

            out.println("# Changelog");
            out.println();

            // resolve titles, ids, etc ....
            for (Change change : relevantChanges)
            {
                out.printf("* #%d - ", change.getRefNumber());
                out.print(change.getRefTitle());
                Set<String> authors = change.getAuthors().stream().filter(Predicate.not(Author::committer)).map(Author::toNiceName).collect(Collectors.toSet());
                if (!authors.isEmpty())
                {
                    out.printf(" (%s)", String.join(", ", authors));
                }
                out.print("\n");
            }
        }
    }

    private boolean isMergeCommit(RevCommit commit)
    {
        return ((commit.getParents() != null) && (commit.getParents().length >= 2));
    }

    private boolean isExcludedPath(String path)
    {
        for (Predicate<String> exclusion : commitPathExclusionFilters)
        {
            if (exclusion.test(path))
                return true;
        }
        return false;
    }

    private GitHubApi getGitHubApi() throws IOException, InterruptedException
    {
        if (github == null)
        {
            github = GitHubApi.connect();
            LOG.info("GitHub API Rate Limits: {}", github.getRateLimits());
        }
        return github;
    }

    private Author getAuthor(Authors authors, RevCommit commit)
    {
        Author author = authors.find(commit.getAuthorIdent().getEmailAddress());

        if (author == null)
        {
            author = new Author(commit.getAuthorIdent().getName())
                .email(commit.getAuthorIdent().getEmailAddress())
                .committer(false);

            try
            {
                String commitId = commit.getId().getName();
                Commit ghCommit = getGitHubApi().commit(this.githubOwner, this.githubRepoName, commitId);
                if (ghCommit != null)
                {
                    ChangeCommit changeCommit = getCommit(commitId);
                    changeCommit.setBody(ghCommit.getCommit().getMessage());

                    if (ghCommit.getAuthor() != null)
                    {
                        String githubAuthorLogin = ghCommit.getAuthor().getLogin();
                        author.github(githubAuthorLogin);
                        changeCommit.setAuthor(author);
                    }
                    else
                    {
                        System.out.printf("Has no author: %s%n", commitId);
                    }
                }
                else
                {
                    System.out.printf("Not a valid commit id: %s%n", commitId);
                }
            }
            catch (InterruptedException | IOException e)
            {
                e.printStackTrace();
            }
            authors.add(author);
        }

        return author;
    }

    private RevCommit findCommitForTag(String tagName) throws IOException
    {
        try (RevWalk walk = new RevWalk(repository))
        {
            String refName = "refs/tags/" + tagName;
            LOG.debug("Finding commit ref {}", refName);
            Ref tagRef = repository.findRef(refName);
            if (tagRef == null)
            {
                throw new ChangelogException("Ref not found: " + tagName);
            }
            return walk.parseCommit(tagRef.getObjectId());
        }
    }
}
