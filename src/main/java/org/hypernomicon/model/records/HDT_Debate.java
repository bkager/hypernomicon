/*
 * Copyright 2015-2022 Jason Winning
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

package org.hypernomicon.model.records;

import static org.hypernomicon.model.HyperDB.Tag.*;
import static org.hypernomicon.model.relations.RelationSet.RelationType.*;

import java.util.List;

import org.hypernomicon.model.HyperDataset;
import org.hypernomicon.model.unities.HDT_RecordWithMainText;

public class HDT_Debate extends HDT_RecordWithMainText
{
  public final List<HDT_Debate> largerDebates, subDebates;
  public final List<HDT_Position> largerPositions, subPositions;

//---------------------------------------------------------------------------
//---------------------------------------------------------------------------

  public HDT_Debate(RecordState xmlState, HyperDataset<HDT_Debate> dataset)
  {
    super(xmlState, dataset, tagName);

    largerDebates   = getObjList(rtParentDebateOfDebate);
    largerPositions = getObjList(rtParentPosOfDebate   );

    subDebates   = getSubjList(rtParentDebateOfDebate);
    subPositions = getSubjList(rtParentDebateOfPos   );
  }

//---------------------------------------------------------------------------
//---------------------------------------------------------------------------

  @Override public String listName()          { return name(); }
  @Override public final boolean isUnitable() { return true; }

  public void setLargerDebates  (List<HDT_Debate> list) { updateObjectsFromList(rtParentDebateOfDebate, list); }
  public void setLargerPositions(List<HDT_Debate> list) { updateObjectsFromList(rtParentPosOfDebate   , list); }

//---------------------------------------------------------------------------
//---------------------------------------------------------------------------

}
