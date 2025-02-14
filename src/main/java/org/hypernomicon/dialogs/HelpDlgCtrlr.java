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

package org.hypernomicon.dialogs;

import static org.hypernomicon.util.UIUtil.*;
import static org.hypernomicon.util.UIUtil.MessageDialogType.*;
import static org.hypernomicon.util.Util.*;

import java.io.IOException;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.web.WebView;

public class HelpDlgCtrlr extends HyperDlg
{
  @FXML private WebView webView;

  @Override protected boolean isValid() { return true; }

//---------------------------------------------------------------------------
//---------------------------------------------------------------------------

  public HelpDlgCtrlr()
  {
    super("HelpDlg", "Hypernomicon", true);

    webView.setOnContextMenuRequested(event -> setHTMLContextMenu());

    StringBuilder html = new StringBuilder();

    try
    {
      readResourceTextFile("resources/Shortcuts.html", html, true);
      webView.getEngine().loadContent(html.toString());
    }
    catch (IOException e)
    {
      onShown = () ->
      {
        messageDialog("Unable to show help content: " + e.getMessage(), mtError);
        Platform.runLater(dialogStage::close);
      };
    }
  }

//---------------------------------------------------------------------------
//---------------------------------------------------------------------------

}
