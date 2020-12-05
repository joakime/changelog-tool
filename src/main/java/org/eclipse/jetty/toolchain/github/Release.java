//
// ========================================================================
// Copyright (c) Webtide LLC and others.
//
// This program and the accompanying materials are made available under
// the terms of the Eclipse Public License 2.0 which is available at
// https://www.eclipse.org/legal/epl-2.0
//
// This Source Code may also be made available under the following
// Secondary Licenses when the conditions for such availability set
// forth in the Eclipse Public License, v. 2.0 are satisfied:
// the Apache License v2.0 which is available at
// https://www.apache.org/licenses/LICENSE-2.0
//
// SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
// ========================================================================
//

package org.eclipse.jetty.toolchain.github;

import java.time.ZonedDateTime;

public class Release
{
    protected int id;
    protected String tagName;
    protected String targetCommitish;
    protected String name;
    protected String body;
    protected boolean draft;
    protected boolean prerelease;
    protected User author;
    protected ZonedDateTime createdAt;
    protected ZonedDateTime publishedAt;

    public int getId()
    {
        return id;
    }

    public String getTagName()
    {
        return tagName;
    }

    public String getTargetCommitish()
    {
        return targetCommitish;
    }

    public String getName()
    {
        return name;
    }

    public String getBody()
    {
        return body;
    }

    public boolean isDraft()
    {
        return draft;
    }

    public boolean isPrerelease()
    {
        return prerelease;
    }

    public User getAuthor()
    {
        return author;
    }

    public ZonedDateTime getCreatedAt()
    {
        return createdAt;
    }

    public ZonedDateTime getPublishedAt()
    {
        return publishedAt;
    }
}
