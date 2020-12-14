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

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class IssueScanner
{
    public static Set<Integer> scan(String message)
    {
        // skip dependabot bodies
        if (message.contains("@dependabot"))
        {
            return Collections.emptySet();
        }

        Set<Integer> issueNums = new HashSet<>();
        // Start of line
        scanPattern(issueNums, message, "^#([0-9]{3,6})");
        // Not prefixed by ">" or alphanumeric character
        // we want to ignore things like "group/repo#1234" and "<a href='github.com/group/repo/issues/1234'>#1234</a>"
        // but allow things like "(#1234)"
        scanPattern(issueNums, message, "[^\\p{Alpha}\\p{Digit}>]+#([0-9]{3,6})");
        // Awkward issue reference (no hashsign)
        scanPattern(issueNums, message, "Issue ([0-9]{3,6})");
        return issueNums;
    }

    public static Set<Integer> scanResolutions(String message)
    {
        // skip dependabot bodies
        if (message.contains("@dependabot"))
        {
            return Collections.emptySet();
        }

        Set<Integer> issueNums = new HashSet<>();
        scanPattern(issueNums, message, "Close[sd] #?([0-9]{3,6})");
        scanPattern(issueNums, message, "Fixe[sd] #?([0-9]{3,6})");
        scanPattern(issueNums, message, "Fix #?([0-9]{3,6})");
        scanPattern(issueNums, message, "Resolve[sd] #?([0-9]{3,6})");
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
