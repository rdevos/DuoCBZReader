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

import EventHandler.{SENSITIVITY, WHEEL_SENSITIVITY, addMenuItemsForModeMenu, open, selectFile}
import CBZImages.Direction.{LeftToRight, RightToLeft}
import ReaderState.Mode
import ReaderState.Mode.{Blank, Dual1, Dual1b, Dual2, Single}

import be.afront.reader.CBZImages.Direction

import java.awt.{FileDialog, Frame, Menu, MenuItem, Point}
import java.awt.event.{ActionEvent, ActionListener, KeyEvent, KeyListener, MouseEvent, MouseListener, MouseMotionListener, MouseWheelEvent, MouseWheelListener}
import javax.swing.{JFrame, SwingUtilities}
import java.awt.event.KeyEvent.{VK_2, VK_4, VK_6, VK_8, VK_ADD, VK_DOWN, VK_LEFT, VK_MINUS, VK_NUMPAD2, VK_NUMPAD4, VK_NUMPAD6, VK_NUMPAD8, VK_PLUS, VK_Q, VK_RIGHT, VK_SHIFT, VK_SUBTRACT, VK_UP}
import java.io.File
import java.awt.event.ItemEvent.SELECTED

class EventHandler(frame:JFrame, panel1:ImagePanel, panel2:ImagePanel, initialState:ReaderState, width:Int)
  extends KeyListener
    with MouseMotionListener
    with MouseWheelListener
    with MouseListener
    with ActionListener{

  private def init():Unit = {
    frame.addKeyListener(this)
    frame.addMouseMotionListener(this)
    frame.addMouseListener(this)
    frame.addMouseWheelListener(this)
  }

  var state:ReaderState = initialState

  var initialMouseDown: (p:Point, h:Double, v:Double) = null

  init()

  override def keyPressed(e: KeyEvent): Unit = {
    stateMachine(e.getKeyCode, true) match {
      case Some(newState) =>
        updateState(newState)
      case None =>
        frame.dispose()
    }
  }

  override def keyReleased(e: KeyEvent): Unit = {
    stateMachine(e.getKeyCode, false) match {
      case Some(newState) =>
        updateState(newState)
      case None =>
        frame.dispose()
    }
  }

  private def updateState(newState: ReaderState): Unit =
    updateState(newState, false)


  private def updateState(newState:ReaderState, afterOpen:Boolean): Unit = {
    if(state.mode != newState.mode) {
      layoutChangeFor(newState)
    }
    if(afterOpen) {
      // do menubar
      val menuBar = frame.getMenuBar
      val menu = menuBar.getMenu(1)
      menu.removeAll()
      if (newState.mode == Dual2) {
        addMenuItemsForModeMenu(menu, this)
      }
    }
    state = newState
    SwingUtilities.invokeLater { () =>
      panel1.setNewState(newState)
      if(newState.state2!=null) panel2.setNewState(newState)
      frame.revalidate()
      frame.repaint()
    }
  }

  private def updateStateForNewFiles(tuple:(files: List[File],mode: Mode,state: ReaderState)):Unit = {
    updateState(tuple.state, true)
  }

  private def layoutChangeFor(newState:ReaderState): Unit = {
    val oldMode = state.mode
    val newMode = newState.mode

    frame.remove(panel1)
    frame.remove(panel2)
    val panels = visiblePanels(newMode)
    panels.foreach(frame.add)

    if(panels.size == 1)
      frame.setSize(width / 2, frame.getHeight)
    else if(panels.size == 2)
      frame.setSize(width, frame.getHeight)

    frame.setVisible(newMode != Blank)
  }

  private def visiblePanels(mode:Mode):List[ImagePanel] = {
    mode match {
      case Blank => List()
      case Single => List(panel1)
      case Dual1 => List(panel1)
      case Dual1b => List(panel2)
      case Dual2 => List(panel1, panel2)
    }
  }

  private def stateMachine(keyCode: Int, pressed:Boolean): Option[ReaderState] = {
    if (pressed) keyCode match {
      case VK_RIGHT => Some(state.right)
      case VK_LEFT => Some(state.left)
      case VK_UP => Some(state.zoomIn)
      case VK_DOWN => Some(state.zoomOut)

      case VK_SHIFT => if(state.mode==Dual1) Some(state.setMode(Dual1b)) else Some(state)

      case VK_MINUS | VK_SUBTRACT => Some(state.minus)
      case VK_PLUS | VK_ADD=> Some(state.plus)

      case VK_8 | VK_NUMPAD8 => Some(state.scrollUp)
      case VK_2 | VK_NUMPAD2 => Some(state.scrollDown)
      case VK_4 | VK_NUMPAD4 => Some(state.scrollLeft)
      case VK_6 | VK_NUMPAD6 => Some(state.scrollRight)

      case VK_Q => None
      case _ => Some(state)
      
    } else keyCode match {
      case VK_SHIFT => if(state.mode==Dual1b) Some(state.setMode(Dual1)) else Some(state)
      case _ => Some(state)
    }
  }

  override def keyTyped(e: KeyEvent): Unit = {}

  override def mousePressed(e: MouseEvent): Unit = {
    initialMouseDown = (e.getPoint, state.hs, state.vs)
  }

  override def mouseReleased(e: MouseEvent): Unit = {
    initialMouseDown = null
  }

  override def mouseDragged(e: MouseEvent): Unit = {
    if(initialMouseDown!=null) {
      // signed for 'normal' dragging, image follows mouse
      val dx = initialMouseDown.p.x - e.getX
      val dy = initialMouseDown.p.y - e.getY
      val multiplier = SENSITIVITY/state.zoomFactor
      updateState(state.scrollTo(
        initialMouseDown.h + multiplier*dx,
        initialMouseDown.v + multiplier*dy))
    } else {
      // initialMouseDown not available for drag event
    }
  }
  
  override def mouseMoved(e: MouseEvent): Unit = { }

  override def mouseClicked(e: MouseEvent): Unit = {
    e.getButton match {
      case MouseEvent.BUTTON1 => updateState(state.nextPage)
      case MouseEvent.BUTTON3 => updateState(state.prevPage)
      case _ =>
    }
  }

  override def mouseEntered(e: MouseEvent): Unit = {}

  override def mouseExited(e: MouseEvent): Unit = {}

  override def actionPerformed(event: ActionEvent): Unit = {
    event.getActionCommand match {
      case "Open" => 
        updateStateForNewFiles(open(state.direction, state.showPageNumbers))

      case "Dual 2 columns" => changeMode(Dual2)
      case "Dual 1 column" => changeMode(Dual1)
      case "Single" => changeMode(Single)

      case _ => println("unimplemented command "+ event.getActionCommand)
    }
  }

  def directionChange(newState:Int):Unit =
    updateState(state.setDirection(if(newState == SELECTED) RightToLeft else LeftToRight ))

  def togglePageNumbers(newState: Int): Unit =
    updateState(state.setShowPageNumbers(newState == SELECTED))

  def changeMode(newMode: String): Unit =
    newMode match {
      case "Dual 2 columns" => changeMode(Dual2)
      case "Dual 1 column" => changeMode(Dual1)
      case "Single" => changeMode(Single)
    }

  def changeMode(newMode:Mode):Unit =
    updateState(state.setMode(newMode))

  override def mouseWheelMoved(e: MouseWheelEvent): Unit =
    updateState(state.scrollVertical(e.getWheelRotation * WHEEL_SENSITIVITY))
}

