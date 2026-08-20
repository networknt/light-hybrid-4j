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

import com.fasterxml.jackson.databind.JsonNode;
import com.networknt.config.Config;
import com.networknt.schema.Error;
import com.networknt.schema.Schema;
import com.networknt.schema.SchemaRegistry;
import com.networknt.schema.SchemaRegistryConfig;
import com.networknt.schema.dialect.DefaultDialectRegistry;
import com.networknt.schema.dialect.Dialect;
import com.networknt.schema.dialect.Dialects;
import com.networknt.schema.keyword.NonValidationKeyword;
import com.networknt.schema.path.NodePath;
import com.networknt.schema.path.PathType;
import com.networknt.status.Status;
import com.networknt.utility.NioUtils;
import io.undertow.server.HttpServerExchange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * This is the interface that every business handler should implement. It has two default methods
 * that can be shared by all handlers.
 *
 * @author Steve Hu
 */
public interface HybridHandler {
    Logger logger = LoggerFactory.getLogger(HybridHandler.class);

    String REQUEST_SUCCESS = "SUC10200";
    String ERROR_NOT_DEFINED = "ERR10042";
    String STATUS_VALIDATION_ERROR = "ERR11004";

    ByteBuffer handle (HttpServerExchange exchange, Object object);

    default ByteBuffer validate(String serviceId, Map<String, Object> schema, Map<String, Object> data) {
        if(logger.isDebugEnabled()) {
            try {
                logger.debug("serviceId = {} data = {}", serviceId, Config.getInstance().getMapper().writeValueAsString(data));
            } catch (Exception e) {
                logger.error("Exception:", e);
            }
        }
        JsonNode jsonNode = Config.getInstance().getMapper().valueToTree(schema);
        Schema jsonSchema = HybridHandlerSchemaRegistry.INSTANCE.getSchema(jsonNode);
        List<Error> errors = jsonSchema.validate(Config.getInstance().getMapper().valueToTree(data));
        ByteBuffer bf = null;
        if(!errors.isEmpty()) {
            // like the light-rest-4j, we only return one validation error.
            Error error = errors.get(0);
            Status status = new Status(STATUS_VALIDATION_ERROR,
                    HybridHandlerSchemaRegistry.formatError(jsonNode, error));
            logger.error("Validation Error:{}", status);
            bf = NioUtils.toByteBuffer(status.toString());
        }
        return bf;
    }

    /**
     * Return a Status object so that the handler can get the HTTP response code to set exchange response.
     * @param exchange HttpServerExchange used to set the response code
     * @param code Error code defined in status.yml
     * @param args A number of arguments in the error description
     * @return status Status object
     */
    default String getStatus(HttpServerExchange exchange, String code, final Object... args) {
        Status status = new Status(code, args);
        if(status.getStatusCode() == 0) {
            // There is no entry in status.yml for this particular error code.
            status = new Status(ERROR_NOT_DEFINED, code);
        }
        StackTraceElement[] elements = Thread.currentThread().getStackTrace();
        logger.error(status.toString() + " at " + elements[2].getClassName() + "." + elements[2].getMethodName() + "(" + elements[2].getFileName() + ":" + elements[2].getLineNumber() + ")");
        // set status code here so that the response has the right status code.
        exchange.setStatusCode(status.getStatusCode());
        return status.toString();
    }

    /**
     * There are situations that the downstream service returns an error status response and we just
     * want to bubble up to the caller and eventually to the original caller.
     *
     * @param exchange HttpServerExchange
     * @param status error status
     * @return String the status string
     */
    default String getStatus(HttpServerExchange exchange, Status status) {
        exchange.setStatusCode(status.getStatusCode());
        StackTraceElement[] elements = Thread.currentThread().getStackTrace();
        logger.error("{} at {}.{}({}:{})", status.toString(), elements[2].getClassName(), elements[2].getMethodName(), elements[2].getFileName(), elements[2].getLineNumber());
        return status.toString();
    }
}

