/*
  Copyright 2025 Paul Janssens

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
import java.awt.{CheckboxMenuItem, GraphicsEnvironment, GridLayout, Menu, MenuBar, MenuItem, Rectangle}
import javax.swing.JFrame
import javax.swing.WindowConstants.EXIT_ON_CLOSE
import EventHandler.{addMenuItemsForEnumeratedMenu, fillModeMenu}
import ReaderState.Mode.Dual2
import ReaderState.{INITIAL_STATE, Mode, Size}
import CBZImages.Dimensions

object DuoCBZReader {

  def main(args: Array[String]): Unit = {

    val availableScreenSize: (width: Int, height: Int) = screenSize

    val frame = new JFrame("CBZ Reader")
    frame.setUndecorated(true)
    frame.setDefaultCloseOperation(EXIT_ON_CLOSE)
    frame.setResizable(false)
    frame.setLayout(new GridLayout(1, 2))

    val state:ReaderState = INITIAL_STATE
    val panel1 = new ImagePanel(state, 0)
    val panel2 = new ImagePanel(state, 1)

    val handler = new EventHandler(frame, panel1, panel2, state, availableScreenSize)

    frame.setMenuBar(initMenus(handler, state.mode))
    frame.setVisible(true)
    frame.requestFocusInWindow()
  }

  private def initMenus(handler: EventHandler, mode: Mode): MenuBar = {
    val menuBar = new MenuBar()
    menuBar.add(fileMenu(handler))
    menuBar.add(modeMenu(handler, mode))
    menuBar.add(sizeMenu(handler))
    menuBar
  }

  private def fileMenu(handler: EventHandler): Menu = {
    val fileMenu = new Menu("File")
    val openItem = new MenuItem("Open")
    openItem.addActionListener(handler)
    fileMenu.add(openItem)
    fileMenu.add(checkBoxMenu("Right To Left", false,
      (e: ItemEvent) => handler.directionChange(e.getStateChange)))
    fileMenu.add(checkBoxMenu("Show Page Numbers", true,
      (e: ItemEvent) => handler.togglePageNumbers(e.getStateChange)))
    fileMenu
  }
  
  private def modeMenu(handler: EventHandler, mode: Mode): Menu = {
    val modeMenu = new Menu("Mode")
    fillModeMenu(mode, modeMenu, handler);
    modeMenu
  }

  private def sizeMenu(handler: EventHandler): Menu = {
    val sizeMenu = new Menu("Size")
    addMenuItemsForEnumeratedMenu(sizeMenu, Size.values.toList, handler,
      (handler, tag) => handler.changeSize(tag))
    sizeMenu
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
}
