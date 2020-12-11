package net.webtide.tools.changelog;

import java.util.EnumSet;
import java.util.Set;

public abstract class ChangeRef
{
    private Set<Skip> skipSet;
    private int changeRef = -1;

    public void setChangeRef(Change change)
    {
        assert(this.changeRef != -1);
        this.changeRef = change.getNumber();
    }

    public int getChangeRef()
    {
        return this.changeRef;
    }

    public boolean hasChangeRef()
    {
        return this.changeRef >= 0;
    }

    public boolean isSkipped()
    {
        return ((this.skipSet != null) && (!this.skipSet.isEmpty()));
    }

    public Set<Skip> getSkipSet()
    {
        return skipSet;
    }

    public void addSkipReason(Skip skip)
    {
        if (skipSet == null)
        {
            skipSet = EnumSet.of(skip);
        }
        else
        {
            skipSet.add(skip);
        }
    }
}
