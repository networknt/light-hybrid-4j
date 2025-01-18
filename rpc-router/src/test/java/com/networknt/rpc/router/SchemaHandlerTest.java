package com.networknt.rpc.router;

import org.junit.Assert;
import org.junit.Test;

public class SchemaHandlerTest {
    @Test
    public void testLoadSpecs() {
        SchemaHandler schemaHandler = new SchemaHandler();
        Assert.assertNotNull(schemaHandler);
    }
}
