package be.afront.reader

import java.awt.color.ColorSpace
import java.awt.image.{BufferedImage, BufferedImageOp, ColorConvertOp}

object Filters {

  private val greyFilter = new ColorConvertOp(ColorSpace.getInstance(ColorSpace.CS_GRAY), null)

  private val tresholdUnsharpOp = ThresholdUnsharpOp(0.8, 2.5, 20)

  def applySelectedFilters(
    img: BufferedImage,
    greyscale: Boolean,
    sharpen: Boolean
  ): BufferedImage = {

    val activeOps: List[BufferedImageOp] = List(
      greyscale -> greyFilter,
      sharpen -> tresholdUnsharpOp,
    ).collect { case (true, op) => op }

    activeOps.foldLeft(img) { (current, op) =>
      op.filter(current, null)
    }
  }
}
