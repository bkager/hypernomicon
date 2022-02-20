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

import static org.hypernomicon.util.UIUtil.*;

import org.hypernomicon.dialogs.HyperDlg;

import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.TextField;

public class NewQueryFavDlgCtrlr extends HyperDlg
{
  @FXML private TextField tfName;
  @FXML private CheckBox chkAutoExec;

//---------------------------------------------------------------------------
//---------------------------------------------------------------------------

  String getNewName()   { return tfName.getText(); }
  boolean getAutoExec() { return chkAutoExec.isSelected(); }

//---------------------------------------------------------------------------
//---------------------------------------------------------------------------

  static NewQueryFavDlgCtrlr build(String newName)
  {
    return ((NewQueryFavDlgCtrlr) createUsingFullPath("query/NewQueryFavDlg", "Add Query Favorite", true)).init(newName);
  }

//---------------------------------------------------------------------------
//---------------------------------------------------------------------------

  private NewQueryFavDlgCtrlr init(String newName)
  {
    tfName.setText(newName);

    onShown = () -> safeFocus(tfName);

    return this;
  }

//---------------------------------------------------------------------------
//---------------------------------------------------------------------------

  @Override protected boolean isValid()
  {
    return tfName.getText().length() > 0 ? true : falseWithErrorMessage("Name cannot be zero-length.", tfName);
  }

//---------------------------------------------------------------------------
//---------------------------------------------------------------------------

}
