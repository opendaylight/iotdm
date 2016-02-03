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
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.tsdrrecord.RecordKeys;
import org.opendaylight.yang.gen.v1.opendaylight.tsdr.rev150219.tsdrrecord.RecordKeysBuilder;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier.PathArgument;

/**
 * @author Sharon Aicler(saichler@gmail.com)
 */
public class TSDRUtils {
    public static final List<RecordKeys> getRecordsKeysFromInstanceIdentifier(InstanceIdentifier<?> id){
        List<RecordKeys> result = new LinkedList<>();
        for(PathArgument pa:id.getPathArguments()){
            RecordKeysBuilder rkb = new RecordKeysBuilder();
            rkb.setKeyName(pa.getType().getSimpleName());
            rkb.setKeyValue(pa.toString());
            result.add(rkb.build());
        }
        return result;
    }
}
