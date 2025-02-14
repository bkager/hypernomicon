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

package org.hypernomicon.view.wrappers;

import static org.hypernomicon.App.*;
import static org.hypernomicon.model.HyperDB.*;
import static org.hypernomicon.model.records.RecordType.*;
import static org.hypernomicon.util.MediaUtil.*;
import static org.hypernomicon.util.UIUtil.*;
import static org.hypernomicon.view.wrappers.HyperTableColumn.CellSortMethod.*;

import java.util.EnumMap;
import java.util.function.Function;

import org.apache.commons.lang3.mutable.MutableBoolean;
import org.hypernomicon.model.records.HDT_Record;
import org.hypernomicon.model.records.RecordType;
import org.hypernomicon.model.records.HDT_Work;
import org.hypernomicon.view.populators.Populator;
import org.hypernomicon.view.wrappers.HyperTable.CellUpdateHandler;
import org.hypernomicon.view.wrappers.ButtonCell.ButtonCellHandler;
import org.hypernomicon.view.wrappers.ButtonCell.ButtonAction;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableColumn.SortType;

//---------------------------------------------------------------------------

public class HyperTableColumn
{
  public enum HyperCtrlType
  {
    ctNone,     ctIncremental, ctDropDownList, ctDropDown, ctEdit, ctUrlBtn,   ctBrowseBtn, ctGoBtn,
    ctGoNewBtn, ctEditNewBtn,  ctCustomBtn,    ctCheckbox, ctIcon, ctInvSelect
  }

  public enum CellSortMethod
  {
    smStandard, smTextSimple, smNumeric, smWork, smIcon
  }

  final private Populator populator;
  final private RecordType objType;
  final private HyperCtrlType ctrlType;
  final private TableColumn<HyperTableRow, ?> tc;
  final EnumMap<ButtonAction, String> tooltips = new EnumMap<>(ButtonAction.class);
  final CellUpdateHandler updateHandler;
  final private int colNdx;
  final private MutableBoolean canEditIfEmpty      = new MutableBoolean(true ),
                               dontCreateNewRecord = new MutableBoolean(false);

  final private ObjectProperty<CellSortMethod> sortMethod = new SimpleObjectProperty<>();

  public Function<HyperTableRow, String> textHndlr = null;

  Pos alignment = null;  // This is currently only respected by ReadOnlyCell

//---------------------------------------------------------------------------

  public HyperCtrlType getCtrlType()                 { return ctrlType; }
  public int getColNdx()                             { return colNdx; }
  public String getHeader()                          { return tc.getText(); }
  RecordType getObjType()                            { return objType; }
  void setCanEditIfEmpty(boolean newVal)             { canEditIfEmpty.setValue(newVal); }
  void setSortMethod(CellSortMethod newSM)           { sortMethod.setValue(newSM); }
  CellSortMethod getSortMethod()                     { return sortMethod.get(); }
  public void setDontCreateNewRecord(boolean newVal) { dontCreateNewRecord.setValue(newVal); }
  void setTooltip(ButtonAction ba, String text)      { tooltips.put(ba, text); }
  void clear()                                       { if (populator != null) populator.clear(); }

  @SuppressWarnings("unchecked") <PopType extends Populator> PopType getPopulator()     { return (PopType) populator; }

//---------------------------------------------------------------------------

  HyperTableColumn(HyperTable table, RecordType objType, HyperCtrlType ctrlType, Populator populator, int targetCol) {
    this(table, objType, ctrlType, populator, targetCol, null, null, null, null, null); }

  HyperTableColumn(HyperTable table, RecordType objType, HyperCtrlType ctrlType, Populator populator, int targetCol, EventHandler<ActionEvent> onAction) {
    this(table, objType, ctrlType, populator, targetCol, null, onAction, null, null, null); }

  HyperTableColumn(HyperTable table, RecordType objType, HyperCtrlType ctrlType, Populator populator, int targetCol, CellUpdateHandler updateHandler) {
    this(table, objType, ctrlType, populator, targetCol, null, null, updateHandler, null, null); }

  HyperTableColumn(HyperTable table, RecordType objType, HyperCtrlType ctrlType, Populator populator, int targetCol,
                   EventHandler<ActionEvent> onAction, CellUpdateHandler updateHandler) {
    this(table, objType, ctrlType, populator, targetCol, null, onAction, updateHandler, null, null); }

  HyperTableColumn(HyperTable table, RecordType objType, HyperCtrlType ctrlType, Populator populator, int targetCol, ButtonCellHandler btnHandler, String btnCaption) {
    this(table, objType, ctrlType, populator, targetCol, btnHandler, null, null, btnCaption, null); }

  HyperTableColumn(HyperTable table, RecordType objType, HyperCtrlType ctrlType, Populator populator, int targetCol,
                   Function<HyperTableRow, Node> graphicProvider) {
    this(table, objType, ctrlType, populator, targetCol, null, null, null, null, graphicProvider); }

//---------------------------------------------------------------------------
//---------------------------------------------------------------------------

