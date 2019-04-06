/*
 * Copyright 2015-2019 Jason Winning
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

import static org.hypernomicon.App.*;
import static org.hypernomicon.util.Util.*;
import static org.hypernomicon.util.Util.MessageDialogType.*;
import static org.hypernomicon.view.tabs.HyperTab.TabEnum.*;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

import org.hypernomicon.App;
import org.hypernomicon.model.records.HDT_Record;
import org.hypernomicon.model.records.HDT_Concept;
import org.hypernomicon.model.records.HDT_RecordType;
import org.hypernomicon.view.HyperView;
import org.hypernomicon.view.HyperView.TextViewInfo;
import org.hypernomicon.view.mainText.MainTextWrapper;
import org.hypernomicon.view.tabs.HyperTab.TabEnum;
import org.hypernomicon.view.wrappers.HyperTableRow;

import static org.hypernomicon.model.HyperDB.*;
import static org.hypernomicon.model.Exceptions.*;
import static org.hypernomicon.model.records.HDT_RecordType.*;

import javafx.fxml.FXMLLoader;
import javafx.scene.control.SplitPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TextField;

@SuppressWarnings("unused")
public abstract class HyperTab<HDT_RT extends HDT_Record, HDT_CT extends HDT_Record>
{
  public static enum TabEnum
  {
    personTab, institutionTab, workTab,  miscFileTab, debateTab, positionTab, argumentTab,
    noteTab,   termTab,        queryTab, treeTab,     omniTab,   listTab
  }

  private static final EnumMap<TabEnum, HyperTab<? extends HDT_Record, ? extends HDT_Record>> enumToHyperTab = new EnumMap<>(TabEnum.class);
  private static final Map<Tab, HyperTab<? extends HDT_Record, ? extends HDT_Record>> tabToHyperTab = new HashMap<>();
  private Tab tab;
  private HyperView<HDT_CT> view = null;

  protected TabEnum tabEnum;

//---------------------------------------------------------------------------

  abstract void init(TabEnum tabEnum);
  abstract void focusOnSearchKey();
  abstract HDT_RecordType getType();

  public abstract String getRecordName();
  public abstract boolean update();
  public abstract void clear();
  public abstract boolean saveToRecord(boolean showMessage);
  public abstract void enable(boolean enabled);
  public abstract void setDividerPositions();
  public abstract void getDividerPositions();
  public abstract void setRecord(HDT_CT record);
  public abstract void findWithinDesc(String text);

  public TextViewInfo getMainTextInfo()       { return new TextViewInfo(); }
  public MainTextWrapper getMainTextWrapper() { return null; }
  public void rescale()                       { return; }
  public int getRecordCount()                 { return db.records(getType()).size(); }
  public final int getActiveID()              { return nullSwitch(activeRecord(), -1, HDT_Record::getID); }
  public final HyperView<HDT_CT> getView()    { return view; }
  public final HDT_CT viewRecord()            { return getView().getViewRecord(); }
  public final Tab getTab()                   { return tab; }
  public final TabEnum getTabEnum()           { return tabEnum; }

  public void newClick(HDT_RecordType objType, HyperTableRow row) { }

  public static void forEachHyperTab(Consumer<HyperTab<? extends HDT_Record, ? extends HDT_Record>> action) { enumToHyperTab.values().forEach(action); }

//---------------------------------------------------------------------------
//---------------------------------------------------------------------------

  public static void addHyperTab(TabEnum tabEnum, Tab tab, String ctrlrFilename) throws IOException
  {
    FXMLLoader loader = new FXMLLoader(App.class.getResource("view/tabs/" + ctrlrFilename));
    tab.setContent(loader.load());
    HyperTab.class.cast(loader.getController()).baseInit(tabEnum, tab);
  }

//---------------------------------------------------------------------------
//---------------------------------------------------------------------------

  void baseInit(TabEnum tabEnum, Tab tab) throws IOException
  {
    this.tab = tab;
    init(tabEnum);

    enumToHyperTab.put(tabEnum, this);
    tabToHyperTab.put(tab, this);

    if ((tabEnum != treeTab) && (tabEnum != queryTab))
      ui.addSelectorTab(tabEnum);
  }

//---------------------------------------------------------------------------
//---------------------------------------------------------------------------

  protected final boolean saveSearchKey(HDT_Record record, TextField tfSearchKey, boolean showMessage)
  {
    try
    {
      record.setSearchKey(tfSearchKey.getText());
    }
    catch (SearchKeyException e)
    {
      if (showMessage)
      {
        if (e.getTooShort())
          messageDialog("Unable to modify record: search key must be at least 3 characters.", mtError);
        else
          messageDialog("Unable to modify record: search key already exists.", mtError);
      }

      focusOnSearchKey();
      return false;
    }

    return true;
  }

//---------------------------------------------------------------------------
//---------------------------------------------------------------------------

  public static final HDT_RecordType getRecordTypeByTabEnum(TabEnum tabEnum)
  {
    return nullSwitch(enumToHyperTab.get(tabEnum), hdtNone, HyperTab::getType);
  }

//---------------------------------------------------------------------------
//---------------------------------------------------------------------------

  @SuppressWarnings("unchecked")
  public static final <HDT_RT extends HDT_Record,
                       HDT_CT extends HDT_Record,
                       HyperTabType extends HyperTab<HDT_RT, HDT_CT>>

    HyperTabType getHyperTab(TabEnum tabEnum) { return (HyperTabType) enumToHyperTab.get(tabEnum); }

//---------------------------------------------------------------------------
//---------------------------------------------------------------------------

  public static final TabEnum getTabEnumByRecordType(HDT_RecordType recordType)
  {
    switch (recordType)
    {
      case hdtTerm        :
      case hdtConcept     : return termTab;

      case hdtInstitution : return institutionTab;
      case hdtDebate      : return debateTab;
      case hdtPosition    : return positionTab;
      case hdtArgument    : return argumentTab;
      case hdtWork        : return workTab;
      case hdtMiscFile    : return miscFileTab;
      case hdtNote        : return noteTab;
      case hdtWorkLabel   : return treeTab;

      default             : return personTab;
    }
  }

//---------------------------------------------------------------------------
//---------------------------------------------------------------------------

  public static final <HDT_CT extends HDT_Record> HyperTab<? extends HDT_Record, HDT_CT> setTabView(HyperView<HDT_CT> hyperView)
  {
    HyperTab<? extends HDT_Record, HDT_CT> hyperTab = hyperView.getHyperTab();
    hyperTab.setView(hyperView);
    return hyperTab;
  }

//---------------------------------------------------------------------------
//---------------------------------------------------------------------------

  public static final HyperTab<? extends HDT_Record, ? extends HDT_Record> getHyperTabByTab(Tab tab)
  {
    return tabToHyperTab.get(tab);
  }

//---------------------------------------------------------------------------
//---------------------------------------------------------------------------

  private final void setView(HyperView<HDT_CT> hyperView)
  {
    view = hyperView;
    setRecord(view.getViewRecord());
  }

//---------------------------------------------------------------------------
//---------------------------------------------------------------------------

  @SuppressWarnings("unchecked")
  public HDT_RT activeRecord()
  {
    HDT_CT viewRecord = view.getViewRecord();

    if (viewRecord == null) return null;

    switch (viewRecord.getType())
    {
      case hdtConcept :
        return (HDT_RT) HDT_Concept.class.cast(viewRecord).term.get();

      default :
        return (HDT_RT) viewRecord;
    }
  }

//---------------------------------------------------------------------------
//---------------------------------------------------------------------------

  public int getRecordNdx()
  {
    if ((getRecordCount() < 1) || (activeRecord() == null))
      return -1;

    HDT_RT record = activeRecord();
    return db.records(record.getType()).getKeyNdxByID(record.getID());
  }

//---------------------------------------------------------------------------
//---------------------------------------------------------------------------

  public void refresh()
  {
    nullSwitch(getView(), view ->
    {
      view.refresh();
      nullSwitch(view.getViewRecord(), this::setRecord);
    });
  }

//---------------------------------------------------------------------------
//---------------------------------------------------------------------------

  private void clearTabAndView()
  {
    setView(new HyperView<>(tabEnum, null));
    clear();
  }

//---------------------------------------------------------------------------
//---------------------------------------------------------------------------

  public static void clearAllTabsAndViews()
  {
    enumToHyperTab.values().forEach(HyperTab::clearTabAndView);
    previewWindow.clearAll();
    fileManagerDlg.clearHistory();
  }

//---------------------------------------------------------------------------
//---------------------------------------------------------------------------

}
