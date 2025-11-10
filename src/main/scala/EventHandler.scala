package be.afront.reader

import EventHandler.{SENSITIVITY, selectFile}

import java.awt.{FileDialog, Frame, Point}
import java.awt.event.{ActionEvent, ActionListener, KeyEvent, KeyListener, MouseEvent, MouseListener, MouseMotionListener}
import javax.swing.{JFrame, SwingUtilities}
import java.awt.event.KeyEvent.{VK_2, VK_4, VK_6, VK_8, VK_ADD, VK_DOWN, VK_LEFT, VK_MINUS, VK_NUMPAD2, VK_NUMPAD4, VK_NUMPAD6, VK_NUMPAD8, VK_PLUS, VK_Q, VK_RIGHT, VK_SUBTRACT, VK_UP}
import java.io.File

class EventHandler(frame:JFrame, panel1:ImagePanel, panel2:ImagePanel)
  extends KeyListener with MouseMotionListener with MouseListener with ActionListener{

  var initialMouseDown:Point = null

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
      case VK_RIGHT => Some(panel1.currentState.nextPage)
      case VK_LEFT => Some(panel1.currentState.prevPage)
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
    initialMouseDown = e.getPoint
  }

  override def mouseReleased(e: MouseEvent): Unit = {
    initialMouseDown = null
  }

  override def mouseDragged(e: MouseEvent): Unit = {
    if(initialMouseDown!=null) {
      // signed for 'normal' dragging, image follows mouse
      val dx = initialMouseDown.x - e.getX
      val dy = initialMouseDown.y - e.getY
      updateState(panel1.currentState.scrollTo(SENSITIVITY*dx,SENSITIVITY*dy))
    } else {
      // initialMouseDown not available for drag event
    }
  }
  
  override def mouseMoved(e: MouseEvent): Unit = { }

  override def mouseClicked(e: MouseEvent): Unit = {}

  override def mouseEntered(e: MouseEvent): Unit = {}

  override def mouseExited(e: MouseEvent): Unit = {}

  override def actionPerformed(event: ActionEvent): Unit = {
    event.getActionCommand match {
      case "Open" => 
        updateState(ReaderState(selectFile("select 1st comic"), selectFile("select 2nd comic")))
      case _ => println("unimplemented command "+ event.getActionCommand)
    }
  }
}

object EventHandler {
  val SENSITIVITY = 0.005

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
