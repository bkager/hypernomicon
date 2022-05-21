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

package org.hypernomicon.tree;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import org.controlsfx.control.BreadCrumbBar;
import org.controlsfx.control.MasterDetailPane;
import org.hypernomicon.App;
import org.hypernomicon.dialogs.RenameDlgCtrlr;
import org.hypernomicon.model.Exceptions.RelationCycleException;
import org.hypernomicon.model.records.*;
import org.hypernomicon.model.records.SimpleRecordTypes.HDT_RecordWithDescription;
import org.hypernomicon.model.relations.RelationSet.RelationType;
import org.hypernomicon.view.HyperView.TextViewInfo;
import org.hypernomicon.view.MainCtrlr;
import org.hypernomicon.view.mainText.MainTextUtil;
import org.hypernomicon.view.mainText.MainTextWrapper;
import org.hypernomicon.view.tabs.HyperTab;
import org.hypernomicon.view.tabs.PositionTab;
import org.hypernomicon.view.wrappers.HyperTable;
import org.hypernomicon.view.wrappers.MenuItemSchema;

import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.SetMultimap;

import static org.hypernomicon.App.*;
import static org.hypernomicon.Const.*;
import static org.hypernomicon.dialogs.RenameDlgCtrlr.NameType.*;
import static org.hypernomicon.model.HyperDB.*;
import static org.hypernomicon.model.records.RecordType.*;
import static org.hypernomicon.model.relations.RelationSet.RelationType.*;
import static org.hypernomicon.previewWindow.PreviewWindow.PreviewSource.*;
import static org.hypernomicon.util.UIUtil.*;
import static org.hypernomicon.util.UIUtil.MessageDialogType.*;
import static org.hypernomicon.util.Util.*;

import javafx.application.Platform;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.concurrent.Worker;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.CheckBox;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeTableView;
import javafx.scene.web.WebView;

//---------------------------------------------------------------------------

public class TreeTabCtrlr extends HyperTab<HDT_Record, HDT_Record>
{
  @FXML private BreadCrumbBar<TreeRow> bcbPath;
  @FXML private MasterDetailPane spMain;
  @FXML private CheckBox chkShowDesc;
  @FXML private WebView webView;

  final private SetMultimap<RecordType, MenuItemSchema<? extends HDT_Record, TreeRow>> recordTypeToSchemas = LinkedHashMultimap.create();
  private TreeTableView<TreeRow> ttv;
  private boolean useViewInfo = false;
  private boolean loaded = false;
  private String lastTextHilited = "";
  String textToHilite = "";
  private TreeWrapper tree;

  @Override protected RecordType type()           { return hdtNone; }
  @Override public void clear()                   { tree.clear(); }
  @Override public boolean saveToRecord()         { return true; }
  @Override public void setRecord(HDT_Record rec) { return; }
  @Override public HDT_Record activeRecord()      { return tree.selectedRecord(); }
  @Override public HDT_Record viewRecord()        { return activeRecord(); }
  @Override public String recordName()            { return nullSwitch(activeRecord(), "", HDT_Record::getCBText); }
  @Override public TextViewInfo mainTextInfo()    { return new TextViewInfo(MainTextUtil.webEngineScrollPos(webView.getEngine())); }
  @Override public void setDividerPositions()     { return; }
  @Override public void getDividerPositions()     { return; }

  public TreeWrapper getTree() { return tree; }

//---------------------------------------------------------------------------
//---------------------------------------------------------------------------

  @Override public void update()
  {
    if (db.isLoaded() == false)
    {
      tree.clear();
      return;
    }

    ttv.getColumns().forEach(col ->
    {
      if (col.isVisible() == false)
        return;

      col.setVisible(false);
      col.setVisible(true);
    });

    tree.sort();
  }

//---------------------------------------------------------------------------
//---------------------------------------------------------------------------

