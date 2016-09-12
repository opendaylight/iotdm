/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.iotdm.onem2m.core.database.lock;

import org.opendaylight.iotdm.onem2m.core.database.Onem2mDb;
import org.opendaylight.iotdm.onem2m.core.database.helper.WeakValueHashMap;
import org.opendaylight.iotdm.onem2m.core.database.transactionCore.ResourceTreeReader;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.onem2m.rev150105.onem2m.resource.tree.Onem2mResource;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.onem2m.rev150105.onem2m.resource.tree.Onem2mResourceKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.StampedLock;

/**
 * Created by gguliash on 5/17/16.
 * e-mail vinmesmiti@gmail.com; gguliash@cisco.com
 *
 *
 * ReadWriteLocker is smarter locker then DumbLocker, but has more overhead.
 * Current ReadWriteLocker uses StampedLock, each node in the tree 'has' it's own StampedLock object.
 * During the change ReadWriteLocker acquires write lock of the node and it's parent, and read lock of all the nodes on the path to the root.
 * ReadWriteLocker uses important RAM saving optimization. It holds StampedLock object only if it's in use.
 * This is achieved with WeakValueHashMap. So if value(StampedLock) is not in used, GC can remove the key-value from the map.
 */
public class ReadWriteLocker implements Locker {
    private static final Logger LOG = LoggerFactory.getLogger(ReadWriteLocker.class);
    private final int concurrency_level;
    private final List<Map<Onem2mResourceKey, StampedLock>> locksMap;
    private StampedLock stampedLock = new StampedLock();

    public ReadWriteLocker(int concurrency_level) {
        this.concurrency_level = concurrency_level;
        locksMap = new ArrayList<>(concurrency_level);
        for (int i = 0; i < concurrency_level; i++)
            locksMap.add(i, new WeakValueHashMap()); // TODO not well tested but I love it
    }

    /** {@inheritDoc}
     */
    @Override
    public Object lockEverything() {
        return stampedLock.writeLock();
    }

    /**
     * Acquired resourceId and it's parent's write lock, and read locks up to the root.
     * @param trc DB reader
     * @param resourceId id of the resource
     * @return object needed for unlock
     */
    @Override
    public Object lockUpdate(ResourceTreeReader trc, String resourceId) {
        long mainL = stampedLock.readLock();
        List<StampedLock> locks = getLockRootPath(trc, resourceId);

        List<Long> stamps = new ArrayList<>(locks.size());
        for (int i = 0; i < locks.size(); i++) stamps.add(0L);

        writeLockRootPath(locks, stamps, 0, Math.min(2, locks.size()));
        readLockRootPath(locks, stamps, 2, locks.size());

        return new LockContainer(stamps, locks, mainL);
    }

    /**
     * Acquired resourceId and it's parent's write lock, and read locks up to the root.
     * @param trc DB reader
     * @param resourceId id of the resource
     * @return object needed for unlock
     */
    @Override
    public Object lockCreate(ResourceTreeReader trc, String resourceId, String resourceName) {
        long mainL = stampedLock.readLock();

        List<StampedLock> locks = getLockRootPath(trc, resourceId);
        locks.add(0, getLock(new Onem2mResourceKey(resourceName)));
        List<Long> stamps = new ArrayList<>(locks.size());
        for (int i = 0; i < locks.size(); i++) stamps.add(0L);

        writeLockRootPath(locks, stamps, 0, Math.min(2, locks.size()));
        readLockRootPath(locks, stamps, 2, locks.size());

        return new LockContainer(stamps, locks, mainL);
    }

    /**
     * Acquired resourceId and it's parent's write lock, and read locks up to the root.
     * @param trc DB reader
     * @param resourceId id of the resource
     * @return object needed for unlock
     */
    @Override
    public Object lockDelete(ResourceTreeReader trc, String resourceId) {
        long mainL = stampedLock.readLock();

        List<StampedLock> locks = getLockRootPath(trc, resourceId);

        List<Long> stamps = new ArrayList<>(locks.size());
        for (int i = 0; i < locks.size(); i++) stamps.add(0L);

        writeLockRootPath(locks, stamps, 0, Math.min(2, locks.size()));
        readLockRootPath(locks, stamps, 2, locks.size());
        return new LockContainer(stamps, locks, mainL);
    }


