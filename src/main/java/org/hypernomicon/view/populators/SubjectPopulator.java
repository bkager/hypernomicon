/*
 * Copyright 2015-2023 Jason Winning
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

import static org.hypernomicon.model.HyperDB.*;
import static org.hypernomicon.model.records.RecordType.*;
import static org.hypernomicon.util.Util.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

import org.hypernomicon.model.records.HDT_Record;
import org.hypernomicon.model.records.RecordType;
import org.hypernomicon.model.relations.RelationSet.RelationType;
import org.hypernomicon.view.wrappers.HyperTableCell;
import org.hypernomicon.view.wrappers.HyperTableRow;

import static org.hypernomicon.view.populators.Populator.CellValueType.*;

//---------------------------------------------------------------------------

public class SubjectPopulator extends RecordPopulator
{
  private final Map<HyperTableRow, Boolean> rowToChanged = new HashMap<>();
  private final Map<HyperTableRow, List<HyperTableCell>> rowToChoices = new HashMap<>();
  private final Map<HyperTableRow, HDT_Record> rowToObj;
  private final RelationType relType;
  private final boolean trackObjByRow;

  private HDT_Record obj;

//---------------------------------------------------------------------------
//---------------------------------------------------------------------------

  public SubjectPopulator(RelationType relType, boolean trackObjByRow, DisplayKind displayKind)
  {
    this(relType, trackObjByRow, null, displayKind);
  }

  public SubjectPopulator(RelationType relType, boolean trackObjByRow, Predicate<Integer> idFilter)
  {
    this(relType, trackObjByRow, idFilter, DisplayKind.cbText);
  }

  public SubjectPopulator(RelationType relType, boolean trackObjByRow)
  {
    this(relType, trackObjByRow, null, DisplayKind.cbText);
  }

  public SubjectPopulator(RelationType relType, boolean trackObjByRow, Predicate<Integer> idFilter, DisplayKind displayKind)
  {
    super(idFilter, displayKind, false);

    this.relType = relType;
    this.trackObjByRow = trackObjByRow;

    rowToObj = trackObjByRow ? new HashMap<>() : null;
  }

//---------------------------------------------------------------------------

  @Override public CellValueType getValueType()                                 { return cvtRecord; }
  @Override public RecordType getRecordType(HyperTableRow row)                  { return db.getSubjType(relType); }
  @Override public HyperTableCell match(HyperTableRow row, HyperTableCell cell) { return equalMatch(row, cell); }
  @Override public void setChanged(HyperTableRow row)                           { rowToChanged.put(nullSwitch(row, dummyRow), true); }

//---------------------------------------------------------------------------
//---------------------------------------------------------------------------

  HDT_Record getObj(HyperTableRow row)
  {
    return trackObjByRow ? rowToObj.get(nullSwitch(row, dummyRow)) : obj;
  }

//---------------------------------------------------------------------------
//---------------------------------------------------------------------------

  public void setObj(HyperTableRow row, HDT_Record newObj)
  {
    HDT_Record oldObj;

    if (row == null) row = dummyRow;

    if (trackObjByRow)
      oldObj = rowToObj.put(row, newObj);
    else
    {
      oldObj = obj;
      obj = newObj;
    }

    rowToChanged.put(row, (oldObj == null) || (oldObj != newObj) || hasChanged(row));
  }

//---------------------------------------------------------------------------
//---------------------------------------------------------------------------

  @Override public List<HyperTableCell> populate(HyperTableRow row, boolean force)
  {
    if (row == null) row = dummyRow;

    if (rowToChoices.containsKey(row) == false)
      rowToChoices.put(row, new ArrayList<>());

    List<HyperTableCell> choices = rowToChoices.get(row);

    if ((hasChanged(row) == false) && (force == false))
      return choices;

    choices.clear();
    choices.add(HyperTableCell.blankCell());

    if (db.isLoaded() == false) return choices;

    HDT_Record curObj;

    if (trackObjByRow)
    {
      if (rowToObj.containsKey(row) == false) return choices;
      curObj = rowToObj.get(row);
    }
    else
    {
      if (obj == null) return choices;
      curObj = obj;
    }

    populateRecordCells(choices, db.getSubjectList(relType, curObj), db.getSubjType(relType));

    rowToChanged.put(row, false);
    return choices;
  }

//---------------------------------------------------------------------------
//---------------------------------------------------------------------------

  @Override public boolean hasChanged(HyperTableRow row)
  {
    if (row == null) row = dummyRow;

    rowToChanged.putIfAbsent(row, true);
    return rowToChanged.get(row);
  }

//---------------------------------------------------------------------------
//---------------------------------------------------------------------------

  @Override public void clear()
  {
    rowToChanged.clear();
    rowToChoices.clear();

    if (trackObjByRow)
      rowToObj.clear();
    else
      obj = null;
  }

//---------------------------------------------------------------------------
//---------------------------------------------------------------------------

  @Override public HyperTableCell addEntry(HyperTableRow row, int id, String text)
  {
    if (row == null) row = dummyRow;

    RecordType type = ((id > 0) || (safeStr(text).length() > 0)) ? db.getSubjType(relType) : hdtNone;

    HyperTableCell cell = new HyperTableCell(id, text, type);

    if (rowToChoices.containsKey(row) == false)
      rowToChoices.put(row, new ArrayList<>());

    rowToChoices.get(row).add(cell);
    return cell;
  }

//---------------------------------------------------------------------------
//---------------------------------------------------------------------------

}
