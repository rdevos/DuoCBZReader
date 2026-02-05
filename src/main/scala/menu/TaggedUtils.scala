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

import java.awt.{MenuComponent, MenuItem}

object TaggedUtils {

  def findMenu(count: => Int, getChild: Int => MenuComponent)(tag: MenuKey): Option[TaggedMenu] =
    Range(0, count).map(ix => getChild(ix))
      .collectFirst { case m @ TaggedMenu(`tag`) => m }
  
  def findMenuItem(count: => Int, getItem: Int => MenuItem)(tag: MenuItemKey): Option[TaggedMenuItem] =
    Range(0, count).map(ix => getItem(ix))
      .collectFirst { case m @ TaggedMenuItem(`tag`) => m }

  def findMenuItemIndex(count: => Int, getItem: Int => MenuItem)(tag: MenuItemKey): Option[Int] =
    Range(0, count).map(ix => (ix, getItem(ix)))
      .collectFirst { case (i, TaggedMenuItem(`tag`)) => i }
}
