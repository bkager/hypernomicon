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

package org.hypernomicon.view.wrappers;

import static org.hypernomicon.App.*;
import static org.hypernomicon.model.HyperDB.*;
import static org.hypernomicon.view.wrappers.HyperTableColumn.HyperCtrlType.*;
import static org.hypernomicon.model.records.RecordType.*;
import static org.hypernomicon.util.Util.*;
import static org.hypernomicon.util.MediaUtil.*;
import static org.hypernomicon.util.UIUtil.*;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.function.Function;

import org.apache.commons.lang3.mutable.MutableBoolean;
import org.hypernomicon.model.records.HDT_Record;
import org.hypernomicon.model.records.RecordType;
import org.hypernomicon.model.records.HDT_Work;
import org.hypernomicon.view.populators.Populator;
import org.hypernomicon.view.wrappers.HyperTable.CellUpdateHandler;
import org.hypernomicon.view.wrappers.ButtonCell.ButtonCellHandler;
import org.hypernomicon.view.wrappers.ButtonCell.ButtonAction;

import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;

//---------------------------------------------------------------------------

public class HyperTableColumn
{
  public static enum HyperCtrlType
  {
    ctNone,     ctIncremental, ctDropDownList, ctDropDown, ctEdit, ctUrlBtn,   ctBrowseBtn, ctGoBtn,
    ctGoNewBtn, ctEditNewBtn,  ctCustomBtn,    ctCheckbox, ctIcon, ctInvSelect
  }

  final private TableColumn<HyperTableRow, HyperTableCell> tc;
  final private TableColumn<HyperTableRow, Boolean> chkCol;
  final private Populator populator;
  final private RecordType objType;
  final private HyperCtrlType ctrlType;
  final EnumMap<ButtonAction, String> tooltips = new EnumMap<>(ButtonAction.class);
  final CellUpdateHandler updateHandler;
  final private int colNdx;
  final private MutableBoolean canEditIfEmpty      = new MutableBoolean(true ),
                               isNumeric           = new MutableBoolean(false),
                               dontCreateNewRecord = new MutableBoolean(false);

  public Function<HyperTableRow, String> textHndlr = null;

//---------------------------------------------------------------------------

  public HyperCtrlType getCtrlType()                 { return ctrlType; }
  public int getColNdx()                             { return colNdx; }
  public String getHeader()                          { return tc.getText(); }
  RecordType getObjType()                            { return objType; }
  void setCanEditIfEmpty(boolean newVal)             { canEditIfEmpty.setValue(newVal); }
  void setNumeric(boolean newVal)                    { isNumeric.setValue(newVal); }
  public void setDontCreateNewRecord(boolean newVal) { dontCreateNewRecord.setValue(newVal); }
  void setTooltip(ButtonAction ba, String text)      { tooltips.put(ba, text); }
  void clear()                                       { if (populator != null) populator.clear(); }

  @SuppressWarnings("unchecked") <PopType extends Populator> PopType getPopulator()     { return (PopType) populator; }

//---------------------------------------------------------------------------

  HyperTableColumn(HyperTable table, RecordType objType, HyperCtrlType ctrlType, Populator populator, int targetCol) {
    this(table, objType, ctrlType, populator, targetCol, null, null, null, null); }

  HyperTableColumn(HyperTable table, RecordType objType, HyperCtrlType ctrlType, Populator populator, int targetCol, EventHandler<ActionEvent> onAction) {
    this(table, objType, ctrlType, populator, targetCol, null, onAction, null, null); }

  HyperTableColumn(HyperTable table, RecordType objType, HyperCtrlType ctrlType, Populator populator, int targetCol, CellUpdateHandler updateHandler) {
    this(table, objType, ctrlType, populator, targetCol, null, null, updateHandler, null); }

  HyperTableColumn(HyperTable table, RecordType objType, HyperCtrlType ctrlType, Populator populator, int targetCol,
                          EventHandler<ActionEvent> onAction, CellUpdateHandler updateHandler) {
    this(table, objType, ctrlType, populator, targetCol, null, onAction, updateHandler, null); }

  HyperTableColumn(HyperTable table, RecordType objType, HyperCtrlType ctrlType, Populator populator, int targetCol, ButtonCellHandler btnHandler, String btnCaption) {
    this(table, objType, ctrlType, populator, targetCol, btnHandler, null, null, btnCaption); }

//---------------------------------------------------------------------------
//---------------------------------------------------------------------------

