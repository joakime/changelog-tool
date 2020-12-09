package org.eclipse.jetty.toolchain;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ChangeCommit extends ChangeRef
{
    private String sha;
    private Author author;
    private String title;
    private String body;
    private ZonedDateTime commitTime;
    private List<String> files;
    private List<String> branches;
    private Set<Integer> issueRefs;
    private Set<Integer> pullRequestRefs;

    public Author getAuthor()
    {
        return author;
    }

    public void setAuthor(Author author)
    {
        this.author = author;
    }

    public String getBody()
    {
        return body;
    }

    public void setBody(String body)
    {
        this.body = body;
    }

    public List<String> getBranches()
    {
        return branches;
    }

    public void setBranches(Collection<String> branches)
    {
        if (branches == null)
            this.branches = null;
        else
        {
            this.branches = new ArrayList<>();
            this.branches.addAll(branches);
        }
    }

    public ZonedDateTime getCommitTime()
    {
        return commitTime;
    }

    public void setCommitTime(ZonedDateTime commitTime)
    {
        this.commitTime = commitTime;
    }

    public List<String> getFiles()
    {
        return files;
    }

    public void addIssueRef(int ref)
    {
        if (this.issueRefs == null)
            this.issueRefs = new HashSet<>();
        this.issueRefs.add(ref);
    }

    public void addIssueRefs(Collection<Integer> refs)
    {
        if (this.issueRefs == null)
            this.issueRefs = new HashSet<>();
        this.issueRefs.addAll(refs);
    }

    public void addPullRequestRefs(Collection<Integer> refs)
    {
        if (this.pullRequestRefs == null)
            this.pullRequestRefs = new HashSet<>();
        this.pullRequestRefs.addAll(refs);
    }

    public void addPullRequestRef(int ref)
    {
        if (this.pullRequestRefs == null)
            this.pullRequestRefs = new HashSet<>();
        this.pullRequestRefs.add(ref);
    }

    public Set<Integer> getIssueRefs()
    {
        return issueRefs;
    }

    public Set<Integer> getPullRequestRefs()
    {
        return pullRequestRefs;
    }

    public void setFiles(Collection<String> files)
    {
        if (files == null)
            this.files = null;
        else
        {
            this.files = new ArrayList<>();
            this.files.addAll(files);
        }
    }

    public String getSha()
    {
        return sha;
    }

    public void setSha(String sha)
    {
        this.sha = sha;
    }

    public String getTitle()
    {
        return title;
    }

    public void setTitle(String title)
    {
        this.title = title;
    }
}
