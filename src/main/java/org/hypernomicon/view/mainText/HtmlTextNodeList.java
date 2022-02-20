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

package org.hypernomicon.view.mainText;

import static org.hypernomicon.util.UIUtil.*;
import static org.hypernomicon.util.UIUtil.MessageDialogType.*;
import static org.hypernomicon.util.Util.*;
import static org.hypernomicon.view.mainText.MainTextUtil.*;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.mutable.MutableInt;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;

public class HtmlTextNodeList
{

//---------------------------------------------------------------------------
//---------------------------------------------------------------------------

  static class HtmlTextNode
  {
    private int startNdx, endNdx;
    private String text;
    private TextNode textNode;

  //---------------------------------------------------------------------------

    public HtmlTextNode(String text, TextNode textNode, int startNdx)
    {
      this.text = text;
      this.textNode = textNode;
      this.startNdx = startNdx;
      endNdx = startNdx + text.length();
    }

    public String getText()       { return text; }
    public int getStartNdx()      { return startNdx; }
    public TextNode getTextNode() { return textNode; }

  //---------------------------------------------------------------------------

    TextNode updateStartNdx(int newStartNdx, boolean split)
    {
      int offset = newStartNdx - startNdx;

      if (split)
        textNode = textNode.splitText(offset);

      text = text.substring(offset);
      startNdx = newStartNdx;

      return textNode;
    }
  }

//---------------------------------------------------------------------------
//---------------------------------------------------------------------------

  private final List<HtmlTextNode> nodes = new ArrayList<>();
  private final StringBuilder plainText;

  @Override public String toString() { return plainText.toString(); }

//---------------------------------------------------------------------------
//---------------------------------------------------------------------------

  HtmlTextNodeList(Element element)
  {
    plainText = new StringBuilder(element.wholeText());

    addNodes(element, new MutableInt(0), false);
  }

//---------------------------------------------------------------------------
//---------------------------------------------------------------------------

  private void addNodes(Element element, MutableInt textNdx, boolean skip)
  {
    skip = skip || skipElement(element);

    for (Node child : element.childNodes())
    {
      if (child instanceof TextNode)
      {
        TextNode textNode = (TextNode)child;
        String nodeText = textNode.getWholeText();

        if (ultraTrim(nodeText).isBlank() == false)
        {
          HtmlTextNode node = new HtmlTextNode(nodeText, textNode, plainText.indexOf(nodeText, textNdx.intValue()));

          if (skip)
          {
            plainText.replace(node.startNdx, node.endNdx, "");
          }
          else
          {
            textNdx.setValue(node.endNdx);
            nodes.add(node);
          }
        }
      }
      else if (child instanceof Element)
        addNodes((Element)child, textNdx, skip);
    }
  }

//---------------------------------------------------------------------------
//---------------------------------------------------------------------------

  private static boolean skipElement(Element element)
  {
    return (element.tagName().equalsIgnoreCase("summary") || // Don't create any keyword links within collapsible headings
            element.tagName().equalsIgnoreCase("a")       || // Don't create any keyword links within anchor tags (they already link to somewhere)
            element.hasAttr(NO_LINKS_ATTR));
  }

//---------------------------------------------------------------------------
//---------------------------------------------------------------------------

  List<HtmlTextNode> getLinkNodes(int startNdx, int endNdx)
  {
    List<HtmlTextNode> linkNodes = new ArrayList<>();

    for (HtmlTextNode node : nodes)
    {
      if (node.startNdx >= endNdx) break;

      if (startNdx < node.endNdx)
        linkNodes.add(node);
    }

    if (linkNodes.isEmpty())
      messageDialog("Internal error #47690", mtError);

    return linkNodes;
  }

//---------------------------------------------------------------------------
//---------------------------------------------------------------------------

}
