package org.eclipse.jetty.toolchain;

import java.util.HashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Represents a change detected within the issues/pull-requests/commits
 */
public class Change
{
    private final int number;
    private final Set<Author> authors = new HashSet<>();
    private Set<RefTitle> commits;
    private Set<NumTitle> issues;
    private Set<NumTitle> pullRequests;
    private boolean skip = false;
    private int refNumber;
    private IssueType refType = IssueType.UNKNOWN;
    private Set<Integer> refsAssociated;
    private String refTitle;

    public Change(int number)
    {
        this.number = number;
    }

    private static String cleanupTitle(String rawTitle)
    {
        String title = rawTitle;

        title = title.replaceAll("Issue #?[0-9]{3,5}", "");
        title = title.replaceAll("Fixe[sd] #?[0-9]{3,5}", "");
        title = title.replaceAll("Fix #?[0-9]{3,5}", "");
        title = title.replaceAll("Resolve[sd] #?[0-9]{3,5}", "");
        title = title.replaceAll("^\\s*[.:-]*", "");
        title = title.replaceAll("^\\s*", "");

        return title;
    }

    public void addAuthor(Author author)
    {
        if (author.github() == null)
            return;
        this.authors.add(author);
    }

    public void addCommit(ChangeCommit commit)
    {
        if (this.commits == null)
            this.commits = new HashSet<>();

        this.commits.add(new RefTitle(commit.getSha(), commit.getTitle(), commit.getSkipSet()));
    }

    public void addIssue(ChangeIssue issue)
    {
        if (this.issues == null)
            this.issues = new HashSet<>();

        this.issues.add(new NumTitle(issue.getNum(), issue.getTitle(), issue.getSkipSet()));
    }

    public void addPullRequest(ChangeIssue pr)
    {
        if (this.pullRequests == null)
            this.pullRequests = new HashSet<>();
        this.pullRequests.add(new NumTitle(pr.getNum(), pr.getTitle(), pr.getSkipSet()));
    }

    public Set<Author> getAuthors()
    {
        return authors;
    }

    public Set<RefTitle> getCommits()
    {
        return commits;
    }

    public Set<NumTitle> getIssues()
    {
        return issues;
    }

    public int getNumber()
    {
        return number;
    }

    public Set<NumTitle> getPullRequests()
    {
        return pullRequests;
    }

    public int getRefNumber()
    {
        return refNumber;
    }

    public IssueType getRefType()
    {
        return refType;
    }

    public String getRefTitle()
    {
        return refTitle;
    }

    public Set<Integer> getRefsAssociated()
    {
        return refsAssociated;
    }

    public boolean isSkip()
    {
        return skip;
    }

    public boolean hasIssues()
    {
        return (issues != null) && (!issues.isEmpty());
    }

    public boolean hasPRs()
    {
        return (pullRequests != null) && (!pullRequests.isEmpty());
    }

    public void normalize(IssueType titlePriority)
    {
        if (hasIssues())
        {
            if (hasPRs())
            {
                // Mixed approach
                if (titlePriority == IssueType.PULL_REQUEST)
                {
                    // Mixed: PR First approach
                    if (!normalizeRefByPR(true))
                    {
                        if (!normalizeRefByIssue())
                        {
                            skip = true;
                        }
                    }
                }
                else if (titlePriority == IssueType.ISSUE)
                {
                    // Mixed: Issue First approach
                    if (!normalizeRefByIssue())
                    {
                        if (!normalizeRefByPR(true))
                        {
                            skip = true;
                        }
                    }
                }
            }
            else
            {
                // Issue Unique approach
                if (!normalizeRefByIssue())
                    skip = true;
            }
        }
        else
        {
            if (hasPRs())
            {
                // PR Unique approach
                if (!normalizeRefByPR(false))
                    skip = true;
            }
            else
            {
                // No issues or prs, ignore this one.
                skip = true;
            }
        }
    }

    private boolean normalizeRefByIssue()
    {
        Optional<NumTitle> ref = issues.stream()
            .filter(Predicate.not(NumTitle::isSkipped))
            .findFirst();
        if (ref.isPresent())
        {
            NumTitle issue = ref.get();
            this.refNumber = issue.getNumber();
            this.refType = IssueType.ISSUE;
            this.refsAssociated = null;
            this.refTitle = cleanupTitle(issue.getTitle());
            return true;
        }
        else
        {
            // Didn't find a relevant PR
            return false;
        }
    }

    private boolean normalizeRefByPR(boolean associateIssues)
    {
        Optional<NumTitle> ref = pullRequests.stream()
            .filter(Predicate.not(NumTitle::isSkipped))
            .findFirst();
        if (ref.isPresent())
        {
            NumTitle pr = ref.get();
            this.refNumber = pr.getNumber();
            this.refType = IssueType.PULL_REQUEST;
            if (associateIssues && (issues != null))
            {
                this.refsAssociated = issues.stream()
                    .filter(Predicate.not(NumTitle::isSkipped))
                    .map(NumTitle::getNumber)
                    .collect(Collectors.toSet());
            }
            else
            {
                this.refsAssociated = null;
            }
            this.refTitle = cleanupTitle(pr.getTitle());
            return true;
        }
        else
        {
            // Didn't find a relevant PR
            return false;
        }
    }

    public static class RefTitle
    {
        private final String id;
        private final String title;
        private final Set<Skip> skips;

        public RefTitle(String id, String title, Set<Skip> skips)
        {
            this.id = id;
            this.title = title;
            this.skips = skips;
        }

        @Override
        public boolean equals(Object o)
        {
            if (this == o)
                return true;
            if (o == null || getClass() != o.getClass())
                return false;
            RefTitle refTitle = (RefTitle)o;
            return Objects.equals(id, refTitle.id);
        }

        public String getId()
        {
            return id;
        }

        public Set<Skip> getSkips()
        {
            return skips;
        }

        public String getTitle()
        {
            return title;
        }

        @Override
        public int hashCode()
        {
            return Objects.hash(id);
        }

        public boolean isSkipped()
        {
            if (skips == null)
                return false;
            return !skips.isEmpty();
        }
    }

    public static class NumTitle
    {
        private final int number;
        private final String title;
        private final Set<Skip> skips;

        public NumTitle(int number, String title, Set<Skip> skips)
        {
            this.number = number;
            this.title = title;
            this.skips = skips;
        }

        @Override
        public boolean equals(Object o)
        {
            if (this == o)
                return true;
            if (o == null || getClass() != o.getClass())
                return false;
            NumTitle numTitle = (NumTitle)o;
            return number == numTitle.number;
        }

        public int getNumber()
        {
            return number;
        }

        public Set<Skip> getSkips()
        {
            return skips;
        }

        public String getTitle()
        {
            return title;
        }

        @Override
        public int hashCode()
        {
            return Objects.hash(number);
        }

        public boolean isSkipped()
        {
            if (skips == null)
                return false;
            return !skips.isEmpty();
        }
    }
}
