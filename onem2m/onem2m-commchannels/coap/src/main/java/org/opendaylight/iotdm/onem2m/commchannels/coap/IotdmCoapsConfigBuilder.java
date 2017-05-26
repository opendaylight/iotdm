/*
 * Copyright (c) 2017 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.iotdm.onem2m.commchannels.coap;

import java.util.HashMap;
import java.util.Map;
import org.opendaylight.iotdm.plugininfra.pluginmanager.api.plugins.IotdmPluginConfigurationBuilder;

/**
 * ConfigurationBuilder for CoAPS CommunicationChannel.
 * Class extends the class implementing the configuration which is being built so
 * the build() method jut returns this object.
 */
public class IotdmCoapsConfigBuilder extends IotdmCoapsPluginServerRx.CoapsConfig
    implements IotdmPluginConfigurationBuilder<IotdmCoapsPluginServerRx.CoapsConfig> {
    public IotdmCoapsConfigBuilder() {
        super();
    }

    private void verifyAll() {
        if (this.usesDefaultConfig && (! this.defaultConfigAvailable)) {
            return;
        }

        if (this.usesPks) {
            if (null == this.presharedKeys || this.presharedKeys.isEmpty()) {
                throw new IllegalArgumentException("Preshared keys not specified");
            }
        } else {
            if (null == this.keyStoreFile || this.keyStoreFile.isEmpty()) {
                throw new IllegalArgumentException("KeyStore file not specified");
            }

            if (null == this.keyStorePassword || this.keyStorePassword.isEmpty()) {
                throw new IllegalArgumentException("KeyStore password not specified");
            }

            if (null == this.keyAlias || this.keyAlias.isEmpty()) {
                throw new IllegalArgumentException("KeyAlias not specified");
            }
        }
    }

    /**
     * Verification passes if:
     *      a) Usage of default configuration is set.
     *      b) Usage of default configuration is not set, usage of PSKs is not set
     *         and at least KeyStore file, KeyStore password and KeyAlias are configured.
     *      c) Usage of default configuration is not set, usage of PSKs is set and
     *         list of PSKs is provided.
     * @return
     * @throws IllegalArgumentException
     */
    @Override
    public IotdmCoapsConfigBuilder verify() throws IllegalArgumentException {
        if (this.usesDefaultConfig) {
            // We can consider as verified since we have only one default config
            return this;
        }

        verifyAll();
        return this;
    }

    @Override
    public IotdmCoapsPluginServerRx.CoapsConfig build() throws IllegalArgumentException {
        // Everything must be set correctly when build is called
        verifyAll();
        if (! this.usesPks && null == this.getKeyManagerPassword()) {
            this.setKeyManagerPassword(this.getKeyStorePassword());
        }
        return this;
    }

    public IotdmCoapsConfigBuilder setUseDefault(boolean useDefault) {
        this.usesDefaultConfig = useDefault;
        return this;
    }

    public IotdmCoapsConfigBuilder setKeyStoreFile(String keyStoreFile) {
        this.keyStoreFile = keyStoreFile;
        return this;
    }

    public IotdmCoapsConfigBuilder setKeyStorePassword(String keyStorePassword) {
        this.keyStorePassword = keyStorePassword;
        return this;
    }

    public IotdmCoapsConfigBuilder setKeyManagerPassword(String keyManagerPassword) {
        this.keyManagerPassword = keyManagerPassword;
        return this;
    }

    public IotdmCoapsConfigBuilder setKeyAlias(String keyAlias) {
        this.keyAlias = keyAlias;
        return this;
    }

    public IotdmCoapsConfigBuilder setUsePsk(boolean use) {
        this.usesPks = use;
        return this;
    }

    public IotdmCoapsConfigBuilder setPresharedKeys(Map<String, String> presharedKeys) {
        this.presharedKeys = presharedKeys;
        return this;
    }

    public IotdmCoapsConfigBuilder addPsk(String cseBaseCseId, String csePsk) {
        if (null == this.presharedKeys) {
            this.presharedKeys = new HashMap<>();
        }

        this.presharedKeys.put(cseBaseCseId, csePsk);
        return this;
    }

    protected boolean getDefaultConfigAvailable() { return super.getDefaultConfigAvailable(); }
    protected void setDefaultConfigAvailable(boolean available) {
        super.setDefaultConfigAvailable(available);
    }
}
