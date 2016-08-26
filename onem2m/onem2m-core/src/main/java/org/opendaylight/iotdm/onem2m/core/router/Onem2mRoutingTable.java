/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.iotdm.onem2m.core.router;

import org.opendaylight.iotdm.onem2m.core.Onem2m;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.util.concurrent.ConcurrentHashMap;


/**
 * Data common for cseBase and remoteCSE resource
 */
abstract class CseRoutingData {
    public final String name;
    public final String resourceId;
    public final String cseId;
    public final String cseType; // true for IN-CSE, false MN-CSE

    protected CseRoutingData(@Nonnull String name, @Nonnull String resourceId,
                             @Nonnull String cseId, @Nonnull String cseType) {
        this.name = name;
        this.resourceId = resourceId;
        this.cseId = cseId;
        this.cseType = cseType;
    }

    /**
     * Creates StringBuilder for debugging.
     * @return
     */
    protected StringBuilder getStringBuilder() {
        return new StringBuilder()
                .append("Name: ").append(this.name)
                .append(", rid: ").append(this.resourceId)
                .append(", CSE_ID: ").append(this.cseId)
                .append(", CSE_TYPE: ").append(this.cseType);
    }

    /**
     * Uses internal StringBuilder and returns built debug string
     * @return
     */
    @Override
    public String toString() {
        return this.getStringBuilder().toString();
    }
}

/**
 * Common implementation of builder for RoutingData.
 */
abstract class CseRoutingDataBuilder {
    protected String name = null;
    protected String resourceId = null;
    protected String cseId = null;
    protected String cseType = null;

    public CseRoutingDataBuilder() {}

    public CseRoutingDataBuilder(@Nonnull CseRoutingData oldCseData) {
        this.setCseId(oldCseData.cseId);
        this.setName(oldCseData.name);
        this.setResourceId(oldCseData.resourceId);
        this.setCseType(oldCseData.cseType);
    }

    public abstract CseRoutingDataBuilder setName(String name);

    public abstract CseRoutingDataBuilder setResourceId(String resourceId);

    public abstract CseRoutingDataBuilder setCseId(String cseId);

    public abstract CseRoutingDataBuilder setCseType(String cseType);

    public abstract CseRoutingData build();

    /**
     * Verification method should be called by build() method to
     * verify if all mandatory attributes have been set.
     * @return true if all mandatory attributes are set
     */
    protected boolean verify() {
        if ((null == this.name) || (null == this.resourceId) || (null == this.cseId) || (null == this.cseType)) {
            return false;
        }
        return true;
    }

    /**
     * Checks whether the CSE is IN type
     * @return true for IN-CSE type
     */
    public boolean isCseTypeInCse() {
        return this.cseType.equals(Onem2m.CseType.INCSE);
    }

    /**
     * Checks whether the CSE is MN type
     * @return true for MN-CSE type
     */
    public boolean isCseTypeMnCse() {
        return this.cseType.equals(Onem2m.CseType.MNCSE);
    }
}


/*
 * RemoteCSE routing data classes
 */

/**
 * Class stores routing data from remoteCSE resource
 */
final class CseRoutingDataRemote extends CseRoutingData {
    protected final String parentCseBaseName;
    public final String parentCseBaseCseId;
    public final boolean requestReachable;
    public final String[] pointOfAccess;
    public final String polingChannel;

    protected CseRoutingDataRemote(@Nonnull String parentCseBaseName,
                                   @Nonnull String parentCseBaseCseId,
                                   @Nonnull String name, @Nonnull String resourceId,
                                   @Nonnull String cseId, @Nonnull String cseType,
                                   boolean requestReachable, String[] pointOfAccess, String polingChannel) {
        super(name, resourceId, cseId, cseType);
        this.requestReachable = requestReachable;
        this.pointOfAccess = pointOfAccess;
        this.polingChannel = polingChannel;
        this.parentCseBaseName = parentCseBaseName;
        this.parentCseBaseCseId = parentCseBaseCseId;
    }

    /**
     * Overrides string builder of superclass and adds debugging information
     * specific to remoteCSE
     * @return The resulting StringBuilder
     */
    @Override
    protected StringBuilder getStringBuilder() {
        StringBuilder builder = super.getStringBuilder();

        builder.append(", RR: ").append(this.requestReachable).append(", PoA: ");

        if (null == this.pointOfAccess || (0 == this.pointOfAccess.length)) {
            builder.append("NONE");
        } else {
            String poa = null;
            for (int i = 0; i < this.pointOfAccess.length; i++) {
                poa = this.pointOfAccess[i];
                if (0 == i) {
                    builder.append("[").append(poa);
                } else {
                    builder.append(", ").append(poa);
                }
            }
            builder.append("]");
        }

        return builder.append(", PCH: ").append(this.polingChannel);
    }
}

