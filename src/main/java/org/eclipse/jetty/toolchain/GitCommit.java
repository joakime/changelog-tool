package org.eclipse.jetty.toolchain;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class GitCommit
{
    protected ZonedDateTime commitTime;
    private String sha;
    private Author author;
    private String shortMessage;
    private boolean isMerge;
    private List<String> files = new ArrayList<>();
    private List<String> branches = new ArrayList<>();

    public Author getAuthor()
    {
        return author;
    }

    public void setAuthor(Author author)
    {
        this.author = author;
    }

    public List<String> getBranches()
    {
        return branches;
    }

    public void setBranches(Collection<String> branches)
    {
        this.branches.clear();
        this.branches.addAll(branches);
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

    public void setFiles(Collection<String> files)
    {
        this.files.clear();
        this.files.addAll(files);
    }

    public String getSha()
    {
        return sha;
    }

    public void setSha(String sha)
    {
        this.sha = sha;
    }

    public String getShortMessage()
    {
        return shortMessage;
    }

    public void setShortMessage(String shortMessage)
    {
        this.shortMessage = shortMessage;
    }

    public boolean isMerge()
    {
        return isMerge;
    }

    public void setMerge(boolean merge)
    {
        isMerge = merge;
    }
}
