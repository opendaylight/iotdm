/*
 * Copyright(c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.iotdm.onem2m.core.utils;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

/**
 * ISO8601 standard YYYY-MM-DD'T'HH:MM:SSZZ
 */
public class Onem2mDateTime {

    private Onem2mDateTime() {
    }

    public static String getCurrDateTime() {
        DateTime dt = new DateTime(DateTimeZone.UTC);
        //return "YYYY-MM-DD'T'HH:MM:SSZZ";
        return dt.toString();
    }

    public static boolean isValidDateTime(String dateTimeString) {

        return true;
        //DateTimeFormatter formatter = DateTimeFormatter.basicDateTimeNoMillis();
        //DateTime dt = formatter.parseDateTime(dateTimeString);

    }
}