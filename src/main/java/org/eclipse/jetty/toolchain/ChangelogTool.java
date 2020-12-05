package org.eclipse.jetty.toolchain;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
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
import java.util.stream.Stream;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.stream.JsonWriter;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.jetty.toolchain.github.Commit;
import org.eclipse.jetty.toolchain.github.GitHubApi;
import org.eclipse.jetty.toolchain.github.GitHubResourceNotFoundException;
import org.eclipse.jetty.toolchain.github.IssueEvents;
import org.eclipse.jetty.toolchain.github.Label;
import org.eclipse.jetty.toolchain.github.PullRequestCommits;
import org.eclipse.jetty.toolchain.gson.ISO8601TypeAdapter;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.ListBranchCommand;
import org.eclipse.jgit.api.LogCommand;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectReader;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.treewalk.AbstractTreeIterator;
import org.eclipse.jgit.treewalk.CanonicalTreeParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.nio.charset.StandardCharsets.UTF_8;

public class ChangelogTool
{
    private static final Logger LOG = LoggerFactory.getLogger(ChangelogTool.class);

    private static final String TAG_OLD_VER = "jetty-10.0.0";
    private static final String TAG_NEW_VER = "jetty-11.0.0";
    private static final String BRANCH_REF = "jetty-11.0.x";

