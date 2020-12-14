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

package net.webtide.tools.changelog;

import java.util.Collection;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;

public class ChangeIssue extends ChangeRef
{
    private final int num;
    private String title;
    private String body;
    private String baseRef;
    private String state;
    private IssueType type = IssueType.UNKNOWN;
    private Set<Integer> referencedIssues = new HashSet<>();
    private Set<String> commits = new HashSet<>();
    private Set<String> labels = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);

    public ChangeIssue(int num)
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

    public String getState()
    {
        return state;
    }

    public void setState(String state)
    {
        this.state = state;
    }

    public String getTitle()
    {
        return title;
    }

    public void setTitle(String title)
    {
        this.title = title;
    }

    public IssueType getType()
    {
        return type;
    }

    public void setType(IssueType type)
    {
        this.type = type;
    }

    public boolean hasLabel(String label)
    {
        return labels.contains(label);
    }
}
