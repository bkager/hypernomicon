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

package org.hypernomicon.bib.zotero;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;

import org.hypernomicon.bib.BibEntry;
import org.hypernomicon.bib.LibraryWrapper;
import org.hypernomicon.bib.BibManager.RelatedBibEntry;
import org.hypernomicon.bib.authors.BibAuthor;
import org.hypernomicon.bib.authors.BibAuthor.AuthorType;
import org.hypernomicon.bib.data.BibField.BibFieldEnum;
import org.hypernomicon.bib.reports.ReportGenerator;
import org.hypernomicon.model.items.PersonName;
import org.hypernomicon.bib.authors.BibAuthors;
import org.hypernomicon.bib.authors.WorkBibAuthors;
import org.hypernomicon.bib.data.BibField;
import org.hypernomicon.bib.data.EntryType;
import org.hypernomicon.util.json.JsonArray;
import org.hypernomicon.util.json.JsonArray.JsonObjIterator;
import org.hypernomicon.util.json.JsonObj;

import com.google.common.collect.Lists;

import static org.hypernomicon.Const.*;
import static org.hypernomicon.bib.data.BibField.BibFieldEnum.*;
import static org.hypernomicon.bib.data.EntryType.*;
import static org.hypernomicon.util.UIUtil.*;
import static org.hypernomicon.util.UIUtil.MessageDialogType.*;
import static org.hypernomicon.util.Util.*;

public class ZoteroItem extends BibEntry implements ZoteroEntity
{
  private final ZoteroWrapper zWrapper;
  private JsonObj jData;

  public ZoteroItem(ZoteroWrapper zWrapper, JsonObj jObj, boolean thisIsBackup)
  {
    super(thisIsBackup);

    update(jObj, false, false);
    this.zWrapper = zWrapper;
  }

//---------------------------------------------------------------------------
//---------------------------------------------------------------------------

  public ZoteroItem(ZoteroWrapper zWrapper, EntryType newType)
  {
    super(false);

    jObj = new JsonObj();
    jData = ZoteroWrapper.getTemplate(newType).clone();
    jData.put(getFieldKey(bfEntryType), ZoteroWrapper.entryTypeMap.getOrDefault(newType, ""));
    jObj.put("data", jData);
    this.zWrapper = zWrapper;

    jObj.put("key", "_!_" + randomAlphanumericStr(12));
  }

//---------------------------------------------------------------------------
//---------------------------------------------------------------------------

  @Override public String toString()        { return jObj.toString(); }
  @Override public String getKey()          { return jObj.getStr("key"); }
  @Override public long getVersion()        { return jObj.getLong("version", 0); }
  @Override protected boolean isNewEntry()  { return jObj.containsKey("version") == false; }
  @Override public BibAuthors getAuthors()  { return linkedToWork() ? new WorkBibAuthors(getWork()) : new ZoteroAuthors(jData.getArray("creators"), getEntryType()); }
  @Override public EntryType getEntryType() { return parseZoteroType(jData.getStrSafe(getFieldKey(bfEntryType))); }

  @Override public LibraryWrapper<?, ?> getLibrary() { return zWrapper; }
  static EntryType parseZoteroType(String zType)     { return ZoteroWrapper.entryTypeMap.inverse().getOrDefault(zType, etOther); }

//---------------------------------------------------------------------------
//---------------------------------------------------------------------------

  @Override public String getEntryURL()
  {
    return isNewEntry() ? "" : nullSwitch(jObj.getObj("links"), "", links ->
                               nullSwitch(links.getObj("alternate"), "", alt -> alt.getStrSafe("href")));
  }

//---------------------------------------------------------------------------
//---------------------------------------------------------------------------

