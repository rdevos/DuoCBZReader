package be.afront.reader

import ThresholdUnsharpOp.{clamp, rgbToLuminance}

import java.awt.*
import java.awt.geom.Point2D
import java.awt.geom.Rectangle2D
import java.awt.image.*


object ThresholdUnsharpOp {
  private def clamp(v: Int, min: Int, max: Int) = Math.max(min, Math.min(max, v))

  // correct colors for perceived brightness
  private def rgbToLuminance(rgb: Int) = {
    val r = (rgb >> 16) & 0xFF
    val g = (rgb >> 8) & 0xFF
    val b = rgb & 0xFF
    (0.299 * r + 0.587 * g + 0.114 * b + 0.5).toInt
  }
}

class ThresholdUnsharpOp(
    private val radius: Float,
    private val amount: Float,
    private val threshold: Int,
    private val hints: RenderingHints
) extends BufferedImageOp {

  if (radius < 0.1f) throw new IllegalArgumentException("radius too small")
  if (amount < 0f) throw new IllegalArgumentException("amount must be >= 0")
  if (threshold < 0) throw new IllegalArgumentException("threshold must be >= 0")

  def this(radius: Float, amount: Float, threshold: Int) =
    this(radius, amount, threshold, null)


  override def filter(src: BufferedImage, dest: BufferedImage): BufferedImage = {
    if (src == null) throw new NullPointerException("src image is null")
    if (amount <= 0f || radius <= 0f) {
      val dest2 = if (dest == null) createCompatibleDestImage(src, src.getColorModel) else dest
      val g2 = dest2.createGraphics
      try g2.drawImage(src, 0, 0, null)
      finally g2.dispose()
      return dest2
    }
    val blurred = gaussianBlur(src, radius)
    val dest2 = if (dest == null) createCompatibleDestImage(src, src.getColorModel) else dest
    val w = src.getWidth
    val h = src.getHeight
    for (y <- 0 until h) {
      for (x <- 0 until w) {
        val origRGB = src.getRGB(x, y)
        val blurRGB = blurred.getRGB(x, y)
        val origLum = rgbToLuminance(origRGB)
        val blurLum = rgbToLuminance(blurRGB)
        val diff = origLum - blurLum
        val absDiff = Math.abs(diff)
        val sharpenedLum =
          if (absDiff >= threshold)
            clamp(origLum + (amount * diff).round, 0, 255)
          else
            origLum
        dest2.setRGB(x, y, pixelValue(origRGB, origLum, sharpenedLum))
      }
    }
    dest2
  }

  private def pixelValue(origRGB: Int, origLum: Int, sharpenedLum: Int) = {
    val a = (origRGB >>> 24) & 0xFF

    def scaleChannel(c: Int, ratio: Float): Int =
      ThresholdUnsharpOp.clamp((c * ratio).round, 0, 255)

    val (r, g, b) =
      if (origLum == 0) (0, 0, 0)
      else {
        val rs = (origRGB >> 16) & 0xFF
        val gs = (origRGB >> 8) & 0xFF
        val bs = origRGB & 0xFF
        val ratio = sharpenedLum.toFloat / origLum
        (scaleChannel(rs, ratio), scaleChannel(gs, ratio), scaleChannel(bs, ratio))
      }

    (a << 24) | (r << 16) | (g << 8) | b
  }

  private def gaussianBlur(src: BufferedImage, radius: Float) = {
    var kernelSize = Math.max(3, (radius * 3).round | 1) // odd size

    if (kernelSize > 15) kernelSize = 15 // don't go crazy
    val kernelData = new Array[Float](kernelSize * kernelSize)
    var sum = 0f

    val value = 1f / (kernelSize * kernelSize)
    for (i <- kernelData.indices) {
      kernelData(i) = value
      sum += value
    }
    val kernel = new Kernel(kernelSize, kernelSize, kernelData)
    val blurOp = new ConvolveOp(kernel, ConvolveOp.EDGE_NO_OP, hints)
    var temp = src
    var result: BufferedImage = null

    val passes = Math.max(1, radius.round)
    for (i <- 0 until passes) {
      result = new BufferedImage(src.getWidth, src.getHeight, src.getType)
      temp = blurOp.filter(temp, result)
    }
    if (result != null) result
    else src
  }

  override def getBounds2D(src: BufferedImage): Rectangle2D = src.getRaster.getBounds

  override def createCompatibleDestImage(src: BufferedImage, destCM: ColorModel): BufferedImage = {
    val destCM2 = if (destCM == null) src.getColorModel else destCM
    new BufferedImage(destCM2, src.getRaster.createCompatibleWritableRaster, destCM2.isAlphaPremultiplied, null)
  }

  override def getPoint2D(srcPt: Point2D, dstPt: Point2D): Point2D = {
    val dstPt2 = if (dstPt == null) new Point2D.Float else dstPt
    dstPt2.setLocation(srcPt.getX, srcPt.getY)
    dstPt2
  }

  override def getRenderingHints: RenderingHints = hints
}
