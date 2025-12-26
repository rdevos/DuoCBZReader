/*
  Copyright 2025 Paul Janssens - All rights reserved

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

import ResourceLookup.MenuKey

import java.awt.{Menu, MenuBar}

class TaggedMenuBar extends MenuBar {

  def addMenu(menu:TaggedMenu):Unit =
    add(menu)

  override
  def add(menu:Menu):Menu = {
    if(menu.isInstanceOf[TaggedMenu])
      super.add(menu)
    else null
  }

  private def findMenu(tag: MenuKey): Option[TaggedMenu] =
    TaggedUtils.find(getMenuCount, ix => getMenu(ix))(tag)

  def withMenuDo(tag: MenuKey, action: TaggedMenu => Unit): Unit =
    findMenu(tag).foreach(action)

  def withSubMenuDo(tag: MenuKey, subTag: MenuKey, action: TaggedMenu => Unit): Unit =
    findMenu(tag).foreach(_.withSubMenuDo(subTag, action))
}
