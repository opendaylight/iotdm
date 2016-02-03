/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.iotdm.tsdr;

import java.util.LinkedList;
import java.util.List;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.DataCategory;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.tsdrrecord.RecordKeys;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.tsdrrecord.RecordKeysBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.onem2m.tsdr.rev160203.AddTSDRLogRecordInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.iotdm.onem2m.tsdr.rev160203.AddTSDRLogRecordInputBuilder;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier.PathArgument;

/**
 * @author Sharon Aicler(saichler@gmail.com)
 */
public abstract class AbstractIoT2TSDRConverter {

    //The log record index inside a single millisecond just in case there are two with the same timestamp
    private int index = 0;

    public static final List<RecordKeys> getRecordsKeysFromInstanceIdentifier(InstanceIdentifier<?> id){
        List<RecordKeys> result = new LinkedList<>();
        for(PathArgument pa:id.getPathArguments()){
            RecordKeysBuilder rkb = new RecordKeysBuilder();
            rkb.setKeyName(pa.getType().getSimpleName());
            //@TODO - place code to extract the actual value and not the "toString" using reflection.
            rkb.setKeyValue(pa.toString());
            result.add(rkb.build());
        }
        return result;
    }

    public final AddTSDRLogRecordInput convert(InstanceIdentifier<?> identifier, DataObject dataObject){
        AddTSDRLogRecordInputBuilder builder = new AddTSDRLogRecordInputBuilder();
        builder.setRecordKeys(getRecordsKeysFromInstanceIdentifier(identifier));
        builder.setIndex(index++);
        if(needTimeStamp()){
            builder.setTimeStamp(System.currentTimeMillis());
        }else{
            builder.setTimeStamp(getTimeStamp());
        }
        builder.setNodeID(getNodeID(identifier,dataObject));
        builder.setRecordFullText(getData(identifier,dataObject));
        //@TODO - Add IoT category to TSDR, we can use EXTERNAL for now
        builder.setTSDRDataCategory(DataCategory.EXTERNAL);
        return builder.build();
    }

    public abstract boolean needTimeStamp();
    public abstract long getTimeStamp();
    public abstract String getNodeID(InstanceIdentifier<?> id, DataObject dataObject);
    public abstract String getData(InstanceIdentifier<?> id, DataObject dataObject);
}
