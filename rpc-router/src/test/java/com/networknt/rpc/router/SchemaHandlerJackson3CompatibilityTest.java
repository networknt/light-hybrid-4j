package com.networknt.rpc.router;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import tools.jackson.core.JacksonException;

import static org.junit.jupiter.api.Assertions.*;

class SchemaHandlerJackson3CompatibilityTest {
    private static InputStream service(String value) {
        return new ByteArrayInputStream(("host: test\nservice: service\naction:\n"
                + "  - name: read\n    version: v1\n" + value).getBytes(StandardCharsets.UTF_8));
    }

    @Test
    void rejectsNonBooleanAuthenticationScalarsWithActionAndKey() {
        for (String scalar : new String[]{"True", "TRUE", "yes", "on", "off", "~", "Null", "1", "[]"}) {
            IOException failure = assertThrows(IOException.class,
                    () -> SchemaHandler.parseYaml(service("    skipAuth: " + scalar + "\n")), scalar);
            assertTrue(failure.getMessage().contains("test/service/read/v1.skipAuth"));
            assertTrue(failure.getMessage().contains("expected true or false"));
        }
    }

    @Test
    void canonicalBooleansAndAbsentOrNullValuesRetainAuthenticationPolicy() throws IOException {
        for (String scalar : new String[]{"true", "false", "null", ""}) {
            var actions = SchemaHandler.parseYaml(service("    skipAuth: " + scalar + "\n"));
            assertEquals("true".equals(scalar), actions.get("test/service/read/v1").get("skipAuth"));
        }
        assertEquals(false, SchemaHandler.parseYaml(service("")).get("test/service/read/v1").get("skipAuth"));
    }

    @Test
    void startupDiagnosticIdentifiesSourceSpecAndAction(@org.junit.jupiter.api.io.TempDir java.nio.file.Path directory) throws IOException {
        var spec = directory.resolve("spec.yaml");
        try (InputStream input = service("    skipAuth: yes\n")) {
            java.nio.file.Files.copy(input, spec);
        }
        IOException failure = assertThrows(IOException.class, () -> SchemaHandler.loadService(spec.toUri().toURL()));
        assertTrue(failure.getMessage().contains(spec.toUri().toURL().toString()));
        assertTrue(failure.getMessage().contains("test/service/read/v1.skipAuth"));
        assertInstanceOf(IOException.class, failure.getCause());
    }

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
