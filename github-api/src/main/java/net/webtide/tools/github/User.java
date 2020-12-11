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

package net.webtide.tools.github;

import java.time.ZonedDateTime;

public class User
{
    protected String login;
    protected String type;
    protected String name;
    protected String company;
    protected String location;
    protected String email;
    protected String bio;
    protected ZonedDateTime createdAt;
    protected ZonedDateTime updatedAt;

    public String getLogin()
    {
        return login;
    }

    public String getType()
    {
        return type;
    }

    public String getName()
    {
        return name;
    }

    public String getCompany()
    {
        return company;
    }

    public String getLocation()
    {
        return location;
    }

    public String getEmail()
    {
        return email;
    }

    public String getBio()
    {
        return bio;
    }

    public ZonedDateTime getCreatedAt()
    {
        return createdAt;
    }

    public ZonedDateTime getUpdatedAt()
    {
        return updatedAt;
    }
}
