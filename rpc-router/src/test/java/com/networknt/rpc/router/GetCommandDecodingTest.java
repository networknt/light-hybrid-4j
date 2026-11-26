package com.networknt.rpc.router;

import io.undertow.util.URLUtils;
import org.junit.jupiter.api.Test;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GetCommandDecodingTest {
    @Test
    void acceptsSingleAndDoubleEncodedCommandsWithoutChangingData() {
        String json = """
                {"data":{"email":"Account+Tag@Example.com","token":"sample%2B%bad"}}
                """;
        String encoded = URLEncoder.encode(json, StandardCharsets.UTF_8);
        for (String query : new String[]{encoded, URLEncoder.encode(encoded, StandardCharsets.UTF_8)}) {
            String parameter = URLUtils.decode(query, "UTF-8", true, true, new StringBuilder());
            assertEquals(json, SchemaHandler.decodeGetCommand(parameter));
        }
    }
}
