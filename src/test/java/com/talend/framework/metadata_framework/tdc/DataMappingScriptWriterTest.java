package com.talend.framework.metadata_framework.tdc;

import com.talend.framework.metadata_framework.audit.Stage;
import com.talend.framework.metadata_framework.model.ColumnDef;
import com.talend.framework.metadata_framework.model.Dataset;
import com.talend.framework.metadata_framework.model.JobLineageGraph;
import com.talend.framework.metadata_framework.model.LineageEdge;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

class DataMappingScriptWriterTest {

    private final DataMappingScriptWriter writer = new DataMappingScriptWriter();

    @Test
    void serializesConnectionsColumnsAndEdge() {
        Dataset file = new Dataset("file:landing/customers.csv", Dataset.Kind.FILE,
                "FileSystem", null, "landing/customers.csv", List.of());
        Dataset bronze = new Dataset("table:bronze.customers", Dataset.Kind.TABLE,
                "PostgreSQL", "bronze", "customers",
                List.of(new ColumnDef("customer_id", "integer", false, 1),
                        new ColumnDef("full_name", "varchar", true, 2)));

        JobLineageGraph graph = new JobLineageGraph(
                "customer_pipeline", null,
                List.of(file, bronze),
                List.of(new LineageEdge(file.id(), bronze.id(),
                        Stage.PROCESSING_FILE, "tFileInputDelimited_1")));

        String script = writer.write(graph);

        assertContains(script, "CREATE CONNECTION \"FileSystem\" TYPE \"File System\";");
        assertContains(script, "CREATE CONNECTION \"PostgreSQL\" TYPE \"Relational Database\";");
        assertContains(script, "INTO   bronze.customers@\"PostgreSQL\"");
        assertContains(script, "FROM   landing/customers.csv@\"FileSystem\"");
        assertContains(script, "customer_id AS customer_id");
        assertContains(script, "-- PROCESSING_FILE via tFileInputDelimited_1");
    }

    @Test
    void fallsBackToStarWhenTargetHasNoColumns() {
        Dataset src = new Dataset("table:silver.a", Dataset.Kind.TABLE,
                "PostgreSQL", "silver", "a", List.of());
        Dataset tgt = new Dataset("table:gold.b", Dataset.Kind.TABLE,
                "PostgreSQL", "gold", "b", List.of());

        JobLineageGraph graph = new JobLineageGraph("j", null, List.of(src, tgt),
                List.of(new LineageEdge(src.id(), tgt.id(), Stage.SILVER_TO_GOLD, null)));

        assertContains(writer.write(graph), "SELECT *");
    }

    @Test
    void skipsEdgesWithUnknownEndpoints() {
        Dataset only = new Dataset("table:silver.a", Dataset.Kind.TABLE,
                "PostgreSQL", "silver", "a", List.of());

        JobLineageGraph graph = new JobLineageGraph("j", null, List.of(only),
                List.of(new LineageEdge("table:silver.a", "table:missing.target",
                        Stage.SILVER_TO_GOLD, null)));

        String script = writer.write(graph);
        assertTrue(!script.contains("INTO"),
                "edge with a missing endpoint must not produce a statement, got:\n" + script);
    }

    private static void assertContains(String haystack, String needle) {
        assertTrue(haystack.contains(needle),
                "expected script to contain:\n  " + needle + "\nbut was:\n" + haystack);
    }
}