  @Override public void update(JsonObj jObj, boolean updatingExistingDataFromServer, boolean preMerge)
  {
    this.jObj = jObj;
    jData = jObj.getObj("data");

    if (thisIsBackup)
    {
      jObj.remove("backupItem");
      return;
    }

    JsonObj jBackupObj;

    if (jObj.containsKey("backupItem"))
    {
      jBackupObj = jObj.getObj("backupItem");
      jObj.remove("backupItem");
    }
    else
      jBackupObj = jObj.clone();

    backupItem = new ZoteroItem(zWrapper, jBackupObj, true);

    if ((updatingExistingDataFromServer == false) || (linkedToWork() == false)) return;

    setMultiStr(bfTitle, backupItem.getMultiStr(bfTitle));
    setMultiStr(bfISBNs, backupItem.getMultiStr(bfISBNs));
    setMultiStr(bfMisc, backupItem.getMultiStr(bfMisc));
    setStr(bfDOI, backupItem.getStr(bfDOI));
    setStr(bfYear, backupItem.getStr(bfYear));

    String url = getStr(bfURL);
    if (url.startsWith(EXT_1) == false)
      setStr(bfURL, backupItem.getStr(bfURL));

    if (preMerge) return; // authors always get updated during merge

    if (authorsChanged() == false) return;

    zWrapper.doMerge(this, jBackupObj);
  }

//---------------------------------------------------------------------------
//---------------------------------------------------------------------------

  @Override protected List<String> getCollKeys(boolean deletedOK)
  {
    JsonArray collArray = jObj.getObj("data").getArray("collections");

    return (collArray != null) && ((zWrapper.getTrash().contains(this) == false) || deletedOK) ?
      Lists.newArrayList((Iterable<String>)collArray.getStrs())
    :
      new ArrayList<>();
  }

//---------------------------------------------------------------------------
//---------------------------------------------------------------------------

  @Override protected void setEntryType(EntryType entryType)
  {
    if (entryType == getEntryType()) return;

    // jData.put("itemType", ZoteroWrapper.entryTypeMap.getOrDefault(entryType, ""));

    throw new UnsupportedOperationException("change Zotero entry type");
  }

//---------------------------------------------------------------------------
//---------------------------------------------------------------------------

  @Override public void setStr(BibFieldEnum bibFieldEnum, String newStr)
  {
    if (linkedToWork())
    {
      switch (bibFieldEnum)
      {
        case bfYear : getWork().setYear(newStr); return;
        case bfDOI  : getWork().setDOI (newStr); return;
        case bfURL  : getWork().setURL (newStr); return;
        default     : break;
      }
    }

    switch (bibFieldEnum)
    {
      case bfDOI : case bfYear : case bfURL : case bfISBNs : case bfMisc : case bfTitle : break;
      default :

        if (thisTypeHasFieldKey(bibFieldEnum) == false) return;
    }

    String fieldKey = getFieldKey(bibFieldEnum);

    switch (bibFieldEnum)
    {
      case bfYear :

        newStr = safeStr(newStr);
        if (newStr.matches("[12]\\d\\d\\d") == false) break;

        String oldDate = jData.getStrSafe(fieldKey);
        String oldYear = extractYear(oldDate);
        if (oldYear.isEmpty()) break;

        if (oldYear.equals(newStr)) return;

        jData.put(fieldKey, oldDate.replaceFirst("[12]\\d\\d\\d", newStr)); // Leave all parts of the date other than the year intact
        return;

      case bfDOI       : case bfURL    : case bfVolume  : case bfIssue    : case bfPages :
      case bfPublisher : case bfPubLoc : case bfEdition : case bfLanguage :

        break;

      default : messageDialog("Internal error #90225", mtError); return;
    }

    if (jData.getStrSafe(fieldKey).equals(safeStr(newStr))) return;

    jData.put(fieldKey, newStr);
  }

//---------------------------------------------------------------------------
//---------------------------------------------------------------------------

