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

package org.hypernomicon.model.unities;

import static org.hypernomicon.model.HyperDB.*;
import static org.hypernomicon.model.records.RecordType.*;

import org.hypernomicon.model.Exceptions.HDB_InternalError;
import org.hypernomicon.model.records.HDT_Record;
import org.hypernomicon.model.records.RecordType;

/**
 * Every record that has a main HTML description field has its own object of
 * this class. Some of those record types, but not all, also can be "united"
 * to other records so that they have the same {@link MainText MainText} object.
 * They then still have separate Connector objects but each refers to the same
 * {@link MainText MainText} and {@link StrongLink StrongLink} objects.
 *
 * The reason for not folding this functionality into the
 * {@link HDT_RecordWithConnector HDT_RecordWithConnector}
 * class is that it has to be in the same package as {@link StrongLink StrongLink},
 * and it is safer for both of those classes to not be in the same package as the
 * record classes.
 *
 * @author  Jason Winning
 * @since   1.0
 */
public final class Connector
{
  //---------------------------------------------------------------------------
  //---------------------------------------------------------------------------

  Connector(HDT_RecordWithConnector record)
  {
    this.record = record;
    mainText = new MainText(this);

    if (record.getType() == hdtHub)
      hub = (HDT_Hub) record;
  }

  //---------------------------------------------------------------------------
  //---------------------------------------------------------------------------

  private final HDT_RecordWithConnector record;
  HDT_Hub hub;
  MainText mainText;
  private boolean alreadyModifying = false;

  public RecordType getType()                { return getSpoke().getType(); }
  boolean hasHub()                           { return hub != null; }
  HDT_Hub getHub()                           { return hub; }
  MainText getMainText()                     { return mainText; }
  public HDT_RecordWithConnector getSpoke()  { return record; }
  static boolean isEmpty(Connector c)        { return (c == null) || HDT_Record.isEmpty(c.getSpoke()); }

  @Override public int hashCode()            { return record == null ? 0 : (31 * record.hashCode()); }

  //---------------------------------------------------------------------------
  //---------------------------------------------------------------------------

  void modifyNow()
  {
    if (db.runningConversion || alreadyModifying) return;

    alreadyModifying = true;

    if (hasHub()) hub.modifyNow();
    record.modifyNow();

    alreadyModifying = false;
  }

  //---------------------------------------------------------------------------
  //---------------------------------------------------------------------------


  void resolvePointers() throws HDB_InternalError
  {
    if (HDT_Record.isEmptyThrowsException(getHub()))
      hub = null;

    mainText.resolvePointers();
  }

  //---------------------------------------------------------------------------
  //---------------------------------------------------------------------------

  void expire()
  {
    if (getType() == hdtHub) return;

    if (hasHub())
      hub.disuniteRecord(getType(), false);

    mainText.expire();
  }

  //---------------------------------------------------------------------------
  //---------------------------------------------------------------------------

  @Override public boolean equals(Object obj)
  {
    if (this == obj) return true;
    if (obj == null) return false;
    if (getClass() != obj.getClass()) return false;

    return record == ((Connector) obj).record;
  }

  //---------------------------------------------------------------------------
  //---------------------------------------------------------------------------

  void initFromHub(HDT_Hub hub)
  {
    this.hub = hub;

    db.replaceMainText(mainText, hub.getMainText());

    mainText = hub.getMainText();
  }

  //---------------------------------------------------------------------------
  //---------------------------------------------------------------------------

}
