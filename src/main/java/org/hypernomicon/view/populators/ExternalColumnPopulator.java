/*
 * Copyright 2015-2021 Jason Winning
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
 *
 */

package org.hypernomicon.view.populators;

import org.hypernomicon.view.wrappers.HyperTable;
import org.hypernomicon.view.wrappers.HyperTableCell;
import org.hypernomicon.view.wrappers.HyperTableRow;
import static org.hypernomicon.view.populators.Populator.CellValueType.*;

import java.util.List;
import java.util.stream.Collectors;

import org.hypernomicon.model.records.RecordType;

//---------------------------------------------------------------------------

public class ExternalColumnPopulator extends Populator
{
  private final HyperTable table;
  private final int colNdx;

  @Override public CellValueType getValueType()                                 { return cvtRecord; }
  @Override public RecordType getRecordType(HyperTableRow row)                  { return table.getTypeByCol(colNdx); }
  @Override public HyperTableCell match(HyperTableRow row, HyperTableCell cell) { return equalMatch(row, cell); }

//---------------------------------------------------------------------------
//---------------------------------------------------------------------------

  public ExternalColumnPopulator(HyperTable table, int colNdx)
  {
    this.table = table;
    this.colNdx = colNdx;
  }

//---------------------------------------------------------------------------
//---------------------------------------------------------------------------

  @Override public List<HyperTableCell> populate(HyperTableRow row, boolean force)
  {
    return table.getSelByCol(colNdx).stream().filter(cell -> filter == null ? true : filter.test(cell.getID()))
                                             .collect(Collectors.toList());
  }

//---------------------------------------------------------------------------
//---------------------------------------------------------------------------

}
