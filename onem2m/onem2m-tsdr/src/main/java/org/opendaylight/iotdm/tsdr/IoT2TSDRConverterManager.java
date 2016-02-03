/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.iotdm.tsdr;

import java.util.HashMap;
import java.util.Map;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.onem2m.tsdr.rev160203.AddTSDRLogRecordInput;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Sharon Aicler(saichler@gmail.com)
 */
public class IoT2TSDRConverterManager {
    private static final IoT2TSDRConverterManager INSTANCE = new IoT2TSDRConverterManager();
    private static final Logger LOG = LoggerFactory.getLogger(IoT2TSDRConverterManager.class);

    private Map<Class<?>,AbstractIoT2TSDRConverter> converters = new HashMap<>();

    private IoT2TSDRConverterManager(){
    }

    public static final IoT2TSDRConverterManager getInstance(){
        return INSTANCE;
    }

    public AddTSDRLogRecordInput convert(InstanceIdentifier<?> id, DataObject dataObject){
        Class<?> dataObjectClass = dataObject.getImplementedInterface();
        AbstractIoT2TSDRConverter converter = converters.get(dataObjectClass);
        if(converter!=null) {
            return converter.convert(id,dataObject);
        }
        LOG.error("Failed to find a TSDR data converter for "+dataObjectClass.getName());
        return null;
    }

    public void addConverter(Class<?> dataObjectClass,AbstractIoT2TSDRConverter converter){
        this.converters.put(dataObjectClass,converter);
    }
}
