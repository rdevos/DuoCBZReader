package be.afront.reader

import java.awt.GridLayout
import java.awt.event.{KeyAdapter, KeyEvent}
import java.awt.event.KeyEvent.{VK_2, VK_4, VK_6, VK_8, VK_DOWN, VK_LEFT, VK_NUMPAD2, VK_NUMPAD4, VK_NUMPAD6, VK_NUMPAD8, VK_Q, VK_RIGHT, VK_UP}
import java.io.File
import javax.swing.{JFrame, SwingUtilities}
import javax.swing.WindowConstants.EXIT_ON_CLOSE
import java.awt.{FileDialog, Frame}

object DuoCBZReader {

  case class Args(width:Int, height:Int, cbzPath1:File, cbzPath2:File)

  def main(args: Array[String]): Unit = {

    val parsedArgs = parseArgs(args)
    val state = ReaderState(parsedArgs.cbzPath1, parsedArgs.cbzPath2)

    val frame = new JFrame("CBZ Reader")
    frame.setSize(parsedArgs.width, parsedArgs.height)
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
        stateMachine(e.getKeyCode, panel1) match {
          case Some(newState) =>
            SwingUtilities.invokeLater { () =>
              panel1.setNewState(newState)
              panel2.setNewState(newState)
              frame.repaint()
            }
          case None =>
            frame.dispose()
            state.close()
        }
      }
    })
    frame.setVisible(true)
    frame.requestFocusInWindow()
  }

  private def stateMachine(keyCode:Int, panel1:ImagePanel):Option[ReaderState] = {
    keyCode match {
      case VK_RIGHT => Some(panel1.currentState.nextPage)
      case VK_LEFT => Some(panel1.currentState.prevPage)
      case VK_UP => Some(panel1.currentState.zoomIn)
      case VK_DOWN => Some(panel1.currentState.zoomOut)

      case VK_8 | VK_NUMPAD8 => Some(panel1.currentState.scrollUp)
      case VK_2 | VK_NUMPAD2 => Some(panel1.currentState.scrollDown)
      case VK_4 | VK_NUMPAD4 => Some(panel1.currentState.scrollLeft)
      case VK_6 | VK_NUMPAD6 => Some(panel1.currentState.scrollRight)

      case VK_Q => None
      case _ => Some(panel1.currentState)
    }
  }

  private def checkFileOrExit(path:String, exitCode:Int):File = {
    val file = new File (path)
    if(!file.exists()) {
      println("not found: " + file.getAbsolutePath)
      sys.exit(exitCode)
    }
    file
  }

  private def selectFile(prompt:String): File = {
    val dummyFrame = new Frame()
    dummyFrame.setSize(0, 0) // Make it invisible

    val dialog = new FileDialog(dummyFrame, prompt, FileDialog.LOAD)
    dialog.setVisible(true)

    val files = dialog.getFiles
    dialog.dispose()
    dummyFrame.dispose()

    files(0)
  }

  private def parseArgs(args: Array[String]):Args = {
    if (args.length == 0) {
      //TODO: base width and height on screen size
      Args(1600, 1100, selectFile("select left comic"), selectFile("select right comic"))
    } else if (args.length != 4) {
      println("Usage: DuoCBZReader <width> <height> <cbzPath1> <cbzPath2>")
      sys.exit(1)
    } else {

      val w = try args(0).toInt catch {
        case _: NumberFormatException => sys.exit(1)
      }
      val h = try args(1).toInt catch {
        case _: NumberFormatException => sys.exit(1)
      }
      Args(w, h, checkFileOrExit(args(2),2), checkFileOrExit(args(3),2))
    }
  }
}