object EventHandler {
  val SENSITIVITY = 0.005

  val WHEEL_SENSITIVITY = 0.01


  def selectFile(prompt: String): Option[File] = {
    val dummyFrame = new Frame()
    dummyFrame.setSize(0, 0)

    val dialog = new FileDialog(dummyFrame, prompt, FileDialog.LOAD)
    dialog.setVisible(true)

    val files = dialog.getFiles
    dialog.dispose()
    dummyFrame.dispose()

    if(files.isEmpty) None else Some(files(0))
  }

  def open(direction:Direction, showPageNumbers:Boolean): (files: List[File],mode: Mode,state: ReaderState) = {
    val files:List[File] = selectFile("select 1st file").toList ++ selectFile("select 2nd file").toList

    val mode = if (files.size == 2) Dual2 else if (files.size == 1) Single else Blank

    val state = mode match {
      case Dual2 => ReaderState(mode, files(0), files(1), direction, showPageNumbers)
      case Single => ReaderState(mode, files(0), null, direction, showPageNumbers)
      case _ => ReaderState(mode, null, null, direction, showPageNumbers)
    }
    (files,mode,state)
  }

  def actions: List[String] = List("Dual 2 columns", "Dual 1 column");

  private class ModeChangeListener(menuItems: List[MenuItem], handler: EventHandler) extends ActionListener {

    override
    def actionPerformed(e: ActionEvent): Unit = {
      val action = e.getActionCommand
      val ix = actions.indexOf(e.getActionCommand)
      actions.indices.foreach(ix2 => menuItems(ix2).setEnabled(ix2 != ix))
      handler.changeMode(action)
    }
  }

  def addMenuItemsForModeMenu(menu:Menu, handler: EventHandler): Unit = {
    val items = actions.map(new MenuItem(_))
    items.head.setEnabled(false)
    val listener = new ModeChangeListener(items, handler)
    items.foreach(item => item.addActionListener(listener))
    items.foreach(menu.add)
  }
}
