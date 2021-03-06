/*
 * Druid - a distributed column store.
 * Copyright 2012 - 2015 Metamarkets Group Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.druid.query.metadata;

import com.google.common.collect.Maps;
import com.google.inject.Inject;
import com.metamx.common.guava.Sequence;
import com.metamx.common.guava.Sequences;
import com.metamx.common.logger.Logger;
import io.druid.query.ChainedExecutionQueryRunner;
import io.druid.query.Query;
import io.druid.query.QueryRunner;
import io.druid.query.QueryRunnerFactory;
import io.druid.query.QueryToolChest;
import io.druid.query.QueryWatcher;
import io.druid.query.metadata.metadata.ColumnAnalysis;
import io.druid.query.metadata.metadata.ColumnIncluderator;
import io.druid.query.metadata.metadata.SegmentAnalysis;
import io.druid.query.metadata.metadata.SegmentMetadataQuery;
import io.druid.segment.QueryableIndex;
import io.druid.segment.Segment;

import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ExecutorService;

public class SegmentMetadataQueryRunnerFactory implements QueryRunnerFactory<SegmentAnalysis, SegmentMetadataQuery>
{
  private static final SegmentAnalyzer analyzer = new SegmentAnalyzer();
  private static final Logger log = new Logger(SegmentMetadataQueryRunnerFactory.class);


  private final SegmentMetadataQueryQueryToolChest toolChest;
  private final QueryWatcher queryWatcher;

  @Inject
  public SegmentMetadataQueryRunnerFactory(
      SegmentMetadataQueryQueryToolChest toolChest,
      QueryWatcher queryWatcher
  )
  {
    this.toolChest = toolChest;
    this.queryWatcher = queryWatcher;
  }

  @Override
  public QueryRunner<SegmentAnalysis> createRunner(final Segment segment)
  {
    return new QueryRunner<SegmentAnalysis>()
    {
      @Override
      public Sequence<SegmentAnalysis> run(Query<SegmentAnalysis> inQ, Map<String, Object> responseContext)
      {
        SegmentMetadataQuery query = (SegmentMetadataQuery) inQ;

        final QueryableIndex index = segment.asQueryableIndex();
        if (index == null) {
          return Sequences.empty();
        }

        final Map<String, ColumnAnalysis> analyzedColumns = analyzer.analyze(index);

        // Initialize with the size of the whitespace, 1 byte per
        long totalSize = analyzedColumns.size() * index.getNumRows();

        Map<String, ColumnAnalysis> columns = Maps.newTreeMap();
        ColumnIncluderator includerator = query.getToInclude();
        for (Map.Entry<String, ColumnAnalysis> entry : analyzedColumns.entrySet()) {
          final String columnName = entry.getKey();
          final ColumnAnalysis column = entry.getValue();

          if (!column.isError()) {
            totalSize += column.getSize();
          }
          if (includerator.include(columnName)) {
            columns.put(columnName, column);
          }
        }

        return Sequences.simple(
            Arrays.asList(
                new SegmentAnalysis(
                    segment.getIdentifier(),
                    Arrays.asList(segment.getDataInterval()),
                    columns,
                    totalSize
                )
            )
        );
      }
    };
  }

  @Override
  public QueryRunner<SegmentAnalysis> mergeRunners(
      ExecutorService exec, Iterable<QueryRunner<SegmentAnalysis>> queryRunners
  )
  {
    return new ChainedExecutionQueryRunner<>(
        exec, toolChest.getOrdering(), queryWatcher, queryRunners
    );
  }

  @Override
  public QueryToolChest<SegmentAnalysis, SegmentMetadataQuery> getToolchest()
  {
    return toolChest;
  }
}