  private static final List<String> titleKeyList = List.of(

      "publicationTitle", "bookTitle"   , "encyclopediaTitle", "proceedingsTitle", "dictionaryTitle",
      "forumTitle",       "programTitle", "websiteTitle"     , "blogTitle"       , "seriesTitle");

  private String getFieldKey(BibFieldEnum bibFieldEnum)
  {
    switch (bibFieldEnum)
    {
      case bfEntryType : return "itemType";
      case bfDOI       : return "DOI";
      case bfURL       : return "url";
      case bfVolume    : return "volume";
      case bfIssue     : return "issue";
      case bfPages     : return "pages";
      case bfPublisher : return "publisher";
      case bfPubLoc    : return "place";
      case bfEdition   : return "edition";
      case bfLanguage  : return "language";
      case bfYear      : return "date";

      case bfContainerTitle :

        JsonObj template = ZoteroWrapper.getTemplate(getEntryType());
        return template == null ? "" : safeStr(findFirst(titleKeyList, template::containsKey));

      case bfISBNs : return "ISBN";
      case bfISSNs : return "ISSN";

      case bfTitle : return "title";
      case bfMisc  : return "extra";

      case bfAuthors: case bfEditors: case bfTranslators:

        return "creators";

      case bfWorkType: return "";
    }

    return "";
  }

//---------------------------------------------------------------------------
//---------------------------------------------------------------------------

  private boolean thisTypeHasFieldKey(BibFieldEnum bibFieldEnum)
  {
    JsonObj template = ZoteroWrapper.getTemplate(getEntryType());
    if (template == null) return false;

    String fieldKey = getFieldKey(bibFieldEnum);
    if (safeStr(fieldKey).isEmpty()) return false;

    return template.containsKey(fieldKey);
  }

//---------------------------------------------------------------------------
//---------------------------------------------------------------------------

  @Override public String getStr(BibFieldEnum bibFieldEnum)
  {
    if (linkedToWork())
    {
      switch (bibFieldEnum)
      {
        case bfDOI   : return getWork().getDOI();
        case bfYear  : return getWork().getYear();
        case bfURL   : return getWork().getURL();
        case bfTitle : return getWork().name();
        default      : break;
      }
    }

    String fieldKey = getFieldKey(bibFieldEnum);

    switch (bibFieldEnum)
    {
      case bfEntryType : return getEntryType().getUserFriendlyName();

      case bfDOI       : case bfURL       : case bfVolume    : case bfIssue     : case bfPages :
      case bfPublisher : case bfPubLoc    : case bfEdition   : case bfLanguage  :

        return jData.getStrSafe(fieldKey);

      case bfYear : return extractYear(jData.getStrSafe(fieldKey));

      case bfAuthors     : return getAuthors().getStr(AuthorType.author);
      case bfEditors     : return getAuthors().getStr(AuthorType.editor);
      case bfTranslators : return getAuthors().getStr(AuthorType.translator);

      case bfContainerTitle : case bfTitle :

        return BibField.buildTitle(getMultiStr(bibFieldEnum));

      case bfMisc : return strListToStr(getMultiStr(bibFieldEnum), false, true);

      default:
        break;
    }

    return "";
  }

//---------------------------------------------------------------------------
//---------------------------------------------------------------------------

  @Override public void setMultiStr(BibFieldEnum bibFieldEnum, List<String> list)
  {
    if (linkedToWork())
    {
      switch (bibFieldEnum)
      {
        case bfTitle : getWork().setName(BibField.buildTitle(list));   return;
        case bfISBNs : getWork().setISBNs(list);                       return;
        case bfMisc  : getWork().setMiscBib(strListToStr(list, true)); return;
        default      : break;
      }
    }

    switch (bibFieldEnum)
    {
      case bfDOI : case bfYear : case bfURL : case bfISBNs : case bfMisc : case bfTitle : break;
      default :

        if (thisTypeHasFieldKey(bibFieldEnum) == false) return;
    }

    String fieldKey = getFieldKey(bibFieldEnum), newStr = null;

    switch (bibFieldEnum)
    {
      case bfContainerTitle : case bfTitle : newStr = strListToStr(list, false      ); break;
      case bfMisc           :                newStr = strListToStr(list, true       ); break;
      case bfISBNs          : case bfISSNs : newStr = getMultiStrSpaceDelimited(list); break;
      default               : return;
    }

    if (jData.getStrSafe(fieldKey).equals(safeStr(newStr))) return;

    jData.put(fieldKey, newStr);
  }

//---------------------------------------------------------------------------
//---------------------------------------------------------------------------