/**
 * Builder class for the remoteCSE routing data
 */
final class CseRoutingDataRemoteBuilder extends CseRoutingDataBuilder {

    private String parentCseBaseName = null;
    private boolean requestReachable = false; // request unreachable by default
    private String[] pointOfAccess = null;
    private String polingChannel = null;
    private String cseBaseCseId = null;

    protected CseRoutingDataRemoteBuilder() {
        super();
    }

    protected CseRoutingDataRemoteBuilder(@Nonnull CseRoutingDataRemote oldRoutingData) {
        super(oldRoutingData);
        this.setRequestReachable(oldRoutingData.requestReachable);
        this.setPointOfAccess(oldRoutingData.pointOfAccess);
        this.setPolingChannel(oldRoutingData.polingChannel);
        this.setParentCseBaseName(oldRoutingData.parentCseBaseName);
        this.setCseBaseCseId(oldRoutingData.parentCseBaseCseId);
    }

    @Override
    public CseRoutingDataRemoteBuilder setName(String name) {
        this.name = name;
        return this;
    }

    @Override
    public CseRoutingDataRemoteBuilder setResourceId(String resourceId) {
        this.resourceId = resourceId;
        return this;
    }

    @Override
    public CseRoutingDataRemoteBuilder setCseId(String cseId) {
        this.cseId = cseId;
        return this;
    }

    @Override
    public CseRoutingDataRemoteBuilder setCseType(String cseType) {
        this.cseType = cseType;
        return this;
    }

    public CseRoutingDataRemoteBuilder setParentCseBaseName(String parentCseBaseName) {
        this.parentCseBaseName = parentCseBaseName;
        return this;
    }

    public CseRoutingDataRemoteBuilder setRequestReachable(boolean requestReachable) {
        this.requestReachable = requestReachable;
        return this;
    }

    public CseRoutingDataRemoteBuilder setPointOfAccess(String[] pointOfAccess) {
        this.pointOfAccess = pointOfAccess;
        return this;
    }

    public CseRoutingDataRemoteBuilder setPolingChannel(String polingChannel) {
        this.polingChannel = polingChannel;
        return this;
    }

    public CseRoutingDataRemoteBuilder setCseBaseCseId(String cseBaseCseId) {
        this.cseBaseCseId = cseBaseCseId;
        return this;
    }

    @Override
    protected boolean verify() {
        if (! super.verify()) {
            return false;
        }

        return ((null != this.parentCseBaseName) && (null != this.cseBaseCseId));
    }

    @Override
    public CseRoutingDataRemote build() {
        if (! this.verify()) {
            return null;
        }

        return new CseRoutingDataRemote(this.parentCseBaseName, this.cseBaseCseId,
                                        this.name, this.resourceId, this.cseId, this.cseType,
                                        this.requestReachable, this.pointOfAccess, this.polingChannel);
    }
}


/*
 * CSEBase routing data classes
 */

/**
 * Class stores routing data of cseBase resource and stores
 * also routing data of all child resources of remoteCSE type.
 */
final class CseRoutingDataBase extends CseRoutingData {
    private static final Logger LOG = LoggerFactory.getLogger(CseRoutingDataBase.class);

    public final String FQDN;
    public final String registrarCseId;
    // Hashmap of remoteCSE routing data, key is cseId of the remoteCSE
    protected final ConcurrentHashMap<String, CseRoutingDataRemote> remoteCseMap;

    protected CseRoutingDataBase(@Nonnull String name, @Nonnull String resourceId,
                                 @Nonnull String cseId, @Nonnull String cseType,
                                 String FQDN, String registrarCseId,
                                 ConcurrentHashMap<String, CseRoutingDataRemote> remoteCseMap) {
        super(name, resourceId, cseId, cseType);
        this.FQDN = FQDN;
        this.registrarCseId = registrarCseId;

        if (null == remoteCseMap) {
            this.remoteCseMap = new ConcurrentHashMap<>();
        } else {
            this.remoteCseMap = remoteCseMap;
        }
    }

    /**
     * Returns remoteCSE routing data of the remoteCSE identified by cseId
     * @param remoteCseId The cse id
     * @return RoutingData if exists, null otherwise.
     */
    public CseRoutingDataRemote getRemoteCse(@Nonnull String remoteCseId) {
        return remoteCseMap.get(remoteCseId);
    }

