package org.eclipse.jetty.toolchain;

public class ChangelogException extends RuntimeException
{
    public ChangelogException(String message)
    {
        super(message);
    }

    public ChangelogException(String message, Throwable cause)
    {
        super(message, cause);
    }
}
