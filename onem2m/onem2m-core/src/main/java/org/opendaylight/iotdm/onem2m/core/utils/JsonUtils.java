/*
 * Copyright © 2016 Red Hat, Inc. and others.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.iotdm.onem2m.core.utils;

import com.google.common.base.Preconditions;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * JSON utility class (mainly to avoid handling JSONException).
 */
public class JsonUtils {
    /**
     * Puts the given key-value pair in the given JSON object.
     *
     * @param jsonObject The JSON object (must not be {@code null}).
     * @param key The key (must not be {@code null}).
     * @param value The value.
     * @return The JSON object.
     */
    public static JSONObject put(JSONObject jsonObject, String key, Object value) {
        Preconditions.checkNotNull(key);
        try {
            jsonObject.put(key, value);
        } catch (JSONException e) {
            // This only happens if the key is null
        }
        return jsonObject;
    }

    public static JSONObject append(JSONObject jsonObject, String key, Object value) {
        Preconditions.checkNotNull(key);
        try {
            jsonObject.append(key, value);
        } catch (JSONException e) {
            // TODO Determine when this can happen
        }
        return jsonObject;
    }
}
