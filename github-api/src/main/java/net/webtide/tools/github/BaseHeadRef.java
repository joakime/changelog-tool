package net.webtide.tools.github;

public class BaseHeadRef
{
    protected String label;
    protected String ref;
    protected String sha;
    protected User user;
    protected Repository repo;

    public String getLabel()
    {
        return label;
    }

    public String getRef()
    {
        return ref;
    }

    public String getSha()
    {
        return sha;
    }

    public User getUser()
    {
        return user;
    }

    public Repository getRepo()
    {
        return repo;
    }
}
