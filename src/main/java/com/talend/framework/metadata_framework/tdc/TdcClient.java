package com.talend.framework.metadata_framework.tdc;

import com.talend.framework.metadata_framework.model.JobLineageGraph;

/**
 * The two things this middleware asks of Talend Data Catalog.
 *
 * <p>TDC already learns the tables/columns ("the dots") by harvesting the
 * database directly via a PostgreSQL (JDBC) Imported Model. This client does
 * NOT create datasets — instead it:
 * <ol>
 *   <li>{@link #refreshModel} — tells TDC to re-harvest that JDBC model so the
 *       catalog is in sync with the live database; and</li>
 *   <li>{@link #pushLineage} — sends the source→target data flow derived from
 *       the ETL audit table into a Data Mapping model, which TDC stitches to
 *       the harvested model to compute lineage ("connecting the dots").</li>
 * </ol>
 */
public interface TdcClient {

    /** Verify the TDC session/connectivity. */
    boolean ping();

    /** Trigger TDC to re-harvest (refresh) the harvested JDBC model so it syncs with the database. */
    void refreshModel(String modelId);

    /** Push the audit-derived source→target lineage into the given Data Mapping model. */
    void pushLineage(String modelId, JobLineageGraph graph);
}
