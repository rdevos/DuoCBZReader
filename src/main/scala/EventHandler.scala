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

import EventHandler.{SENSITIVITY, WHEEL_SENSITIVITY, handle, modeMenuItems, open, openSelectedFiles}
import CBZImages.Direction.{LeftToRight, RightToLeft}
import ReaderState.{Encoding, MenuItemSource, Mode, Size}
import ReaderState.Mode.{Blank, Dual1, Dual1b, Dual2, Single, SingleEvenOdd, SingleOddEven}
import CBZImages.{Dimensions, FileCheck, checkFile}
import ResourceLookup.MenuItemKey

import java.awt.desktop.{OpenFilesEvent, OpenFilesHandler}
import java.awt.{FileDialog, Frame, Menu, MenuItem, Point}
import java.awt.event.{ActionEvent, ActionListener, KeyEvent, KeyListener, MouseEvent, MouseListener, MouseMotionListener, MouseWheelEvent, MouseWheelListener}
import javax.swing.{JFrame, SwingUtilities}
import java.awt.event.KeyEvent.{VK_2, VK_4, VK_6, VK_8, VK_ADD, VK_DOWN, VK_LEFT, VK_MINUS, VK_NUMPAD2, VK_NUMPAD4, VK_NUMPAD6, VK_NUMPAD8, VK_PLUS, VK_Q, VK_RIGHT, VK_SHIFT, VK_SUBTRACT, VK_UP}
import java.awt.event.ItemEvent.SELECTED
import java.io.File
import scala.List
import scala.jdk.CollectionConverters.given
import scala.util.{Failure, Success, Try}


class EventHandler(frame:JFrame, panel1:ImagePanel, panel2:ImagePanel,
                   initialState:ReaderState, screenSize:Dimensions)(using lookup:ResourceLookup)
  extends KeyListener
    with MouseMotionListener
    with MouseWheelListener
    with MouseListener
    with ActionListener
    with OpenFilesHandler {

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
      frame.revalidate()
    }
    if(afterOpen) {
      updateMenuBar(newState.partialStates.size);
    }
    state = newState
    panel1.setNewState(newState)
    if (newState.partialStates.size == 2 || newState.mode == SingleEvenOdd || newState.mode == SingleOddEven)
      panel2.setNewState(newState)

    SwingUtilities.invokeLater { () =>
      frame.repaint()
    }
  }
  
  private def updateMenuBar(count:Int):Unit = {
    val menuBar = frame.getMenuBar
    val menu = menuBar.getMenu(1)
    menu.removeAll()
    modeMenuItems(count)(using this, lookup).foreach(menu.add)
  }

  private def updateStateForNewFiles(state: ReaderState):Unit = {
    updateState(state, true)
  }

  private def layoutChangeFor(newState:ReaderState): Unit = {
    val oldMode = state.mode
    val newMode = newState.mode

    frame.remove(panel1)
    frame.remove(panel2)
    val panels = visiblePanels(newMode)
    panels.foreach(frame.add)

    if(panels.size == 1)
      frame.setSize(screenSize.width / 2, screenSize.height)
    else if(panels.size == 2)
      frame.setSize(screenSize.width, screenSize.height)
    else frame.setSize(0, 0)   
  }

  private def visiblePanels(mode:Mode):List[ImagePanel] = {
    mode match {
      case Blank => List()
      case Single => List(panel1)
      case SingleOddEven => List(panel1, panel2)
      case SingleEvenOdd => List(panel1, panel2)
      case Dual1 => List(panel1)
      case Dual1b => List(panel2)
      case Dual2 => List(panel1, panel2)
    }
  }

  private def ifNotBlank(r: => Option[ReaderState]):Option[ReaderState] =
    if(state.mode == Blank) Some(state) else r   
  
  private def stateMachine(keyCode: Int, pressed:Boolean): Option[ReaderState] = {
    if (pressed) keyCode match {
      case VK_RIGHT => ifNotBlank(Some(state.right))
      case VK_LEFT => ifNotBlank(Some(state.left))
      case VK_UP => ifNotBlank(Some(state.zoomIn))
      case VK_DOWN => ifNotBlank(Some(state.zoomOut))

      case VK_SHIFT => if(state.mode==Dual1) Some(state.setMode(Dual1b)) else Some(state)

      case VK_MINUS | VK_SUBTRACT => ifNotBlank(Some(state.minus))
      case VK_PLUS | VK_ADD=> ifNotBlank(Some(state.plus))

      case VK_8 | VK_NUMPAD8 => ifNotBlank(Some(state.scrollUp))
      case VK_2 | VK_NUMPAD2 => ifNotBlank(Some(state.scrollDown))
      case VK_4 | VK_NUMPAD4 => ifNotBlank(Some(state.scrollLeft))
      case VK_6 | VK_NUMPAD6 => ifNotBlank(Some(state.scrollRight))

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
      val multiplier = SENSITIVITY / state.zoomFactor
      val dx = (initialMouseDown.p.x - e.getX) * multiplier
      val dy = (initialMouseDown.p.y - e.getY) * multiplier
      updateState(state.scrollTo(initialMouseDown.h + dx, initialMouseDown.v + dy))
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
      case MenuItemKey.Open.description => 
        updateStateForNewFiles(open(state))
      case MenuItemKey.Info.description =>
        displayMetadata()

      case _ => println("unimplemented command "+ event.getActionCommand)
    }
  }

  def displayMetadata(): Unit = {
    val dummyOwner = new Frame()
    dummyOwner.setVisible(false)
    val dialog = new InfoDialog(dummyOwner, state.partialStates.map(_.metadata).mkString("\n\n"))
    dialog.setLocationRelativeTo(null)
    dialog.setVisible(true)
    dummyOwner.dispose()
  }

  def directionChange(newState:Int):Unit =
    updateState(state.setDirection(if(newState == SELECTED) RightToLeft else LeftToRight ))

  def togglePageNumbers(newState: Int): Unit =
    updateState(state.setShowPageNumbers(newState == SELECTED))

  def changeMode(newMode:Mode):Unit =
    updateState(state.setMode(newMode))

  def changeSize(newSize:Size):Unit =
    updateState(state.setSize(newSize))

  def changeEncoding(newEncoding: Encoding): Unit =
    updateState(state.setEncoding(newEncoding))

  override def mouseWheelMoved(e: MouseWheelEvent): Unit =
    updateState(state.scrollVertical(e.getWheelRotation * WHEEL_SENSITIVITY))

  override
  def openFiles(e: OpenFilesEvent): Unit = {
    val paths = e.getFiles.asScala.toList
    if (paths.nonEmpty) {
      val images = paths.take(2).map(p => checkFile(p, state.encoding)).flatMap((checkResult:FileCheck) => checkResult match {
        case (file, Failure(err)) => {
          handle(file, err);None
        }
        case (file, Success(image)) => Some(image)
      })
      updateStateForNewFiles(openSelectedFiles(state, images))
    }
  }
}

