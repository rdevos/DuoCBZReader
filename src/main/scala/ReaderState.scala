/*
  Copyright 2025 Paul Janssens - All rights reserved

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

import ReaderState.{Encoding, Mode, SCROLL_STEP, Size, ZOOM_STEP, modeFrom}
import CBZImages.Direction
import CBZImages.Direction.LeftToRight
import ReaderState.Mode.{Blank, Dual1, Dual1b, Dual2, Single}
import ReaderState.Size.Image

import java.awt.image.BufferedImage
import java.nio.charset.{Charset, StandardCharsets}
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
    
  def metadata:String =
    images.metadata  
}

object PartialState {

  def apply(images: CBZImages):PartialState =
    new PartialState(images)
}

case class ReaderState(
   mode: Mode,
   partialStates: List[PartialState],
   zoomLevel: Int,
   hs: Double,
   vs: Double,
   size: Size,
   direction:Direction,
   encoding: Encoding,
   showPageNumbers:Boolean                   
) extends AutoCloseable {

  private def checkScroll(pos: Double): Double =
    math.max(0.0, math.min(1.0, pos))

  def this(cbzImages: List[CBZImages], size:Size, direction:Direction, encoding:Encoding, showPageNumbers:Boolean) =
    this(modeFrom(cbzImages.size),  cbzImages.map(m => PartialState(m)), 0, 0.5, 0.5, size, direction, encoding,  showPageNumbers)

  def this(cbzImages: List[CBZImages], currentState:ReaderState) =
    this(cbzImages, currentState.size, currentState.direction, currentState.encoding, currentState.showPageNumbers)

  def zoomFactor:Double = pow(ZOOM_STEP, zoomLevel)

  def right:ReaderState =
    if(direction == LeftToRight) nextPage else prevPage

  def left:ReaderState =
    if(direction == LeftToRight) prevPage else nextPage

  def withNewPartialStates(main: PartialState, f: PartialState => PageSkip, adjust: Boolean): ReaderState =
    val newPartialStates = main :: partialStates.tail.map(f(_).state)
    if (adjust) copy(partialStates = newPartialStates, hs = 0.5, vs = 0.0) else
      copy(partialStates = newPartialStates)

  def nextPage: ReaderState =
    val newState1 = partialStates.head.nextPage
    if(!newState1.success) this else withNewPartialStates(newState1.state, _.nextPage, true)

  def prevPage: ReaderState =
    val newState1 = partialStates.head.prevPage
    if(!newState1.success) this else withNewPartialStates(newState1.state, _.prevPage, true)

  def zoomIn: ReaderState =
    copy(zoomLevel = zoomLevel + 1)

  def zoomOut: ReaderState =
    copy(zoomLevel = zoomLevel - 1)

  def minus: ReaderState =
    if(partialStates.size<2) this else
      withNewPartialStates(partialStates.head, _.prevPage, false)

  def plus: ReaderState =
    if(partialStates.size<2) this else
      withNewPartialStates(partialStates.head, _.nextPage, false)

  private def horizontalScroll(delta: Double): ReaderState =
    copy(hs = checkScroll(hs + delta))

  private def verticalScroll(delta: Double): ReaderState =
    copy(vs = checkScroll(vs + delta))

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

    val state = mode match {
      case Blank => None
      case Dual1 | Single => Option(partialStates.head)
      case Dual1b => Option(partialStates(1))
      case _ => {
        val signedColumn = direction.swapIfNeeded(column)
        if (signedColumn == 0) Option(partialStates.head) else if (signedColumn == 1) Option(partialStates(1)) else None
      }
    }
    state.flatMap(_.getCurrentImage(direction))
  }

  def getPageIndicator(column:Int):String =
    (if(column==0) partialStates.head else partialStates(1)).getPageIndicator

  def setDirection(dir:Direction): ReaderState = {
    partialStates.foreach(_.partiallyClearCache())
    copy(direction = dir)
  }

  def setShowPageNumbers(show:Boolean):ReaderState =
    copy(showPageNumbers = show)

  def setMode(newMode:Mode):ReaderState =
    copy(mode = newMode)

  def setSize(newSize: Size): ReaderState =
    copy(size = newSize, zoomLevel = 0)

  def setEncoding(newEncoding: Encoding): ReaderState =
    copy(encoding = newEncoding)

  override def close(): Unit =
    partialStates.foreach(_.close())
}

object ReaderState {

  trait MenuItemSource extends ResourceKey {
    def selectable:Boolean
  }

  enum Mode(val description:String, val selectable:Boolean) extends MenuItemSource {
    case Blank extends Mode("Blank", false)
    case Single extends Mode("Single", false)
    case Dual2 extends Mode("MENU_ITEM_Dual_2_columns", true)
    case Dual1 extends Mode("MENU_ITEM_Dual_1_column", true)
    case Dual1b extends Mode("Dual 1 column alt", false)
  }

  enum Size(val description:String) extends MenuItemSource {
    case Image extends Size("MENU_ITEM_Fit_image")
    case Width extends Size("MENU_ITEM_Fit_width")
    case Actual extends Size("MENU_ITEM_Actual")

    def selectable:Boolean = true
  }

  enum Encoding(val description:String, val charset:Charset) extends MenuItemSource {
    case DEFAULT extends Encoding("MENU_ITEM_ENCODING_Default", null)
    case LATIN1 extends Encoding("MENU_ITEM_ENCODING_Latin1", Charset.forName("ISO-8859-1"))
    case CP932 extends Encoding("MENU_ITEM_ENCODING_CP932", Charset.forName("CP932"))

    def selectable: Boolean = true
  }

  def SCROLL_STEP = 0.125
  
  def ZOOM_STEP = 1.2

  def INITIAL_STATE = new ReaderState(List(), Image, LeftToRight, Encoding.DEFAULT, true);

  def apply(files:List[CBZImages], size:Size, direction:Direction, encoding:Encoding, showPageNumbers:Boolean): ReaderState =
    new ReaderState(files, size, direction, encoding, showPageNumbers)

  def modeFrom(size: Int):Mode =
    if (size == 2) Dual2 else if (size == 1) Single else Blank
}
