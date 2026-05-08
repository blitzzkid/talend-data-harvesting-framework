package com.talend.framework.metadata_framework.audit;

import tools.jackson.core.JacksonException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import com.talend.framework.metadata_framework.model.ColumnDef;
import com.talend.framework.metadata_framework.model.NamedColumnSet;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Parses the JSON-in-TEXT schema columns from the audit table.
 *
 * Two shapes are observed in the data:
 * <ul>
 *   <li>Array form — {@code [{name,type,nullable,position}, ...]} — for
 *       single-source steps (FILE_INGESTED, PROCESSING_FILE, SILVER_TO_GOLD,
 *       COLUMN_MAPPING_LOADED).</li>
 *   <li>Object form — {@code {"source1": [...], "source2": [...]}} — for
 *       BRONZE_TO_SILVER where two bronze tables are joined into silver.</li>
 * </ul>
 */
@Component
public class AuditPayloadParser {

    private static final TypeReference<List<ColumnDef>> COLUMN_LIST = new TypeReference<>() {
    };

    private final ObjectMapper mapper;

    public AuditPayloadParser(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    public List<NamedColumnSet> parseSchema(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            JsonNode node = mapper.readTree(json);
            if (node.isArray()) {
                return List.of(NamedColumnSet.unnamed(mapper.convertValue(node, COLUMN_LIST)));
            }
            if (node.isObject()) {
                List<NamedColumnSet> sets = new ArrayList<>();
                for (Map.Entry<String, JsonNode> entry : node.properties()) {
                    sets.add(new NamedColumnSet(entry.getKey(),
                            mapper.convertValue(entry.getValue(), COLUMN_LIST)));
                }
                return sets;
            }
            throw new IllegalArgumentException(
                    "Expected JSON array or object, got: " + node.getNodeType());
        } catch (JacksonException ex) {
            throw new IllegalStateException("Failed to parse schema JSON: " + ex.getMessage(), ex);
        }
    }
}
