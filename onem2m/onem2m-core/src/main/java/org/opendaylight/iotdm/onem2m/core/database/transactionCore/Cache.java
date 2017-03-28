/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.iotdm.onem2m.core.database.transactionCore;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.cache.Weigher;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import org.opendaylight.iotdm.onem2m.core.Onem2m;
import org.opendaylight.iotdm.onem2m.core.database.dao.DaoResourceTreeReader;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.onem2m.rev150105.onem2m.cse.list.Onem2mCse;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.onem2m.rev150105.onem2m.cse.list.Onem2mCseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.onem2m.rev150105.onem2m.cse.list.Onem2mCseKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.onem2m.rev150105.onem2m.resource.tree.Onem2mParentChildListKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.onem2m.rev150105.onem2m.resource.tree.Onem2mResource;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.onem2m.rev150105.onem2m.resource.tree.Onem2mResourceKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.onem2m.rev150105.onem2m.resource.tree.onem2m.parent.child.list.Onem2mParentChild;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.onem2m.rev150105.onem2m.resource.tree.onem2m.parent.child.list.Onem2mParentChildBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ExecutionException;

/**
 * Created by gguliash on 4/28/16.
 * e-mail vinmesmiti@gmail.com; gguliash@cisco.com
 */
public class Cache implements WriteOnlyCache, ReadOnlyCache {
    private static final Logger LOG = LoggerFactory.getLogger(Cache.class);

    private static final int CSE_MAP_BYTE_LIMIT = 1000;
    private static final int RESOURCE_MAP_SIZE_LIMIT = 400000;
    private static final int RESOURCE_CHILDREN_MAP_SIZE_LIMIT = RESOURCE_MAP_SIZE_LIMIT;
    private static final int AE_MAP_BYTE_LIMIT = 10000;

    private final DaoResourceTreeReader daoResourceTreeReader;

    private final LoadingCache<Onem2mResourceKey, Onem2mResourceElem> onem2mResourceCache =
            CacheBuilder.<Onem2mResourceKey, Onem2mResourceElem>newBuilder()
                    .maximumWeight(RESOURCE_MAP_SIZE_LIMIT).weigher(new Weigher<Onem2mResourceKey, Onem2mResourceElem>() {
                @Override
                public int weigh(Onem2mResourceKey onem2mResourceKey, Onem2mResourceElem el) {
                    return 1;
                }
            })
                    .concurrencyLevel(200)
                    .initialCapacity(400000)

                    .build(new CacheLoader<Onem2mResourceKey, Onem2mResourceElem>() {
                        @Override
                        public Onem2mResourceElem load(Onem2mResourceKey onem2mResourceKey) throws Exception {
                            return daoResourceTreeReader.retrieveResourceById(onem2mResourceKey);
                        }
                    });

    private final LoadingCache<Onem2mCseKey, Onem2mCse> onem2mCseCache =
            CacheBuilder.<Onem2mCseKey, Onem2mCse>newBuilder()
                    .maximumWeight(CSE_MAP_BYTE_LIMIT).weigher(new Weigher<Onem2mCseKey, Onem2mCse>() {
                @Override
                public int weigh(Onem2mCseKey onem2mCseKey, Onem2mCse onem2mCse) {
                    return 1;
                }
            }).concurrencyLevel(200)
                    .initialCapacity(16)
                    .build(new CacheLoader<Onem2mCseKey, Onem2mCse>() {
                        @Override
                        public Onem2mCse load(Onem2mCseKey onem2mCseKey) throws Exception {
                            return daoResourceTreeReader.retrieveCseByName(onem2mCseKey);
                        }
                    });

    private final ConcurrentHashMap<String, LoadingCache> onem2mAeRegCacheMap = new ConcurrentHashMap<>();