object EventHandler {
  val SENSITIVITY = 0.005

  val WHEEL_SENSITIVITY = 0.01


  def selectFile(prompt: String, encoding:Encoding): Option[FileCheck] = {
    val dummyFrame = new Frame()
    dummyFrame.setSize(0, 0)

    val dialog = new FileDialog(dummyFrame, prompt, FileDialog.LOAD)
    dialog.setVisible(true)

    val files = dialog.getFiles
    dialog.dispose()
    dummyFrame.dispose()

    if(files.isEmpty) None else Some(checkFile(files(0), encoding))
  }

  @annotation.tailrec
  def selectFileLoop(prompt:String, encoding:Encoding): Option[CBZImages] = {
    selectFile(prompt, encoding) match {
      case Some((file, Failure(err))) => { handle(file, err); selectFileLoop(prompt, encoding) }
      case Some((file, Success(image))) => Some(image)
      case None => None
    }
  }

  def handle(file:File, err:Throwable):Unit = {
    val dummyOwner = new Frame()
    dummyOwner.setVisible(false)
    val simpleFileName = file.getName
    val alert = new AlertDialog(dummyOwner, s"Failed to open \"$simpleFileName\"", err.getMessage)
    alert.setLocationRelativeTo(null)
    alert.setVisible(true)
    dummyOwner.dispose()
  }

  def open(state:ReaderState): ReaderState =
    openSelectedFiles(state,
      List(selectFileLoop("select first file", state.encoding), selectFileLoop("select 2nd file", state.encoding)).flatten)

  def openSelectedFiles(currentState:ReaderState, files:List[CBZImages]) : ReaderState =
    new ReaderState(files, currentState)

  private class EnumeratedValueChangeListener[K <: MenuItemSource](handler: EventHandler, action:(EventHandler, K)=>Unit) extends ActionListener {

    override
    def actionPerformed(e: ActionEvent): Unit = {
      val selected: EnumeratedMenuItem[K] = e.getSource.asInstanceOf[EnumeratedMenuItem[K]]
      val menu = selected.getParent.asInstanceOf[Menu]
      for (i <- 0 until menu.getItemCount) {
        val item = menu.getItem(i)
        item.setEnabled(item != selected)
      }
      action(handler, selected.tag)
    }
  }

  def menuItemsForEnumeratedMenu[K <: MenuItemSource]
      (itemValues:List[K], action:(EventHandler, K)=>Unit)
      (using handler:EventHandler, lookup:ResourceLookup): List[MenuItem] = {
    val items = itemValues.filter(_.selectable).map(q => new EnumeratedMenuItem(q, lookup(q)))
    if(items.nonEmpty)
      items.head.setEnabled(false)
    val listener = new EnumeratedValueChangeListener(handler, action)
    items.foreach(item => item.addActionListener(listener))
    items
  }
  
  def modeMenuItems(count:Int)(using EventHandler, ResourceLookup):List[MenuItem] =
    menuItemsForEnumeratedMenu[Mode](
      Mode.values.toList.filter(item => item.num == count),
      (handler, tag) => handler.changeMode(tag))
}