  @Override protected void init()
  {
    tree = new TreeWrapper(bcbPath, true, ui.cbTreeGoTo);

    initTTV();

    spMain.showDetailNodeProperty().bind(chkShowDesc.selectedProperty());

    tree.addContextMenuItem("Select", HDT_Record.class,
      record -> (ui.treeSelector.getBase() != null) && (record != null) && db.isLoaded(),
      record -> ui.treeSelector.select(record, true));

    tree.addContextMenuItem("Go to this record", HDT_Record.class,
      record -> (record != null) && db.isLoaded(),
      record -> ui.goToRecord(record, false));

    tree.addContextMenuItem("Choose parent to assign", HDT_Record.class,
      record ->
      {
        if ((db.isLoaded() == false) || (record == null)) return false;
        return (record.getType() != hdtConcept) && (record.getType() != hdtGlossary);
      },
      TreeTabCtrlr::chooseParent);

    tree.addContextMenuItem("Detach from this parent",
      row -> tree.canDetach(row, false),
      row -> tree.canDetach(row, true));

    tree.addContextMenuItem("Rename...", HDT_WorkLabel.class,
      label -> db.isLoaded(),
      TreeTabCtrlr::renameRecord);

    tree.addContextMenuItem("Rename...", HDT_Glossary.class,
      glossary -> db.isLoaded(),
      TreeTabCtrlr::renameRecord);

    addCreateNewSchema(tree.addContextMenuItem("Create new sub-label under this label", HDT_WorkLabel.class,
      label -> db.isLoaded(),
      this::createLabel));

    addCreateNewSchema(tree.addContextMenuItem("Create new position under this debate", HDT_Debate.class,
      debate -> db.isLoaded(),
      debate -> createChild(debate, rtParentDebateOfPos)));

    addCreateNewSchema(tree.addContextMenuItem("Create new debate under this position", HDT_Position.class,
      pos -> db.isLoaded(),
      pos -> createChild(pos, rtParentPosOfDebate)));

    addCreateNewSchema(tree.addContextMenuItem("Create new sub-debate under this debate", HDT_Debate.class,
      debate -> db.isLoaded(),
      debate -> createChild(debate, rtParentDebateOfDebate)));

    addCreateNewSchema(tree.addContextMenuItem("Create new argument for/against this position", HDT_Position.class,
      pos -> db.isLoaded(),
      PositionTab::newArgumentClick));

    addCreateNewSchema(tree.addContextMenuItem("Create new position under this position", HDT_Position.class,
      pos -> db.isLoaded(),
      pos -> createChild(pos, rtParentPosOfPos)));

    addCreateNewSchema(tree.addContextMenuItem("Create new counterargument to this argument", HDT_Argument.class,
      arg -> db.isLoaded(),
      arg -> MainCtrlr.argumentHyperTab().newCounterargumentClick(arg)));

    addCreateNewSchema(tree.addContextMenuItem("Create new note under this note", HDT_Note.class,
      note -> db.isLoaded(),
      note -> createChild(note, rtParentNoteOfNote)));

    addCreateNewSchema(tree.addContextMenuItem("Create new term in this glossary", HDT_Glossary.class,
      glossary -> db.isLoaded(),
      TreeTabCtrlr::createTerm));

    addCreateNewSchema(tree.addContextMenuItem("Create new glossary under this glossary", HDT_Glossary.class,
      glossary -> db.isLoaded(),
      this::createGlossary));

    addCreateNewSchema(tree.addContextMenuItem("Create new term in this glossary under this term", HDT_Concept.class,
      concept -> db.isLoaded(),
      TreeTabCtrlr::createSubTerm));

    tree.addDefaultMenuItems();

    webView.getEngine().titleProperty().addListener((ob, oldValue, newValue) ->
    {
      textToHilite = lastTextHilited;
      String mainText = "";

      HDT_Record record = tree.selectedRecord();
      if (record == null) return;

      if (record.hasDesc())
        mainText = ((HDT_RecordWithDescription) record).getDesc().getHtml();

      MainTextUtil.handleJSEvent(MainTextUtil.prepHtmlForDisplay(mainText), webView.getEngine(), new TextViewInfo());
    });

    webView.setOnContextMenuRequested(event -> setHTMLContextMenu());

    webView.getEngine().getLoadWorker().stateProperty().addListener((ob, oldState, newState) ->
    {
      if (newState == Worker.State.SUCCEEDED)
      {
        if (textToHilite.length() > 0)
          MainTextWrapper.hiliteText(textToHilite, webView.getEngine());

        lastTextHilited = textToHilite;
        textToHilite = "";
      }
    });

    MainTextUtil.webViewAddZoom(webView, PREF_KEY_TREETAB_ZOOM);

  //---------------------------------------------------------------------------
  //
  // Tree Update Handlers
  //
  //---------------------------------------------------------------------------

  // NOTE: There is some code (like RecordTreeEdge constructor) that assumes that
  //       if the subject and object type is the same, then the child record is the
  //       subject record, so there could be bugs if a non-forward relation is added
  //       where the subject and object type is the same

    db.addDeleteHandler(tree::removeRecord);

    TreeModel<TreeRow> debateTree = tree.debateTree,
                       termTree   = tree.termTree,
                       labelTree  = tree.labelTree,
                       noteTree   = tree.noteTree;

    noteTree  .addKeyWorkRelation(hdtNote     , true);
    termTree  .addKeyWorkRelation(hdtConcept  , true);
    debateTree.addKeyWorkRelation(hdtDebate   , true);
    debateTree.addKeyWorkRelation(hdtPosition , true);
    labelTree .addKeyWorkRelation(hdtWorkLabel, true);

    debateTree.addParentChildRelation(rtParentDebateOfDebate, true);
    debateTree.addParentChildRelation(rtParentDebateOfPos   , true);
    debateTree.addParentChildRelation(rtParentPosOfDebate   , true);
    debateTree.addParentChildRelation(rtParentPosOfPos      , true);
    debateTree.addParentChildRelation(rtPositionOfArgument  , true);
    debateTree.addParentChildRelation(rtCounterOfArgument   , true);
    debateTree.addParentChildRelation(rtWorkOfArgument      , false);

    noteTree.addParentChildRelation(rtParentNoteOfNote, true);

    labelTree.addParentChildRelation(rtParentLabelOfLabel, true);
    labelTree.addParentChildRelation(rtWorkOfArgument    , true);

    termTree.addParentChildRelation(rtParentGlossaryOfGlossary, true);
    termTree.addGlossaryOfConceptRelation();
    termTree.addConceptParentChildRelation();

    List.of(debateTree, noteTree, labelTree, termTree).forEach(treeModel ->
    {
      treeModel.addParentChildRelation(rtParentWorkOfWork, true);
      treeModel.addParentChildRelation(rtWorkOfMiscFile  , true);
    });

    db.addCloseDBHandler(this::initTTV);
    db.addDBLoadedHandler(() -> { loaded = true; });
  }

//---------------------------------------------------------------------------
//---------------------------------------------------------------------------

