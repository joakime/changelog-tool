package org.eclipse.jetty.toolchain;

import java.util.Collection;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;

public class Issue
{
    public enum Type
    {
        UNKNOWN,
        ISSUE,
        PULL_REQUEST,
        INVALID;
    }

    private final int num;
    private String title;
    private String body;
    private String baseRef;
    private Type type = Type.UNKNOWN;
    private boolean skip = false;
    private Set<Integer> referencedIssues = new HashSet<>();
    private Set<String> commits = new HashSet<>();
    private Set<String> labels = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);

    public Issue(int num)
    {
        this.num = num;
    }

    public void addCommit(String commitId)
    {
        this.commits.add(commitId.toLowerCase(Locale.US));
    }

    public void addLabels(Collection<String> labels)
    {
        this.labels.addAll(labels);
    }

    public void addReferencedIssues(Collection<Integer> issueNums)
    {
        this.referencedIssues.addAll(issueNums);
    }

    public String getBaseRef()
    {
        return baseRef;
    }

    public void setBaseRef(String baseRef)
    {
        this.baseRef = baseRef;
    }

    public String getBody()
    {
        return body;
    }

    public void setBody(String body)
    {
        this.body = body;
    }

    public Set<String> getCommits()
    {
        return commits;
    }

    public Set<String> getLabels()
    {
        return labels;
    }

    public int getNum()
    {
        return num;
    }

    public Set<Integer> getReferencedIssues()
    {
        return referencedIssues;
    }

    public String getTitle()
    {
        return title;
    }

    public void setTitle(String title)
    {
        this.title = title;
    }

    public Type getType()
    {
        return type;
    }

    public void setType(Type type)
    {
        this.type = type;
    }

    public boolean hasLabel(String label)
    {
        return labels.contains(label);
    }

    public boolean isSkipped()
    {
        return this.skip;
    }

    public void setSkip(boolean flag)
    {
        this.skip = flag;
    }
}
