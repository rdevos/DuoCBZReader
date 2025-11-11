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

import ReaderState.SCROLL_STEP

import java.awt.image.BufferedImage
import java.io.File
import scala.math.pow

case class PartialState(
  images: CBZImages,
  currentPage: Int,
) extends AutoCloseable {
  private val pageCount = images.totalPages

  def this(images: CBZImages) =
    this(images, if (images.totalPages > 0) 0 else -1)

  private def checkPage(page: Int): Int =
    math.max(0, math.min(pageCount - 1, page))

  def pageChange(newPage:Int):(PartialState,Boolean) =
    if(newPage == currentPage) (this,false) else (copy(currentPage = newPage),true)
  
  def nextPage: (PartialState,Boolean) =
    pageChange(checkPage(currentPage + 1))

  def prevPage: (PartialState,Boolean) =
    pageChange(checkPage(currentPage - 1))

  def getCurrentImage(direction:Int): Option[BufferedImage] =
    if (currentPage >= 0 && currentPage < pageCount) {
      Some(images.getImage(currentPage, direction))
    } else {
      None
    }

  def partiallyClearCache(): Unit =
    images.partiallyClearCache()  
  
  override def close(): Unit =
    images.close()
}

case class ReaderState(
   state1: PartialState,
   state2: PartialState,
   zoomLevel: Int,
   hs: Double,
   vs: Double,
   direction:Int                   
) extends AutoCloseable {
  
  private def checkScroll(pos: Double): Double =
    math.max(0.0, math.min(1.0, pos))

  def this(images1: CBZImages, images2: CBZImages) =
    this(new PartialState(images1), new PartialState(images2), 0, 0.5, 0.5, 0)

  def zoomFactor:Double = pow(1.5, zoomLevel)

  def right:ReaderState =
    if(direction == 0) nextPage else prevPage

  def left:ReaderState =
    if(direction == 0) prevPage else nextPage
  
  def nextPage: ReaderState = {
    val newState1 = state1.nextPage
    if(!newState1._2) this else copy(state1 = newState1._1, state2 = state2.nextPage._1, hs = 0.5, vs = 0.0)
  }

  def prevPage: ReaderState = {
    val newState1 = state1.prevPage
    if(!newState1._2) this else copy(state1 = newState1._1, state2 = state2.prevPage._1, hs = 0.5, vs = 0.0)
  }

  def zoomIn: ReaderState = copy(zoomLevel =
    zoomLevel + 1)

  def zoomOut: ReaderState = copy(zoomLevel =
    zoomLevel - 1)

  def minus: ReaderState = copy(state2 = state2.prevPage._1)

  def plus: ReaderState = copy(state2 = state2.nextPage._1)

  def conditionalScroll(scrolled: => ReaderState):ReaderState =
    if(zoomLevel>0) scrolled else this

  private def horizontalScroll(delta: Double): ReaderState =
    conditionalScroll(copy(hs = checkScroll(hs + delta)))

  private def verticalScroll(delta: Double): ReaderState =
    conditionalScroll(copy(vs = checkScroll(vs + delta)))

  def scrollLeft: ReaderState =
    horizontalScroll(-SCROLL_STEP)

  def scrollRight: ReaderState =
    horizontalScroll(SCROLL_STEP)

  def scrollUp: ReaderState =
    verticalScroll(-SCROLL_STEP)

  def scrollDown: ReaderState =
    verticalScroll(SCROLL_STEP)

  def scrollTo(px:Double, py:Double): ReaderState =
    copy(hs=checkScroll(px), vs=checkScroll(py))

  def getCurrentImage(column:Int): Option[BufferedImage] = {
    val signedColumn = if(direction==0) column else 1-column
    if (signedColumn==0) state1.getCurrentImage(direction) else if(signedColumn==1) state2.getCurrentImage(direction) else None
  }

  def setDirection(dir:Int): ReaderState = {
    state1.partiallyClearCache()
    state2.partiallyClearCache()
    copy(direction = dir)
  }

  override def close(): Unit = {
    state1.close()
    state2.close()
  }
}

object ReaderState {

  def SCROLL_STEP = 0.125
  
  def apply(file1:File, file2:File): ReaderState =
    new ReaderState(new CBZImages(file1), new CBZImages(file2))
}
