/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.iotdm.onem2m.core.database.dao;

import org.opendaylight.iotdm.onem2m.core.database.dao.IotdmDaoException;

/**
 * Created by awkumar on 11/12/2016.
 */
public class IotdmDaoWriteException extends IotdmDaoException {
    public IotdmDaoWriteException(String message) {
        super(message);
    }
}
