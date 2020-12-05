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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Spliterator;
import java.util.function.Consumer;

public class ListReleasesSpliterator implements Spliterator<Release>
{
    private final GitHubApi github;
    private final String repoOwner;
    private final String repoName;
    private final int perPage;
    private final List<Release> activeReleases = new ArrayList<>();
    private int activeOffset = Integer.MAX_VALUE; // already past end at start, to trigger fetch of next releases page
    private int activePage = 1;

    public ListReleasesSpliterator(GitHubApi gitHubApi, String repoOwner, String repoName, int perPage)
    {
        this.github = gitHubApi;
        this.repoOwner = repoOwner;
        this.repoName = repoName;
        this.perPage = perPage;
    }

    @Override
    public boolean tryAdvance(Consumer<? super Release> action)
    {
        Release release = getNextRelease();
        if (release == null)
            return false;
        else
        {
            action.accept(release);
            return true;
        }
    }

    private Release getNextRelease()
    {
        if (activeOffset >= activeReleases.size())
        {
            try
            {
                activeReleases.clear();
                while (activeReleases.isEmpty())
                {
                    Releases releases = github.listReleases(repoOwner, repoName, perPage, activePage++);
                    if ((releases == null) || releases.isEmpty())
                        return null;
                    activeReleases.addAll(releases);
                    activeOffset = 0;
                }
            }
            catch (IOException e)
            {
                e.printStackTrace(System.err);
            }
            catch (InterruptedException e)
            {
                e.printStackTrace();
            }
        }

        if (activeReleases.isEmpty())
        {
            return null;
        }

        return activeReleases.get(activeOffset++);
    }

    @Override
    public Spliterator<Release> trySplit()
    {
        return null;
    }

    @Override
    public long estimateSize()
    {
        return Long.MAX_VALUE;
    }

    @Override
    public int characteristics()
    {
        return ORDERED;
    }
}
