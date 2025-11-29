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
import java.awt.{CheckboxMenuItem, Desktop, GraphicsEnvironment, GridLayout, Menu, MenuBar, MenuItem, Rectangle}
import javax.swing.{ImageIcon, JFrame, JOptionPane}
import javax.swing.WindowConstants.EXIT_ON_CLOSE
import EventHandler.{addMenuItemsForEnumeratedMenu, fillModeMenu}
import ReaderState.{INITIAL_STATE, Mode, Size}
import CBZImages.Dimensions

import be.afront.reader.ResourceLookup.{Label, MenuItemKey, MenuKey, MessageKey}

import java.awt.desktop.{AboutEvent, AboutHandler}
import java.util.{Locale, ResourceBundle}

object DuoCBZReader {

  def main(args: Array[String]): Unit = {

    val availableScreenSize: (width: Int, height: Int) = screenSize
    val lookup = ResourceLookup(Locale.getDefault)
    
    val frame = new JFrame(lookup(Label.Application))
    frame.setUndecorated(true)
    frame.setDefaultCloseOperation(EXIT_ON_CLOSE)
    frame.setResizable(false)
    frame.setLayout(new GridLayout(1, 2))

    val state:ReaderState = INITIAL_STATE
    val panel1 = new ImagePanel(state, 0)
    val panel2 = new ImagePanel(state, 1)

    val handler = new EventHandler(frame, panel1, panel2, state, availableScreenSize, lookup)

    frame.setMenuBar(initMenus(handler, state.mode, lookup))
    frame.setVisible(true)
    frame.requestFocusInWindow()
    setupDesktop(handler, lookup)
  }

  private def initMenus(handler: EventHandler, mode: Mode, lookup:ResourceLookup): MenuBar = {
    val menuBar = new MenuBar()
    menuBar.add(fileMenu(handler, lookup))
    menuBar.add(modeMenu(handler, mode, lookup))
    menuBar.add(sizeMenu(handler, lookup))
    menuBar.add(optionsMenu(handler, lookup))
    menuBar
  }

  private def fileMenu(handler: EventHandler, lookup:ResourceLookup): Menu = {
    val fileMenu = new Menu(lookup(MenuKey.File))
    val openItem = new MenuItem(lookup(MenuItemKey.Open))
    openItem.addActionListener(handler)
    fileMenu.add(openItem)
    fileMenu
  }
  
  private def modeMenu(handler: EventHandler, mode: Mode, lookup:ResourceLookup): Menu = {
    val modeMenu = new Menu(lookup(MenuKey.Mode))
    fillModeMenu(mode, modeMenu, handler, lookup);
    modeMenu
  }

  private def sizeMenu(handler: EventHandler, lookup:ResourceLookup): Menu = {
    val sizeMenu = new Menu(lookup(MenuKey.Size))
    addMenuItemsForEnumeratedMenu(sizeMenu, Size.values.toList, handler, lookup,
      (handler, tag) => handler.changeSize(tag))
    sizeMenu
  }

  private def optionsMenu(handler: EventHandler, lookup:ResourceLookup): Menu = {
    val optionsMenu = new Menu(lookup(MenuKey.Options))
    optionsMenu.add(checkBoxMenu(lookup(MenuItemKey.RightToLeft), false,
      (e: ItemEvent) => handler.directionChange(e.getStateChange)))
    optionsMenu.add(checkBoxMenu(lookup(MenuItemKey.PageNumbers), true,
      (e: ItemEvent) => handler.togglePageNumbers(e.getStateChange)))
    optionsMenu
  }


  private def checkBoxMenu(content: String, value: Boolean, itemListener: ItemListener): MenuItem = {
    val menuItem = new CheckboxMenuItem(content)
    menuItem.setState(value)
    menuItem.addItemListener(itemListener)
    menuItem
  }

  private def screenSize: Dimensions = {
    val ge = GraphicsEnvironment.getLocalGraphicsEnvironment
    val usableBounds: Rectangle = ge.getMaximumWindowBounds
    val width = usableBounds.getWidth.toInt
    val height = usableBounds.getHeight.toInt
    (width, height)
  }

  private def setupDesktop(handler: EventHandler, lookup:ResourceLookup):Unit = {
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
