/*
  Copyright 2025-2026 Paul Janssens - All rights reserved

  Licensed under the Apache License, Version 2.0 (the "License");
  you may not use this file except in compliance with the License.
  You may obtain a copy of the License at

      http://www.apache.org/licenses/LICENSE-2.0

  Unless required by applicable law or agreed to in writing, software
  distributed under the License is distributed on an "AS IS" BASIS,
  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  See the License for the specific language governing permissions and
  limitations under the License.
*/

package be.afront.reader
package menu

import ResourceLookup.{MenuItemKey, MenuKey}

import java.awt.{Menu, MenuBar, MenuComponent, MenuItem}

case class TaggedMenu(key:MenuKey)(using lookup:ResourceLookup) extends Menu(lookup(key)) {

  private def findSubMenu(tag: MenuKey): Option[TaggedMenu] =
    TaggedUtils.findMenu(getItemCount, ix => getItem(ix))(tag)

  private def findItem(tag: MenuItemKey): Option[MenuItem] =
    TaggedUtils.findMenuItem(getItemCount, ix => getItem(ix))(tag)

  private def findItemPosition(tag: MenuItemKey): Option[Int] =
    TaggedUtils.findMenuItemIndex(getItemCount, ix => getItem(ix))(tag)

  def withSubMenuDo(subTag: MenuKey, action: TaggedMenu => Unit): Unit =
    findSubMenu(subTag).foreach(action)
    
  def replaceMenuItems(newItems:List[MenuItem]):TaggedMenu = {
    removeAll()
    newItems.foreach(add)
    this
  }

  def setMenuItem(key: MenuItemKey, enabled:Boolean): Unit =
    findItem(key).foreach(item => item.setEnabled(enabled))

  def removeMenuItem(key:MenuItemKey):Unit =
    findItemPosition(key).foreach(remove)

  def addMenuItemAfter(previousKey: MenuItemKey, toAdd:MenuItem): Unit =
    findItemPosition(previousKey).foreach(ix => insert(toAdd,ix+1))
}


