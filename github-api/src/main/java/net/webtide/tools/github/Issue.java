package net.webtide.tools.github;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

public class Issue
{
    protected int number;
    protected String title;
    protected String state;
    protected List<Label> labels = new ArrayList<>();
    protected User user;
    protected User closedBy;
    protected List<User> assignees = new ArrayList<>();
    protected String body;
    protected ZonedDateTime createdAt;
    protected ZonedDateTime updatedAt;
    protected ZonedDateTime closedAt;
    protected String authorAssociation;
    protected PullRequestRef pullRequest;

    public int getNumber()
    {
        return number;
    }

    public String getTitle()
    {
        return title;
    }

    public String getState()
    {
        return state;
    }

    public User getUser()
    {
        return user;
    }

    public List<Label> getLabels()
    {
        return labels;
    }

    public List<User> getAssignees()
    {
        return assignees;
    }

    public String getBody()
    {
        return body;
    }

    public ZonedDateTime getCreatedAt()
    {
        return createdAt;
    }

    public ZonedDateTime getUpdatedAt()
    {
        return updatedAt;
    }

    public ZonedDateTime getClosedAt()
    {
        return closedAt;
    }

    public String getAuthorAssociation()
    {
        return authorAssociation;
    }

    public User getClosedBy()
    {
        return closedBy;
    }

    public PullRequestRef getPullRequest()
    {
        return pullRequest;
    }
}