  private void initTTV()
  {
    if (ttv != null)
    {
      TreeItem<TreeRow> root = ttv.getRoot();

      if ((loaded == false) && (root != null) && root.getChildren().stream().filter(Objects::nonNull).allMatch(TreeItem::isLeaf))
      {
        tree.reset(ttv, false, false);
        return;
      }

      HyperTable.saveColWidthsForTable(ttv.getColumns(), PREF_KEY_HT_TREE, true);
      removeFromParent(ttv.getParent());

      loaded = false;
    }

    // There is a memory leak in TreeTableView such that it never releases references to values of TreeItems even if they are
    // removed from the tree. So anytime the tree is cleared, we need to release the TTV reference and start from scratch with
    // a new one.

    FXMLLoader loader = new FXMLLoader(App.class.getResource("tree/Tree.fxml"));
    try { spMain.setMasterNode(loader.load()); } catch (Exception e) { noOp(); }
    TreeCtrlr treeCtrlr = (TreeCtrlr) loader.getController();

    ttv = treeCtrlr.ttv;

    treeCtrlr.tcName.setCellValueFactory(row -> new SimpleObjectProperty<>(row.getValue().getValue().getNameCell()));
    treeCtrlr.tcDesc.setCellValueFactory(row -> new SimpleStringProperty(row.getValue().getValue().getDescString()));

    treeCtrlr.tcLinked.setCellValueFactory(row -> new SimpleObjectProperty<>(row.getValue().getValue()));
    treeCtrlr.tcLinked.setCellFactory(row -> TreeRow.typeCellFactory());

    ttv.getSelectionModel().selectedItemProperty().addListener((ob, oldValue, newValue) ->
    {
      boolean clearWV = true, clearPreview = true;

      tree.setBreadCrumb(newValue);

      if (newValue != null)
      {
        ui.updateBottomPanel(true);

        TreeRow row = newValue.getValue();

        HDT_Record record = row.getRecord();
        if (record != null)
        {
          switch (record.getType())
          {
            case hdtWorkLabel : case hdtGlossary :

              if (record.getID() > 1)
                record.viewNow();

              break;

            case hdtWork :

              HDT_Work work = (HDT_Work)record;
              previewWindow.setPreview(pvsTreeTab, work.filePathIncludeExt(), work.getStartPageNum(), work.getEndPageNum(), work);
              clearPreview = false;
              break;

            case hdtMiscFile :

              HDT_MiscFile miscFile = (HDT_MiscFile)record;
              previewWindow.setPreview(pvsTreeTab, miscFile.filePath(), miscFile);
              clearPreview = false;
              break;

            default : break;
          }

          String desc = record.hasDesc() ? ((HDT_RecordWithDescription)record).getDesc().getHtml() : "";

          MainTextWrapper.setReadOnlyHTML(desc, webView.getEngine(), useViewInfo ? getView().getTextInfo() : new TextViewInfo(), null);
          clearWV = false;
        }
      }

      if (clearWV && (ui.isShuttingDown() == false))
        webView.getEngine().loadContent("");

      if (clearPreview)
        previewWindow.clearPreview(pvsTreeTab);
    });

    HyperTable.loadColWidthsForTable(ttv.getColumns(), PREF_KEY_HT_TREE);

    tree.reset(ttv, false, true);
  }

//---------------------------------------------------------------------------
//---------------------------------------------------------------------------

