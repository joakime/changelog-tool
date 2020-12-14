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

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import com.google.gson.Gson;

import static java.nio.charset.StandardCharsets.UTF_8;

public class Authors extends ArrayList<Author>
{
    public static Authors load() throws IOException
    {
        URL url = Authors.class.getClassLoader().getResource("authors.json");
        try (InputStream in = url.openStream();
             InputStreamReader reader = new InputStreamReader(in, UTF_8))
        {
            Gson gson = new Gson();
            Authors authors = gson.fromJson(reader, Authors.class);
            return authors;
        }
    }

    private Map<String, Integer> emailMap = new HashMap<>();

    public Authors()
    {
    }

    @Override
    public boolean add(Author author)
    {
        boolean ret = super.add(author);
        updateEmailMap();
        return ret;
    }

    public Author find(String email)
    {
        Integer idx = emailMap.get(email);
        if (idx == null)
            return null;
        return get(idx);
    }

    private void updateEmailMap()
    {
        emailMap.clear();
        int size = this.size();
        for (int i = 0; i < size; i++)
        {
            Author author = this.get(i);
            for (String email : author.emails())
            {
                emailMap.put(email, i);
            }
        }
    }

    public boolean isCommitter(String email)
    {
        Integer idx = emailMap.get(email);
        if (idx == null)
            return false;
        Author author = get(idx);
        if (author == null)
            return false;
        return author.committer();
    }
}
