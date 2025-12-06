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

import java.awt.event.{ItemEvent, ItemListener}
import java.awt.{CheckboxMenuItem, Desktop, Dimension, GraphicsEnvironment, GridLayout, Menu, MenuBar, MenuItem, Rectangle}
import javax.swing.{ImageIcon, JFrame, JOptionPane}
import javax.swing.WindowConstants.EXIT_ON_CLOSE
import EventHandler.{menuItemsForEnumeratedMenu, modeMenuItems}
import ReaderState.{Encoding, INITIAL_STATE, Mode, Size}
import CBZImages.Dimensions
import ResourceLookup.{Label, MenuItemKey, MenuKey, MessageKey}

import java.awt.desktop.{AboutEvent, AboutHandler}
import java.util.Locale

object DuoCBZReader {

  def main(args: Array[String]): Unit = {

    val availableScreenSize: (width: Int, height: Int, depth:Int, size:Long) = screenSize
    val lookup = ResourceLookup(Locale.getDefault)
    
    val frame = new JFrame(lookup(Label.Application))
    frame.setUndecorated(false)
    frame.setDefaultCloseOperation(EXIT_ON_CLOSE)
    frame.setResizable(true)
    frame.setMinimumSize(new Dimension(300, 0))

    frame.setLayout(new GridLayout(1, 2))

    val state:ReaderState = INITIAL_STATE
    val panel1 = new ImagePanel(state, 0)
    val panel2 = new ImagePanel(state, 1)

    given ResourceLookup = lookup
    val handler = new EventHandler(frame, panel1, panel2, state, availableScreenSize)
    given EventHandler = handler

    frame.setMenuBar(initMenus(state.mode))
    frame.setVisible(true)
    frame.requestFocusInWindow()
    setupDesktop
  }

  private def initMenus(mode: Mode)(using EventHandler, ResourceLookup): MenuBar = {
    val menuBar = new MenuBar()
    menuBar.add(fileMenu)
    menuBar.add(modeMenu(mode))
    menuBar.add(sizeMenu)
    menuBar.add(optionsMenu)
    menuBar.add(encodingMenu)
    menuBar
  }

  private def localizedMenu(key:MenuKey, items:List[MenuItem])(using lookup:ResourceLookup): Menu = {
    val menu = new Menu(lookup(key))
    items.foreach(menu.add)
    menu
  }

  private def menuItem(key: MenuItemKey)(using handler: EventHandler, lookup: ResourceLookup): MenuItem =
    val item = new MenuItem(lookup(key))
    item.setActionCommand(key.description)
    item.addActionListener(handler)
    item

  private def checkBoxMenu(key: MenuItemKey, value: Boolean, action: (EventHandler,Int) => Unit)
                          (using handler:EventHandler, lookup: ResourceLookup): MenuItem = {
    val menuItem = new CheckboxMenuItem(lookup(key))
    menuItem.setState(value)
    menuItem.addItemListener((e: ItemEvent) => action(handler, e.getStateChange))
    menuItem
  }

  private def fileMenu(using EventHandler, ResourceLookup): Menu =
    localizedMenu(MenuKey.File, List(
      menuItem(MenuItemKey.Open),
      menuItem(MenuItemKey.Info)))

  private def modeMenu(mode: Mode)(using EventHandler, ResourceLookup): Menu =
    localizedMenu(MenuKey.Mode, modeMenuItems(mode))

  private def sizeMenu(using EventHandler, ResourceLookup): Menu =
    localizedMenu(MenuKey.Size, menuItemsForEnumeratedMenu(Size.values.toList,(handler, tag) => handler.changeSize(tag)))

  private def optionsMenu(using handler:EventHandler, lookup:ResourceLookup): Menu =
    localizedMenu(MenuKey.Options, List(
      checkBoxMenu(MenuItemKey.RightToLeft, false, (a,b) => a.directionChange(b)),
      checkBoxMenu(MenuItemKey.PageNumbers, true, (a,b) => a.togglePageNumbers(b))))

  private def encodingMenu(using EventHandler, ResourceLookup): Menu =
    localizedMenu(MenuKey.Encoding, menuItemsForEnumeratedMenu(Encoding.values.toList, (handler, tag) => handler.changeEncoding(tag)))

  private def screenSize: Dimensions = {
    val ge = GraphicsEnvironment.getLocalGraphicsEnvironment
    val usableBounds: Rectangle = ge.getMaximumWindowBounds
    val width = usableBounds.getWidth.toInt
    val height = usableBounds.getHeight.toInt
    val depth = ge.getDefaultScreenDevice.getDisplayMode.getBitDepth
    (width, height, depth, 0)
  }

  private def setupDesktop(using handler: EventHandler, lookup:ResourceLookup):Unit = {
    if (Desktop.isDesktopSupported) {
      val desktop = Desktop.getDesktop
      val iconPath = "/icon_128x128.png"
      val iconUrl = getClass.getResource(iconPath)
      val icon = if (iconUrl != null) new ImageIcon(iconUrl) else null

      desktop.setAboutHandler(new AboutHandler() {
        def handleAbout(e: AboutEvent): Unit = {
          JOptionPane.showMessageDialog(null,
            lookup(MessageKey.Copyright),
            lookup(MessageKey.About),
            JOptionPane.INFORMATION_MESSAGE, icon)
        }
      })

      if (desktop.isSupported(Desktop.Action.APP_OPEN_FILE))
        desktop.setOpenFileHandler(handler)
    }
  }
}
