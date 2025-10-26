package be.afront.reader

import java.awt.GridLayout
import java.awt.event.{KeyAdapter, KeyEvent}
import java.awt.event.KeyEvent.{VK_2, VK_4, VK_6, VK_8, VK_DOWN, VK_LEFT, VK_NUMPAD2, VK_NUMPAD4, VK_NUMPAD6, VK_NUMPAD8, VK_Q, VK_RIGHT, VK_UP}
import java.io.File
import javax.swing.{JFrame, SwingUtilities}
import javax.swing.WindowConstants.EXIT_ON_CLOSE

object DuoCBZReader {

  def main(args: Array[String]): Unit = {
    if (args.length != 4) {
      println("Usage: DuoCBZReader <width> <height> <cbzPath1> <cbzPath2>")
      sys.exit(1)
    }

    val width = try args(0).toInt catch {
      case _: NumberFormatException => sys.exit(1)
    }
    val height = try args(1).toInt catch {
      case _: NumberFormatException => sys.exit(1)
    }
    val cbzPath1 = args(2)
    val cbzPath2 = args(3)

    val state = ReaderState(checkFileOrExit(cbzPath1,2), checkFileOrExit(cbzPath2,2))

    val frame = new JFrame("CBZ Reader")
    frame.setSize(width, height)
    frame.setUndecorated(true)
    frame.setDefaultCloseOperation(EXIT_ON_CLOSE)
    frame.setResizable(false)
    frame.setLayout(new GridLayout(1, 2))

    val panel1 = new ImagePanel(state, 1)
    val panel2 = new ImagePanel(state, 2)
    frame.add(panel1)
    frame.add(panel2)

    frame.addKeyListener(new KeyAdapter {
      override def keyPressed(e: KeyEvent): Unit = {
        val newState = e.getKeyCode match {
          case VK_RIGHT => panel1.currentState.nextPage
          case VK_LEFT => panel1.currentState.prevPage
          case VK_UP => panel1.currentState.zoomIn
          case VK_DOWN => panel1.currentState.zoomOut

          case VK_8 | VK_NUMPAD8 => panel1.currentState.scrollUp
          case VK_2 | VK_NUMPAD2 => panel1.currentState.scrollDown
          case VK_4 | VK_NUMPAD4 => panel1.currentState.scrollLeft
          case VK_6 | VK_NUMPAD6 => panel1.currentState.scrollRight

          // also close CBZImages for good measure
          case VK_Q => frame.dispose(); state.close(); return
          case _ => panel1.currentState
        }
        SwingUtilities.invokeLater { () =>
          panel1.setNewState(newState)
          panel2.setNewState(newState)
          frame.repaint()
        }
      }
    })
    frame.setVisible(true)
    frame.requestFocusInWindow()
  }

  def checkFileOrExit(path:String, exitCode:Int):File = {
    val file = new File (path)
    if(!file.exists()) {
      println("not found: " + file.getAbsolutePath)
      sys.exit(exitCode)
    }
    file
  }
}