    private final LoadingCache<Onem2mResourceKey, Map<String, Onem2mParentChild>> onem2mResourceChildrenCache =
        CacheBuilder.<Onem2mResourceKey, Onem2mResourceElem>newBuilder()
            .maximumWeight(RESOURCE_CHILDREN_MAP_SIZE_LIMIT).weigher(new Weigher<Onem2mResourceKey,
                                                                     Map<String, Onem2mParentChild>>() {
            @Override
            public int weigh(Onem2mResourceKey onem2mResourceKey, Map<String, Onem2mParentChild> el) {
                return 1;
            }
        }).concurrencyLevel(200)
          .initialCapacity(400000)
            .build(new CacheLoader<Onem2mResourceKey, Map<String, Onem2mParentChild>>() {
                @Override
                public Map<String, Onem2mParentChild> load(Onem2mResourceKey onem2mResourceKey) throws Exception {
                    List<Onem2mParentChild> children =
                        daoResourceTreeReader.retrieveParentChildList(new Onem2mParentChildListKey(
                                                                                onem2mResourceKey.getResourceId()));
                    if (null == children) {
                        return null;
                    }
                    Map<String, Onem2mParentChild> childrenMap =
                        children.parallelStream().collect(
                            Collectors.toConcurrentMap(parentChild -> parentChild.getName(),
                                                       parentChild -> parentChild));
                    return childrenMap;
                }
            });

    /**
     * Constructs empty Cache container
     *
     * @param daoResourceTreeReader is used to retrieve elements not in cache
     */
    public Cache(DaoResourceTreeReader daoResourceTreeReader) {
        this.daoResourceTreeReader = daoResourceTreeReader;
    }

    private LoadingCache<String, String> newOnem2mAeCache(final String cseBaseCseId) {
        return  CacheBuilder.<String, String>newBuilder()
            .maximumWeight(CSE_MAP_BYTE_LIMIT).weigher(new Weigher<String, String>() {
                @Override
                public int weigh(String aeId, String resourceId) {
                    return 1;
                }
            }).concurrencyLevel(200)
            .initialCapacity(16)
            .build(new CacheLoader<String, String>() {
                @Override
                public String load(String aeId) throws Exception {
                    return daoResourceTreeReader.retrieveAeResourceIdByAeId(cseBaseCseId, aeId);
                }
            });
    }

    @Override
    public boolean createCseByName(String name, String resourceId) {
        final Onem2mCseKey key = new Onem2mCseKey(name);

        Onem2mCse onem2mCse = new Onem2mCseBuilder()
                .setKey(key)
                .setName(name)
                .setResourceId(resourceId)
                .build();
        onem2mCseCache.put(key, onem2mCse);

        // cseBae name is equal to it's CSE-ID in this implementation,
        // so we can use it also for cache of registered AEs
        onem2mAeRegCacheMap.put(name, newOnem2mAeCache(name));
        return true;
    }

    /**
     * Returns Cse element if exists.
     * If element is in cache loads from cache, if not retrieves from DB.
     *
     * @param key of the Cse element.
     * @return returns null if Cse element with key does not exist, or DB threw error(Writes log.error).
     */
    @Override
    public Onem2mCse retrieveCseByName(Onem2mCseKey key) {
        try {
            return onem2mCseCache.get(key);
        } catch (ExecutionException e) {
            LOG.error("retrieveCseByName {}", e.getMessage());
            return null;
        } catch (CacheLoader.InvalidCacheLoadException e) {
            return null;
        }
    }

    @Override
    public List<Onem2mCse> retrieveCseBaseList() {
        return new ArrayList<>(onem2mCseCache.asMap().values());
    }

    @Override
    public Map<Onem2mCseKey, Onem2mCse> retrieveCseBaseMap() {
        return onem2mCseCache.asMap();
    }

    private boolean isLeafResourceType(Integer resourceType) {

        if (null == resourceType) {
            return false;
        }

        switch(resourceType) {
            case Onem2m.ResourceType.CONTENT_INSTANCE:
                // Content Instance is leaf resource, don't need to cached children of leaf resources because
                // they will never exist
                // TODO define all leaf resources
                return true;
        }

        return false;
    }

