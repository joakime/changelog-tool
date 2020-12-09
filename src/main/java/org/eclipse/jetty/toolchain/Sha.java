package org.eclipse.jetty.toolchain;

import java.util.Locale;

public class Sha
{
    public static String toLowercase(String sha)
    {
        return sha.toLowerCase(Locale.US);
    }
}
