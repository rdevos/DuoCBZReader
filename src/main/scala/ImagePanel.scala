package be.afront.reader

import java.awt.{Graphics, Graphics2D}
import java.awt.RenderingHints.{KEY_INTERPOLATION, VALUE_INTERPOLATION_BILINEAR}
import javax.swing.JPanel

class ImagePanel(initialState: ReaderState, column:Int) extends JPanel {

  private var state = initialState

  def setNewState(newState: ReaderState): Unit = {
    this.state = newState
    state.getCurrentImage(column)
  }
  
  def currentState:ReaderState =
    state  
  
  override def paintComponent(g: Graphics): Unit = {
    super.paintComponent(g)
    state.getCurrentImage(column).foreach { img =>
      val g2d = g.asInstanceOf[Graphics2D]
      g2d.setRenderingHint(KEY_INTERPOLATION, VALUE_INTERPOLATION_BILINEAR)

      val imgWidth = img.getWidth
      val imgHeight = img.getHeight
      val panelWidth = getWidth
      val panelHeight = getHeight

      val fitScale = Math.min(panelWidth.toDouble / imgWidth, panelHeight.toDouble / imgHeight)
      val scale = fitScale * state.zoomFactor

      val scaledWidth = (imgWidth * scale).toInt
      val scaledHeight = (imgHeight * scale).toInt

      val deltaX = panelWidth.toDouble - scaledWidth
      val deltaY = panelHeight.toDouble - scaledHeight
      
      val x = (state.hs * deltaX).toInt
      val y = (state.vs * deltaY).toInt

      g2d.drawImage(img, x, y, scaledWidth, scaledHeight, this)
    }
  }
}
