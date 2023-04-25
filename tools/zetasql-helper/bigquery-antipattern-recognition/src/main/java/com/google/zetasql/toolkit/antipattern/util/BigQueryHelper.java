package com.google.zetasql.toolkit.antipattern.util;

import com.google.api.gax.rpc.FixedHeaderProvider;
import com.google.api.gax.rpc.HeaderProvider;
import com.google.cloud.bigquery.BigQuery;
import com.google.cloud.bigquery.BigQueryOptions;
import com.google.cloud.bigquery.InsertAllRequest;
import com.google.cloud.bigquery.Job;
import com.google.cloud.bigquery.JobInfo;
import com.google.cloud.bigquery.QueryJobConfiguration;
import com.google.cloud.bigquery.TableId;
import com.google.cloud.bigquery.TableResult;
import com.google.common.collect.ImmutableMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BigQueryHelper {

  private static final String USER_AGENT_HEADER = "user-agent";
  private static final String USER_AGENT_VALUE = "google-pso-tool/bq-anti-pattern-recognition/0.1.0";
  private static final HeaderProvider headerProvider =
      FixedHeaderProvider.create(ImmutableMap.of(USER_AGENT_HEADER, USER_AGENT_VALUE));
  private static final Logger logger = LoggerFactory.getLogger(BigQueryHelper.class);

  public static TableResult getQueries(String projectId, String daysBack, String ISTable) throws InterruptedException {
    logger.info("Running job on project {}, reading from: {}, scanning last {} days.", projectId, ISTable, daysBack);
    BigQuery bigquery = BigQueryOptions.newBuilder().setProjectId(projectId).setHeaderProvider(headerProvider).build().getService();
    QueryJobConfiguration queryConfig =
        QueryJobConfiguration.newBuilder(
                "SELECT\n"
                    + "  project_id,\n"
                    + "  CONCAT(project_id, \":US.\",  job_id) job_id, \n"
                    + "  query, \n"
                    + "  total_slot_ms / (1000 * 60 * 60 ) AS slot_hours\n"
                    + "FROM\n"
                    + ISTable + "\n"
                    + "WHERE \n"
                    + "  start_time >= CURRENT_TIMESTAMP - INTERVAL "+ daysBack + " DAY\n"
                    + "  AND total_slot_ms > 0\n"
                    + "  AND (statement_type != \"SCRIPT\" OR statement_type IS NULL)\n"
                    + "  AND (reservation_id != 'default-pipeline' or reservation_id IS NULL)\n"
                    + "ORDER BY\n"
                    + "  project_id, start_time desc\n")
            .setUseLegacySql(false)
            .build();

    Job queryJob = bigquery.create(JobInfo.newBuilder(queryConfig).build());
    return queryJob.getQueryResults();
  }

  public static void writeResults(String processingProject, String outputTable, Map<String, Object> rowContent) {
    String[] tableName = outputTable.split("\\.");
    TableId tableId = TableId.of(tableName[0], tableName[1], tableName[2]);
    BigQuery bigquery = BigQueryOptions.newBuilder().setProjectId(processingProject).setHeaderProvider(headerProvider).build().getService();
    bigquery.insertAll(InsertAllRequest.newBuilder(tableId).addRow(rowContent).build());
  }
}
