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

package org.hypernomicon.model;

import static org.hypernomicon.model.HyperDB.*;

import org.hypernomicon.model.records.HDT_Record;
import org.hypernomicon.model.records.RecordState;
import org.hypernomicon.model.records.RecordType;

@SuppressWarnings("serial")
public final class Exceptions
{
  private Exceptions() { throw new UnsupportedOperationException(); }

  public static class InvalidItemException extends HyperDataException
  {
    public InvalidItemException(int recordID, RecordType recordType, String itemName)
    {
      super("Invalid item tag: \"" + itemName + "\". Record type: " + Tag.getTypeTagStr(recordType) + " ID: " + recordID);
    }
  }

//---------------------------------------------------------------------------
//---------------------------------------------------------------------------

  public static class CancelledTaskException extends Exception
  {
    public CancelledTaskException() { super("Task was cancelled by user."); }
  }

//---------------------------------------------------------------------------
//---------------------------------------------------------------------------

  public static class DuplicateRecordException extends HyperDataException
  {
    DuplicateRecordException(int id, RecordType type)
    {
      super("Duplicate record: type = " + Tag.getTypeTagStr(type) + ", ID = " + id);
    }
  }

//---------------------------------------------------------------------------
//---------------------------------------------------------------------------

  public static class SearchKeyException extends HyperDataException
  {
    private final boolean tooShort;

    SearchKeyException(boolean tooShort, HDT_Record record, String key)
    {
      super(tooShort ? "Search key: \"" + key + "\" is too short. Record type: " + getTypeName(record.getType()) + " ID: " + record.getID() :
                       "Duplicate search key: \"" + key + "\". Record type: " + getTypeName(record.getType()) + " ID: " + record.getID());

      this.tooShort = tooShort;
    }

    public boolean getTooShort() { return tooShort; }
  }

//---------------------------------------------------------------------------
//---------------------------------------------------------------------------

  /**
   * Exception intended to be thrown in {@link org.hypernomicon.model.records.HDT_Record#restoreTo(RecordState,boolean) HDT_Record.restoreTo}, when restoring data to a record from its backup record state.
   * Thrown when existing data in the record (like the record's hub, or a term record's concept records) conflicts with
   * data in the recordState.
   */
  public static class RestoreException extends HyperDataException { RestoreException(String msg) { super(msg); } }

  public static class ConceptChangedException extends RestoreException
  {
    public ConceptChangedException()
    {
      super("The set of concept records associated with this term record has changed.");
    }
  }

  public static class HubChangedException extends RestoreException
  {
    public HubChangedException(boolean formerlyDisunited)
    {
      super(formerlyDisunited ? "The record is now united with a record that it was not previously united with." :
                                "The record has been disunited from a record it was previously united with.");
    }
  }

//---------------------------------------------------------------------------
//---------------------------------------------------------------------------

  public static class RelationCycleException extends HyperDataException
  {
    public RelationCycleException(HDT_Record child, HDT_Record parent)
    {
      super("Unable to assign " + getTypeName(child.getType()) + " ID " + child.getID() + " as child of " +
            getTypeName(parent.getType()) + " ID " + parent.getID() + ": A cycle would result.");
    }
  }

//---------------------------------------------------------------------------
//---------------------------------------------------------------------------

  public static class HyperDataException extends Exception
  {
    public HyperDataException(String msg, Throwable e) { super(msg, e);            }
    public HyperDataException(Throwable e)             { super(e.getMessage(), e); }
    public HyperDataException(String msg)              { super(msg);               }
  }

//---------------------------------------------------------------------------
//---------------------------------------------------------------------------

  public static class HDB_InternalError extends HyperDataException
  {
    public HDB_InternalError(int newNum)
    {
      super("Internal error #" + newNum);
    }

    HDB_InternalError(int newNum, String msg)
    {
      super("Internal error #" + newNum + ": " + msg);
    }
  }

//---------------------------------------------------------------------------
//---------------------------------------------------------------------------

}
