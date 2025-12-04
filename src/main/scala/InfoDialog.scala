package be.afront.reader

import java.awt.{Color, Dialog, Frame, Graphics, TextArea}
import java.awt.event.{MouseAdapter, MouseEvent, WindowAdapter, WindowEvent}

class InfoDialog (owner: Frame, contents: String) extends Dialog(owner, "Info", true) {
  addMouseListener(new MouseAdapter {
    override def mouseClicked(e: MouseEvent): Unit = {
      dispose()
    }
  })

  addWindowListener(new WindowAdapter {
    override def windowClosing(e: WindowEvent): Unit = {
      dispose()
    }
  })

  val textArea = new TextArea(contents, 20, 40, TextArea.SCROLLBARS_BOTH)
  textArea.setEditable(false)

  add(textArea)
  setSize(400, 300)

}