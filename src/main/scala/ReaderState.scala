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

  def getCurrentImage: Option[BufferedImage] =
    if (currentPage >= 0 && currentPage < pageCount) {
      Some(images.getImage(currentPage))
    } else {
      None
    }

  override def close(): Unit =
    images.close()
}

case class ReaderState(
   stateLeft: PartialState,
   stateRight: PartialState,
   zoomLevel: Int,
   hs: Double,
   vs: Double
) extends AutoCloseable {
  
  private def checkScroll(pos: Double): Double =
    math.max(0.0, math.min(1.0, pos))

  def this(images1: CBZImages, images2: CBZImages) =
    this(new PartialState(images1), new PartialState(images2), 0, 0.5, 0.5)

  def zoomFactor:Double = pow(1.5, zoomLevel)

  def nextPage: ReaderState = {
    val newLeftState = stateLeft.nextPage
    if(!newLeftState._2) this else copy(stateLeft = newLeftState._1, stateRight = stateRight.nextPage._1, hs = 0.5, vs = 0.0)
  }

  def prevPage: ReaderState = {
    val newLeftState = stateLeft.prevPage
    if(!newLeftState._2) this else copy(stateLeft = newLeftState._1, stateRight = stateRight.prevPage._1, hs = 0.5, vs = 0.0)
  }

  def zoomIn: ReaderState = copy(zoomLevel =
    zoomLevel + 1)

  def zoomOut: ReaderState = copy(zoomLevel =
    zoomLevel - 1)

  def minus: ReaderState = copy(stateRight = stateRight.prevPage._1)

  def plus: ReaderState = copy(stateRight = stateRight.nextPage._1)

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

  def getCurrentImage(column:Int): Option[BufferedImage] =
    if (column==1) stateLeft.getCurrentImage else if(column==2) stateRight.getCurrentImage else None

  override def close(): Unit = {
    stateLeft.close()
    stateRight.close()
  }
}

object ReaderState {

  def SCROLL_STEP = 0.125
  
  def apply(file1:File, file2:File): ReaderState =
    new ReaderState(new CBZImages(file1), new CBZImages(file2))
}