  public List<MenuItem> getCreateMenuItems()
  {
    List<MenuItem> items = new ArrayList<>();
    TreeRow row = nullSwitch(tree.selectedItem(), null, TreeItem::getValue);
    if (row == null) return items;

    Set<MenuItemSchema<? extends HDT_Record, TreeRow>> schemas = recordTypeToSchemas.get(row.getRecordType());

    schemas.forEach(schema ->
    {
      if (schema.testWhetherToShow(row) == false) return;

      MenuItem menuItem = new MenuItem(schema.getCaption());
      menuItem.setOnAction(event -> schema.doAction(row));
      items.add(menuItem);
    });

    return items;
  }

//---------------------------------------------------------------------------
//---------------------------------------------------------------------------

  private void addCreateNewSchema(MenuItemSchema<? extends HDT_Record, TreeRow> schema)
  {
    recordTypeToSchemas.put(schema.recordType, schema);
  }

//---------------------------------------------------------------------------
//---------------------------------------------------------------------------

  private static void createChild(HDT_Record parent, RelationType relType)
  {
    HDT_Record child = db.createNewBlankRecord(db.getSubjType(relType));

    ui.treeSelector.attach(child, parent);
  }

//---------------------------------------------------------------------------
//---------------------------------------------------------------------------

  private static void createTerm(HDT_Glossary glossary)
  {
    HDT_Term term = HDT_Term.create(glossary);

    ui.goToRecord(term, false);
  }

//---------------------------------------------------------------------------
//---------------------------------------------------------------------------

