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

package net.webtide.tools.github;

import java.io.IOException;

public class GitHubApiException extends IOException
{
    public GitHubApiException(String message)
    {
        super(message);
    }

    public GitHubApiException(String message, Throwable cause)
    {
        super(message, cause);
    }
}