    public static void main(String[] args) throws IOException, GitAPIException
    {
        Path localRepo = Paths.get("/home/joakim/code/jetty/jetty.project-alt");

        ChangelogTool changelog = new ChangelogTool(localRepo);
        changelog.setGithubRepo("eclipse", "jetty.project");
        changelog.onelineOutput = true;

        changelog.addLabelExclusion("test");
        changelog.addLabelExclusion("documentation");
        changelog.addLabelExclusion("build");

        changelog.addLogFilter(Predicate.not(GitCommit::isMerge));
        changelog.addCommitPathExclusionFilter((filename) -> StringUtils.isBlank(filename));
        changelog.addCommitPathExclusionFilter((filename) -> filename.contains("/src/test/"));
        changelog.addCommitPathExclusionFilter((filename) -> filename.contains("/src/main/webapp/"));
        changelog.addCommitPathExclusionFilter((filename) -> filename.startsWith(".git"));
        changelog.addCommitPathExclusionFilter((filename) -> filename.startsWith("/dev/"));
        changelog.addCommitPathExclusionFilter((filename) -> filename.startsWith("Jenkins"));
        changelog.addCommitPathExclusionFilter((filename) -> filename.endsWith(".md"));
        changelog.addCommitPathExclusionFilter((filename) -> filename.endsWith(".txt"));
        changelog.addCommitPathExclusionFilter((filename) -> filename.endsWith(".adoc"));
        changelog.addCommitPathExclusionFilter((filename) -> filename.endsWith(".properties"));
        changelog.addCommitPathExclusionFilter((filename) -> filename.endsWith(".jpg"));
        changelog.addCommitPathExclusionFilter((filename) -> filename.endsWith(".png"));

        changelog.addBranchExclusion((branch) ->
            branch.endsWith("/jetty-9.4.x") ||
                branch.endsWith("/jetty-10.0.x"));

        changelog.setBranch(BRANCH_REF);
        changelog.resolveCommits(TAG_OLD_VER, TAG_NEW_VER);
        changelog.resolveUnknownIssues();
        changelog.resolvePullRequestCommits();

        System.out.printf("Found %,d commit entries%n", changelog.commitMap.size());
        System.out.printf("Found %,d issue/pr references%n", changelog.issueMap.size());
        changelog.authors.save(Paths.get("target/authors-scan.json"));

        Gson gson = new GsonBuilder().setPrettyPrinting()
            .registerTypeAdapter(ZonedDateTime.class, new ISO8601TypeAdapter())
            .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
            .create();

        Path issueLog = Paths.get("target/issues.json");
        try (BufferedWriter writer = Files.newBufferedWriter(issueLog, UTF_8);
             JsonWriter jsonWriter = gson.newJsonWriter(writer))
        {
            gson.toJson(changelog.issueMap.values(), Set.class, jsonWriter);
        }

        Path commitsLog = Paths.get("target/commits.json");
        try (BufferedWriter writer = Files.newBufferedWriter(commitsLog, UTF_8);
             JsonWriter jsonWriter = gson.newJsonWriter(writer))
        {
            gson.toJson(changelog.commitMap.values(), Set.class, jsonWriter);
        }

        Path changedFilesLog = Paths.get("target/changed-files.log");
        try (BufferedWriter writer = Files.newBufferedWriter(changedFilesLog))
        {
            Set<String> changedFiles = new HashSet<>();
            for (GitCommit commit : changelog.commitMap.values())
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

        Path markdownOutput = Paths.get("target/changelog.md");
        changelog.writeMarkdown(markdownOutput);
    }

    private final Git git;
    private final Repository repository;
    private final Authors authors = Authors.load();
    private String githubOwner;
    private String githubRepoName;
    private GitHubApi github;
    private String branch;
    private boolean onelineOutput = false;
    private boolean includeMergeCommits = false;
    private Map<Integer, Issue> issueMap = new HashMap<>();
    private Map<String, GitCommit> commitMap = new HashMap<>();
    private List<Predicate<GitCommit>> logFilters = new ArrayList<>();
    private List<Predicate<String>> branchExclusion = new ArrayList<>();
    private List<Predicate<String>> commitPathExclusionFilters = new ArrayList<>();
    private Set<String> excludedLabels = new HashSet<>();

    public ChangelogTool(Path localGitRepo) throws IOException
    {
        git = Git.open(localGitRepo.toFile());
        repository = git.getRepository();
        System.out.println("Repository: " + repository);
    }

    public void setGithubRepo(String owner, String repoName)
    {
        this.githubOwner = owner;
        this.githubRepoName = repoName;
    }

    private void setBranch(String branch)
    {
        this.branch = branch;
    }

    /**
     * Only include Commits passing this predicate.
     */
    public void addLogFilter(Predicate<GitCommit> predicate)
    {
        this.logFilters.add(predicate);
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

    public void resolveCommits(String oldTag, String newTag) throws IOException, GitAPIException
    {
        RevCommit commitOld = findCommitForTag(oldTag);
        RevCommit commitNew = findCommitForTag(newTag);
        System.out.println("Ref (old): " + commitOld);
        System.out.println("Ref (new): " + commitNew);

        Predicate<GitCommit> predicateLog = getGitCommitPredicate(logFilters);

        LogCommand logCommand = git.log().addRange(commitOld, commitNew);

        for (RevCommit commit : logCommand.call())
        {
            Author author = getAuthor(authors, commit);

            GitCommit gitCommit = getCommit(commit.getId().getName());

            gitCommit.setCommitTime(ZonedDateTime.ofInstant(Instant.ofEpochMilli(commit.getCommitTime()), ZoneId.systemDefault()));
            gitCommit.setAuthor(author);
            gitCommit.setTitle(commit.getShortMessage());
            gitCommit.setBody(commit.getFullMessage());
            gitCommit.setMerge(isMergeCommit(commit));

            collectIssueReferences(gitCommit);

            if (predicateLog.test(gitCommit))
            {
                gitCommit.setSkipped(true);
            }

            if (onelineOutput)
            {
                System.out.printf("Commit: [%s] %s - %s%n", commit.getId().getName(), author.toNiceName(), commit.getShortMessage());
            }
            else
            {
                System.out.println("----------------");
                System.out.printf("Commit: %s (%s)%n", commit.getShortMessage(), commit.getId().getName());
                System.out.printf("Author: %s%n", commit.getAuthorIdent());
                System.out.printf("Committer: %s%n", commit.getCommitterIdent());
                System.out.printf("Parents: %s%n", Stream.of(commit.getParents()).map((rc) -> rc.getId().getName()).collect(Collectors.joining(", ")));
                System.out.printf("Branches: %s%n", String.join(", ", gitCommit.getBranches()));
                System.out.printf("%s%n", commit.getFullMessage());
            }
        }

        System.out.println();
    }

    private GitCommit getCommit(String sha)
    {
        String lowerSha = sha.toLowerCase(Locale.US);
        GitCommit commit = commitMap.get(lowerSha);
        if (commit == null)
        {
            commit = new GitCommit();
            commit.setSha(lowerSha);
            commitMap.put(lowerSha, commit);
        }
        return commit;
    }

    private static Predicate<GitCommit> getGitCommitPredicate(Collection<Predicate<GitCommit>> filters)
    {
        Predicate<GitCommit> predicate = gitCommit -> true;
        for (Predicate<GitCommit> logPredicate : filters)
        {
            predicate = predicate.and(logPredicate);
        }
        return predicate;
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

    private void collectIssueReferences(GitCommit commit)
    {
        Set<Integer> issueNums = new HashSet<>();
        issueNums.addAll(IssueScanner.scan(commit.getTitle()));
        issueNums.addAll(IssueScanner.scan(commit.getBody()));
        for (int issueNum : issueNums)
        {
            Issue issue = issueMap.get(issueNum);
            if (issue == null)
            {
                issue = new Issue(issueNum);
                issueMap.put(issueNum, issue);
            }
            issue.addCommit(commit.getSha());
        }
    }

    public void resolveUnknownIssues()
    {
        boolean done = false;

        while (!done)
        {
            List<Issue> unknownIssues = issueMap.values().stream()
                .filter((issue) -> issue.getType() == Issue.Type.UNKNOWN)
                .collect(Collectors.toList());

            if (unknownIssues.isEmpty())
                done = true;
            else
            {
                int issuesLeft = unknownIssues.size();
                for (Issue unknownIssue : unknownIssues)
                {
                    LOG.info("Need to resolve {} more issues ...", issuesLeft--);
                    resolveUnknownIssue(unknownIssue);
                }
            }
        }

        // Back reference the issues into the commits
        for (Issue issue : issueMap.values())
        {
            for (String commitSha : issue.getCommits())
            {
                // Only pull in commits found via log, don't create new ones.
                // This is done to avoid referencing commits outside of the log range.
                GitCommit gitCommit = commitMap.get(commitSha.toLowerCase(Locale.US));
                if (gitCommit != null)
                {
                    if (issue.getType() == Issue.Type.ISSUE)
                        gitCommit.addIssueRef(issue.getNum());
                    else if (issue.getType() == Issue.Type.PULL_REQUEST)
                        gitCommit.addPullRequestRef(issue.getNum());
                }
            }
        }
    }

    private void resolveUnknownIssue(Issue issue)
    {
        try
        {
            org.eclipse.jetty.toolchain.github.Issue ghIssue = getGitHubApi().issue(githubOwner, githubRepoName, issue.getNum());
            issue.addLabels(ghIssue.getLabels().stream().map(Label::getName).collect(Collectors.toSet()));

            if (ghIssue.getPullRequest() != null)
            {
                org.eclipse.jetty.toolchain.github.PullRequest ghPullRequest = getGitHubApi().pullRequest(githubOwner, githubRepoName, issue.getNum());
                issue.addLabels(ghPullRequest.getLabels().stream().map(Label::getName).collect(Collectors.toSet()));
                issue.setBaseRef(ghPullRequest.getBase().getRef());
                issue.setTitle(ghPullRequest.getTitle());
                issue.setBody(ghPullRequest.getBody());
                issue.setType(Issue.Type.PULL_REQUEST);
            }
            else
            {
                issue.setTitle(ghIssue.getTitle());
                issue.setBody(ghIssue.getBody());
                issue.setType(Issue.Type.ISSUE);
            }

            Set<Integer> issueRefs = new HashSet<>();
            issueRefs.addAll(IssueScanner.scan(issue.getTitle()));
            issueRefs.addAll(IssueScanner.scan(issue.getBody()));
            issueRefs.remove(issue.getNum());
            issue.addReferencedIssues(issueRefs);

            // Discover any newly referenced issue for later resolve
            for (int issueNum : issueRefs)
            {
                Issue ref = issueMap.get(issueNum);
                if (ref == null)
                {
                    ref = new Issue(issueNum);
                    issueMap.put(issueNum, ref);
                }
            }

            // Test labels
            for (String excludedLabel : excludedLabels)
            {
                if (issue.hasLabel(excludedLabel))
                {
                    issue.setSkip(true);
                    return;
                }
            }

            if (issue.getType() == Issue.Type.ISSUE)
            {
                org.eclipse.jetty.toolchain.github.IssueEvents ghIssueEvents = getGitHubApi().issueEvents(githubOwner, githubRepoName, issue.getNum());
                for (IssueEvents.IssueEvent event : ghIssueEvents)
                {
                    if (StringUtils.isNotBlank(event.getCommitId()))
                    {
                        issue.addCommit(event.getCommitId());
                    }
                }
            }
            else if (issue.getType() == Issue.Type.PULL_REQUEST)
            {
                org.eclipse.jetty.toolchain.github.PullRequestCommits ghPullRequestCommits = getGitHubApi().pullRequestCommits(githubOwner, githubRepoName, issue.getNum());
                for (PullRequestCommits.Commit commit : ghPullRequestCommits)
                {
                    issue.addCommit(commit.getSha());
                }
            }
        }
        catch (GitHubResourceNotFoundException e)
        {
            issue.setType(Issue.Type.INVALID);
            issue.setSkip(true);
        }
        catch (IOException | InterruptedException e)
        {
            e.printStackTrace();
        }
    }

    public void resolvePullRequestCommits() throws IOException, GitAPIException
    {
        List<Issue> sortedPullRequests = getRelevantPullRequests();

        int prsLeft = sortedPullRequests.size();
        System.out.printf("Resolving commit branches and paths on %,d pull requests ...%n", prsLeft);

        Predicate<String> branchesExclusionPredicate = getStringPredicate(branchExclusion);

        // Now populate the time consume parts
        try (RevWalk walk = new RevWalk(repository))
        {
            for (Issue issue : sortedPullRequests)
            {
                System.out.printf("\r%,d pull-requests left ...      ", prsLeft--);

                for (String commitSha : issue.getCommits())
                {
                    GitCommit commit = commitMap.get(commitSha.toLowerCase(Locale.US));
                    if ((commit != null) && (!commit.isSkipped()))
                    {
                        ObjectId commitId = ObjectId.fromString(commitSha);
                        RevCommit revCommit = walk.parseCommit(commitId);
                        Set<String> listOfFiles = collectPathsInCommit(revCommit);
                        commit.setFiles(listOfFiles);
                        if (listOfFiles.isEmpty())
                        {
                            commit.setSkipped(true);
                            continue; // skip this commit, no files left
                        }
                        Set<String> branchesWithCommit = getBranchesWithCommit(commitSha);
                        commit.setBranches(branchesWithCommit);
                        if (branchesWithCommit.stream().anyMatch(branchesExclusionPredicate))
                            commit.setSkipped(true);
                    }
                }
            }
        }
    }

    public List<Issue> getRelevantPullRequests()
    {
        return issueMap.values().stream()
            .filter((issue) -> !issue.isSkipped())
            .filter((issue) -> issue.getType() == Issue.Type.PULL_REQUEST)
            .filter((issue) -> branch.equals(issue.getBaseRef()))
            .sorted(Comparator.comparing(Issue::getNum).reversed())
            .collect(Collectors.toList());
    }

    public void writeMarkdown(Path markdownOutput) throws IOException
    {
        try (BufferedWriter writer = Files.newBufferedWriter(markdownOutput, UTF_8);
             PrintWriter out = new PrintWriter(writer))
        {
            List<Issue> sortedPullRequests = getRelevantPullRequests();

            // Collect list of community member participation
            Set<String> community = new HashSet<>();
            for (Issue issue : sortedPullRequests)
            {
                community.addAll(collectCommunityCommitAuthors(issue.getCommits()));
            }

            out.println("# Changelog");
            out.println();

            if (!community.isEmpty())
            {
                out.printf("**Special thanks to the following Eclipse Jetty community members for participating in this release: %s**%n",
                    String.join(", ", community));
                out.println();
            }

            for (Issue issue : sortedPullRequests)
            {
                String title = issue.getTitle();
                title = title.replaceAll("Issue #?[0-9]{3,5}", "");
                title = title.replaceAll("Fixe[sd] #?[0-9]{3,5}", "");
                title = title.replaceAll("Fix #?[0-9]{3,5}", "");
                title = title.replaceAll("Resolve[sd] #?[0-9]{3,5}", "");
                title = title.replaceAll("^\\s*[.:-]*", "");
                title = title.replaceAll("^\\s*", "");
                out.printf("+ #%d - %s", issue.getNum(), title);
                Set<String> authors = collectCommunityCommitAuthors(issue.getCommits());
                if (!authors.isEmpty())
                {
                    out.printf(" (%s)", String.join(", ", authors));
                }
                out.print("\n");
            }
        }
    }

    private Set<String> collectCommunityCommitAuthors(Set<String> commits)
    {
        Set<String> authors = new HashSet<>();

        for (String sha : commits)
        {
            GitCommit commit = commitMap.get(sha.toLowerCase(Locale.US));
            if (commit == null)
                continue; // skip
            if (commit.getAuthor() == null)
                continue; // skip
            if (!commit.getAuthor().committer())
            {
                authors.add(commit.getAuthor().toNiceName());
            }
        }

        return authors;
    }

    private boolean isMergeCommit(RevCommit commit)
    {
        return ((commit.getParents() != null) && (commit.getParents().length >= 2));
    }

    private Set<String> getBranchesWithCommit(String commitId) throws GitAPIException
    {
        return git.branchList()
            .setListMode(ListBranchCommand.ListMode.ALL)
            .setContains(commitId)
            .call()
            .stream()
            .map(Ref::getName)
            .filter((name) -> name.startsWith("refs/remotes/origin/jetty-"))
            .filter((name) -> name.endsWith(".x"))
            .collect(Collectors.toSet());
    }

    private Set<String> collectPathsInCommit(RevCommit commit) throws IOException, GitAPIException
    {
        final String sha = commit.getId().getName();
        final List<DiffEntry> diffs = git.diff()
            .setOldTree(prepareTreeParser(sha + "^"))
            .setNewTree(prepareTreeParser(sha))
            .call();

        final Set<String> paths = new HashSet<>();

        for (DiffEntry diff : diffs)
        {
            if (!isExcludedPath(diff.getOldPath()))
                paths.add(diff.getOldPath());
            if (!isExcludedPath(diff.getNewPath()))
                paths.add(diff.getNewPath());
        }

        return paths;
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

    private AbstractTreeIterator prepareTreeParser(String objectId) throws IOException
    {
        try (RevWalk walk = new RevWalk(repository))
        {
            RevCommit commit = walk.parseCommit(repository.resolve(objectId));
            RevTree tree = walk.parseTree(commit.getTree().getId());

            CanonicalTreeParser treeParser = new CanonicalTreeParser();
            try (ObjectReader reader = repository.newObjectReader())
            {
                treeParser.reset(reader, tree.getId());
            }

            walk.dispose();
            return treeParser;
        }
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
                    GitCommit gitCommit = getCommit(commitId);
                    gitCommit.setBody(ghCommit.getCommit().getMessage());

                    if (ghCommit.getAuthor() != null)
                    {
                        String githubAuthorLogin = ghCommit.getAuthor().getLogin();
                        author.github(githubAuthorLogin);
                        gitCommit.setAuthor(author);
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
            Ref tagRef = repository.findRef("refs/tags/" + tagName);
            return walk.parseCommit(tagRef.getObjectId());
        }
    }
}
