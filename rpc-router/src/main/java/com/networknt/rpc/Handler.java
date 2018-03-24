/*
 * Copyright (c) 2017 Network New Technologies Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.networknt.rpc;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.networknt.config.Config;
import com.networknt.exception.ExpiredTokenException;
import com.networknt.rpc.router.JsonHandler;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.ValidationMessage;
import com.networknt.security.JwtHelper;
import com.networknt.status.Status;
import com.networknt.utility.Constants;
import com.networknt.utility.NioUtils;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.HttpString;
import org.jose4j.jwt.JwtClaims;
import org.jose4j.jwt.consumer.InvalidJwtException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.ext.XLogger;
import org.slf4j.ext.XLoggerFactory;

import java.nio.ByteBuffer;
import java.util.Map;
import java.util.Set;

/**
 * Created by steve on 03/10/16.
 */
public interface Handler {
    Logger logger = LoggerFactory.getLogger(Handler.class);

    String STATUS_VALIDATION_ERROR = "ERR11004";

    ByteBuffer handle (HttpServerExchange exchange, Object object);

    default ByteBuffer validate(String serviceId, Object object) {
        // get schema from serviceId, remember that the schema is for the data object only.
        // the input object is the data attribute of the request body.
        Map<String, Object> serviceMap = (Map<String, Object>)JsonHandler.schema.get(serviceId);
        if(logger.isDebugEnabled()) {
            try {
                logger.debug("serviceId = " + serviceId  + " serviceMap = " + Config.getInstance().getMapper().writeValueAsString(serviceMap));
            } catch (Exception e) {
                logger.error("Exception:", e);
            }
        }
        JsonNode jsonNode = Config.getInstance().getMapper().valueToTree(serviceMap.get("schema"));
        JsonSchemaFactory factory = JsonSchemaFactory.getInstance();
        JsonSchema schema = factory.getSchema(jsonNode);
        Set<ValidationMessage> errors = schema.validate(Config.getInstance().getMapper().valueToTree(object));
        ByteBuffer bf = null;
        if(errors.size() > 0) {
            try {
                Status status = new Status(STATUS_VALIDATION_ERROR, Config.getInstance().getMapper().writeValueAsString(errors));
                bf = NioUtils.toByteBuffer(status.toString());
            } catch (JsonProcessingException e) {
                logger.error("Exception:", e);
            }
        }
        return bf;
    }
}
