package com.portalsplatform.api.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@ConfigurationProperties(prefix = "schema-validation")
@Data
public class SchemaValidationProperties {

    private List<Rule> rules = new ArrayList<>();

    @Data
    public static class Rule {
        private String path;
        private String method;
        private String schema;
    }
}
