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

import state.ReaderState.{Encoding, Help, MenuItemSource, Mode, Size}
import ResourceLookup.{MenuItemKey, MenuKey}
import state.{Preferences, ReaderState, RecentStates}

import Preferences.PreferenceKey.AutoRestore

import java.awt.event.{ActionEvent, ActionListener, ItemEvent}
import java.awt.{CheckboxMenuItem, Menu, MenuBar, MenuItem}

object MenuBuilder {

  def initMenus(state: ReaderState)(using EventHandler, ResourceLookup): MenuBar = {
    val menuBar = new MenuBar()
    menuBar.add(fileMenu(state.recentStates))
    menuBar.add(modeMenu(state.mode))
    menuBar.add(sizeMenu(state.size))
    menuBar.add(preferencesMenu(state.preferences))
    menuBar.add(optionsMenu)
    menuBar.add(encodingMenu(state.encoding))
    menuBar.add(helpMenu)
    menuBar
  }

  def localizedMenu(key: MenuKey, items: List[MenuItem])(using lookup: ResourceLookup): Menu = {
    val menu = new Menu(lookup(key))
    items.foreach(menu.add)
    menu
  }
  
  def menuItem(key: MenuItemKey, enabled:Boolean)(using handler: EventHandler, lookup: ResourceLookup): MenuItem =
    val item = new MenuItem(lookup(key))
    item.setActionCommand(key.description)
    item.setEnabled(enabled)
    item.addActionListener(handler)
    item

  def checkBoxMenu(key: MenuItemKey, value: Boolean, action: (EventHandler, Int) => Unit)
                          (using handler: EventHandler, lookup: ResourceLookup): MenuItem = {
    val menuItem = new CheckboxMenuItem(lookup(key))
    menuItem.setState(value)
    menuItem.addItemListener((e: ItemEvent) => action(handler, e.getStateChange))
    menuItem
  }

  def fileMenu(recentStates:RecentStates)(using EventHandler, ResourceLookup): Menu =
    localizedMenu(MenuKey.File, List(
      menuItem(MenuItemKey.Open, true),
      recentMenu(recentStates),
      menuItem(MenuItemKey.Info, true)))

  def recentMenu(recentStates:RecentStates)(using handler:EventHandler, lookup:ResourceLookup): Menu = {
    val menu = new Menu(lookup(MenuKey.Recent))
    handler.fillRecentFileMenu(menu, recentStates)
    menu
  }

  def modeMenu(mode: Mode)(using EventHandler, ResourceLookup): Menu =
    localizedMenu(MenuKey.Mode, modeMenuItems(mode.numFiles, mode))

  def sizeMenu(size:Size)(using EventHandler, ResourceLookup): Menu =
    localizedMenu(MenuKey.Size,
      menuItemsForEnumeratedMenu(Size.values.toList, (handler, tag) => handler.changeSize(tag), _ != size))

  def preferencesMenu(preferences:Preferences)(using handler: EventHandler, lookup: ResourceLookup): Menu =
    localizedMenu(MenuKey.Preferences, List(
      checkBoxMenu(MenuItemKey.AutoRestore, preferences.autoRestore, (a, b) => a.changePreference(AutoRestore, b))))

  def optionsMenu(using handler: EventHandler, lookup: ResourceLookup): Menu =
    localizedMenu(MenuKey.Options, List(
      checkBoxMenu(MenuItemKey.RightToLeft, false, (a, b) => a.directionChange(b)),
      checkBoxMenu(MenuItemKey.PageNumbers, true, (a, b) => a.togglePageNumbers(b))))

  def encodingMenu(encoding:Encoding)(using EventHandler, ResourceLookup): Menu =
    localizedMenu(MenuKey.Encoding,
      menuItemsForEnumeratedMenu(Encoding.values.toList, (handler, tag) => handler.changeEncoding(tag), _ != encoding))

  def helpMenu(using EventHandler, ResourceLookup): Menu =
    localizedMenu(MenuKey.Help, allMenuItems(Help.values.toList, (handler, tag) => handler.displayHelp(tag)))

  def alterMenu[K <: MenuItemSource](menu:Menu, selected:K):Unit = {
    for (i <- 0 until menu.getItemCount) {
      val item = menu.getItem(i)
      item.setEnabled(item.asInstanceOf[EnumeratedMenuItem[K]].tag != selected)
    }
  }
  
  private class EnumeratedValueChangeListener[K <: MenuItemSource](handler: EventHandler, action: (EventHandler, K) => Unit) extends ActionListener {

    override
    def actionPerformed(e: ActionEvent): Unit = {
      val selected: EnumeratedMenuItem[K] = e.getSource.asInstanceOf[EnumeratedMenuItem[K]]
      alterMenu(selected.getParent.asInstanceOf[Menu], selected.tag)
      action(handler, selected.tag)
    }
  }

  def menuItemsForEnumeratedMenu[K <: MenuItemSource]
    (itemValues: List[K], action: (EventHandler, K) => Unit, enabled:K => Boolean)
    (using handler: EventHandler, lookup: ResourceLookup): List[MenuItem] =
      val items = itemValues.filter(_.selectable).map(k => menuItemForEnumeratedMenu(k, enabled(k)))
      val listener = new EnumeratedValueChangeListener(handler, action)
      items.foreach(item => item.addActionListener(listener))
      items

  def menuItemForEnumeratedMenu[K <: MenuItemSource]
    (itemValue:K, enabled:Boolean)
    (using lookup: ResourceLookup): EnumeratedMenuItem[K] =
      val item = new EnumeratedMenuItem(itemValue, lookup(itemValue))
      item.setEnabled(enabled)
      item

  def modeMenuItems(count: Int, mode:Mode)(using EventHandler, ResourceLookup): List[MenuItem] =
    menuItemsForEnumeratedMenu[Mode](
      Mode.values.toList.filter(item => item.numFiles == count),
      (handler, tag) => handler.changeMode(tag), _ != mode)

  def allMenuItems[K <: MenuItemSource]
  (itemValues: List[K], action: (EventHandler, K) => Unit)
  (using handler: EventHandler, lookup: ResourceLookup): List[MenuItem] = {
    val items = itemValues.map(k => new EnumeratedMenuItem(k, lookup(k)))
    val actionListener: ActionListener = (e: ActionEvent) =>
      action(handler, e.getSource.asInstanceOf[EnumeratedMenuItem[K]].tag)
    items.foreach(item => item.addActionListener(actionListener))
    items
  }
}
