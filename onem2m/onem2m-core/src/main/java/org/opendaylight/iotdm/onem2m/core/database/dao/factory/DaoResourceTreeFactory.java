/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.iotdm.onem2m.core.database.dao.factory;

import org.opendaylight.iotdm.onem2m.core.database.dao.DaoResourceTreeReader;
import org.opendaylight.iotdm.onem2m.core.database.dao.DaoResourceTreeWriter;

import java.io.Closeable;

/**
 * Created by gguliash on 5/20/16.
 * e-mail vinmesmiti@gmail.com; gguliash@cisco.com
 */
public interface DaoResourceTreeFactory extends Closeable{
    /**
     *
     * @return Database reader interface
     */
    DaoResourceTreeWriter getDaoResourceTreeWriter();

    /**
     *
     * @return Database writer interface
     */
    DaoResourceTreeReader getDaoResourceTreeReader();

    void close();
}