final class HybridHandlerSchemaRegistry {
    private static final Set<String> DIRECT_SUBSCHEMA_KEYWORDS = Set.of(
            "additionalItems", "additionalProperties", "contains", "contentSchema",
            "else", "if", "items", "not", "propertyNames", "then",
            "unevaluatedItems", "unevaluatedProperties");
    private static final Set<String> SUBSCHEMA_MAP_KEYWORDS = Set.of(
            "$defs", "definitions", "dependencies", "dependentSchemas",
            "patternProperties", "properties");
    private static final Set<String> SUBSCHEMA_ARRAY_KEYWORDS = Set.of(
            "allOf", "anyOf", "oneOf", "prefixItems");
    private static final SchemaRegistryConfig CONFIG = SchemaRegistryConfig.builder()
            .errorMessageKeyword("message")
            .pathType(PathType.LEGACY)
            .build();
    private static final Dialect DEFAULT_DIALECT = withNullableKeyword(Dialects.getDraft202012());
    private static final List<Dialect> DIALECTS = List.of(
            DEFAULT_DIALECT,
            withNullableKeyword(Dialects.getDraft201909()),
            withNullableKeyword(Dialects.getDraft7()),
            withNullableKeyword(Dialects.getDraft6()),
            withNullableKeyword(Dialects.getDraft4()));
    static final SchemaRegistry INSTANCE = SchemaRegistry.builder()
            .defaultDialectId(DEFAULT_DIALECT.getId())
            .dialectRegistry(new DefaultDialectRegistry(DIALECTS))
            .schemaRegistryConfig(CONFIG)
            .build();

    private HybridHandlerSchemaRegistry() {
    }

    private static Dialect withNullableKeyword(Dialect dialect) {
        return Dialect.builder(dialect)
                .keyword(new NonValidationKeyword("nullable"))
                .build();
    }

    static String formatError(JsonNode schemaNode, Error error) {
        return usesCustomMessage(schemaNode, error) ? error.getMessage() : error.toString();
    }

    private static boolean usesCustomMessage(JsonNode schemaNode, Error error) {
        if (schemaNode == null || error.getSchemaLocation() == null || error.getKeyword() == null) {
            return false;
        }
        NodePath fragment = error.getSchemaLocation().getFragment();
        if (fragment == null) {
            return false;
        }
        List<JsonNode> ancestors = new ArrayList<>();
        JsonNode current = schemaNode;
        SchemaTraversalState state = SchemaTraversalState.SCHEMA;
        ancestors.add(current);
        for (int i = 0; i < fragment.getNameCount() - 1; i++) {
            Object element = fragment.getElement(i);
            current = element instanceof Number
                    ? current.get(((Number) element).intValue())
                    : current.get(String.valueOf(element));
            if (current == null) {
                return false;
            }
            state = nextState(state, element, current);
            if (state == SchemaTraversalState.SCHEMA) {
                ancestors.add(current);
            }
        }
        for (int i = ancestors.size() - 1; i >= 0; i--) {
            JsonNode messages = ancestors.get(i).get("message");
            if (messages == null) {
                continue;
            }
            JsonNode customMessage = messages.get(error.getKeyword());
            if (customMessage == null) {
                return false;
            }
            if (customMessage.isTextual()) {
                return !customMessage.asText().isEmpty();
            }
            if (customMessage.isObject()) {
                JsonNode propertyMessage = error.getProperty() == null
                        ? null : customMessage.get(error.getProperty());
                JsonNode selectedMessage = propertyMessage == null ? customMessage.get("") : propertyMessage;
                return selectedMessage != null && selectedMessage.isTextual()
                        && !selectedMessage.asText().isEmpty();
            }
            return false;
        }
        return false;
    }

    private static SchemaTraversalState nextState(SchemaTraversalState state, Object element, JsonNode node) {
        if (state == SchemaTraversalState.SUBSCHEMA_MAP) {
            return element instanceof String && isSchemaNode(node)
                    ? SchemaTraversalState.SCHEMA : SchemaTraversalState.UNKNOWN;
        }
        if (state == SchemaTraversalState.SUBSCHEMA_ARRAY) {
            return element instanceof Number && isSchemaNode(node)
                    ? SchemaTraversalState.SCHEMA : SchemaTraversalState.UNKNOWN;
        }
        if (state != SchemaTraversalState.SCHEMA || !(element instanceof String)) {
            return SchemaTraversalState.UNKNOWN;
        }
        String keyword = (String) element;
        if (SUBSCHEMA_MAP_KEYWORDS.contains(keyword)) {
            return SchemaTraversalState.SUBSCHEMA_MAP;
        }
        if (SUBSCHEMA_ARRAY_KEYWORDS.contains(keyword)) {
            return SchemaTraversalState.SUBSCHEMA_ARRAY;
        }
        if (DIRECT_SUBSCHEMA_KEYWORDS.contains(keyword)) {
            if (isSchemaNode(node)) {
                return SchemaTraversalState.SCHEMA;
            }
            if ("items".equals(keyword) && node.isArray()) {
                return SchemaTraversalState.SUBSCHEMA_ARRAY;
            }
        }
        return SchemaTraversalState.UNKNOWN;
    }

    private static boolean isSchemaNode(JsonNode node) {
        return node.isObject() || node.isBoolean();
    }

    private enum SchemaTraversalState {
        SCHEMA,
        SUBSCHEMA_MAP,
        SUBSCHEMA_ARRAY,
        UNKNOWN
    }
}