    @Override
    public Onem2mResource createResource(String resourceId, String resourceName, String jsonContent,
                                         String parentResourceId, Integer resourceType, String parentTargetUri) {

        // Initialize the resource
        Onem2mResourceKey key = new Onem2mResourceKey(resourceId);

        Onem2mResourceElem cacheElem = new Onem2mResourceElem(daoResourceTreeReader, resourceId, parentResourceId,
                resourceName, resourceType.toString(), jsonContent, parentTargetUri);

        if (isLeafResourceType(resourceType)) {
            onem2mResourceCache.put(key, cacheElem);

            Map<String, Onem2mParentChild> childMap = new ConcurrentHashMap<>();
            onem2mResourceChildrenCache.put(key, childMap);
        }

        // Add the resource as child of it's parent
        if (null != parentResourceId && ! parentResourceId.isEmpty()) {
            Onem2mResourceKey parentKey = new Onem2mResourceKey(parentResourceId);
            boolean failed = true;
            try {
                Map<String, Onem2mParentChild> childMap = onem2mResourceChildrenCache.get(parentKey);
                childMap.put(resourceName,
                             new Onem2mParentChildBuilder().setName(resourceName).setResourceId(resourceId).build());
                failed = false;
            }
            catch (ExecutionException e) {
                LOG.error("Failed to retrieve parent resource's cached children map, parent resourceId: {}",
                          parentResourceId);
                return null;
            }
            catch (CacheLoader.InvalidCacheLoadException e) {
                return null;
            }

            if (failed) {
                if (isLeafResourceType(resourceType)) {
                    // TODO maybe we can merge onem2mResourceCache and onem2mResourceChildrenCache
                    // TODO to avoid handling of this kind of inconsistency
                    onem2mResourceCache.invalidate(key);
                    onem2mResourceChildrenCache.invalidate(key);
                }
            }
        }

        return cacheElem;
    }

    @Override
    public Onem2mResourceElem retrieveResourceById(Onem2mResourceKey key) {
        try {
            Onem2mResourceElem onem2mResourceElem = onem2mResourceCache.get(key);
//            LOG.info("cache: retrieveResourceById: resourceId:{}, type: {}, parentTargetUri: {}, name: {}",
//                    onem2mResourceElem.getResourceId(), onem2mResourceElem.getResourceType(),
//                    onem2mResourceElem.getParentTargetUri(), onem2mResourceElem.getName());
            return onem2mResourceElem;
        } catch (ExecutionException e) {
            LOG.error("retrieveResourceById: Had problem while retrieving Resource resourceId = {}", key.getResourceId());
            return null;
        } catch (CacheLoader.InvalidCacheLoadException e) {
            return null;
        }
    }

    private boolean loadCseBaseToAeRegCache(final String cseBaseCseId) {

        Onem2mCseKey cseKey = new Onem2mCseKey(cseBaseCseId);

        Onem2mCse cse = retrieveCseByName(cseKey);
        if (null == cse) {
            return false;
        }

        // cseBae name is equal to it's CSE-ID in this implementation,
        // so we can use it also for cache of registered AEs
        onem2mAeRegCacheMap.put(cse.getName(), newOnem2mAeCache(cse.getName()));
        return true;
    }

