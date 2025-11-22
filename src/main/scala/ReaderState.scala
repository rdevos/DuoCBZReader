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

import ReaderState.{Mode, SCROLL_STEP, ZOOM_STEP}
import CBZImages.Direction
import CBZImages.Direction.LeftToRight

import be.afront.reader.PartialState.fromImage
import be.afront.reader.ReaderState.Mode.{Dual1, Dual1b, Single}

import java.awt.image.BufferedImage
import java.io.File
import scala.math.pow

type PageSkip = (state:PartialState, success:Boolean)

case class PartialState(
  images: CBZImages,
  currentPage: Int
) extends AutoCloseable {
  private val pageCount = images.totalPages

  def this(images: CBZImages) =
    this(images, if (images.totalPages > 0) 0 else -1)

  private def checkPage(page: Int): Int =
    math.max(0, math.min(pageCount - 1, page))

  def pageChange(newPage:Int):PageSkip = {
    if(newPage == currentPage) (state=this, success=false) else
      (state=copy(currentPage = newPage), success=true)
  }

  def getPageIndicator:String =
    s"${currentPage+1}/${pageCount}"

  def nextPage: PageSkip =
    pageChange(checkPage(currentPage + 1))

  def prevPage: PageSkip =
    pageChange(checkPage(currentPage - 1))

  def getCurrentImage(direction:Direction): Option[BufferedImage] =
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

object PartialState {

  def fromImage(images: CBZImages):PartialState =
    if(images == null) null else new PartialState(images)
}

case class ReaderState(
   mode: Mode,
   state1: PartialState,
   state2: PartialState,
   zoomLevel: Int,
   hs: Double,
   vs: Double,
   direction:Direction,   
   showPageNumbers:Boolean                   
) extends AutoCloseable {
  
  private def checkScroll(pos: Double): Double =
    math.max(0.0, math.min(1.0, pos))

  def this(mode:Mode, images1: CBZImages, images2: CBZImages, direction:Direction, showPageNumbers:Boolean) =
    this(mode, fromImage(images1), fromImage(images2), 0, 0.5, 0.5, direction, showPageNumbers)

  def zoomFactor:Double = pow(ZOOM_STEP, zoomLevel)

  def right:ReaderState =
    if(direction == LeftToRight) nextPage else prevPage

  def left:ReaderState =
    if(direction == LeftToRight) prevPage else nextPage

  def safe2(f: PartialState=>PartialState): PartialState =
    if(state2==null) null else f(state2)

  def nextPage: ReaderState = {
    val newState1 = state1.nextPage
    if(!newState1.success) this else
      copy(state1 = newState1.state, state2 = safe2(_.nextPage.state), hs = 0.5, vs = 0.0)
  }

  def prevPage: ReaderState = {
    val newState1 = state1.prevPage
    if(!newState1.success) this else
      copy(state1 = newState1.state, state2 = safe2(_.prevPage.state), hs = 0.5, vs = 0.0)
  }

  def zoomIn: ReaderState = copy(zoomLevel =
    zoomLevel + 1)

  def zoomOut: ReaderState = copy(zoomLevel =
    zoomLevel - 1)

  def minus: ReaderState = copy(state2 = safe2(_.prevPage.state))

  def plus: ReaderState = copy(state2 = safe2(_.nextPage.state))

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

  def scrollVertical(dy: Double): ReaderState =
    scrollTo(hs, vs+dy)

  def getCurrentImage(column:Int): Option[BufferedImage] = {
    println(s"image requested for column $column, direction = $direction")
    if(mode == Dual1 || mode == Single) {
      state1.getCurrentImage(direction)
    } else if (mode == Dual1b) {
      state2.getCurrentImage(direction)
    } else {
      // This is a bit of a pain, but we do this so panel1 is always to the left of pane;2
      val signedColumn = if (direction == LeftToRight) column else 1 - column
      if (signedColumn == 0) state1.getCurrentImage(direction) else if (signedColumn == 1 && state2 != null) state2.getCurrentImage(direction) else None
    }
  }

  def getPageIndicator(column:Int):String =
    (if(column==0) state1 else state2).getPageIndicator

  def setDirection(dir:Direction): ReaderState = {
    state1.partiallyClearCache()
    if(state2 != null) state2.partiallyClearCache()
    copy(direction = dir)
  }

  def setShowPageNumbers(show:Boolean):ReaderState = {
    copy(showPageNumbers = show)
  }

  def setMode(newMode:Mode):ReaderState = {
    copy(mode = newMode)
  }
  
  override def close(): Unit = {
    state1.close()
    if(state2!=null) state2.close()
  }
}

object ReaderState {

  enum Mode {
    case Blank, Single, Dual2, Dual1, Dual1b
  }

  def SCROLL_STEP = 0.125
  
  def ZOOM_STEP = 1.2
  
  def apply(mode:Mode, file1:File, file2:File, direction:Direction, showPageNumbers:Boolean): ReaderState =
    new ReaderState(mode,
      Option(file1).map(new CBZImages(_)).orNull,
      Option(file2).map(new CBZImages(_)).orNull, direction, showPageNumbers)
}
