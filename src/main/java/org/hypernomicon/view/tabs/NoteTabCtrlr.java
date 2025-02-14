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

package org.hypernomicon.view.tabs;

import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Control;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SplitMenuButton;
import javafx.scene.control.Tab;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.stage.DirectoryChooser;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import org.hypernomicon.model.records.HDT_Record;
import org.hypernomicon.model.records.HDT_Folder;
import org.hypernomicon.model.records.HDT_Note;
import org.hypernomicon.dialogs.RenameDlgCtrlr;
import org.hypernomicon.model.items.HyperPath;
import org.hypernomicon.model.records.RecordType;
import org.hypernomicon.model.records.SimpleRecordTypes.HDT_RecordWithDescription;
import org.hypernomicon.model.unities.HDT_Hub;
import org.hypernomicon.model.unities.HDT_RecordWithMainText;
import org.hypernomicon.util.filePath.FilePath;
import org.hypernomicon.view.wrappers.HyperTable;
import org.hypernomicon.view.wrappers.HyperTableCell;
import org.hypernomicon.view.wrappers.HyperTableRow;

import static org.hypernomicon.App.*;
import static org.hypernomicon.model.HyperDB.*;
import static org.hypernomicon.Const.*;
import static org.hypernomicon.dialogs.RenameDlgCtrlr.NameType.*;
import static org.hypernomicon.model.records.RecordType.*;
import static org.hypernomicon.util.Util.*;
import static org.hypernomicon.util.DesktopUtil.*;
import static org.hypernomicon.util.UIUtil.*;
import static org.hypernomicon.util.UIUtil.MessageDialogType.*;
import static org.hypernomicon.view.tabs.HyperTab.TabEnum.*;
import static org.hypernomicon.view.wrappers.HyperTableColumn.HyperCtrlType.*;
import static org.hypernomicon.view.wrappers.HyperTableColumn.CellSortMethod.*;

//---------------------------------------------------------------------------

public final class NoteTabCtrlr extends HyperNodeTab<HDT_Note, HDT_Note>
{
  private final SplitMenuButton btnFolder = new SplitMenuButton();
  private final Button btnBrowse = new Button("..."), btnCreateFolder = new Button("Create Folder");
  private final TextField tfFolder = new TextField();
  private final BorderPane bp;
  private final HyperTable htParents, htSubnotes, htMentioners;

  private FilePath folderPath;
  private HDT_Note curNote;

//---------------------------------------------------------------------------

  public NoteTabCtrlr(Tab tab) throws IOException
  {
    super(noteTabEnum, tab);

    tvParents.getColumns().remove(2);

    tvLeftChildren .setColumnResizePolicy(TableView.UNCONSTRAINED_RESIZE_POLICY);
    tvRightChildren.setColumnResizePolicy(TableView.UNCONSTRAINED_RESIZE_POLICY);

    tvLeftChildren.getColumns().get(1).setText("Sub-Notes Under This Note");
    tvLeftChildren.getColumns().get(2).setText("Text");
    tvLeftChildren.getColumns().add(new TableColumn<HyperTableRow, HyperTableCell>("Folder"));

    tvRightChildren.getColumns().get(0).setText("Type");
    tvRightChildren.getColumns().get(1).setText("Name of Record Linking to This Note");
    tvRightChildren.getColumns().add(new TableColumn<HyperTableRow, HyperTableCell>("Description"));

    spMain.setDividerPosition(1, 0.8);

    btnFolder.setText("Folder:");
    addFolderMenuItem("Show in system explorer", event -> launchFile(folderPath));
    addFolderMenuItem("Show in file manager"   , event -> ui.goToFileInManager(folderPath));
    addFolderMenuItem("Copy path to clipboard" , event -> copyToClipboard(folderPath.toString()));
    addFolderMenuItem("Unassign folder"        , event ->
    {
      if (ui.cantSaveRecord()) return;
      curNote.folder.set(null);
      ui.update();
    });

    setToolTip(btnFolder, "Show folder in system explorer");
    setToolTip(btnCreateFolder, "Create a new folder and assign it to this note");

    BorderPane.setMargin(btnBrowse, new Insets(0, 2, 0, 0));
    BorderPane.setMargin(tfFolder , new Insets(0, 4, 0, 4));

    bp = new BorderPane();
    bp.setLeft(btnFolder);
    bp.setCenter(tfFolder);
    bp.setRight(btnBrowse);

    tfFolder.setEditable(false);

    GridPane.setColumnIndex(bp, 1);
    gpToolBar.getColumnConstraints().get(0).setMinWidth(560.0);
    gpToolBar.getColumnConstraints().get(0).setMaxWidth(560.0);
    gpToolBar.getColumnConstraints().get(0).setHgrow(javafx.scene.layout.Priority.NEVER);

    gpToolBar.getColumnConstraints().get(1).setMinWidth(Control.USE_COMPUTED_SIZE);
    gpToolBar.getColumnConstraints().get(1).setMaxWidth(Control.USE_COMPUTED_SIZE);
    gpToolBar.getColumnConstraints().get(1).setPrefWidth(Control.USE_COMPUTED_SIZE);
    gpToolBar.getColumnConstraints().get(1).setHgrow(javafx.scene.layout.Priority.ALWAYS);
    gpToolBar.getChildren().set(1, bp);

    htParents = new HyperTable(tvParents, 2, true, PREF_KEY_HT_NOTE_PARENTS);

    htParents.addActionCol(ctGoBtn, 2);
    htParents.addActionCol(ctBrowseBtn, 2);
    htParents.addCol(hdtNote, ctDropDownList);

    htParents.addRemoveMenuItem();
    htParents.addChangeOrderMenuItem(true);

    htSubnotes = new HyperTable(tvLeftChildren, 2, true, PREF_KEY_HT_NOTE_SUB);

    htSubnotes.addActionCol(ctGoNewBtn, 2);
    htSubnotes.addLabelCol(hdtNote);
    htSubnotes.addLabelCol(hdtNote, smTextSimple);
    htSubnotes.addLabelCol(hdtNote, smTextSimple);

    htMentioners = new HyperTable(tvRightChildren, 1, false, PREF_KEY_HT_NOTE_MENTIONERS);

    htMentioners.addIconCol();
    htMentioners.addLabelCol(hdtNone);
    htMentioners.addLabelCol(hdtNone);

    db.addMentionsNdxCompleteHandler(this::updateMentioners);

    btnFolder      .setOnAction(event -> launchFile(folderPath));
    btnCreateFolder.setOnAction(event -> createFolder());
    btnBrowse      .setOnAction(event -> browseClick());

    htMentioners.addDefaultMenuItems();
    htSubnotes  .addDefaultMenuItems();

    htSubnotes.addContextMenuItem("Go to subnote", HDT_Note.class,
      note -> ui.goToRecord(note, true));
  }

//---------------------------------------------------------------------------

