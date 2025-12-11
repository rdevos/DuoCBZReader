package be.afront.reader

import CBZImages.Direction
import ReaderState.Mode
import ReaderState.Mode.{SingleEvenOdd, SingleOddEven}

import PartialState.pageForColumn

import java.awt.image.BufferedImage

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
  
  def pageChange(newPage:Int):PageSkip = {
    if(newPage == currentPage) (state=this, success=false) else
      (state=copy(currentPage = newPage), success=true)
  }

  def getPageIndicator:String =
    getIndicator(currentPage)

  def getIndicator(ix:Int): String =
    s"${ix + 1}/${pageCount}"


  def nextPage(delta:Int): PageSkip =
    pageChange(checkPage(currentPage + delta))

  def prevPage(delta:Int): PageSkip =
    pageChange(checkPage(currentPage - delta))

  def getCurrentImage(direction:Direction, mode:Mode, column:Int): Option[BufferedImage] = {
    val actualPage = pageForColumn(currentPage, column, direction, mode)
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
}

object PartialState {

  def apply(images: CBZImages):PartialState =
    new PartialState(images)

  def pageForColumn(page:Int, column:Int, direction:Direction, mode:Mode):Int = {
    mode match {
      case SingleEvenOdd | SingleOddEven => if(page %2 == mode.modulo) {
        page + direction.swapIfNeeded(column)
      } else {
        page - 1 + direction.swapIfNeeded(column)
      }
      case _ => page
    }
  }
}