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

package org.hypernomicon.query;

import org.hypernomicon.App;
import org.hypernomicon.model.records.HDT_Institution;
import org.hypernomicon.model.records.HDT_Person;
import org.hypernomicon.query.Query.PersonQuery;
import org.hypernomicon.view.wrappers.HyperTableCell;
import org.hypernomicon.view.wrappers.HyperTableRow;

import com.google.common.collect.ListMultimap;

import static org.hypernomicon.model.HyperDB.db;
import static org.hypernomicon.model.HyperDB.Tag.*;
import static org.hypernomicon.query.ui.QueryTabCtrlr.*;

public final class PersonQueries
{

//---------------------------------------------------------------------------

  private PersonQueries() { throw new UnsupportedOperationException(); }

  private static final int QUERY_SET_DECEASED_AS_PAST = QUERY_FIRST_NDX + 1,
                           QUERY_MULTIPLE_INST        = QUERY_FIRST_NDX + 2;

//---------------------------------------------------------------------------
//---------------------------------------------------------------------------

  private static void addQuery(ListMultimap<QueryType, Query<?>> queryTypeToQueries, PersonQuery query)
  {
    queryTypeToQueries.put(QueryType.qtPersons, query);
  }

//---------------------------------------------------------------------------
//---------------------------------------------------------------------------

  public static void addQueries(ListMultimap<QueryType, Query<?>> queryTypeToQueries)
  {
    if (App.debugging()) addQuery(queryTypeToQueries, new PersonQuery(QUERY_SET_DECEASED_AS_PAST, "Set deceased people as past members of institutions")
    {
      @Override public boolean evaluate(HDT_Person person, HyperTableRow row, HyperTableCell op1, HyperTableCell op2, HyperTableCell op3, boolean firstCall, boolean lastCall)
      {
        if (person.status.getID() == 5)  // Deceased
        {
          boolean foundOne = false;
          for (HDT_Institution inst : person.institutions)
          {
            if (person.instIsPast(inst) == false)
            {
              foundOne = true;
              db.updateNestedBoolean(person, inst, tagPast, true);
            }
          }

          return foundOne;
        }

        return false;
      }

      @Override public boolean hasOperand(int opNum, HyperTableCell prevOp) { return false; }
    });

  //---------------------------------------------------------------------------
  //---------------------------------------------------------------------------

    addQuery(queryTypeToQueries, new PersonQuery(QUERY_MULTIPLE_INST, "with multiple affiliations")
    {
      @Override public boolean evaluate(HDT_Person person, HyperTableRow row, HyperTableCell op1, HyperTableCell op2, HyperTableCell op3, boolean firstCall, boolean lastCall)
      {
        return person.institutions.size() > 1;
      }

      @Override public boolean hasOperand(int opNum, HyperTableCell prevOp) { return false; }
    });
  }

//---------------------------------------------------------------------------
//---------------------------------------------------------------------------

}
