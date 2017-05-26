/*
 * Copyright (c) 2017 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.iotdm.plugininfra.commchannels.utils.application.descriptors;

import org.opendaylight.iotdm.plugininfra.commchannels.utils.application.IotdmAppProtoDefs;
import org.opendaylight.iotdm.plugininfra.commchannels.utils.application.IotdmRxAppProtocolConfigration;

public abstract class IotdmRxDescriptorApplicationBaseBuilder<Tconfig extends IotdmRxAppProtocolConfigration> {

    // Interface variables
    private  String ipVersion = IotdmAppProtoDefs.IPVersionIPv4;
    private  String ipAddress = IotdmAppProtoDefs.IPv4AllInterfaces;
    private  String transportProtocol = null;
    private  int portNumber = IotdmAppProtoDefs.PortInit;
    private  String role = null;
    private  boolean shared = false;

    // Protocol variables
    private String protocolChannelId = null;
    private String schema = null;
    private boolean isSecure = false;

    // Endpoint variables
    private String endpoint = null;
    private IotdmAppProtoDefs.EndpointRegistryMode mode = null;


    protected void validateMandatoryInterfaceParams()
        throws IllegalStateException {
        if (null == this.getIpVersion() || this.getIpVersion().isEmpty()) {
            throw new IllegalStateException("IP protocol version is missing");
        }

        if (null == this.getIpAddress() || this.getIpAddress().isEmpty()) {
            throw new IllegalStateException("Interface IP address is missing");
        }

        if (null == this.getTransportProtocol() || this.getTransportProtocol().isEmpty()) {
            throw new IllegalStateException("Transport protocol type not set");
        }

        if (null == this.getRole() || this.getRole().isEmpty()) {
            throw new IllegalStateException("Role not set");
        }
    }

    protected void validateMandatoryProtoParams()
        throws IllegalStateException {
        if (null == this.getProtocolChannelId() || this.getProtocolChannelId().isEmpty()) {
            throw new IllegalStateException("ProtocolID not set");
        }

        if (null == this.getSchema() || this.getSchema().isEmpty()) {
            throw new IllegalStateException("Schema not set");
        }
    }

    protected void validateMandatoryEndpointParams()
        throws IllegalStateException {
        if (null == this.endpoint || this.endpoint.isEmpty()) {
            throw new IllegalStateException("Endpoint not set");
        }

        if (null == this.mode) {
            throw new IllegalStateException("Mode not set");
        }
    }

    protected void validateAll() throws IllegalStateException {
        this.validateMandatoryInterfaceParams();
        this.validateMandatoryProtoParams();
        this.validateMandatoryEndpointParams();
        this.validateConfiguration();
    }


    protected abstract void validateConfiguration() throws IllegalStateException;
//
//    protected abstract IotdmRxDescriptorApplicationInterface buildInterfaceDesc();
//
//    protected abstract IotdmRxDescriptorApplicationProtocol<Tconfig> buildProtocolDesc(Tconfig config);
//
//    protected abstract IotdmRxDescriptorApplicationEndpoint buildEndpointDesc();
//
//    protected abstract Tconfig buildConfiguration();


    /**
     * Validates all parameters first and then builds particular parts of the
     * descriptor instance in this order:
     *  1. Configuration
     *  2. InterfaceDescriptor
     *  3. ProtocolDescriptor
     *  4. EndpointDescriptor
     *  5. The resulting descriptor instance
     * @return
     */
//    protected final void prepareBuild() throws IllegalStateException {
//        this.validateAll();
//
//        Tconfig config = this.buildConfiguration();
//        IotdmRxDescriptorApplicationInterface intDesc = this.buildInterfaceDesc();
//        IotdmRxDescriptorApplicationProtocol<Tconfig> protoDesc = this.buildProtocolDesc(config);
//        IotdmRxDescriptorApplicationEndpoint endpointDesc = this.buildEndpointDesc();
//
//        // TODO this was fine for plugin manager, but in new scenario, channel will pass registration
//        // TODO on behalf of the plugin
//        return new IotdmRxDescriptorApplication<Tconfig>(intDesc, protoDesc, endpointDesc);
//    }


    /*
     * Protected Setters and getters
     */

    protected String getIpVersion() {
        return ipVersion;
    }

    protected void setIpVersion(String ipVersion) {
        this.ipVersion = ipVersion;
    }

    protected String getIpAddress() {
        return ipAddress;
    }

    protected void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    protected String getTransportProtocol() {
        return transportProtocol;
    }

    protected void setTransportProtocol(String transportProtocol) {
        this.transportProtocol = transportProtocol;
    }

    protected String getRole() {
        return role;
    }

    protected void setRole(String role) {
        this.role = role;
    }

    protected boolean isShared() {
        return shared;
    }

    protected void setShared(boolean shared) {
        this.shared = shared;
    }

    protected String getProtocolChannelId() {
        return protocolChannelId;
    }

    protected void setProtocolChannelId(String protocolChannelId) {
        this.protocolChannelId = protocolChannelId;
    }

    protected String getSchema() {
        return schema;
    }

    protected void setSchema(String schema) {
        this.schema = schema;
    }

    protected boolean isSecure() {
        return isSecure;
    }

    protected void setSecure(boolean secure) {
        isSecure = secure;
    }

    protected IotdmAppProtoDefs.EndpointRegistryMode getMode() {
        return mode;
    }

    protected void setMode(IotdmAppProtoDefs.EndpointRegistryMode mode) {
        this.mode = mode;
    }

    protected void setPortNumber(int port) {
        this.portNumber = port;
    }

    protected int getPortNumber() {
        return this.portNumber;
    }

    protected void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }

    protected String getEndpoint() {
        return this.endpoint;
    }
}