  @Override public List<String> getMultiStr(BibFieldEnum bibFieldEnum)
  {
    if (linkedToWork())
    {
      switch (bibFieldEnum)
      {
        case bfTitle : return Collections.singletonList(getWork().name());
        case bfISBNs : return getWork().getISBNs();
        case bfMisc  : return convertMultiLineStrToStrList(getWork().getMiscBib(), true);
        default      : break;
      }
    }

    switch (bibFieldEnum)
    {
      case bfTitle : return convertMultiLineStrToStrList(jData.getStrSafe(getFieldKey(bibFieldEnum)), false);

      case bfContainerTitle :

        String                        containerTitle = jData.getStrSafe("publicationTitle");
        if (containerTitle.isEmpty()) containerTitle = jData.getStrSafe("bookTitle");
        if (containerTitle.isEmpty()) containerTitle = jData.getStrSafe("seriesTitle");

        return convertMultiLineStrToStrList(containerTitle, false);

      case bfMisc : return convertMultiLineStrToStrList(jData.getStrSafe(getFieldKey(bibFieldEnum)), true);

      case bfISBNs : return matchISBN(jData.getStrSafe(getFieldKey(bibFieldEnum)));

      case bfISSNs : return matchISSN(jData.getStrSafe(getFieldKey(bibFieldEnum)));

      default : return null;
    }
  }

//---------------------------------------------------------------------------
//---------------------------------------------------------------------------

  @Override public boolean isSynced()
  {
    if (isNewEntry()) return false;
    if (thisIsBackup) return true;
    if (authorsChanged()) return false;

    return Arrays.stream(BibFieldEnum.values()).allMatch(bibFieldEnum ->
      (thisTypeHasFieldKey(bibFieldEnum) == false) || fieldsAreEqual(bibFieldEnum, backupItem, true));
  }

//---------------------------------------------------------------------------
//---------------------------------------------------------------------------

