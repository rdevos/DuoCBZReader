package be.afront.reader

import java.awt.{FileDialog, Frame, GridLayout}
import java.io.File
import javax.swing.JFrame
import javax.swing.WindowConstants.EXIT_ON_CLOSE

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

    val handler = new EventHandler(frame, panel1, panel2)

    frame.addKeyListener(handler)
    frame.addMouseMotionListener(handler)
    frame.addMouseListener(handler)

    frame.setVisible(true)
    frame.requestFocusInWindow()
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

  private def noArguments(args: Array[String]):Boolean =
    args.length == 0
}

