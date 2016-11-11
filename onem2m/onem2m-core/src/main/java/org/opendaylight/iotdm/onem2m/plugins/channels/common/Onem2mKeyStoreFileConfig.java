/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.iotdm.onem2m.plugins.channels.common;

/**
 * Common implementation of KeyStore configuration
 */
public class Onem2mKeyStoreFileConfig {
    protected String keyStoreFile;
    protected String keyStorePassword;
    protected String keyManagerPassword;
    protected boolean usesDefaultConfig;
    protected boolean defaultConfigAvailable = false;

    protected Onem2mKeyStoreFileConfig() {

    }

    public String getKeyStoreFile() {
        return keyStoreFile;
    }
    public String getKeyStorePassword() {
        return keyStorePassword;
    }
    public String getKeyManagerPassword() {
        return keyManagerPassword;
    }
    public boolean getUsesDefaultConfig() { return usesDefaultConfig; }
    protected boolean getDefaultConfigAvailable() { return defaultConfigAvailable; }
    protected void setDefaultConfigAvailable(boolean available) {
        this.defaultConfigAvailable = available;
    }

    protected boolean compareConfig(Onem2mKeyStoreFileConfig configuration) {
        if (this.usesDefaultConfig != configuration.getUsesDefaultConfig()) {
            return false;
        }

        if (this.usesDefaultConfig) {
            // Return true because we have only one default configuration
            return true;
        }

        if ((! this.getKeyStoreFile().equals(configuration.getKeyStoreFile())) ||
            (! this.getKeyStorePassword().equals(configuration.getKeyStorePassword())) ||
            (! this.getKeyManagerPassword().equals(configuration.getKeyManagerPassword()))) {
            return false;
        }

        return true;
    }

    public StringBuilder getConfigString() {
        StringBuilder builder = new StringBuilder()
            .append(this.usesDefaultConfig ? "(Default) " : "")
            .append("KeyStore: ")
            .append(this.keyStoreFile);
        return builder;
    }
}
