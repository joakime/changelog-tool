package org.eclipse.jetty.toolchain;

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class IssueScanner
{
    public static Set<Integer> scan(String message)
    {
        Set<Integer> issueNums = new HashSet<>();
        scanPattern(issueNums, message, "#([0-9]{4,6})");
        scanPattern(issueNums, message, "Issue ([0-9]{4,6})");
        return issueNums;
    }

    private static void scanPattern(Set<Integer> issueNums, String message, String regex)
    {
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(message);
        int offset = 0;
        while (matcher.find(offset))
        {
            int issueNum = Integer.parseInt(matcher.group(1));
            issueNums.add(issueNum);
            offset = matcher.end();
        }
    }
}
