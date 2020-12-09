package org.eclipse.jetty.toolchain;

import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;

public abstract class ChangeRef
{
    private Set<Skip> skipSet;
    private Set<Integer> changeRef;

    public void addChangeRef(Change change)
    {
        addChangeRef(change.getNumber());
    }

    public void addChangeRef(int changeNum)
    {
        if (changeRef == null)
        {
            changeRef = new HashSet<>();
        }
        this.changeRef.add(changeNum);
    }

    public Set<Integer> getChangeRefs()
    {
        return this.changeRef;
    }

    public boolean hasChangeRef(Change change)
    {
        if (this.changeRef == null)
            return false;
        return this.changeRef.contains(change.getNumber());
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
