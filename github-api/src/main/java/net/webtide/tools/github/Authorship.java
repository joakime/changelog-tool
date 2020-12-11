package net.webtide.tools.github;

import java.time.ZonedDateTime;

public class Authorship
{
    protected String name;
    protected String email;
    protected ZonedDateTime timestamp;

    public String getName()
    {
        return name;
    }

    public String getEmail()
    {
        return email;
    }

    public ZonedDateTime getTimestamp()
    {
        return timestamp;
    }
}
