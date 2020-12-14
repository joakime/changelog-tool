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

public enum Skip
{
    // Commit has 2 or more parents
    IS_MERGE_COMMIT,
    // Commit has no interesting diff paths left (after exclusions)
    NO_INTERESTING_PATHS_LEFT,
    // ISPR has label that is excluded
    EXCLUDED_LABEL,
    // Commit and ISPR has "#<num>" reference to an invalid issue
    INVALID_ISSUE_REF,
    // PR has base-ref that is not correct
    NOT_CORRECT_BASE_REF,
    // Commit has branch that is specifically excluded
    EXCLUDED_BRANCH,
    // ISPR has state that isn't "closed"
    NOT_CLOSED,
    // ISPR has no relevant commits (either no commits, or only commits that are skipped)
    NO_RELEVANT_COMMITS;
}
