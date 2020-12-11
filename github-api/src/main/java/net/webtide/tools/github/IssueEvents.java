package net.webtide.tools.github;

import java.time.ZonedDateTime;
import java.util.ArrayList;

public class IssueEvents extends ArrayList<IssueEvents.IssueEvent>
{
    public static class IssueEvent
    {
        protected String event;
        protected User actor;
        protected User assignee;
        protected String commitId;
        protected ZonedDateTime createdAt;

        public String getEvent()
        {
            return event;
        }

        public User getActor()
        {
            return actor;
        }

        public User getAssignee()
        {
            return assignee;
        }

        public String getCommitId()
        {
            return commitId;
        }

        public ZonedDateTime getCreatedAt()
        {
            return createdAt;
        }
    }
}