    /**
     * Adds cseBase specific data into debugging string buffer
     * @return The string buffer extended by cseBase specific information
     */
    @Override
    protected StringBuilder getStringBuilder() {
        return super.getStringBuilder()
                .append(", FQDN: ").append(this.FQDN)
                .append(", registrarCSE: ").append(this.registrarCseId);
    }
}

/**
 * Class implements buider for cseBase routing data instances
 */
final class CseRoutingDataBaseBuilder extends CseRoutingDataBuilder {
    private String FQDN;
    private String registrarCseId;
    private final ConcurrentHashMap<String, CseRoutingDataRemote> remoteCseMap;

    protected CseRoutingDataBaseBuilder() {
        super();
        this.remoteCseMap = null;
    }

    protected CseRoutingDataBaseBuilder(@Nonnull CseRoutingDataBase oldCseBase) {
        super(oldCseBase);
        this.setFQDN(oldCseBase.FQDN);
        this.setRegistrarCseId(oldCseBase.registrarCseId);
        this.remoteCseMap = oldCseBase.remoteCseMap;
    }

    @Override
    public CseRoutingDataBaseBuilder setName(String name) {
        this.name = name;
        return this;
    }

    @Override
    public CseRoutingDataBaseBuilder setResourceId(String resourceId) {
        this.resourceId = resourceId;
        return this;
    }

    @Override
    public CseRoutingDataBaseBuilder setCseId(String cseId) {
        this.cseId = cseId;
        return this;
    }

    @Override
    public CseRoutingDataBaseBuilder setCseType(String cseType) {
        this.cseType = cseType;
        return this;
    }

    public CseRoutingDataBaseBuilder setFQDN(String FQDN) {
        this.FQDN = FQDN;
        return this;
    }

    public CseRoutingDataBaseBuilder setRegistrarCseId(String registrarCseId) {
        this.registrarCseId = registrarCseId;
        return this;
    }

    @Override
    protected boolean verify() {
        if (! super.verify()) {
            return false;
        }

        if (this.isCseTypeInCse() && (null != this.registrarCseId)) {
            // IN-CSE should not have registrarCSE
            return false;
        }

        return true;
    }

    @Override
    public CseRoutingDataBase build() {
        if (! this.verify()) {
            return null;
        }

        return new CseRoutingDataBase(this.name, this.resourceId, this.cseId, this.cseType,
                                      this.FQDN, this.registrarCseId, this.remoteCseMap);
    }
}


/*
 * Onem2mRouting table
 */

/**
 * Implements the Onem2m routing table for RouterService.
 * Uses RoutingData classes and related builders to cache routing data
 * from cseBase and remoteCSE resources.
 */
public class Onem2mRoutingTable {
    private static final Logger LOG = LoggerFactory.getLogger(Onem2mRoutingTable.class);
    // cseBase routing data by cseBase name
    private final ConcurrentHashMap<String, CseRoutingDataBase> cseBaseMap = new ConcurrentHashMap<>();
    // cseBase routing data by cseBase cseID
    private final ConcurrentHashMap<String, CseRoutingDataBase> cseBaseMapByCseId = new ConcurrentHashMap<>();

    /**
     * Deletes all data from routing table
     */
    public void cleanRoutingTable() {
        this.cseBaseMap.clear();
        this.cseBaseMapByCseId.clear();
    }

    /**
     * Helper mehod, should be used to keep cseBase hash maps consistent.
     * @param base
     * @return
     */
    private CseRoutingDataBase putCseBase(CseRoutingDataBase base) {
        cseBaseMap.put(base.name, base);
        cseBaseMapByCseId.put(base.cseId, base);
        return base;
    }

    /**
     * Helper method, should be used to keep cseBase hash maps consistent.
     * @param baseName
     * @return
     */
    private CseRoutingDataBase delCseBase(String baseName) {
        CseRoutingDataBase old = cseBaseMap.remove(baseName);
        if (null != old) {
            cseBaseMapByCseId.remove(old.cseId);
        }
        return old;
    }


    /*
     * CRUD methods of cseBase routing data
     */

    /**
     * Adds new instance of cseBase routing data
     * @param newCseBase The new cseBase routing data
     * @return New cseBase routing data if success, null otherwise
     */
    public CseRoutingDataBase addCseBase(CseRoutingDataBase newCseBase) {
        if (cseBaseMap.containsKey(newCseBase.name)) {
            LOG.warn("CSEBase {} already exists in routing table, deleting", newCseBase.name);
            delCseBase(newCseBase.name);
        }
        putCseBase(newCseBase);
        return newCseBase;
    }