    private boolean checkCseBaseCseId(final String cseBaseCseId) {
        if (null == cseBaseCseId || cseBaseCseId.isEmpty()) {
            return false;
        }

        if (! onem2mAeRegCacheMap.containsKey(cseBaseCseId)) {
            if (! loadCseBaseToAeRegCache(cseBaseCseId)) {
                LOG.error("No such cseBase created: {}", cseBaseCseId);
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean createAeResourceIdByAeId(final String cseBaseCseId, final String aeId, final String aeResourceId) {
        if (! checkCseBaseCseId(cseBaseCseId)) {
            return false;
        }

        LoadingCache<String, String> cache = onem2mAeRegCacheMap.get(cseBaseCseId);
        cache.put(aeId, aeResourceId);
        return true;
    }

    @Override
    public void deleteAeResourceIdByAeId(final String cseBaseCseId, final String aeId) {
        if (! checkCseBaseCseId(cseBaseCseId)) {
            return;
        }

        LoadingCache<String, String> cache = onem2mAeRegCacheMap.get(cseBaseCseId);
        cache.invalidate(cseBaseCseId);
    }

    @Override
    public String retrieveAeResourceIdByAeId(final String cseBaseCseId, final String aeId) {
        if (! checkCseBaseCseId(cseBaseCseId)) {
            return null;
        }

        LoadingCache<String, String> cache = onem2mAeRegCacheMap.get(cseBaseCseId);

        try {
            String resourceId = cache.get(aeId);
            return resourceId;
        } catch (ExecutionException e) {
            LOG.error("Failed to retrieve resourceId of AE with AE-ID:{} registered to cseBase:{}",
                      aeId, cseBaseCseId);
            return null;
        } catch (CacheLoader.InvalidCacheLoadException e) {
            return null;
        }
    }

    @Override
    public Onem2mParentChild retrieveChildByName(String resourceId, String name) {
        try {
            Map<String, Onem2mParentChild> map =
                onem2mResourceChildrenCache.get(new Onem2mResourceKey(resourceId));
            if (null == map) {
                return null;
            }
            return map.get(name);
        } catch (ExecutionException e) {
            LOG.error("Failed to retrieve child resource with name: {} of parent resource with ID: {}",
                      name, resourceId);
            return null;
        } catch (CacheLoader.InvalidCacheLoadException e) {
            return null;
        }
    }

    @Override
    public List<Onem2mParentChild> retrieveParentChildListLimitN(Onem2mParentChildListKey key, int limit) {
        List<Onem2mParentChild> empty = new ArrayList<>();
        List<Onem2mParentChild> list = null;

        try {
            Map<String, Onem2mParentChild> map =
                onem2mResourceChildrenCache.get(new Onem2mResourceKey(key.getParentResourceId()));
            if (limit > 0) {
                list = map.values()
                          .parallelStream()
                          .limit(limit)
                          .collect(Collectors.toList());
            } else {
                list = map.values().parallelStream().collect(Collectors.toList());
            }
        } catch (ExecutionException e) {
            LOG.error("Failed to retrieve map of child resources for parent resource: {}",
                      key.getParentResourceId());
            return empty;
        } catch (CacheLoader.InvalidCacheLoadException e) {
            return empty;
        }

        if (null == list) {
            return empty;
        }
        return list;
    }


    @Override
    public void updateJsonResourceContentString(String resourceId, String jsonResourceContent) {
        Onem2mResourceKey key = new Onem2mResourceKey(resourceId);

        Onem2mResourceElem head = onem2mResourceCache.getIfPresent(key);
        if (head == null) return;

        head.setResourceContentJsonString(jsonResourceContent);

    }



    @Override
    public void deleteResourceById(String resourceId) {
        Onem2mResourceKey key = new Onem2mResourceKey(resourceId);
        Onem2mResource resource = retrieveResourceById(key);
        if (null != resource && null != resource.getParentId()) {
            Onem2mResourceKey parentKey = new Onem2mResourceKey(resource.getParentId());

            try {
                Map<String, Onem2mParentChild> parentChildrenMap = onem2mResourceChildrenCache.get(parentKey);
                parentChildrenMap.remove(resource.getName());
            } catch (ExecutionException e) {
                LOG.error("Failed to retrieve map of child resources for parent resource: {}",
                          parentKey.getResourceId());
            } catch (CacheLoader.InvalidCacheLoadException e) {
                LOG.error(
                    "InvalidCacheLoadException: Failed to retrieve map of child resources for parent resource: {}",
                    parentKey.getResourceId());
            }
        }

        onem2mResourceCache.invalidate(key);

        // Get list of all children in order to invalidate them all
        Onem2mParentChildListKey parentChildKey = new Onem2mParentChildListKey(resourceId);
        List<Onem2mParentChild> childList = retrieveParentChildListLimitN(parentChildKey, 0);

        // Invalidate the parent first to avoid loops
        onem2mResourceChildrenCache.invalidate(key);

        // Invalidate all child resources now
        // TODO do we need this  ?
        for (Onem2mParentChild child: childList) {
            deleteResourceById(child.getResourceId());
        }
    }

    /**
     * Cleanup the data store.
     */
    @Override
    public void reInitializeDatastore() {
        onem2mResourceCache.invalidateAll();
        onem2mCseCache.invalidateAll();
        onem2mResourceChildrenCache.invalidateAll();

        for (Map.Entry<String, LoadingCache> aeEntry: onem2mAeRegCacheMap.entrySet()) {
            aeEntry.getValue().invalidateAll();
        }
        onem2mAeRegCacheMap.clear();
    }
}