  @SuppressWarnings("unchecked")
  private HyperTableColumn(HyperTable table, RecordType objType, HyperCtrlType ctrlType, Populator populator, int targetCol,
                           ButtonCellHandler btnHandler, EventHandler<ActionEvent> onAction, CellUpdateHandler updateHandler, String btnCaption,
                           Function<HyperTableRow, Node> graphicProvider)
  {
    this.ctrlType = ctrlType;
    this.populator = populator;
    this.objType = objType;
    this.updateHandler = updateHandler;

    colNdx = table.getColumns().size();
    tc = table.getTV().getColumns().get(colNdx);

    tc.setComparator((obj1, obj2) ->
    {
      if ((obj1 == null) && (obj2 == null)) return 0;
      if (obj1 == null) return -1;
      if (obj2 == null) return 1;

      HyperTableCell cell1 = (HyperTableCell) obj1,
                     cell2 = (HyperTableCell) obj2;

      if (cell1.sortToBottom)
        return tc.getSortType() == SortType.ASCENDING ? 1 : -1;

      if (cell2.sortToBottom)
        return tc.getSortType() == SortType.ASCENDING ? -1 : 1;

      if (table.getCanAddRows())
      {
        HyperTableRow lastRow = table.getRows().get(table.getRows().size() - 1);

        if (lastRow.getCell(colNdx) == cell1)
          return tc.getSortType() == SortType.ASCENDING ? 1 : -1;

        if (lastRow.getCell(colNdx) == cell2)
          return tc.getSortType() == SortType.ASCENDING ? -1 : 1;
      }

      if (sortMethod.get() != null)
        return HyperTableCell.compareCells(cell1, cell2, sortMethod.get());

      if (objType == hdtWork)
        return HyperTableCell.compareCells(cell1, cell2, smWork);

      return cell1.compareTo(cell2);
    });

    TableColumn<HyperTableRow, HyperTableCell> htcCol = (TableColumn<HyperTableRow, HyperTableCell>) tc;

    switch (ctrlType)
    {
      case ctGoBtn : case ctGoNewBtn : case ctEditNewBtn : case ctBrowseBtn : case ctUrlBtn : case ctCustomBtn :

        htcCol.setCellFactory(tableCol -> new ButtonCell(ctrlType, table, this, targetCol, btnHandler, btnCaption));
        break;

      case ctEdit :

        htcCol.setEditable(true);
        htcCol.setCellValueFactory(cellData -> new SimpleObjectProperty<>(cellData.getValue().getCell(colNdx)));
        htcCol.setCellFactory(tableCol -> new TextFieldCell(table, canEditIfEmpty, sortMethod));

        htcCol.setOnEditCommit(event ->
          event.getRowValue().setCellValue(colNdx, event.getNewValue().getCopyWithID(event.getOldValue().getID()))); // preserve ID value

        break;

      case ctCheckbox :

        htcCol.setEditable(true);
        htcCol.setCellValueFactory(cellData -> new SimpleObjectProperty<>(cellData.getValue().getCell(colNdx)));
        htcCol.setCellFactory(tableCol -> new CheckboxCell(table));

        break;

      case ctNone : case ctIncremental :

        htcCol.setEditable(false);
        htcCol.setCellValueFactory(cellData -> new SimpleObjectProperty<>(cellData.getValue().getCell(colNdx)));
        htcCol.setCellFactory(tableCol -> new ReadOnlyCell(table, this, graphicProvider));

        break;

      case ctIcon :

        sortMethod.set(smIcon);

        htcCol.setEditable(false);
        htcCol.setCellValueFactory(cellData -> new SimpleObjectProperty<>(cellData.getValue().getCell(colNdx)));
        htcCol.setCellFactory(tableCol -> new TableCell<>()
        {
          @Override public void updateItem(HyperTableCell cell, boolean empty)
          {
            if (empty || (cell == null) || (getTableRow().getItem() == null))
            {
              super.updateItem(cell, empty);

              setText("");
              setGraphic(null);
              setTooltip(null);
              return;
            }

            HDT_Record record = HyperTableCell.getRecord(cell);
            RecordType type = HyperTableCell.getCellType(cell);

            String relPath = cell.getImgRelPath();

            super.updateItem(cell, empty);
            setGraphic(imgViewFromRelPath(relPath));

            if ((type == hdtWork) && (record != null))
            {
              HDT_Work work = (HDT_Work)record;

              if (work.workType.isNotNull())
              {
                setToolTip(this, work.workType.get().getCBText());
                return;
              }
            }

            setToolTip(this, getTypeName(type));
          }
        });

        break;

      case ctDropDownList : case ctDropDown :

        htcCol.setCellValueFactory(cellData -> new SimpleObjectProperty<>(cellData.getValue().getCell(colNdx)));
        htcCol.setCellFactory(tableCol -> new ComboBoxCell(table, ctrlType, populator, onAction, dontCreateNewRecord, textHndlr));
        htcCol.setOnEditStart(event -> populator.populate(event.getRowValue(), false));

        break;

      case ctInvSelect :

        htcCol.setCellValueFactory(cellData -> new SimpleObjectProperty<>(cellData.getValue().getCell(colNdx)));
        htcCol.setCellFactory(tableCol -> new TableCell<>()
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
