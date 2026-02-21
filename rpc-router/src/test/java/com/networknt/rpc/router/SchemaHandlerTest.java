package com.networknt.rpc.router;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class SchemaHandlerTest {
    @Test
    public void testLoadSpecs() {
        SchemaHandler schemaHandler = new SchemaHandler();
        Assertions.assertNotNull(schemaHandler);
    }
}
