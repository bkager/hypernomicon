/*
 * Copyright 2015-2021 Jason Winning
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

import org.hypernomicon.HyperTask;
import org.hypernomicon.HyperTask.HyperThread;

import static org.hypernomicon.App.*;
import static org.hypernomicon.util.Util.*;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

import javafx.fxml.FXML;

//---------------------------------------------------------------------------
//---------------------------------------------------------------------------

public class InternetCheckDlgCtrlr extends HyperDlg
{
  private HyperTask task;

  @Override protected boolean isValid() { return false; }

//---------------------------------------------------------------------------
//---------------------------------------------------------------------------

  public static boolean check()
  {
    return ((InternetCheckDlgCtrlr) create("InternetCheckDlg", appTitle, true)).checkInternet();
  }
  
//---------------------------------------------------------------------------
//---------------------------------------------------------------------------

  @FXML private void btnSkip()
  {
    okClicked = true;
    task.cancel();
    dialogStage.close();
  }

//---------------------------------------------------------------------------
//---------------------------------------------------------------------------

  private boolean checkInternet()
  {
    okClicked = false;

    task = new HyperTask("CheckForInternet") { @Override protected Boolean call() throws Exception
    {
      HttpURLConnection con;
      try
      {
        con = (HttpURLConnection) new URL("https://www.google.com/").openConnection();
        con.connect();

        if (con.getResponseCode() == HttpURLConnection.HTTP_OK)
        {
          succeeded();
          okClicked = true;
          return true;
        }

      } catch (IOException e) { noOp(); }

      failed();
      return false;
    }};

    onShown = () ->
    {
      task.setOnSucceeded(event -> getStage().close());

      HyperThread thread = new HyperThread(task);
      task.setThread(thread);
      thread.start();
    };

    dialogStage.setOnHiding(event ->
    {
      if (task.isRunning())
        task.cancel();
    });

    showModal();

    return okClicked;
  }

//---------------------------------------------------------------------------
//---------------------------------------------------------------------------

  @FXML @Override protected void btnCancelClick()
  {
    task.cancel();
    super.btnCancelClick();
  }

//---------------------------------------------------------------------------
//---------------------------------------------------------------------------

}
