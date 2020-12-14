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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import org.apache.commons.lang3.StringUtils;

public class Author
{
    private String github;
    private String name;
    private List<String> emails = new ArrayList<>();
    private boolean committer = false;

    public Author()
    {
    }

    public Author(String name)
    {
        this.name = name;
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        Author author = (Author)o;
        return Objects.equals(github, author.github);
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(github);
    }

    public String github()
    {
        return github;
    }

    public Author github(String github)
    {
        this.github = github;
        return this;
    }

    public String name()
    {
        return name;
    }

    public Author name(String name)
    {
        this.name = name;
        return this;
    }

    public List<String> emails()
    {
        return emails;
    }

    public Author emails(List<String> emails)
    {
        this.emails = emails;
        return this;
    }

    public Author email(String email)
    {
        this.emails.add(email);
        return this;
    }

    public Author email(String... emails)
    {
        this.emails.addAll(Arrays.asList(emails));
        return this;
    }

    public boolean committer()
    {
        return committer;
    }

    public Author committer(boolean committer)
    {
        this.committer = committer;
        return this;
    }

    public String toNiceName()
    {
        if (StringUtils.isNotBlank(github))
            return "@" + github;
        if (StringUtils.isNotBlank(name))
            return name;
        String email = emails.get(0);
        int idx = email.indexOf('@');
        if (idx >= 0)
            return email.substring(0, idx);
        else
            return email;
    }
}