  private static void createSubTerm(HDT_Concept parentConcept)
  {
    HDT_Glossary glossary = parentConcept.glossary.get();
    HDT_Term newTerm = HDT_Term.create(glossary);
    HDT_Concept childConcept = newTerm.getConcept(glossary, null);

    try { childConcept.addParentConcept(parentConcept); } catch (RelationCycleException e) { noOp(); }

    ui.goToRecord(childConcept, false);
  }

//---------------------------------------------------------------------------
//---------------------------------------------------------------------------

  private void createGlossary(HDT_Glossary glossary)
  {
    RenameDlgCtrlr dlg = RenameDlgCtrlr.build("Glossary Name", ntRecord, "");

    if (dlg.showModal())
    {
      HDT_Glossary newGlossary = db.createNewBlankRecord(hdtGlossary);
      newGlossary.setActive(true);
      newGlossary.setName(dlg.getNewName());

      ui.treeSelector.attach(newGlossary, glossary);

      Platform.runLater(() -> { tree.sort(); tree.selectRecord(newGlossary, 0, false); });
    }
  }

//---------------------------------------------------------------------------
//---------------------------------------------------------------------------

  private void createLabel(HDT_WorkLabel label)
  {
    RenameDlgCtrlr dlg = RenameDlgCtrlr.build("Label Name", ntRecord, "");

    if (dlg.showModal())
    {
      HDT_WorkLabel newLabel = db.createNewBlankRecord(hdtWorkLabel);
      newLabel.setName(dlg.getNewName());
      newLabel.parentLabels.add(label);

      Platform.runLater(() -> { tree.sort(); tree.selectRecord(newLabel, 0, false); });
    }
  }

//---------------------------------------------------------------------------
//---------------------------------------------------------------------------

  private static void renameRecord(HDT_Record record)
  {
    String typeName = getTypeName(record.getType());

    if (isUnstoredRecord(record.getID(), record.getType()))
    {
      messageDialog("That " + typeName + " cannot be renamed.", mtError);
      return;
    }

    RenameDlgCtrlr dlg = RenameDlgCtrlr.build(typeName + " Name", ntRecord, record.name());

    if (dlg.showModal())
    {
      record.setName(dlg.getNewName());
      ui.update();
    }
  }

//---------------------------------------------------------------------------
//---------------------------------------------------------------------------

  private static void chooseParent(HDT_Record child)
  {
    ChooseParentDlgCtrlr dlg = ChooseParentDlgCtrlr.build(child);

    if (dlg.showModal() == false) return;

    new RecordTreeEdge(dlg.getParent(), child).attach(null, true);

    Platform.runLater(() -> ui.update());
  }

//---------------------------------------------------------------------------
//---------------------------------------------------------------------------

  @Override public void findWithinDesc(String text)
  {
    if (tree.selectedRecord() != null)
      MainTextWrapper.hiliteText(text, webView.getEngine());
  }

//---------------------------------------------------------------------------
//---------------------------------------------------------------------------

  @Override public int recordCount()
  {
    return nullSwitch(activeRecord(), 0, ar -> tree.getRowsForRecord(ar).size());
  }

//---------------------------------------------------------------------------
//---------------------------------------------------------------------------

  @Override public int recordNdx()
  {
    return nullSwitch(nullSwitch(tree.selectedItem(), null, TreeItem::getValue), -1, row -> tree.getRowsForRecord(row.getRecord()).indexOf(row));
  }

//---------------------------------------------------------------------------
//---------------------------------------------------------------------------

  public void selectRecord(HDT_Record record, boolean useViewInfo)
  {
    if ((record != null) && HDT_Record.isEmpty(record)) return; // Record was probably just deleted; go with whatever is currently selected

    this.useViewInfo = useViewInfo;
    tree.selectRecord(record, record == null ? 0 : record.keyNdx(), false);
    this.useViewInfo = false;
  }

  //---------------------------------------------------------------------------
  //---------------------------------------------------------------------------

}
