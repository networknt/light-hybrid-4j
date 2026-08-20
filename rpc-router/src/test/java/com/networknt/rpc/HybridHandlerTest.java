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

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.networknt.config.Config;
import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HybridHandlerTest {
    private static final HybridHandler HANDLER = (exchange, object) -> null;

    @Test
    void shouldValidateWithDefaultDialect() throws Exception {
        Map<String, Object> schema = readMap("{\"type\":\"object\"}");

        assertNull(HANDLER.validate("test", schema, Map.of("value", 1)));
    }

    @Test
    void shouldAllowStandardNonDefaultDialects() throws Exception {
        Map<String, Object> schema = readMap("{\"$schema\":\"http://json-schema.org/draft-07/schema#\",\"type\":\"object\",\"required\":[\"name\"]}");

        ByteBuffer error = HANDLER.validate("test", schema, Map.of());

        assertNotNull(error);
        JsonNode response = Config.getInstance().getMapper().readTree(StandardCharsets.UTF_8.decode(error).toString());
        assertEquals("ERR11004", response.get("code").asText());
        assertTrue(response.get("description").asText().contains("$: "));
    }

    @Test
    void shouldPreserveCustomValidationMessages() throws Exception {
        Map<String, Object> schema = readMap("{\"type\":\"object\",\"required\":[\"name\"],\"message\":{\"required\":\"A custom validation message\"}}");
        Map<String, Object> nestedSchema = readMap("{\"type\":\"object\",\"properties\":{\"user\":{\"type\":\"object\",\"required\":[\"name\"],\"message\":{\"required\":{\"name\":\"A nested custom message\"}}}}}");

        ByteBuffer error = HANDLER.validate("test", schema, Map.of());
        ByteBuffer nestedError = HANDLER.validate("test", nestedSchema, Map.of("user", Map.of()));

        assertNotNull(error);
        JsonNode response = Config.getInstance().getMapper().readTree(StandardCharsets.UTF_8.decode(error).toString());
        assertTrue(response.get("description").asText().contains("A custom validation message"));
        assertFalse(response.get("description").asText().contains("$: A custom validation message"));
        assertNotNull(nestedError);
        JsonNode nestedResponse = Config.getInstance().getMapper().readTree(StandardCharsets.UTF_8.decode(nestedError).toString());
        assertTrue(nestedResponse.get("description").asText().contains("A nested custom message"));
        assertFalse(nestedResponse.get("description").asText().contains("$.user: A nested custom message"));
    }

    @Test
    void shouldNotTreatMessagePropertySchemaAsCustomMessage() throws Exception {
        Map<String, Object> schema = readMap("{\"type\":\"object\",\"properties\":{\"message\":{\"type\":\"string\"},\"count\":{\"type\":\"integer\"}}}");

        ByteBuffer error = HANDLER.validate("test", schema, Map.of("count", "invalid"));

        assertNotNull(error);
        JsonNode response = Config.getInstance().getMapper().readTree(StandardCharsets.UTF_8.decode(error).toString());
        assertTrue(response.get("description").asText().contains("$.count: "));
    }

    @Test
    void shouldTreatPropertyNamesAsOpaqueWhileFindingCustomMessages() throws Exception {
        Map<String, Object> propertiesSchema = readMap("{\"type\":\"object\",\"properties\":{\"properties\":{\"type\":\"object\",\"properties\":{\"message\":{\"type\":\"string\"},\"n\":{\"type\":\"integer\"}}}}}");
        Map<String, Object> itemsSchema = readMap("{\"type\":\"object\",\"properties\":{\"items\":{\"type\":\"object\",\"properties\":{\"message\":{\"type\":\"string\"},\"n\":{\"type\":\"integer\"}}}}}");

        ByteBuffer propertiesError = HANDLER.validate("test", propertiesSchema,
                Map.of("properties", Map.of("n", "invalid")));
        ByteBuffer itemsError = HANDLER.validate("test", itemsSchema,
                Map.of("items", Map.of("n", "invalid")));

        assertNotNull(propertiesError);
        JsonNode propertiesResponse = Config.getInstance().getMapper().readTree(
                StandardCharsets.UTF_8.decode(propertiesError).toString());
        assertTrue(propertiesResponse.get("description").asText().contains("$.properties.n: "));
        assertNotNull(itemsError);
        JsonNode itemsResponse = Config.getInstance().getMapper().readTree(
                StandardCharsets.UTF_8.decode(itemsError).toString());
        assertTrue(itemsResponse.get("description").asText().contains("$.items.n: "));
    }

    @Test
    void shouldHonorNullableKeywordByDefault() throws Exception {
        Map<String, Object> defaultDialectSchema = readMap("{\"type\":\"object\",\"properties\":{\"email\":{\"type\":\"string\",\"nullable\":true}}}");
        Map<String, Object> draft7Schema = readMap("{\"$schema\":\"http://json-schema.org/draft-07/schema#\",\"type\":\"object\",\"properties\":{\"email\":{\"type\":\"string\",\"nullable\":true}}}");

        assertNull(HANDLER.validate("test", defaultDialectSchema, Collections.singletonMap("email", null)));
        assertNull(HANDLER.validate("test", draft7Schema, Collections.singletonMap("email", null)));
    }

    private static Map<String, Object> readMap(String json) throws Exception {
        return Config.getInstance().getMapper().readValue(json, new TypeReference<>() { });
    }
}