    /**
     * Stores updated cseBase routing data
     * @param updatedCseBase The updated routing data
     * @return Updated cseBase routing data if success, null otherwise
     */
    public CseRoutingDataBase updateCseBase(@Nonnull CseRoutingDataBase updatedCseBase) {
        if (null == delCseBase(updatedCseBase.name)) {
            LOG.warn("CSEBase with name: {} doesn't exist, will be created", updatedCseBase.name);
        }
        putCseBase(updatedCseBase);
        return updatedCseBase;
    }

    /**
     * Deletes cseBase routing data identified by name of the cseBase
     * @param name The cseBase name
     * @return Delete cseBase routing data if success, null otherwise
     */
    public CseRoutingDataBase removeCseBase(@Nonnull String name) {
        return delCseBase(name);
    }

    /**
     * Returns cseBase routing data identified by name of the cseBase
     * @param name The cseBase name
     * @return CseBase routing data if success, null otherwise
     */
    public CseRoutingDataBase getCseBase(@Nonnull String name) {
        return cseBaseMap.get(name);
    }

    /**
     * Returns cseBase routing data identified by cseId of the cseBase
     * @param cseBaseCseId The cseId
     * @return CseBase routing data if success, null otherwise
     */
    public CseRoutingDataBase getCseBaseByCseId(@Nonnull String cseBaseCseId) {
        return cseBaseMapByCseId.get(cseBaseCseId);
    }


    /*
     * CRUD methods of remoteCSE
     */

    /**
     * Adds new remoteCSE routing data. The routing data includes
     * information about cseBase which the remoteCSE routing data belongs to
     * @param newRemoteCse RemoteCSE routing data to be added
     * @return Added routing data if success, null otherwise
     */
    public CseRoutingDataRemote addRemoteCse(@Nonnull CseRoutingDataRemote newRemoteCse) {
        if (! cseBaseMap.containsKey(newRemoteCse.parentCseBaseName)) {
            LOG.error("Attempt to add RemoteCSE to non-existing CSEBase {}", newRemoteCse.parentCseBaseName);
            return null;
        }

        CseRoutingDataBase base = this.getCseBase(newRemoteCse.parentCseBaseName);
        if (base.remoteCseMap.containsKey(newRemoteCse.cseId)) {
            LOG.warn("RemoteCSE with CSE_ID: {} already exists under CSEBase {}, will be deleted",
                     newRemoteCse.cseId, newRemoteCse.parentCseBaseName);
            base.remoteCseMap.remove(newRemoteCse.cseId);
        }

        base.remoteCseMap.put(newRemoteCse.cseId, newRemoteCse);
        return newRemoteCse;
    }

    /**
     * Updates remoteCSE routing data by the data passed as paramter.
     * @param updatedRemoteCse RemoteCSE routing data to be updated
     * @return Updated routing data if success, null otherwise
     */
    public CseRoutingDataRemote updateRemoteCse(@Nonnull CseRoutingDataRemote updatedRemoteCse) {
        if (! cseBaseMap.containsKey(updatedRemoteCse.parentCseBaseName)) {
            LOG.error("Attempt to update RemoteCSE to non-existing CSEBase {}", updatedRemoteCse.parentCseBaseName);
            return null;
        }

        CseRoutingDataBase base = this.getCseBase(updatedRemoteCse.parentCseBaseName);
        if (! base.remoteCseMap.containsKey(updatedRemoteCse.cseId)) {
            LOG.warn("RemoteCSE with CSE_ID: {} doesn't exist under CSEBase {}, will be created",
                     updatedRemoteCse.cseId, updatedRemoteCse.parentCseBaseName);
        } else {
            base.remoteCseMap.remove(updatedRemoteCse.cseId);
        }

        base.remoteCseMap.put(updatedRemoteCse.cseId, updatedRemoteCse);
        return updatedRemoteCse;
    }

    /**
     * Retrieves remoteCSE routing data identified by cseBase name and remoteCSE cseId
     * @param baseCseName The cseBase name
     * @param remoteCseId The remoteCSE cseId
     * @return RemoteCSE routing data if found, null otherwise
     */
    public CseRoutingDataRemote getRemoteCse(@Nonnull String baseCseName, @Nonnull String remoteCseId) {
        if (! cseBaseMap.containsKey(baseCseName)) {
            LOG.error("Attempt to get RemoteCSE from non-existing CSEBase {}", baseCseName);
            return null;
        }

        CseRoutingDataBase base = this.getCseBase(baseCseName);
        if (! base.remoteCseMap.containsKey(remoteCseId)) {
            return null;
        }

        return base.remoteCseMap.get(remoteCseId);
    }

