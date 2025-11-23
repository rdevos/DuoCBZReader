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

import ImagePanel.indicatorFont

import ReaderState.Size.Width

import java.awt.{Color, Font, Graphics, Graphics2D}
import java.awt.RenderingHints.{KEY_INTERPOLATION, VALUE_INTERPOLATION_BILINEAR}
import javax.swing.JPanel

class ImagePanel(initialState: ReaderState, column:Int) extends JPanel {

  private var state = initialState

  def setNewState(newState: ReaderState): Unit = {
    state = newState
    state.getCurrentImage(column)
  }
  
  override def paintComponent(g: Graphics): Unit = {
    super.paintComponent(g)
    state.getCurrentImage(column).foreach { img =>
      val g2d = g.asInstanceOf[Graphics2D]
      g2d.setRenderingHint(KEY_INTERPOLATION, VALUE_INTERPOLATION_BILINEAR)

      val imgWidth = img.getWidth
      val imgHeight = img.getHeight
      val panelWidth = getWidth
      val panelHeight = getHeight

      val widthScale = panelWidth.toDouble / imgWidth
      val heightScale = panelHeight.toDouble / imgHeight

      val fitScale = if(state.size == Width) widthScale else
        Math.min(widthScale, heightScale)

      val scale = fitScale * state.zoomFactor

      val scaledWidth = (imgWidth * scale).toInt
      val scaledHeight = (imgHeight * scale).toInt

      val deltaX = panelWidth.toDouble - scaledWidth
      val deltaY = panelHeight.toDouble - scaledHeight
      
      val x = (state.hs * deltaX).toInt
      val y = (state.vs * deltaY).toInt

      g2d.drawImage(img, x, y, scaledWidth, scaledHeight, this)

      if(state.showPageNumbers) {
        g2d.setColor(Color.BLACK)
        g2d.setFont(indicatorFont)
        g2d.drawString(state.getPageIndicator(column), 20, panelHeight - 20)
      }
    }
  }
}

object ImagePanel {
  private val indicatorFont: Font = new Font("SansSerif", Font.PLAIN, 12)
}
