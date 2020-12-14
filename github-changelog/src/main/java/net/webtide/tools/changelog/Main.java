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

import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.commons.lang3.StringUtils;

public class Main
{
    public static void main(String[] args) throws Exception
    {
        Path localRepo = Paths.get("/home/joakim/code/jetty/jetty.project-alt");

        try (ChangelogTool changelog = new ChangelogTool(localRepo))
        {
            // the github repo
            changelog.setGithubRepo("eclipse", "jetty.project");
            // branch for this changelog
            changelog.setBranch("jetty-10.0.x");
            // the tag range
            changelog.setVersionRange("jetty-9.4.35.v20201120", "jetty-10.0.0");

            // labels (on issues and prs) that will flag them as ignored
            changelog.addLabelExclusion("test");
            changelog.addLabelExclusion("build");
            changelog.addLabelExclusion("duplicate");

            // skip the following commit paths
            changelog.addCommitPathExclusionFilter(StringUtils::isBlank);
            changelog.addCommitPathExclusionFilter((filename) -> filename.contains("/src/test/"));
            changelog.addCommitPathExclusionFilter((filename) -> filename.contains("/src/main/webapp/"));
            changelog.addCommitPathExclusionFilter((filename) -> filename.startsWith(".git"));
            changelog.addCommitPathExclusionFilter((filename) -> filename.startsWith("/dev/"));
            changelog.addCommitPathExclusionFilter((filename) -> filename.startsWith("Jenkins"));
            changelog.addCommitPathExclusionFilter((filename) -> filename.endsWith(".md"));
            changelog.addCommitPathExclusionFilter((filename) -> filename.endsWith(".txt"));
            changelog.addCommitPathExclusionFilter((filename) -> filename.endsWith(".properties"));
            changelog.addCommitPathExclusionFilter((filename) -> filename.endsWith(".jpg"));
            changelog.addCommitPathExclusionFilter((filename) -> filename.endsWith(".png"));

            // exclude the following commits based on "contains" branches
            changelog.addBranchExclusion((branch) -> branch.endsWith("/jetty-9.4.x"));

            // equivalent of git log <old>..<new>
            changelog.resolveCommits();

            // resolve all title/body fields (in commits, issues, and prs) for textual issues references (recursively)
            changelog.resolveIssues();

            // resolve all of the issue and pull requests commits
            changelog.resolveIssueCommits();

            // link up commits / issues / pull requests
            changelog.linkActivity();

            System.out.printf("Found %,d commit entries%n", changelog.getCommits().size());
            System.out.printf("Found %,d issue/pr references%n", changelog.getIssues().size());

            changelog.save(Paths.get("target"));
        }
    }
}
