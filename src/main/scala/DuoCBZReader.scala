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

import java.awt.{Desktop, Dimension, GraphicsEnvironment, GridLayout, Rectangle}
import javax.swing.{ImageIcon, JFrame, JOptionPane}
import javax.swing.WindowConstants.EXIT_ON_CLOSE
import ReaderState.INITIAL_STATE
import CBZImages.Dimensions
import ResourceLookup.{Label, MessageKey}
import CBZImages.PanelID.{LeftOrFront, RightOrBack}

import MenuBuilder.initMenus

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
    val panel1 = new ImagePanel(state, LeftOrFront)
    val panel2 = new ImagePanel(state, RightOrBack)

    given ResourceLookup = lookup
    val handler = new EventHandler(frame, panel1, panel2, state, availableScreenSize)
    given EventHandler = handler

    frame.setMenuBar(initMenus(state.mode))
    frame.setVisible(true)
    frame.requestFocusInWindow()
    setupDesktop
  }
  
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
