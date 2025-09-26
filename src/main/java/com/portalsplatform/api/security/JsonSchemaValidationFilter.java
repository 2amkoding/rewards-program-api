package com.portalsplatform.api.security;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SchemaLocation;
import com.networknt.schema.SpecVersion;
import com.networknt.schema.ValidationMessage;
import com.portalsplatform.api.config.SchemaValidationProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class JsonSchemaValidationFilter extends OncePerRequestFilter {

    private final Map<String, JsonSchema> schemas;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final SchemaValidationProperties validationProperties;

    public JsonSchemaValidationFilter(SchemaValidationProperties validationProperties) {
        this.validationProperties = validationProperties;
        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012);
        this.schemas = validationProperties.getRules().stream()
                .collect(Collectors.toMap(
                        SchemaValidationProperties.Rule::getSchema,
                        rule -> factory.getSchema(SchemaLocation.of("classpath:schemas/" + rule.getSchema()))
                ));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String path = request.getRequestURI();
        String method = request.getMethod();

        for (SchemaValidationProperties.Rule rule : validationProperties.getRules()) {
            if (path.equals(rule.getPath()) && method.equalsIgnoreCase(rule.getMethod())) {
                try {
                    JsonNode jsonNode = objectMapper.readTree(request.getInputStream());
                    JsonSchema schema = schemas.get(rule.getSchema());

                    if (schema != null) {
                        Set<ValidationMessage> errors = schema.validate(jsonNode);
                        if (!errors.isEmpty()) {
                            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                            response.getWriter().write(errors.toString());
                            return;
                        }
                    }

                    // If validation is successful, you need to create a new request with the consumed input stream
                    final byte[] jsonBytes = objectMapper.writeValueAsBytes(jsonNode);
                    request = new ReReadableRequestWrapper(request, jsonBytes);

                } catch (Exception e) {
                    response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    response.getWriter().write("Invalid JSON format");
                    return;
                }
                break; // Exit after finding the first matching rule
            }
        }

        filterChain.doFilter(request, response);
    }
}
