package net.webtide.tools.github;

public class Verification
{
    protected boolean verified;
    protected String reason;
    protected String signature;

    public boolean isVerified()
    {
        return verified;
    }

    public String getReason()
    {
        return reason;
    }

    public String getSignature()
    {
        return signature;
    }
}