    /**
     * Removes remoteCSE routing data identified by cseBase name and remoteCSE cseId
     * @param baseCseName The cseBase name
     * @param remoteCseId The remoteCSE cseId
     * @return Removed remoteCSE routing data if success, null otherwise
     */
    public CseRoutingDataRemote removeRemoteCse(@Nonnull String baseCseName, @Nonnull String remoteCseId) {
        if (! cseBaseMap.containsKey(baseCseName)) {
            LOG.error("Attempt to remove RemoteCSE from non-existing CSEBase {}", baseCseName);
            return null;
        }

        CseRoutingDataBase base = this.getCseBase(baseCseName);
        if (! base.remoteCseMap.containsKey(remoteCseId)) {
            return null;
        }

        return base.remoteCseMap.remove(remoteCseId);
    }


    /*
     * Helper methods
     */

    /**
     * Finds first remoteCSE routing data related to remoteCSE identified by
     * cseId
     * @param remoteCseId The remotCSE cseId
     * @return RemoteCSE roting data if success, null otherwise
     */
    // TODO this is needed for now, because we don't know which cseBase is destination
    public CseRoutingDataRemote findFirstRemoteCse(@Nonnull String remoteCseId) {
        for (String cseBase : this.cseBaseMap.keySet()) {
            if (this.getCseBase(cseBase).remoteCseMap.containsKey(remoteCseId)) {
                return this.getRemoteCse(cseBase, remoteCseId);
            }
        }
        return null;
    }

    /**
     * Returns builder for cseBase routing data for add operation
     * @return cseBase routing data builder
     */
    public CseRoutingDataBaseBuilder getCseBaseAddBuilder() {
        return new CseRoutingDataBaseBuilder();
    }

    /**
     * Returns builder for cseBase routing data for update operation. CseBase
     * resource with baseCseName name must already exists in routing table.
     * @param baseCseName The name of the cseBase routing data to be updated
     * @return cseBase routing data builder if success, null otherwise
     */
    public CseRoutingDataBaseBuilder getCseBaseUpdateBuilder(@Nonnull String baseCseName) {
        CseRoutingDataBase currentData = this.getCseBase(baseCseName);
        if (null == currentData) {
            return null;
        }
        return new CseRoutingDataBaseBuilder(currentData);
    }

    /**
     * Returns builder for remoteCSE routing data for add operation
     * @return remoteCSE routing data builder if success, null otherwise
     */
    public CseRoutingDataRemoteBuilder getCseRemoteAddBuilder() {
        return new CseRoutingDataRemoteBuilder();
    }

    /**
     * Returns builder for remoteCSE routing data for updated operation.
     * RemoteCSE with given cseId must already exists in routing table under
     * cseBase identified by baseCseName
     * @param baseCseName The name of cseBase
     * @param remoteCseId The cseId of remoteCSE routing data
     * @return remoteCSE routing data builder if success, null otherwise
     */
    public CseRoutingDataRemoteBuilder getCseRemoteUpdateBuilder(@Nonnull String baseCseName,
                                                                 @Nonnull String remoteCseId) {
        CseRoutingDataRemote currentData = this.getRemoteCse(baseCseName, remoteCseId);
        if (null == currentData) {
            return null;
        }
        return new CseRoutingDataRemoteBuilder(currentData);
    }

    /**
     * Dumps whole routing table to log as debug messages
     * @param message The first line leading message. Can be set to null.
     */
    public void dumpDebug(String message) {
        message = (null == message ? "" : message);
        StringBuilder builder = new StringBuilder();
        builder.append(message).append(":: ").append("Onem2m Routing Table: ");

        if (cseBaseMap.isEmpty()) {
            LOG.debug("{} EMPTY", builder.toString());
            return;
        }

        CseRoutingDataBase baseCse = null;
        LOG.debug("{}", builder.toString());
        for (String baseName: cseBaseMap.keySet()) {
            baseCse = cseBaseMap.get(baseName);
            LOG.debug("\t CSEBase {}: {}", baseName, baseCse);

            if (baseCse.remoteCseMap.isEmpty()) {
                LOG.debug("\t\t NO_REMOTE_CSE");
                continue;
            }

            for (String remoteCseId: baseCse.remoteCseMap.keySet()) {
                LOG.debug("\t\t remoteCSE {}: {}", remoteCseId, baseCse.getRemoteCse(remoteCseId));
            }
        }
    }
}
