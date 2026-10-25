package com.networknt.rpc.router;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import tools.jackson.core.JacksonException;

import static org.junit.jupiter.api.Assertions.*;

class SchemaHandlerJackson3CompatibilityTest {
    @Test
    void malformedYamlRetainsCheckedErrorContract() {
        InputStream input = new ByteArrayInputStream("host: [".getBytes(StandardCharsets.UTF_8));
        IOException failure = assertThrows(IOException.class, () -> SchemaHandler.parseYaml(input));
        assertInstanceOf(JacksonException.class, failure.getCause());
    }

    @Test
    void streamFailureRetainsCheckedErrorContract() {
        InputStream input = new InputStream() {
            @Override
            public int read() throws IOException {
                throw new IOException("synthetic stream failure");
            }
        };
        IOException failure = assertThrows(IOException.class, () -> SchemaHandler.parseYaml(input));
        assertInstanceOf(JacksonException.class, failure.getCause());
        Throwable root = failure;
        while (root.getCause() != null) {
            root = root.getCause();
        }
        assertInstanceOf(IOException.class, root);
        assertEquals("synthetic stream failure", root.getMessage());
    }
}
