/*
 * Copyright (c) 2016 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.iotdm.onem2m.plugins;

import org.slf4j.Logger;
import org.slf4j.helpers.MessageFormatter;

/**
 * Utility methods for PluginManager and related services
 */
public final class Onem2mPluginManagerUtils {

    /**
     * Handles registration error. Logs message and throws RegistrationException with
     * the same message as logged.
     * @param LOG Logger instance
     * @param format Formatting string (Log4j style)
     * @param args Error message arguments
     * @throws IotdmPluginRegistrationException
     */
    public static void handleRegistrationError(Logger LOG, String format, String... args)
            throws IotdmPluginRegistrationException {
        String msg = MessageFormatter.arrayFormat(format, args).getMessage();
        LOG.error("IotdmPlugin registration error: {}", msg);
        throw new IotdmPluginRegistrationException(msg);
    }
}
