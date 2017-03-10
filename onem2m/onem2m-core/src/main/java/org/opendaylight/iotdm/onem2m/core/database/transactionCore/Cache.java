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
import com.google.common.collect.Lists;
import java.util.List;
import org.opendaylight.iotdm.onem2m.core.Onem2m;
import org.opendaylight.iotdm.onem2m.core.database.dao.DaoResourceTreeReader;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.onem2m.rev150105.onem2m.cse.list.Onem2mCse;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.onem2m.rev150105.onem2m.cse.list.Onem2mCseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.onem2m.rev150105.onem2m.cse.list.Onem2mCseKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.onem2m.rev150105.onem2m.resource.tree.Onem2mResource;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.onem2m.rev150105.onem2m.resource.tree.Onem2mResourceKey;
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
    private LoadingCache<Onem2mCseKey, Onem2mCse> onem2mCseCache =
            CacheBuilder.<Onem2mCseKey, Onem2mCse>newBuilder().
                    maximumWeight(CSE_MAP_BYTE_LIMIT).weigher(new Weigher<Onem2mCseKey, Onem2mCse>() {
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

    /**
     * Constructs empty Cache container
     *
     * @param daoResourceTreeReader is used to retrieve elements not in cache
     */
    public Cache(DaoResourceTreeReader daoResourceTreeReader) {
        this.daoResourceTreeReader = daoResourceTreeReader;
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
    public Onem2mResource createResource(String resourceId, String resourceName, String jsonContent,
                                         String parentResourceId, Integer resourceType, String parentTargetUri) {
        /**
         * Initialize the resource
         */
        Onem2mResourceKey key = new Onem2mResourceKey(resourceId);

        Onem2mResourceElem cacheElem = new Onem2mResourceElem(daoResourceTreeReader, resourceId, parentResourceId,
                resourceName, resourceType.toString(), jsonContent, parentTargetUri);

        if (resourceType != Onem2m.ResourceType.CONTENT_INSTANCE) {
            onem2mResourceCache.put(key, cacheElem);
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
        onem2mResourceCache.invalidate(key);
    }

    /**
     * Cleanup the data store.
     */
    @Override
    public void reInitializeDatastore() {
        onem2mResourceCache.invalidateAll();
        onem2mCseCache.invalidateAll();
    }
}