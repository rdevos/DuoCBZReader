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
package state

import CBZImages.{Direction, PanelID}
import state.PartialState.pageForPanel
import state.ReaderState.Mode
import state.ReaderState.Mode.{SingleEvenOdd, SingleOddEven}

import java.awt.image.BufferedImage
import java.io.File

case class PartialState(
   images: CBZImages,
   currentPage: Int
 ) extends AutoCloseable {

  private val pageCount = images.totalPages

  def this(images: CBZImages) =
    this(images, if (images.totalPages > 0) 0 else -1)

  private def checkPage(page: Int): Int =
    math.max(0, math.min(pageCount - 1, page))

  def name:String =
    images.name
  
  private def pageChange(newPage:Int):PageSkip =
    if(newPage == currentPage) (state=this, success=false) else
      (state=copy(currentPage = newPage), success=true)

  def getPageIndicator:String =
    getIndicator(currentPage)

  def getIndicator(ix:Int): String =
    s"${ix + 1}/${pageCount}"

  def setPage(newPage:Int):PartialState =
    pageChange(checkPage(newPage)).state

  def nextPage(delta:Int): PageSkip =
    pageChange(checkPage(currentPage + delta))

  def prevPage(delta:Int): PageSkip =
    pageChange(checkPage(currentPage - delta))

  def getCurrentImage(direction:Direction, mode:Mode, panelID:PanelID): Option[BufferedImage] = {
    val actualPage = pageForPanel(currentPage, panelID, direction, mode)
    if (actualPage >= 0 && actualPage < pageCount) {
      Some(images.getImage(actualPage, direction))
    } else {
      None
    }
  }

  def partiallyClearCache(): Unit =
    images.partiallyClearCache()

  override def close(): Unit =
    images.close()

  def metadata:String =
    images.metadata
    
  def file:File =
    images.file  
}

object PartialState {

  // odd/even semantics use conventional page numbering starting from 1
  private def CONVENTIONAL_PAGE_NUMBERING_OFFSET = 1

  def apply(images: CBZImages):PartialState =
    new PartialState(images)

  def pageForPanel(page:Int, panel:PanelID, direction:Direction, mode:Mode):Int = {
    mode match {
      case SingleEvenOdd | SingleOddEven => if((page + CONVENTIONAL_PAGE_NUMBERING_OFFSET) %2 == mode.modulo) {
        page + panel.indexForDirection(direction)
      } else {
        page - 1 + panel.indexForDirection(direction)
      }
      case _ => page
    }
  }
}