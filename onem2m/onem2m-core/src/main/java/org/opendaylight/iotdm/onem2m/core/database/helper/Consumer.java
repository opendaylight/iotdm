/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.iotdm.onem2m.core.database.helper;

/**
 * Created by gguliash on 4/5/16.
 * e-mail vinmesmiti@gmail.com; gguliash@cisco.com
 */
public interface Consumer<T> {
    /**
     * If it returns true, consume will not be called anymore.
     *
     * @return true if wants to stop consuming
     */
    boolean isFull();

    /**
     * Reader will call consume providing element of type T until everything is read or isFull returns true
     * @param food element to be consumed
     */
    void consume(T food);
}