  private boolean authorsChanged()
  {
    List<BibAuthor> authorList1     = new ArrayList<>(), authorList2     = new ArrayList<>(),
                    editorList1     = new ArrayList<>(), editorList2     = new ArrayList<>(),
                    translatorList1 = new ArrayList<>(), translatorList2 = new ArrayList<>();

    getAuthors().getLists(authorList1, editorList1, translatorList1);
    backupItem.getAuthors().getLists(authorList2, editorList2, translatorList2);

    EntryType entryType = getEntryType();

    // Backup item will not have authors of certain types if they are not allowed by Zotero for this entry type

    if (ZoteroAuthors.getCreatorTypeStr(entryType, AuthorType.author    ).isEmpty()) authorList1    .clear();
    if (ZoteroAuthors.getCreatorTypeStr(entryType, AuthorType.editor    ).isEmpty()) editorList1    .clear();
    if (ZoteroAuthors.getCreatorTypeStr(entryType, AuthorType.translator).isEmpty()) translatorList1.clear();

    if ((authorList1    .size() != authorList2    .size()) ||
        (editorList1    .size() != editorList2    .size()) ||
        (translatorList1.size() != translatorList2.size()))   return true;

    // Now check book editors

    if (((ZoteroAuthors) backupItem.getAuthors()).ignoreEditors())
    {
      JsonArray creatorsArr1 = jData.getArray("creators"),
                creatorsArr2 = ((ZoteroItem) backupItem).jData.getArray("creators");

      if ((creatorsArr1 == null) != (creatorsArr2 == null)) return true;
      if (creatorsArr1 != null)
      {
        JsonArray jArr1 = new JsonArray(), jArr2 = new JsonArray();

        creatorsArr1.getObjs().forEach(creator ->
        {
          String type = creator.getStrSafe("creatorType");
          if ("editor".equals(type) || "bookAuthor".equals(type))
            jArr1.add(creator);
        });

        creatorsArr2.getObjs().forEach(creator ->
        {
          String type = creator.getStrSafe("creatorType");
          if ("editor".equals(type) || "bookAuthor".equals(type))
            jArr2.add(creator);
        });

        if (jArr1.size() != jArr2.size()) return true;

        for (int ndx = 0; ndx < jArr1.size(); ndx++)
        {
          JsonObj ed1 = jArr1.getObj(ndx),
                  ed2 = jArr2.getObj(ndx);

          if (ed1.getStrSafe("creatorType").equals(ed2.getStrSafe("creatorType")) == false) return true;

          if (ed1.getStrSafe("firstName").equals(ed2.getStrSafe("firstName")) == false) return true;
          if (ed1.getStrSafe("lastName" ).equals(ed2.getStrSafe("lastName" )) == false) return true;
        }
      }
    }

    for (int ndx = 0; ndx < authorList1.size(); ndx++)
      if (authorList1.get(ndx).getName().equalsExceptParenthetical(authorList2.get(ndx).getName()) == false) return true;

    for (int ndx = 0; ndx < editorList1.size(); ndx++)
      if (editorList1.get(ndx).getName().equalsExceptParenthetical(editorList2.get(ndx).getName()) == false) return true;

    for (int ndx = 0; ndx < translatorList1.size(); ndx++)
      if (translatorList1.get(ndx).getName().equalsExceptParenthetical(translatorList2.get(ndx).getName()) == false) return true;

    return false;
  }

//---------------------------------------------------------------------------
//---------------------------------------------------------------------------

  JsonObj exportJsonObjForUploadToServer(boolean missingKeysOK)
  {
    JsonObj jServerObj = jObj.clone();

    EnumSet.allOf(BibFieldEnum.class).forEach(bibFieldEnum -> { switch (bibFieldEnum)
    {
      case bfDOI : case bfYear : case bfURL : case bfISBNs : case bfMisc : case bfTitle :
        if (missingKeysOK)
          break;

        // Else: Fall through

      default :

        if (thisTypeHasFieldKey(bibFieldEnum) == false)
        {
          String fieldKey = getFieldKey(bibFieldEnum);
          if (fieldKey.length() > 0)
            jServerObj.getObj("data").remove(getFieldKey(bibFieldEnum)); // This should be done even if missingKeysOK == true
        }

        break;
    }});

    if (linkedToWork())
    {
      ZoteroItem serverItem = new ZoteroItem(zWrapper, jServerObj, true);

      if (missingKeysOK || thisTypeHasFieldKey(bfDOI  )) serverItem.setStr(bfDOI, getStr(bfDOI));
      if (missingKeysOK || thisTypeHasFieldKey(bfYear )) serverItem.setStr(bfYear, getStr(bfYear));

      if (missingKeysOK || thisTypeHasFieldKey(bfURL  ))
      {
        String url = getStr(bfURL);
        if (url.startsWith(EXT_1) == false)
          serverItem.setStr(bfURL, url);
      }

      if (missingKeysOK || thisTypeHasFieldKey(bfISBNs)) serverItem.setMultiStr(bfISBNs, getMultiStr(bfISBNs));
      if (missingKeysOK || thisTypeHasFieldKey(bfMisc )) serverItem.setMultiStr(bfMisc, getMultiStr(bfMisc));
      if (missingKeysOK || thisTypeHasFieldKey(bfTitle)) serverItem.setTitle(getStr(bfTitle));

      BibAuthors serverAuthors = serverItem.getAuthors();
      serverAuthors.clear();

      getAuthors().forEach(serverAuthors::add);
    }

    return isNewEntry() ? jServerObj.getObj("data") : jServerObj;
  }

//---------------------------------------------------------------------------
//---------------------------------------------------------------------------

