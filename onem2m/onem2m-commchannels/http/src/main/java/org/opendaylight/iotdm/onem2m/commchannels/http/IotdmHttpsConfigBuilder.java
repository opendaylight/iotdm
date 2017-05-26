/*
 * Copyright (c) 2017 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.iotdm.onem2m.commchannels.http;

import org.opendaylight.iotdm.plugininfra.pluginmanager.api.plugins.IotdmPluginConfigurationBuilder;

/**
 * ConfigurationBuilder for HTTPS CommunicationChannel.
 * Class extends the class implementing the configuration which is being built so
 * the build() method jut returns this object.
 */
public class IotdmHttpsConfigBuilder extends IotdmHttpsPluginServerRx.HttpsServerConfiguration
    implements IotdmPluginConfigurationBuilder<IotdmHttpsPluginServerRx.HttpsServerConfiguration> {

    private void verifyAll() {
        if (this.usesDefaultConfig && (! this.defaultConfigAvailable)) {
            return;
        }

        if (null == this.keyStoreFile || this.keyStoreFile.isEmpty()) {
            throw new IllegalArgumentException("KeyStore file not specified");
        }

        if (null == this.keyStorePassword || this.keyStorePassword.isEmpty()) {
            throw new IllegalArgumentException("KeyStore password not specified");
        }
    }

    /**
     * Verification passes if:
     *      a) Usage of default configuration is set.
     *      b) Usage of default configuration is not set and at least KeyStore file
     *         and KeyStore password are configured.
     * @return
     * @throws IllegalArgumentException
     */
    @Override
    public IotdmHttpsConfigBuilder verify() throws IllegalArgumentException {

        if (this.usesDefaultConfig) {
            // We can consider as verified since we have only one default config
            return this;
        }

        verifyAll();
        return this;
    }

    @Override
    public IotdmHttpsPluginServerRx.HttpsServerConfiguration build() throws IllegalArgumentException {
        verifyAll();
        if (null == this.getKeyManagerPassword()) {
            this.setKeyManagerPassword(this.getKeyStorePassword());
        }
        // Simply return this because this builder just extends built configuration
        return this;
    }

    public IotdmHttpsConfigBuilder setUseDefault(boolean useDefault) {
        this.usesDefaultConfig = useDefault;
        return this;
    }

    public IotdmHttpsConfigBuilder setKeyStoreFile(String keyStoreFile) {
        this.keyStoreFile = keyStoreFile;
        return this;
    }

    public IotdmHttpsConfigBuilder setKeyStorePassword(String keyStorePassword) {
        this.keyStorePassword = keyStorePassword;
        return this;
    }

    public IotdmHttpsConfigBuilder setKeyManagerPassword(String keyManagerPassword) {
        this.keyManagerPassword = keyManagerPassword;
        return this;
    }

    protected boolean getDefaultConfigAvailable() { return super.getDefaultConfigAvailable(); }
    protected void setDefaultConfigAvailable(boolean available) {
        super.setDefaultConfigAvailable(available);
    }
}
