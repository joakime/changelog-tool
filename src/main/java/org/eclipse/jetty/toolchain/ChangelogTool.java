package org.eclipse.jetty.toolchain;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.jetty.toolchain.github.Commit;
import org.eclipse.jetty.toolchain.github.GitHubApi;
import org.eclipse.jetty.toolchain.github.IssueEvents;
import org.eclipse.jetty.toolchain.github.Label;
import org.eclipse.jetty.toolchain.github.PullRequestCommits;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.ListBranchCommand;
import org.eclipse.jgit.api.LogCommand;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ChangelogTool
{
    private static final Logger LOG = LoggerFactory.getLogger(ChangelogTool.class);

    private static final String TAG_JETTY94 = "jetty-9.4.35.v20201120";
    private static final String TAG_JETTY10 = "jetty-10.0.0";
    private static final String TAG_JETTY11 = "jetty-11.0.0";

    public static void main(String[] args) throws IOException, GitAPIException
    {
        Path localRepo = Paths.get("/home/joakim/code/jetty/jetty.project-alt");

        ChangelogTool changelog = new ChangelogTool(localRepo);
        changelog.setGithubRepo("eclipse", "jetty.project");
        changelog.onelineOutput = true;

        changelog.addLogFilter(Predicate.not(GitCommit::isMerge));
        changelog.addFilesFilter(Predicate.not((filename) -> filename.contains("/src/test/")));
        changelog.addBranchExclusion((branch) -> branch.endsWith("/jetty-9.4.x"));

        changelog.log(TAG_JETTY94, TAG_JETTY10);
        changelog.expandIssues();

        System.out.printf("Found %,d commit entries%n", changelog.commits.size());
        System.out.printf("Found %,d issue/pr references%n", changelog.issueMap.size());
        changelog.authors.save(Paths.get("target/authors-scan.json"));

        for (Issue issue : changelog.issueMap.values())
        {
            System.out.printf("Issue: %d [%s] - %s%n", issue.getNum(), issue.getType(), issue.getTitle());
            if (issue.isSkipped())
            {
                System.out.printf("  Skipped: true%n");
            }
            System.out.printf("  Labels: [%s]%n", String.join(", ", issue.getLabels()));
            System.out.printf("  Referenced: %s%n", issue.getReferencedIssues().stream().map(Objects::toString).collect(Collectors.joining(", ", "[", "]")));
            System.out.printf("  Commits: [%d] %s%n", issue.getCommits().size(), String.join(", ", issue.getCommits()));
        }

        Set<String> changedFiles = new HashSet<>();
        for (GitCommit commit : changelog.commits)
        {
            changedFiles.addAll(commit.getFiles());
        }
        changedFiles.stream().sorted().forEach(System.out::println);
        System.out.printf("Found %,d Files changed in the various commits%n", changedFiles.size());
    }

    private final Git git;
    private final Repository repository;
    private final Authors authors = Authors.load();
    private String githubOwner;
    private String githubRepoName;
    private GitHubApi github;
    private boolean onelineOutput = false;
    private boolean includeMergeCommits = false;
    private Map<Integer, Issue> issueMap = new HashMap<>();
    private List<GitCommit> commits = new ArrayList<>();
    private List<Predicate<GitCommit>> logFilters = new ArrayList<>();
    private List<Predicate<String>> branchExclusion = new ArrayList<>();
    private List<Predicate<String>> filesFilters = new ArrayList<>();

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

    /**
     * Only include Commits passing this predicate.
     */
    public void addLogFilter(Predicate<GitCommit> predicate)
    {
        this.logFilters.add(predicate);
    }

    /**
     * Filter filenames on commit with this predicate.
     * If the resulting file list is devoid of files as a result, it is not included in the commits.
     */
    public void addFilesFilter(Predicate<String> predicate)
    {
        this.filesFilters.add(predicate);
    }

    /**
     * If commit has specific branch, do not include it in the results.
     */
    public void addBranchExclusion(Predicate<String> predicate)
    {
        this.branchExclusion.add(predicate);
    }

    public List<GitCommit> log(String oldTag, String newTag) throws IOException, GitAPIException
    {
        RevCommit commitOld = findCommitForTag(oldTag);
        RevCommit commitNew = findCommitForTag(newTag);
        System.out.println("Ref (old): " + commitOld);
        System.out.println("Ref (new): " + commitNew);

        Predicate<GitCommit> predicateLog = getGitCommitPredicate(logFilters);

        LogCommand logCommand = git.log().addRange(commitOld, commitNew).setMaxCount(100);

        for (RevCommit commit : logCommand.call())
        {
            Author author = getAuthor(authors, commit);
            GitCommit gitCommit = new GitCommit();
            gitCommit.setSha(commit.getId().getName());
            gitCommit.setCommitTime(ZonedDateTime.ofInstant(Instant.ofEpochMilli(commit.getCommitTime()), ZoneId.systemDefault()));
            gitCommit.setAuthor(author);
            gitCommit.setShortMessage(commit.getShortMessage());
            gitCommit.setMerge(isMergeCommit(commit));

            collectIssueReferences(commit);

            if (predicateLog.test(gitCommit))
            {
                commits.add(gitCommit);
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

        int commitsLeft = commits.size();

        System.out.println("Resolving branches and file lists ...");

        // Now populate the time consume parts
        try (RevWalk walk = new RevWalk(repository))
        {
            Predicate<String> filesPredicate = getStringPredicate(filesFilters);
            Predicate<String> branchesExclusionPredicate = getStringPredicate(branchExclusion);

            for (GitCommit gitCommit : commits)
            {
                System.out.printf("\r%,d commits left ...      ", commitsLeft--);
                if (gitCommit.isMerge())
                    continue; // skip merge commits
                ObjectId commitId = ObjectId.fromString(gitCommit.getSha());
                RevCommit commit = walk.parseCommit(commitId);
                Set<String> listOfFiles = getListOfFiles(commit.getTree());
                Set<String> filteredFiles = listOfFiles.stream().filter(filesPredicate).collect(Collectors.toSet());
                if (filteredFiles.isEmpty())
                    continue; // skip this commit, no files left
                gitCommit.setFiles(listOfFiles);
                Set<String> branchesWithCommit = getBranchesWithCommit(commit.getId().getName());
                if (branchesWithCommit.stream().noneMatch(branchesExclusionPredicate))
                    gitCommit.setBranches(branchesWithCommit);
            }
        }

        System.out.println();

        return commits;
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

    private void collectIssueReferences(RevCommit commit)
    {
        Set<Integer> issueNums = IssueScanner.scan(commit.getFullMessage());
        for (int issueNum : issueNums)
        {
            Issue issue = issueMap.get(issueNum);
            if (issue == null)
            {
                issue = new Issue(issueNum);
                issueMap.put(issueNum, issue);
            }
            issue.addCommit(commit.getId().getName());
        }
    }

    private void expandIssues()
    {
        for (int issueNum : issueMap.keySet())
        {
            Issue issue = issueMap.get(issueNum);

            try
            {
                org.eclipse.jetty.toolchain.github.Issue ghIssue = getGitHubApi().issue(githubOwner, githubRepoName, issueNum);
                issue.addLabels(ghIssue.getLabels().stream().map(Label::getName).collect(Collectors.toSet()));

                if (ghIssue.getPullRequest() != null)
                {
                    org.eclipse.jetty.toolchain.github.PullRequest ghPullRequest = getGitHubApi().pullRequest(githubOwner, githubRepoName, issueNum);
                    issue.addLabels(ghPullRequest.getLabels().stream().map(Label::getName).collect(Collectors.toSet()));
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

                if (issue.hasLabel("test") || issue.hasLabel("documentation"))
                {
                    issue.setSkip(true);
                    continue; // skip
                }

                if (issue.getType() == Issue.Type.ISSUE)
                {
                    org.eclipse.jetty.toolchain.github.IssueEvents ghIssueEvents = getGitHubApi().issueEvents(githubOwner, githubRepoName, issueNum);
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
                    org.eclipse.jetty.toolchain.github.PullRequestCommits ghPullRequestCommits = getGitHubApi().pullRequestCommits(githubOwner, githubRepoName, issueNum);
                    for (PullRequestCommits.Commit commit : ghPullRequestCommits)
                    {
                        issue.addCommit(commit.getSha());
                    }
                }
            }
            catch (IOException | InterruptedException e)
            {
                e.printStackTrace();
            }
        }
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

    private Set<String> getListOfFiles(RevTree tree) throws IOException
    {
        Set<String> filelist = new HashSet<>();
        try (TreeWalk treeWalk = new TreeWalk(repository))
        {
            treeWalk.reset(tree);
            treeWalk.setRecursive(true);
            while (treeWalk.next())
            {
                String path = treeWalk.getPathString();
                filelist.add(path);
            }
        }
        return filelist;
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
                    if (ghCommit.getAuthor() != null)
                    {
                        String githubAuthorLogin = ghCommit.getAuthor().getLogin();
                        author.github(githubAuthorLogin);
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
