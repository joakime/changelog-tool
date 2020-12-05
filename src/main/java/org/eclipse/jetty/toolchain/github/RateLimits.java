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

import java.util.HashMap;
import java.util.Map;

public class RateLimits
{
    public static class Rate
    {
        protected int limit;
        protected int used;
        protected int remaining;
        protected long reset;

        public int getLimit()
        {
            return limit;
        }

        public int getUsed()
        {
            return used;
        }

        public int getRemaining()
        {
            return remaining;
        }

        public long getReset()
        {
            return reset;
        }

        @Override
        public String toString()
        {
            long resetInSecs = reset - (System.currentTimeMillis() / 1000);
            String human = (resetInSecs > 0) ? String.format("%,ds", resetInSecs) :
                String.format("%,ds ago", resetInSecs * -1);
            return String.format("Rate[u:%d/l:%d(r:%d),reset=%s]",
                used, limit, remaining, human);
        }
    }

    protected Map<String, Rate> resources = new HashMap<>();
    protected Rate rate;

    public Rate getResourceLimit(String key)
    {
        return resources.get(key);
    }

    public Rate getRate()
    {
        return rate;
    }

    @Override
    public String toString()
    {
        final StringBuilder sb = new StringBuilder("RateLimits[");
        sb.append("rate=").append(rate);
        sb.append(", [").append(resources).append("]");
        sb.append("]");
        return sb.toString();
    }
}
