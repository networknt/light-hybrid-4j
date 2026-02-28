package com.networknt.rpc.router;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.networknt.config.Config;
import com.networknt.rpc.HybridHandler;
import io.undertow.server.HttpServerExchange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A built-in service handler designed to expose all registered JSON-RPC services (HybridHandlers)
 * as MCP (Model Context Protocol) Tools.
 *
 * It iterates over the parsed `spec.yaml` services loaded into `SchemaHandler.services`,
 * extracting the schema properties and constructing a list of standardized MCP Tool definitions.
 *
 * @author Steve Hu
 */
@ServiceHandler(id="tools/list")
public class ToolsListHandler implements HybridHandler {
    private static final Logger logger = LoggerFactory.getLogger(ToolsListHandler.class);

    @Override
    public ByteBuffer handle(HttpServerExchange exchange, Object input) {
        if(logger.isDebugEnabled()) logger.debug("ToolsListHandler.handle is called.");

        List<Map<String, Object>> tools = new ArrayList<>();

        for (Map.Entry<String, Object> entry : SchemaHandler.services.entrySet()) {
            String serviceId = entry.getKey();

            // Skip the tools/list itself to avoid recursive exposure
            if ("tools/list".equals(serviceId)) {
                continue;
            }

            Map<String, Object> serviceMap = (Map<String, Object>) entry.getValue();
            Map<String, Object> requestMap = (Map<String, Object>) serviceMap.get(SchemaHandler.REQUEST);

            if (requestMap != null) {
                Map<String, Object> schemaMap = (Map<String, Object>) requestMap.get(SchemaHandler.SCHEMA);
                if (schemaMap != null) {
                    Map<String, Object> tool = new HashMap<>();

                    // The MCP tool 'name' field expects a simple identifier, so we use the serviceId.
                    // Hyphens and underscores are often safer than slashes, but slashes might be allowed.
                    // For now, we will use the raw serviceId for exact matching. Replace slashes if MCP throws an error later.
                    tool.put("name", serviceId);

                    // Try to extract a human-readable description, fallback to the title or serviceId
                    String description = (String) schemaMap.get("description");
                    if (description == null || description.trim().isEmpty()) {
                        description = (String) schemaMap.get("title");
                    }
                    if (description == null || description.trim().isEmpty()) {
                        description = "Service " + serviceId;
                    }
                    tool.put("description", description);

                    // MCP uses 'inputSchema' as the root object
                    Map<String, Object> inputSchema = new HashMap<>();
                    inputSchema.put("type", "object");

                    // Copy properties if they exist
                    if (schemaMap.containsKey("properties")) {
                        inputSchema.put("properties", schemaMap.get("properties"));
                    }

                    // Copy required array if it exists
                    if (schemaMap.containsKey("required")) {
                        inputSchema.put("required", schemaMap.get("required"));
                    }

                    tool.put("inputSchema", inputSchema);

                    tools.add(tool);
                }
            }
        }

        // The JSON-RPC 2.0 result payload will be {"tools": [...]}
        Map<String, Object> responseMap = new HashMap<>();
        responseMap.put("tools", tools);

        try {
            String responseString = Config.getInstance().getMapper().writeValueAsString(responseMap);
            return ByteBuffer.wrap(responseString.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        } catch (JsonProcessingException e) {
            logger.error("Error serializing tools list", e);
            throw new RuntimeException(e);
        }
    }
}
