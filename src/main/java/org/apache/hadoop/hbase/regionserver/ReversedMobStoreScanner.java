/**
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.hadoop.hbase.regionserver;

import java.io.IOException;
import java.util.List;
import java.util.NavigableSet;

import org.apache.hadoop.classification.InterfaceAudience;
import org.apache.hadoop.hbase.Cell;
import org.apache.hadoop.hbase.KeyValue;
import org.apache.hadoop.hbase.KeyValueUtil;
import org.apache.hadoop.hbase.client.Scan;
import org.apache.hadoop.hbase.mob.MobUtils;

/**
 * ReversedMobStoreScanner extends from ReversedStoreScanner, and is used to support
 * reversed scanning in both the memstore and the MOB store.
 *
 */
@InterfaceAudience.Private
public class ReversedMobStoreScanner extends ReversedStoreScanner {

  private boolean cacheMobBlocks = false;

  ReversedMobStoreScanner(Store store, ScanInfo scanInfo, Scan scan, NavigableSet<byte[]> columns,
      long readPt) throws IOException {
    super(store, scanInfo, scan, columns, readPt);
    cacheMobBlocks = MobUtils.isCacheMobBlocks(scan);
  }

  /**
   * Firstly reads the cells from the HBase. If the cell are a reference cell (which has the
   * reference tag), the scanner need seek this cell from the mob file, and use the cell found
   * from the mob file as the result.
   */
  @Override
  public boolean next(List<Cell> outResult, int limit) throws IOException {
    boolean result = super.next(outResult, limit);
    if (!MobUtils.isRawMobScan(scan)) {
      // retrieve the mob data
      if (outResult.isEmpty()) {
        return result;
      }
      HMobStore mobStore = (HMobStore) store;
      for (int i = 0; i < outResult.size(); i++) {
        Cell cell = outResult.get(i);
        if (MobUtils.isMobReferenceCell(cell)) {
          KeyValue kv = KeyValueUtil.ensureKeyValue(cell);
          outResult.set(i, mobStore.resolve(kv, cacheMobBlocks));
        }
      }
    }
    return result;
  }
}