  @Override protected RecordType type()             { return hdtNote; }
  @Override protected void setRecord(HDT_Note note) { curNote = note; }
  @Override protected HDT_Note getNodeRecord()      { return curNote; }

//---------------------------------------------------------------------------
//---------------------------------------------------------------------------

  @Override public void updateFromRecord()
  {
    super.updateFromRecord();

    tfFolder.setText(curNote.getFolderStr());

    bp.setLeft(curNote.folder.isNull() ? btnCreateFolder : btnFolder);

    bp.setDisable(isUnstoredRecord(curNote));

    folderPath = curNote.filePath();

    htParents.buildRows(curNote.parentNotes, (row, otherNote) -> row.setCellValue(2, otherNote, otherNote.name()));

    htSubnotes.buildRows(curNote.subNotes, (row, subNote) ->
    {
      row.setCellValue(1, subNote, subNote.name());
      row.setCellValue(2, subNote, subNote.getMainText().getPlainForDisplay());
      row.setCellValue(3, subNote, subNote.getFolderStr());
    });

    htSubnotes.sortAscending(1);

    updateMentioners();
  }

//---------------------------------------------------------------------------
//---------------------------------------------------------------------------

  private void updateMentioners()
  {
    htMentioners.clear();

    if ((db.isLoaded() == false) || (curNote == null)) return;

    if (db.reindexingMentioners())
    {
      htMentioners.newDataRow().setCellValue(1, "(Indexing in progress)", hdtNone);
      return;
    }

    Set<HDT_Record> mentioners = removeDupMentioners(db.getMentionerSet(curNote, true));

    htMentioners.buildRows(mentioners, (row, mentioner) ->
    {
      row.setCellValue(0, mentioner, "");
      row.setCellValue(1, mentioner, mentioner.getCBText());

      if (mentioner.hasDesc())
        row.setCellValue(2, mentioner, ((HDT_RecordWithDescription) mentioner).getDesc().getPlainForDisplay());
    });
  }

//---------------------------------------------------------------------------
//---------------------------------------------------------------------------

  private Set<HDT_Record> removeDupMentioners(Iterable<HDT_Record> mentioners)
  {
    Set<HDT_Record> output = new HashSet<>();
    Set<HDT_Hub> usedHubs = new HashSet<>();
    HDT_Hub thisHub = curNote.getHub();

    mentioners.forEach(mentioner ->
    {
      if (mentioner.equals(curNote)) return;

      HDT_Hub hub = ((HDT_RecordWithMainText) mentioner).getHub();

      if (hub == null)
      {
        output.add(mentioner);
        return;
      }

      if ((hub == thisHub) || usedHubs.contains(hub)) return;

      usedHubs.add(hub);

      output.add(hub.mainSpoke());
    });

    return output;
  }

//---------------------------------------------------------------------------
//---------------------------------------------------------------------------

