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

package org.hypernomicon.settings;

import static org.hypernomicon.model.HyperDB.db;
import static org.hypernomicon.util.Util.*;
import static org.hypernomicon.util.Util.MessageDialogType.*;

import org.hypernomicon.HyperTask.HyperThread;
import org.hypernomicon.bib.LibraryWrapper.SyncTask;
import org.hypernomicon.dialogs.HyperDlg;
import org.hypernomicon.model.Exceptions.HyperDataException;

import javafx.concurrent.Worker.State;
import javafx.fxml.FXML;
import javafx.scene.control.ProgressBar;

public class SyncBibDlgCtrlr extends HyperDlg
{
  @FXML private ProgressBar progressBar;

  private SyncTask syncTask = null;

  @Override protected boolean isValid() { return true; }

//---------------------------------------------------------------------------
//---------------------------------------------------------------------------

  static SyncBibDlgCtrlr build()
  {
    return createUsingFullPath("settings/SyncBibDlg", "Link to " + db.getBibLibrary().type().getUserFriendlyName(), true);
  }

//---------------------------------------------------------------------------
//---------------------------------------------------------------------------

  boolean sync()
  {
    onShown = () ->
    {
      syncTask = db.getBibLibrary().createNewSyncTask();

      syncTask.runningProperty().addListener((ob, wasRunning, isRunning) ->
      {
        if (wasRunning && Boolean.FALSE.equals(isRunning))
        {
          if ((syncTask.getState() == State.FAILED) || (syncTask.getState() == State.CANCELLED))
          {
            Throwable ex = syncTask.getException();

            if (ex instanceof HyperDataException)
              messageDialog(ex.getMessage(), mtError);
          }

          getStage().close();
        }
      });

      HyperThread thread = new HyperThread(syncTask);
      thread.setDaemon(true);
      syncTask.setThread(thread);
      thread.start();
    };

    dialogStage.setOnHiding(event ->
    {
      if ((syncTask != null) && syncTask.isRunning())
        syncTask.cancel();

      db.getBibLibrary().stop();
    });

    showModal();

    return okClicked;
  }

//---------------------------------------------------------------------------
//---------------------------------------------------------------------------

}