  @Override public void syncBookAuthorsTo(RelatedBibEntry relative)
  {
    ZoteroItem dest = (ZoteroItem) relative.entry;

    switch (relative.relation)
    {
      case Child:

        BibAuthors authors = getAuthors();

        List<BibAuthor> authorList     = new ArrayList<>(),
                        editorList     = new ArrayList<>(),
                        translatorList = new ArrayList<>();

        authors.getLists(authorList, editorList, translatorList);

        JsonArray creatorsArr = dest.jData.getArray("creators");

        JsonObjIterator it = creatorsArr.getObjs();
        while (it.hasNext())
        {
          JsonObj creator = it.next();
          String type = creator.getStrSafe("creatorType");
          if ("editor".equals(type) || "bookAuthor".equals(type))
            it.remove();
        }

        for (BibAuthor author : authorList)
        {
          JsonObj creatorObj = new JsonObj();

          creatorObj.put("firstName", removeAllParentheticals(author.getGiven()));
          creatorObj.put("lastName", author.getFamily());
          creatorObj.put("creatorType", "bookAuthor");

          creatorsArr.add(creatorObj);
        }

        for (BibAuthor editor : editorList)
        {
          JsonObj creatorObj = new JsonObj();

          creatorObj.put("firstName", removeAllParentheticals(editor.getGiven()));
          creatorObj.put("lastName", editor.getFamily());
          creatorObj.put("creatorType", "editor");

          creatorsArr.add(creatorObj);
        }

        break;

      case Parent:

        JsonObj jDestObj  = dest.exportJsonObjForUploadToServer(true),
                jDestData = nullSwitch(jDestObj.getObj("data"), jDestObj);

        JsonArray oldCreatorsArr = jDestData.getArray("creators"), newCreatorsArr = new JsonArray();

        it = oldCreatorsArr.getObjs();
        while (it.hasNext())
        {
          JsonObj creator = it.next();
          String type = creator.getStrSafe("creatorType");
          if ("editor".equals(type) || "author".equals(type))
            it.remove();
        }

        jData.getArray("creators").getObjs().forEach(oldCreator ->
        {
          String type = oldCreator.getStrSafe("creatorType");
          if ("editor".equals(type) || "bookAuthor".equals(type))
          {
            JsonObj newCreator = oldCreator.clone();
            if ("bookAuthor".equals(newCreator.getStrSafe("creatorType")))
              newCreator.put("creatorType", "book");

            newCreatorsArr.add(newCreator);
          }
        });

        oldCreatorsArr.getObjs().forEach(creator -> newCreatorsArr.add(creator.clone()));

        dest.getWork().getAuthors().setAll(new ZoteroAuthors(newCreatorsArr, dest.getEntryType()));

        break;

      case Sibling:

        JsonArray destCreatorsArr = dest.jData.getArray("creators");

        it = destCreatorsArr.getObjs();
        while (it.hasNext())
        {
          JsonObj creator = it.next();
          String type = creator.getStrSafe("creatorType");
          if ("editor".equals(type) || "bookAuthor".equals(type))
            it.remove();
        }

        JsonArray srcCreatorsArr = jData.getArray("creators");

        srcCreatorsArr.getObjs().forEach(creator ->
        {
          String type = creator.getStrSafe("creatorType");
          if ("editor".equals(type) || "bookAuthor".equals(type))
            destCreatorsArr.add(creator.clone());
        });

        break;
    }
  }

//---------------------------------------------------------------------------
//---------------------------------------------------------------------------

