package be.afront.reader

import java.awt.event.{ItemEvent, ItemListener}
import java.awt.{CheckboxMenuItem, GridLayout, Menu, MenuBar, MenuItem, Toolkit}
import java.io.File
import javax.swing.JFrame
import javax.swing.WindowConstants.EXIT_ON_CLOSE
import EventHandler.selectFile

object DuoCBZReader {

  case class Args(width:Int, height:Int, cbzPath1:File, cbzPath2:File)

  def main(args: Array[String]): Unit = {

    val args:Args = initialArgs()
    val state = ReaderState(args.cbzPath1, args.cbzPath2)

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

    val handler = new EventHandler(frame, panel1, panel2)

    frame.setMenuBar(initMenus(handler))

    frame.addKeyListener(handler)
    frame.addMouseMotionListener(handler)
    frame.addMouseListener(handler)

    frame.setVisible(true)
    frame.requestFocusInWindow()
  }

  private def initMenus(handler:EventHandler):MenuBar = {
    val menuBar = new MenuBar()
    val fileMenu = new Menu("File")
    val openItem = new MenuItem("Open")
    openItem.addActionListener(handler)
    fileMenu.add(openItem)
    val leftToRight = new CheckboxMenuItem("Left To Right")
    leftToRight.setState(false)
    fileMenu.add(leftToRight)
    leftToRight.addItemListener((e: ItemEvent) => handler.directionChange(e.getStateChange))
    menuBar.add(fileMenu)
    menuBar
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
    val screenSize = Toolkit.getDefaultToolkit.getScreenSize
    val width = screenSize.getWidth.toInt
    val height = screenSize.getHeight.toInt
    Args(width, height, selectFile("select left comic"), selectFile("select right comic"))
}

