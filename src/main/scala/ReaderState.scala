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

import ReaderState.{Encoding, Mode, SCROLL_STEP, Size, ZOOM_STEP, modeFrom, processRecents}
import CBZImages.{Direction, PanelID}
import CBZImages.Direction.LeftToRight
import ReaderState.Mode.{Blank, Dual1, Dual1b, Dual2, Single, SingleEvenOdd, SingleOddEven}
import ReaderState.Size.Image
import PartialState.pageForPanel

import java.awt.image.BufferedImage
import java.io.File
import java.nio.charset.Charset
import scala.math.pow

type PageSkip = (state:PartialState, success:Boolean)



case class ReaderState(
   mode: Mode,
   partialStates: List[PartialState],
   zoomLevel: Int,
   hs: Double,
   vs: Double,
   size: Size,
   direction:Direction,
   encoding: Encoding,
   showPageNumbers:Boolean,
   recentFiles:List[List[File]]
) extends AutoCloseable {

  private def checkScroll(pos: Double): Double =
    math.max(0.0, math.min(1.0, pos))

  def this(cbzImages: List[CBZImages], size:Size, direction:Direction, encoding:Encoding, showPageNumbers:Boolean, recentFiles:List[List[File]]) =
    this(modeFrom(cbzImages.size),  cbzImages.map(m => PartialState(m)), 0, 0.5, 0.5, size, direction, encoding,  showPageNumbers, recentFiles)

  def this(cbzImages: List[CBZImages], currentState:ReaderState) =
    this(cbzImages, currentState.size, currentState.direction, currentState.encoding, currentState.showPageNumbers,
      processRecents(currentState.recentFiles, cbzImages.map(_.file)))

  def zoomFactor:Double = pow(ZOOM_STEP, zoomLevel)

  def partialNames:String =
    (if(direction ==LeftToRight) partialStates else partialStates.reverse)
      .map(_.name)
      .mkString(" / ")
  
  def right:ReaderState =
    if(direction == LeftToRight) nextPage else prevPage

  def left:ReaderState =
    if(direction == LeftToRight) prevPage else nextPage

  def withNewPartialStates(main: PartialState, f: PartialState => PageSkip, adjust: Boolean): ReaderState =
    val newPartialStates = main :: partialStates.tail.map(f(_).state)
    if (adjust) copy(partialStates = newPartialStates, hs = 0.5, vs = 0.0) else
      copy(partialStates = newPartialStates)

  def nextPage: ReaderState =
    val newState1 = partialStates.head.nextPage(mode.delta)
    if(!newState1.success) this else withNewPartialStates(newState1.state, _.nextPage(mode.delta), true)

  def prevPage: ReaderState =
    val newState1 = partialStates.head.prevPage(mode.delta)
    if(!newState1.success) this else withNewPartialStates(newState1.state, _.prevPage(mode.delta), true)

  def zoomIn: ReaderState =
    copy(zoomLevel = zoomLevel + 1)

  def zoomOut: ReaderState =
    copy(zoomLevel = zoomLevel - 1)

  def minus: ReaderState =
    if(partialStates.size<2) this else
      withNewPartialStates(partialStates.head, _.prevPage(1), false)

  def plus: ReaderState =
    if(partialStates.size<2) this else
      withNewPartialStates(partialStates.head, _.nextPage(1), false)

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

  def getCurrentImage(panelID:PanelID): Option[BufferedImage] = {

    val state = mode match {
      case Blank => None
      case Dual1 | Single | SingleEvenOdd | SingleOddEven => Option(partialStates.head)
      case Dual1b => Option(partialStates(1))
      case _ => {
        val signedColumn = panelID.indexForDirection(direction)
        if (signedColumn == 0) Option(partialStates.head) else if (signedColumn == 1) Option(partialStates(1)) else None
      }
    }
    state.flatMap(_.getCurrentImage(direction, mode, panelID))
  }

  def getPageIndicator(panelID:PanelID):String = {
    if (mode == SingleEvenOdd || mode == SingleOddEven)
      partialStates.head.getIndicator(pageForPanel(partialStates.head.currentPage, panelID, direction, mode))
    else partialStates(panelID.index).getPageIndicator
  }

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

  enum Mode(val description:String, val selectable:Boolean, val numFiles:Int, val modulo:Int, val delta:Int) extends MenuItemSource {
    case Blank extends Mode("Blank", false, 0, -1, 0)
    case Single extends Mode("MENU_ITEM_Single", true, 1, -1, 1)
    case SingleOddEven extends Mode("MENU_ITEM_SingleWideOddEven", true, 1, 0, 2)
    case SingleEvenOdd extends Mode("MENU_ITEM_SingleWideEvenOdd", true, 1, 1, 2)
    case Dual2 extends Mode("MENU_ITEM_Dual_2_columns", true, 2, -1, 1)
    case Dual1 extends Mode("MENU_ITEM_Dual_1_column", true, 2, -1, 1)
    case Dual1b extends Mode("Dual 1 column alt", false, 2, -1, 1)
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

  enum Help(val description: String, val relativeUrl:String) extends MenuItemSource {
    case UserGuide extends Help("MENU_ITEM_User_Guide", "user.html")
    case ChangeLog extends Help("MENU_ITEM_Change_log",  "log.html")

    def selectable: Boolean = true
  }


  def SCROLL_STEP = 0.125
  
  def ZOOM_STEP = 1.2

  def INITIAL_STATE = new ReaderState(List(), Image, LeftToRight, Encoding.DEFAULT, true, List());

  def apply(files:List[CBZImages], size:Size, direction:Direction, encoding:Encoding, showPageNumbers:Boolean, recentFiles:List[List[File]]): ReaderState =
    new ReaderState(files, size, direction, encoding, showPageNumbers, recentFiles)

  def modeFrom(size: Int):Mode =
    if (size == 2) Dual2 else if (size == 1) Single else Blank

  def processRecents(currentRecents:List[List[File]], newlyOpened:List[File]):List[List[File]] =
    if(currentRecents.contains(newlyOpened)) currentRecents else
      (newlyOpened::currentRecents).take(10)
}