  private void addFolderMenuItem(String text, EventHandler<ActionEvent> handler)
  {
    MenuItem menuItem = new MenuItem(text);
    menuItem.setOnAction(handler);
    btnFolder.getItems().add(menuItem);
  }

//---------------------------------------------------------------------------
//---------------------------------------------------------------------------

  private void createFolder()
  {
    HDT_Folder parentFolder = HyperPath.getFolderFromFilePath(getParentForNewFolder(), true);

    String folderName = FilePath.removeInvalidFileNameChars(recordName());

    if (folderName.isBlank())
      folderName = FilePath.removeInvalidFileNameChars(curNote.name());

    if (folderName.isBlank())
    {
      RenameDlgCtrlr dlg = new RenameDlgCtrlr("Create Folder in: " + parentFolder.filePath(), ntFolder, "");

      if (dlg.showModal() == false) return;

      folderName = dlg.getNewName();
    }

    nullSwitch(parentFolder.createSubfolder(folderName), this::assignFolder);
  }

//---------------------------------------------------------------------------
//---------------------------------------------------------------------------

  private FilePath getParentForNewFolder()
  {
    HDT_Folder folder = curNote.getDefaultFolder();

    return (folder != null) && folder.filePath().exists() ? folder.filePath() : db.topicalPath();
  }

//---------------------------------------------------------------------------
//---------------------------------------------------------------------------

  private void browseClick()
  {
    if (ui.cantSaveRecord()) return;

    DirectoryChooser dirChooser = new DirectoryChooser();

    dirChooser.setInitialDirectory(FilePath.isEmpty(folderPath) || (folderPath.exists() == false) ?
      getParentForNewFolder().toFile()
    :
      folderPath.toFile());

    dirChooser.setTitle("Select Folder");

    FilePath filePath = ui.windows.showDirDialog(dirChooser, ui.getStage());

    if (FilePath.isEmpty(filePath)) return;

    HDT_Folder folder = HyperPath.getFolderFromFilePath(filePath, true);

    if (folder == null)
      messageDialog("You must choose a subfolder of the main database folder.", mtError);
    else
      assignFolder(folder);
  }

//---------------------------------------------------------------------------
//---------------------------------------------------------------------------

  public void assignFolder(HDT_Folder folder)
  {
    boolean noOtherNotes = folder.notes.isEmpty();

    curNote.folder.set(folder == db.getTopicalFolder() ? null : folder);

    ui.update();

    if (noOtherNotes && (folder != db.getTopicalFolder()) && nameCtrl().getText().isBlank())
      nameCtrl().setText(folder.getPath().getNameStr());
  }

//---------------------------------------------------------------------------
//---------------------------------------------------------------------------

  @Override public void clear(boolean resetRecord)
  {
    setHeights(btnFolder      , 24.0 * displayScale);
    setHeights(tfFolder       , 24.0 * displayScale);
    setHeights(btnCreateFolder, 24.0 * displayScale);
    setHeights(btnBrowse      , 24.0 * displayScale);

    super       .clear(resetRecord);
    htParents   .clear();
    htSubnotes  .clear();
    htMentioners.clear();
    tfFolder    .clear();

    curNote = resetRecord ? null : HDT_Record.getCurrentInstance(curNote);

    if (curNote == null)
      bp.setLeft(btnFolder);
  }

//---------------------------------------------------------------------------
//---------------------------------------------------------------------------

  @Override public boolean saveToRecord()
  {
    if (super.saveToRecord() == false)
      return false;

    if (curNote.setParentNotes(htParents.saveToList(2, hdtNote)) == false)
      return false;

    db.attachOrphansToRoots();

    return true;
  }

//---------------------------------------------------------------------------
//---------------------------------------------------------------------------

  @Override public void newClick(RecordType objType, HyperTableRow row)
  {
    if (ui.cantSaveRecord()) return;

    switch (objType)
    {
      case hdtNote :

        HDT_Note subNote = db.createNewBlankRecord(hdtNote);
        subNote.parentNotes.add(curNote);
        ui.goToRecord(subNote, false);
        break;

      default:
        break;
    }
  }

//---------------------------------------------------------------------------
//---------------------------------------------------------------------------

  @Override public void setDividerPositions()
  {
    setDividerPosition(spMain, PREF_KEY_NOTE_TOP_VERT        , 0);
    setDividerPosition(spMain, PREF_KEY_NOTE_BOTTOM_VERT     , 1);
    setDividerPosition(spChildren, PREF_KEY_NOTE_BOTTOM_HORIZ, 0);
  }

//---------------------------------------------------------------------------
//---------------------------------------------------------------------------

  @Override public void getDividerPositions()
  {
    getDividerPosition(spMain, PREF_KEY_NOTE_TOP_VERT        , 0);
    getDividerPosition(spMain, PREF_KEY_NOTE_BOTTOM_VERT     , 1);
    getDividerPosition(spChildren, PREF_KEY_NOTE_BOTTOM_HORIZ, 0);
  }

//---------------------------------------------------------------------------
//---------------------------------------------------------------------------

}
