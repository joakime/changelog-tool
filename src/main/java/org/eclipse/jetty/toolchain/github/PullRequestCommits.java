package org.eclipse.jetty.toolchain.github;

import java.util.ArrayList;
import java.util.List;

public class PullRequestCommits extends ArrayList<PullRequestCommits.Commit>
{
    public static class Details
    {
        protected Authorship author;
        protected Authorship committer;
        protected String message;

        public Authorship getAuthor()
        {
            return author;
        }

        public Authorship getCommitter()
        {
            return committer;
        }

        public String getMessage()
        {
            return message;
        }
    }

    public static class Commit
    {
        protected String sha;
        protected Details commit;
        protected List<Sha> parents = new ArrayList<>();

        public String getSha()
        {
            return sha;
        }

        public Details getCommit()
        {
            return commit;
        }

        public List<Sha> getParents()
        {
            return parents;
        }
    }
}
