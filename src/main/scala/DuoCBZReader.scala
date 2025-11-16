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
import java.awt.{CheckboxMenuItem, GraphicsEnvironment, GridLayout, Menu, MenuBar, MenuItem, Rectangle, Toolkit}
import java.io.File
import javax.swing.JFrame
import javax.swing.WindowConstants.EXIT_ON_CLOSE
import EventHandler.selectFile

import be.afront.reader.CBZImages.Direction

object DuoCBZReader {

  case class Args(width:Int, height:Int, cbzPath1:File, cbzPath2:File)

  def main(args: Array[String]): Unit = {

    val args:Args = initialArgs()
    val state = ReaderState(args.cbzPath1, args.cbzPath2, Direction.LeftToRight, true)

    val frame = new JFrame("CBZ Reader")
    frame.setSize(args.width, args.height)
    frame.setUndecorated(true)
    frame.setDefaultCloseOperation(EXIT_ON_CLOSE)
    frame.setResizable(false)
    frame.setLayout(new GridLayout(1, 2))

    val panel1 = new ImagePanel(state, 0)
    val panel2 = new ImagePanel(state, 1)
    frame.add(panel1)
    frame.add(panel2)

    val handler = new EventHandler(frame, panel1, panel2, state)

    frame.setMenuBar(initMenus(handler))
    frame.setVisible(true)
    frame.requestFocusInWindow()
  }
  
  private def initMenus(handler:EventHandler):MenuBar = {
    val menuBar = new MenuBar()
    val fileMenu = new Menu("File")
    val openItem = new MenuItem("Open")
    openItem.addActionListener(handler)
    fileMenu.add(openItem)
    fileMenu.add(checkBoxMenu("Right To Left", false,
      (e: ItemEvent) => handler.directionChange(e.getStateChange)))
    fileMenu.add(checkBoxMenu("Show Page Numbers", true,
      (e: ItemEvent) => handler.directionChange(e.getStateChange)))
    menuBar.add(fileMenu)
    menuBar
  }

  private def checkBoxMenu(content:String, value:Boolean, itemListener:ItemListener):MenuItem = {
    val menuItem = new CheckboxMenuItem(content)
    menuItem.setState(false)
    menuItem.addItemListener(itemListener)
    menuItem
  }

  private def checkFileOrExit(path:String, exitCode:Int):File = {
    val file = new File (path)
    if(!file.exists()) {
      println("not found: " + file.getAbsolutePath)
      sys.exit(exitCode)
    }
    file
  }
  
  private def initialArgs():Args =
    val ge = GraphicsEnvironment.getLocalGraphicsEnvironment
    val usableBounds: Rectangle = ge.getMaximumWindowBounds
    val width = usableBounds.getWidth.toInt
    val height = usableBounds.getHeight.toInt
    Args(width, height, selectFile("select left comic"), selectFile("select right comic"))
}