  @Override public void createReport(ReportGenerator report)
  {
    createReport(this, report);
  }

  private static void createReport(ZoteroItem item, ReportGenerator report)
  {
    JsonObj jObj  = item.exportJsonObjForUploadToServer(true),
            jData = nullSwitch(jObj.getObj("data"), jObj);

    jData.keySet().forEach(key ->
    {
      String fieldName = key;

      switch (fieldName)
      {
        case "relations" : case "collections" : case "key" :
        case "dateAdded" : case "accessDate"  : case "dateModified" : return;

        case "url"  : fieldName = "URL"; break;

        case "ISBN" : case "DOI" : case "ISSN" : break;

        default : fieldName = camelToTitle(fieldName); break;
      }

      switch (jData.getType(key))
      {
        case ARRAY:

          JsonArray jArr = jData.getArray(key);

          if ("creators".equals(key))
            report.addField("Creators", makeCreatorsReportContent(report, jArr));
          else if ("tags".equals(key))
            report.addField("Tags", makeTagsReportContent(report, jArr));
          else
            report.addField(fieldName, makeReportArray(report, fieldName, jArr));

          break;

        case STRING:

          report.addField(fieldName, makeReportString(report, fieldName, jData.getStrSafe(key)));
          break;

        default:
          break;
      }
    });
  }

//---------------------------------------------------------------------------
//---------------------------------------------------------------------------

  private static String makeReportString(ReportGenerator report, String fieldName, String str)
  {
    if (str.isEmpty()) return "";

    if ("Item Type".equals(fieldName))
      str = camelToTitle(str);

    if ("URL".equals(fieldName))
      str = report.getUrlContent(str);

    return report.makeRow(fieldName, str);
  }

//---------------------------------------------------------------------------
//---------------------------------------------------------------------------

  private static String makeTagsReportContent(ReportGenerator report, JsonArray tagsArr)
  {
    List<String> list = new ArrayList<>();

    tagsArr.getObjs().forEach(node -> list.add(node.getStrSafe("tag")));

    return report.makeRows("Tags", list);
  }

//---------------------------------------------------------------------------
//---------------------------------------------------------------------------

  private static String makeCreatorsReportContent(ReportGenerator report, JsonArray creatorsArr)
  {
    StringBuilder content = new StringBuilder();
    boolean foundAny = false;

    for (JsonObj node : creatorsArr.getObjs())
    {
      String type = node.getStrSafe("creatorType");

      if (type.isEmpty()) continue;

      type = camelToTitle(type);
      PersonName personName;
      String firstName = ultraTrim(node.getStrSafe("firstName")),
             lastName  = ultraTrim(node.getStrSafe("lastName" ));

      if ((firstName.length() > 0) || (lastName.length() > 0))
        personName = new PersonName(firstName, lastName);
      else
      {
        personName = new PersonName(node.getStrSafe("name"));
        if (personName.isEmpty()) continue;
      }

      if (foundAny)
        content.append(report.lineSeparator());
      else
        foundAny = true;

      content.append(report.makeRow(type, personName.getLastFirst()));
    }

    return content.toString();
  }

//---------------------------------------------------------------------------
//---------------------------------------------------------------------------

  private static String makeReportArray(ReportGenerator report, String fieldName, JsonArray jArr)
  {
    return report.makeRows(fieldName, Lists.newArrayList((Iterable<String>)jArr.getStrs()));
  }

//---------------------------------------------------------------------------
//---------------------------------------------------------------------------

  @Override public List<String> getReportFieldOrder()
  {
    return List.of(

      "Title",
      "Item Type",
      "Date",
      "Creators",
      "Publication Title",
      "Book Title",
      "Edition",
      "Volume",
      "Issue",
      "Pages",
      "Place",
      "Publisher");
  }

//---------------------------------------------------------------------------
//---------------------------------------------------------------------------

}