    /** {@inheritDoc}
     */
    @Override
    public void unlockEverything(Object o) {
        if (o instanceof Long)
            stampedLock.unlockWrite((Long) o);
        else
            LOG.error("Wrong type");
    }
    /** {@inheritDoc}
     */
    @Override
    public void unlockUpdate(Object o) {
        if (o instanceof LockContainer) {
            LockContainer lc = (LockContainer) o;
            if (lc.getLocks() != null) {
                readUnlockRootPath(lc.getLocks(), lc.getStamps(), 2, lc.getLocks().size());
                writeUnlockRootPath(lc.getLocks(), lc.getStamps(), 0, Math.min(2, lc.getLocks().size()));
            }
            stampedLock.unlockRead(lc.getMainL());
        } else
            LOG.error("Wrong type");
    }
    /** {@inheritDoc}
     */
    @Override
    public void unlockCreate(Object o) {
        unlockUpdate(o);
    }
    /** {@inheritDoc}
     */
    @Override
    public void unlockDelete(Object o) {
        unlockUpdate(o);
    }

    private StampedLock getLock(Onem2mResourceKey key) {
        int index = Math.abs(key.hashCode() % concurrency_level);
        Map m = locksMap.get(index);
        StampedLock ret;
        synchronized (m) {
            ret = (StampedLock) m.get(key);
            if (ret == null) {
                ret = new StampedLock();
                m.put(key, ret);
            }
        }
        return ret;
    }

    private List<StampedLock> getLockRootPath(ResourceTreeReader trc, String resourceID) {
        List<StampedLock> ret = new ArrayList<>(1);
        Onem2mDb db = Onem2mDb.getInstance();
        Onem2mResource onem2mResource = db.getResource(trc, resourceID);
        while (onem2mResource != null) {
            StampedLock cur = getLock(new Onem2mResourceKey(onem2mResource.getResourceId()));
            ret.add(cur);

            String resourceId = onem2mResource.getParentId();
            if (resourceId.equals(db.NULL_RESOURCE_ID)) {
                break;
            }
            onem2mResource = trc.retrieveResourceById(resourceId);
        }
        return ret;
    }
    private void readLockRootPath(List<StampedLock> locksFromCurToRoot, List<Long> stamps, int x, int y) {
        for (int i = x; i < Math.min(y, locksFromCurToRoot.size()); i++)
            stamps.set(i, locksFromCurToRoot.get(i).readLock());
    }

    private void readUnlockRootPath(List<StampedLock> locksFromCurToRoot, List<Long> stamps, int x, int y) {
        for (int i = x; i < Math.min(y, locksFromCurToRoot.size()); i++)
            locksFromCurToRoot.get(i).unlockRead(stamps.get(i));
    }

    private void writeLockRootPath(List<StampedLock> locksFromCurToRoot, List<Long> stamps, int x, int y) {
        for (int i = x; i < Math.min(y, locksFromCurToRoot.size()); i++)
            stamps.set(i, locksFromCurToRoot.get(i).writeLock());
    }

    private void writeUnlockRootPath(List<StampedLock> locksFromCurToRoot, List<Long> stamps, int x, int y) {
        for (int i = x; i < Math.min(y, locksFromCurToRoot.size()); i++)
            locksFromCurToRoot.get(i).unlockWrite(stamps.get(i));
    }
    private class LockContainer {
        private List<Long> stamps;
        private List<StampedLock> locks;
        private long mainL;

        public LockContainer(List<Long> stamps, List<StampedLock> locks, long mainL) {
            this.stamps = stamps;
            this.locks = locks;
            this.mainL = mainL;
        }

        public long getMainL() {
            return mainL;
        }

        public List<Long> getStamps() {
            return stamps;
        }

        public List<StampedLock> getLocks() {
            return locks;
        }
    }
}
