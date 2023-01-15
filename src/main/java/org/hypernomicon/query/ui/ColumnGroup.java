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

package org.hypernomicon.query.ui;

import static org.hypernomicon.bib.data.BibField.BibFieldEnum.*;
import static org.hypernomicon.model.HyperDB.*;
import static org.hypernomicon.model.Tag.*;
import static org.hypernomicon.model.records.RecordType.*;
import static org.hypernomicon.model.relations.RelationSet.RelationType.*;
import static org.hypernomicon.query.ui.ResultsTable.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumMap;
import java.util.List;
import java.util.Set;

import org.hypernomicon.model.Tag;
import org.hypernomicon.model.records.HDT_Record;
import org.hypernomicon.model.records.HDT_Work;
import org.hypernomicon.model.records.RecordType;
import org.hypernomicon.model.relations.RelationSet;
import org.hypernomicon.query.ui.SelectColumnsDlgCtrlr.TypeCheckBox;

import com.google.common.collect.ForwardingCollection;

class ColumnGroup extends ForwardingCollection<ColumnGroupItem>
{
  //---------------------------------------------------------------------------
  //---------------------------------------------------------------------------

  final RecordType recordType;
  final String caption;
  private final List<ColumnGroupItem> items = new ArrayList<>();
  TypeCheckBox checkBox;

  //---------------------------------------------------------------------------
  //---------------------------------------------------------------------------

  ColumnGroup(String caption)
  {
    recordType = hdtNone;
    this.caption = caption;
  }

  //---------------------------------------------------------------------------

  ColumnGroup(RecordType recordType, Set<Tag> tags)
  {
    this.recordType = recordType;
    caption = getTypeName(recordType);

    tags.forEach(tag -> items.add(new ColumnGroupItem(db.mainTextTagForRecordType(recordType) == tag ? tagMainText : tag)));

    RelationSet.getRelationsForObjType(recordType, false).forEach(relType -> items.add(new ColumnGroupItem(relType)));
  }

  //---------------------------------------------------------------------------
  //---------------------------------------------------------------------------

  void addColumnsToTable(ResultsTable resultsTable)
  {
    for (ColumnGroupItem item : this)
    {
      if (item.tag == tagName) continue;

      ResultColumn<? extends Comparable<?>> col = null;
      EnumMap<RecordType, ColumnGroupItem> map = new EnumMap<>(RecordType.class);
      map.put(recordType, item);

      for (ColumnGroup grp : colGroups) for (ColumnGroupItem otherItem : grp)
        if ((item.tag != tagNone) && (item.tag == otherItem.tag))
        {
          map.put(grp.recordType, otherItem);

          if (otherItem.col != null)
          {
            col = otherItem.col;

            if (item.relType == rtNone)
              col.setVisible(true);

            col.map.putAll(map);
            map = col.map;
          }
        }

      if (col == null)
        col = resultsTable.addNonGeneralColumn(map);

      for (ColumnGroupItem otherItem : map.values())
        otherItem.col = col;
    }
  }

  //---------------------------------------------------------------------------
  //---------------------------------------------------------------------------

  public static ColumnGroup newBibFieldsColumnGroup()
  {
    return new ColumnGroup("Reference Manager Fields")
    {
      @Override void addColumnsToTable(ResultsTable resultsTable)
      {
        List.of(bfEntryType, bfContainerTitle, bfPublisher, bfPubLoc, bfEdition, bfVolume, bfIssue, bfLanguage, bfISSNs, bfPages).forEach(field ->
        {
          ResultColumn<String> strCol = new ResultColumn<>(field.getUserFriendlyName());

          strCol.setCellValueFactory(cellData ->
          {
            HDT_Record record = cellData.getValue().getRecord();
            String text = record.getType() == hdtWork ? ((HDT_Work)record).getBibData().getStr(field) : "";
            return new ResultCellValue<>(text, text.trim().toLowerCase()).getObservable();
          });

          strCol.setVisible(false);

          add(new ColumnGroupItem(strCol, resultsTable.getTV(), -1));
        });
      }
    };
  }

  //---------------------------------------------------------------------------
  //---------------------------------------------------------------------------

  @Override protected Collection<ColumnGroupItem> delegate() { return items; }

  //---------------------------------------------------------------------------
  //---------------------------------------------------------------------------

}
