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
import com.networknt.config.Config;
import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Map;

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

        String response = StandardCharsets.UTF_8.decode(error).toString();
        assertTrue(response.contains("ERR11004"));
        assertTrue(response.contains("$: required property 'name' not found"));
    }

    @Test
    void shouldPreserveCustomValidationMessages() throws Exception {
        Map<String, Object> schema = readMap("{\"type\":\"object\",\"required\":[\"name\"],\"message\":{\"required\":\"A custom validation message\"}}");

        ByteBuffer error = HANDLER.validate("test", schema, Map.of());

        assertTrue(StandardCharsets.UTF_8.decode(error).toString().contains("A custom validation message"));
    }

    private static Map<String, Object> readMap(String json) throws Exception {
        return Config.getInstance().getMapper().readValue(json, new TypeReference<>() { });
    }
}
