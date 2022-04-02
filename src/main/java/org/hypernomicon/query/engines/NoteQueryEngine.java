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

package org.hypernomicon.query.engines;

import org.hypernomicon.view.populators.QueryPopulator;
import org.hypernomicon.view.populators.VariablePopulator;
import org.hypernomicon.view.wrappers.HyperTableCell;
import org.hypernomicon.view.wrappers.HyperTableRow;

import static org.hypernomicon.model.records.RecordType.*;

import org.hypernomicon.model.records.HDT_Note;
import org.hypernomicon.query.sources.DatasetQuerySource;
import org.hypernomicon.query.sources.QuerySource;

public class NoteQueryEngine extends QueryEngine<HDT_Note>
{

//---------------------------------------------------------------------------
//---------------------------------------------------------------------------

  @Override public void addQueries(QueryPopulator pop, HyperTableRow row)
  {

  }

//---------------------------------------------------------------------------
//---------------------------------------------------------------------------

  @Override public void queryChange(int query, HyperTableRow row, VariablePopulator vp1, VariablePopulator vp2, VariablePopulator vp3)
  {
    switch (query)
    {

    }
  }

//---------------------------------------------------------------------------
//---------------------------------------------------------------------------

  @Override public boolean evaluate(int curQuery, HDT_Note note, HyperTableCell op1, HyperTableCell op2, HyperTableCell op3, boolean firstCall, boolean lastCall)
  {
    switch (curQuery)
    {

    }

    return false;
  }

//---------------------------------------------------------------------------
//---------------------------------------------------------------------------

  @Override public QueryType getQueryType()
  {
    return QueryType.qtNotes;
  }

//---------------------------------------------------------------------------
//---------------------------------------------------------------------------

  @Override public QuerySource getSource(int query, HyperTableCell op1, HyperTableCell op2, HyperTableCell op3)
  {
    switch (query)
    {
      default :
        break;
    }

    return new DatasetQuerySource(hdtNote);
  }

//---------------------------------------------------------------------------
//---------------------------------------------------------------------------

  @Override public boolean needsMentionsIndex(int query)
  {
    return false;
  }

//---------------------------------------------------------------------------
//---------------------------------------------------------------------------

  @Override public boolean hasOperand(int query, int opNum, HyperTableCell prevOp)
  {
    return true;
  }

//---------------------------------------------------------------------------
//---------------------------------------------------------------------------

}
