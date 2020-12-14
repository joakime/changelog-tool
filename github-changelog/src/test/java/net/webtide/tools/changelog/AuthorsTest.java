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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class AuthorsTest
{
    @Test
    public void testLoad() throws IOException
    {
        Authors authors = Authors.load();
        assertEquals(11, authors.size());
    }

    @ParameterizedTest
    @ValueSource(strings = {"joakim@erdfelt.com", "gregw@webtide.com"})
    public void testIsCommitter(String email) throws IOException
    {
        Authors authors = Authors.load();
        assertTrue(authors.isCommitter(email), "Email should be a committer: " + email);
    }

    @ParameterizedTest
    @ValueSource(strings = {"bogus@webtide.com", "nobody@github.com"})
    public void testIsNotCommitter(String email) throws IOException
    {
        Authors authors = Authors.load();
        assertFalse(authors.isCommitter(email), "Email should NOT be a committer: " + email);
    }
}
