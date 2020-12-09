package org.eclipse.jetty.toolchain;

public enum Skip
{
    IS_MERGE_COMMIT,
    NO_INTERESTING_PATHS_LEFT,
    EXCLUDED_LABEL,
    INVALID_ISSUE_REF,
    EXCLUDED_BRANCH,
    NOT_CLOSED;
}
