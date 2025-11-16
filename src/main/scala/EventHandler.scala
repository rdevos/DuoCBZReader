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

import EventHandler.{SENSITIVITY, WHEEL_SENSITIVITY, selectFile}
import CBZImages.Direction.{LeftToRight, RightToLeft}

import java.awt.{FileDialog, Frame, Point}
import java.awt.event.{ActionEvent, ActionListener, KeyEvent, KeyListener, MouseEvent, MouseListener, MouseMotionListener, MouseWheelEvent, MouseWheelListener}
import javax.swing.{JFrame, SwingUtilities}
import java.awt.event.KeyEvent.{VK_2, VK_4, VK_6, VK_8, VK_ADD, VK_DOWN, VK_LEFT, VK_MINUS, VK_NUMPAD2, VK_NUMPAD4, VK_NUMPAD6, VK_NUMPAD8, VK_PLUS, VK_Q, VK_RIGHT, VK_SUBTRACT, VK_UP}
import java.io.File
import java.awt.event.ItemEvent.SELECTED

class EventHandler(frame:JFrame, panel1:ImagePanel, panel2:ImagePanel)
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

  var initialMouseDown: (p:Point, h:Double, v:Double) = null

  init()

  override def keyPressed(e: KeyEvent): Unit = {
    stateMachine(e.getKeyCode, panel1) match {
      case Some(newState) =>
        updateState(newState)
      case None =>
        frame.dispose()
    }
  }
  
  private def updateState(newState:ReaderState): Unit = {
    SwingUtilities.invokeLater { () =>
      panel1.setNewState(newState)
      panel2.setNewState(newState)
      frame.repaint()
    }
  }

  private def stateMachine(keyCode: Int, panel1: ImagePanel): Option[ReaderState] = {
    keyCode match {
      case VK_RIGHT => Some(panel1.currentState.right)
      case VK_LEFT => Some(panel1.currentState.left)
      case VK_UP => Some(panel1.currentState.zoomIn)
      case VK_DOWN => Some(panel1.currentState.zoomOut)

      case VK_MINUS | VK_SUBTRACT => Some(panel1.currentState.minus)
      case VK_PLUS | VK_ADD=> Some(panel1.currentState.plus)

      case VK_8 | VK_NUMPAD8 => Some(panel1.currentState.scrollUp)
      case VK_2 | VK_NUMPAD2 => Some(panel1.currentState.scrollDown)
      case VK_4 | VK_NUMPAD4 => Some(panel1.currentState.scrollLeft)
      case VK_6 | VK_NUMPAD6 => Some(panel1.currentState.scrollRight)

      case VK_Q => None
      case _ => Some(panel1.currentState)
    }
  }

  override def keyTyped(e: KeyEvent): Unit = {}

  override def keyReleased(e: KeyEvent): Unit = {}

  override def mousePressed(e: MouseEvent): Unit = {
    initialMouseDown = (e.getPoint, panel1.currentState.hs, panel1.currentState.vs)
  }

  override def mouseReleased(e: MouseEvent): Unit = {
    initialMouseDown = null
  }

  override def mouseDragged(e: MouseEvent): Unit = {
    if(initialMouseDown!=null) {
      // signed for 'normal' dragging, image follows mouse
      val dx = initialMouseDown.p.x - e.getX
      val dy = initialMouseDown.p.y - e.getY
      val multiplier = SENSITIVITY/panel1.currentState.zoomFactor
      updateState(panel1.currentState.scrollTo(
        initialMouseDown.h + multiplier*dx,
        initialMouseDown.v + multiplier*dy))
    } else {
      // initialMouseDown not available for drag event
    }
  }
  
  override def mouseMoved(e: MouseEvent): Unit = { }

  override def mouseClicked(e: MouseEvent): Unit = {
    e.getButton match {
      case MouseEvent.BUTTON1 => updateState(panel1.currentState.nextPage)
      case MouseEvent.BUTTON3 => updateState(panel1.currentState.prevPage)
      case _ =>
    }
  }

  override def mouseEntered(e: MouseEvent): Unit = {}

  override def mouseExited(e: MouseEvent): Unit = {}

  override def actionPerformed(event: ActionEvent): Unit = {
    event.getActionCommand match {
      case "Open" => 
        updateState(ReaderState(selectFile("select 1st comic"), selectFile("select 2nd comic")))
      case _ => println("unimplemented command "+ event.getActionCommand)
    }
  }

  def directionChange(newState:Int):Unit =
    updateState(panel1.currentState.setDirection(if(newState == SELECTED) RightToLeft else LeftToRight ))

  override def mouseWheelMoved(e: MouseWheelEvent): Unit =
    updateState(panel1.currentState.scrollVertical(e.getWheelRotation * WHEEL_SENSITIVITY))
}

object EventHandler {
  val SENSITIVITY = 0.005

  val WHEEL_SENSITIVITY = 0.01


  def selectFile(prompt: String): File = {
    val dummyFrame = new Frame()
    dummyFrame.setSize(0, 0)

    val dialog = new FileDialog(dummyFrame, prompt, FileDialog.LOAD)
    dialog.setVisible(true)

    val files = dialog.getFiles
    dialog.dispose()
    dummyFrame.dispose()

    files(0)
  }

}
