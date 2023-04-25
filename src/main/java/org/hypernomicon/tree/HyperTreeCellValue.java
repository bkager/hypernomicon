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

package org.hypernomicon.tree;

import org.hypernomicon.model.records.HDT_Record;
import org.hypernomicon.model.records.HDT_Work;

import static org.hypernomicon.model.records.RecordType.*;

class HyperTreeCellValue implements Comparable<HyperTreeCellValue>
{
  final private TreeRow row;
  final private String key;

//---------------------------------------------------------------------------

  HyperTreeCellValue(TreeRow treeRow)
  {
    row = treeRow;
    key = makeKey();
  }

//---------------------------------------------------------------------------

  @Override public String toString() { return row.getName(); }
  @Override public int hashCode()    { return key.hashCode(); }

//---------------------------------------------------------------------------
//---------------------------------------------------------------------------

  @Override public int compareTo(HyperTreeCellValue other)
  {
    HDT_Record record1 = row == null ? null : row.getRecord();
    HDT_Record record2 = other.row == null ? null : other.row.getRecord();

    if ((HDT_Record.isEmpty(record1) == false) && (HDT_Record.isEmpty(record2) == false))
    {
      if ((record1.getType() == hdtWork) && (record2.getType() == hdtWork))
        return ((HDT_Work)record1).compareTo((HDT_Work)record2);
    }

    return key.compareTo(other.key);
  }

//---------------------------------------------------------------------------
//---------------------------------------------------------------------------

  @Override public boolean equals(Object obj)
  {
    if (this == obj) return true;
    if (obj == null) return false;
    if (getClass() != obj.getClass()) return false;

    return compareTo((HyperTreeCellValue)obj) == 0;
  }

//---------------------------------------------------------------------------
//---------------------------------------------------------------------------

  private String makeKey()
  {
    String prefix;

    switch (row.getRecordType())
    {
      case hdtDebate      : prefix = "JM."; break;
      case hdtPosition    : prefix = "KM."; break;
      case hdtArgument    : prefix = "LM."; break;
      case hdtWorkLabel   : prefix = "MM."; break;
      case hdtNote        : prefix = "NM."; break;
      case hdtWork        : prefix = "OM."; break;
      case hdtMiscFile    : prefix = "PM."; break;
      case hdtPersonGroup : prefix = "QM."; break;
      case hdtPerson      : prefix = "RM."; break;
      case hdtGlossary    : prefix = "SM."; break;
      case hdtConcept     : prefix = "TM."; break;
      default             : prefix = "ZM.";
    }

    return prefix + toString().toLowerCase();
  }

//---------------------------------------------------------------------------
//---------------------------------------------------------------------------

}