  @SuppressWarnings("unchecked")
  private HyperTableColumn(HyperTable table, RecordType objType, HyperCtrlType ctrlType, Populator populator, int targetCol,
                           ButtonCellHandler btnHandler, EventHandler<ActionEvent> onAction, CellUpdateHandler updateHandler, String btnCaption)
  {
    this.ctrlType = ctrlType;
    this.populator = populator;
    this.objType = objType;
    this.updateHandler = updateHandler;

    colNdx = table.getColumns().size();

    if (ctrlType == ctCheckbox)
    {
      chkCol = (TableColumn<HyperTableRow, Boolean>) table.getTV().getColumns().get(colNdx);
      tc = null;
    }
    else
    {
      tc = (TableColumn<HyperTableRow, HyperTableCell>) table.getTV().getColumns().get(colNdx);
      chkCol = null;
    }

    switch (ctrlType)
    {
      case ctGoBtn : case ctGoNewBtn : case ctEditNewBtn : case ctBrowseBtn : case ctUrlBtn : case ctCustomBtn :

        tc.setCellFactory(tableCol -> new ButtonCell(ctrlType, table, this, targetCol, btnHandler, btnCaption));
        break;

      case ctEdit :

        tc.setEditable(true);
        tc.setCellValueFactory(cellDataFeatures -> new SimpleObjectProperty<>(cellDataFeatures.getValue().getCell(colNdx)));
        tc.setCellFactory(tableCol -> new TextFieldCell(table, canEditIfEmpty, isNumeric));

        tc.setOnEditCommit(event ->
        {
          HyperTableCell newCell = event.getNewValue().getCopyWithID(event.getOldValue().getID()); // preserve ID value
          event.getRowValue().setCellValue(colNdx, newCell);
        });

        break;

      case ctCheckbox :

        chkCol.setEditable(true);
        chkCol.setCellValueFactory(cellData ->
        {
          HyperTableCell cell = cellData.getValue().getCell(colNdx);
          int id = HyperTableCell.getCellID(cell);

          return new SimpleBooleanProperty(id == 1);
        });

        chkCol.setCellFactory(tableCol -> new CheckboxCell(table));

        break;

      case ctNone :

        tc.setEditable(false);
        tc.setCellValueFactory(cellData -> new SimpleObjectProperty<>(cellData.getValue().getCell(colNdx)));
        tc.setCellFactory(tableCol -> new ReadOnlyCell(table, false));

        break;

      case ctIcon :

        tc.setEditable(false);
        tc.setCellValueFactory(cellData -> new SimpleObjectProperty<>(cellData.getValue().getCell(colNdx)));
        tc.setCellFactory(tableCol -> new TableCell<>()
        {
          @Override public void updateItem(HyperTableCell cell, boolean empty)
          {
            super.updateItem(cell, empty);

            setText("");

            if (empty || (cell == null) || (getTableRow().getItem() == null)) { setGraphic(null); setTooltip(null); return; }

            HDT_Record record = HyperTableCell.getRecord(cell);
            RecordType type = HyperTableCell.getCellType(cell);

            setGraphic(record == null ? imgViewForRecordType(type) : imgViewForRecord(record));

            if ((type == hdtWork) && (record != null))
            {
              HDT_Work work = (HDT_Work)record;

              if (work.workType.isNotNull())
              {
                setToolTip(this, work.workType.get().getCBText());
                return;
              }
            }

            setToolTip(this, db.getTypeName(type));
          }
        });

        break;

      case ctIncremental :

        tc.setEditable(false);
        tc.setCellValueFactory(cellData -> new SimpleObjectProperty<>(cellData.getValue().getCell(colNdx)));
        tc.setCellFactory(tableCol -> new ReadOnlyCell(table, true));

        break;

      case ctDropDownList : case ctDropDown :

        tc.setCellValueFactory(cellData -> new SimpleObjectProperty<>(cellData.getValue().getCell(colNdx)));
        tc.setCellFactory(tableCol -> new ComboBoxCell(table, ctrlType, populator, onAction, dontCreateNewRecord, textHndlr));
        tc.setOnEditStart(event -> populator.populate(event.getRowValue(), false));

        break;

      case ctInvSelect :

        tc.setCellValueFactory(cellData -> new SimpleObjectProperty<>(cellData.getValue().getCell(colNdx)));
        tc.setCellFactory(tableCol -> new TableCell<>()
        {
          @Override public void startEdit()
          {
            super.startEdit();
            super.cancelEdit();

            ui.personHyperTab().showInvSelectDialog(getTableRow().getItem());
          }

          @Override public void updateItem(HyperTableCell item, boolean empty)
          {
            super.updateItem(item, empty);

            setText(empty ? null : HyperTableCell.getCellText(getItem()));
          }
        });

        break;

      default :
        break;
    }
  }

//---------------------------------------------------------------------------
//---------------------------------------------------------------------------

  List<HyperTableCell> getSelectedItems()
  {
    List<HyperTableCell> choices = new ArrayList<>();

    tc.getTableView().getItems().forEach(row ->
      nullSwitch(row.getRecord(colNdx), (HDT_Record record) -> choices.add(new HyperTableCell(record, record.getCBText()))));

    choices.add(HyperTableCell.blankCell);

    return choices;
  }

//---------------------------------------------------------------------------
//---------------------------------------------------------------------------

  static Button makeButton(TableCell<HyperTableRow, HyperTableCell> tableCell)
  {
    Button cellButton = new Button();

    setHeights(cellButton, 18.0 * displayScale);
    cellButton.setPadding(new Insets(0.0, 7.0, 0.0, 7.0));

    tableCell.emptyProperty().addListener((ob, oldValue, newValue) -> cellButton.setVisible(Boolean.FALSE.equals(newValue)));

    return cellButton;
  }

//---------------------------------------------------------------------------
//---------------------------------------------------------------------------

}
