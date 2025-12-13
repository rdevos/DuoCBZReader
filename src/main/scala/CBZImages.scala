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

import CBZImages.{Dimensions, Direction, Part, getDimensions}
import CBZImages.Part.{First, Latter}
import ReaderState.Encoding

import be.afront.reader.CBZImages.Direction.{LeftToRight, RightToLeft}

import java.awt.image.BufferedImage
import org.apache.commons.compress.archivers.zip.{ZipArchiveEntry, ZipFile}

import java.awt.Graphics2D
import java.io.File
import javax.imageio.stream.ImageInputStream
import javax.imageio.{ImageIO, ImageReader, ImageTypeSpecifier}
import scala.collection.mutable
import scala.jdk.CollectionConverters.*
import scala.util.{Try, Using}


class CBZImages(file: File, encoding: Encoding) extends AutoCloseable {

  extension [B](builder: B)
    def setIfNonNull[T](value: T)(setter: (B, T) => B): B =
      if (value != null) setter(builder, value) else builder

  private val zipFile: ZipFile = ZipFile.builder()
    .setFile(file)
    .setIfNonNull(encoding.charset)((b,d)=> b.setCharset(d))
    .get()
  
  private val rootEntries: List[ZipArchiveEntry] = {
    zipFile.getEntries.asScala.toList
      .filter(entry => !entry.isDirectory && isImageEntry(entry.getName))
      .sortBy(_.getName)
  }

  type EntryName = String

  type ImageIndex = Int
  
  type PageIndex = Int

  type Combo = (rawPage: ImageIndex, part:Option[Part])
  
  val entries: List[EntryName] = rootEntries.map(_.getName)

  def name:String =
    file.getName
    
  private val dimensions: Map[EntryName, Dimensions] =
    (for {
      entry <- rootEntries
      d <- getDimensions(zipFile, entry)
    } yield entry.getName -> d).toMap

  private val wideIndices: Set[ImageIndex] = entries.indices
    .filter(i => dimensions.get(entries(i)).exists(d => d.width > d.height))
    .toSet

  private val widePages: Int = wideIndices.size

  val totalPages: Int = rootEntries.size + widePages

  private val pageMap: Map[PageIndex, Combo] = {
    entries.indices.flatMap { rawIdx =>
      if (wideIndices.contains(rawIdx)) {
        Seq((rawIdx, Some(First)), (rawIdx, Some(Latter)))
      } else {
        Seq((rawIdx, None))
      }
    }.zipWithIndex.map { case ((rawIdx, part), pageIdx) =>
      pageIdx -> (rawIdx, part)
    }.toMap
  }

  private val cache: mutable.Map[PageIndex, BufferedImage] = mutable.Map.empty

  def partiallyClearCache():Unit =
    pageMap.collect { case (page, (raw, part)) => page }.foreach(cache.remove)

  def getImage(page: Int, direction:Direction): BufferedImage = {

    if (page < 0 || page >= totalPages) {
      throw new IndexOutOfBoundsException(s"Page index $page out of bounds [0, ${totalPages - 1}]")
    }

    cache.getOrElseUpdate(page, {
      val combo = pageMap(page)
      val entry = rootEntries(combo.rawPage)
      val fullImg = Using(zipFile.getInputStream(entry)) { inputStream =>
        ImageIO.read(inputStream)
      }.getOrElse(null)
      if(fullImg == null)
        throw new ImageDecodingException("could not decode file "+entry);
      combo.part.map(part => part.ofImage(fullImg, direction)).getOrElse(fullImg)
    })
  }

  override def close(): Unit = {
    zipFile.close()
  }

  def metadata:String = {
    entries.map(entry => {
      val dim:Dimensions = dimensions(entry)
      s"$entry: $dim"
    }).mkString("\n")
  }

  private def isImageEntry(name: String): Boolean = {
    val lower = name.toLowerCase
    (lower.endsWith(".jpg") || lower.endsWith(".jpeg") || lower.endsWith(".png") || lower.endsWith(".gif")) &&
      !name.contains("__MACOSX")
  }
}

object CBZImages {

  type Dimensions = (width: Int, height: Int, depth:Int, size:Long)

  type FileCheck = (file: File, image: Try[CBZImages])

  enum PanelID(var index:Int) {
    case LeftOrFront extends PanelID(0)
    case RightOrBack extends PanelID(1)

    def indexForDirection(direction:Direction):Int = direction match {
      case LeftToRight => index
      case RightToLeft => 1 - index
    }
  }
  
  enum Side {
    case Left, Right
  }

  enum Direction {
    case RightToLeft, LeftToRight

    def sideFor(part:Part):Side = this match {
      case LeftToRight => if(part == First) Side.Left else Side.Right
      case RightToLeft => if(part == First) Side.Right else Side.Left
    }
  }

  enum Part {
    case First, Latter

    def ofImage(img: BufferedImage, direction: Direction): BufferedImage = {
      val fullWidth = img.getWidth
      val halfWidth = fullWidth / 2
      val height = img.getHeight
      val halfImg = new BufferedImage(halfWidth, height, img.getType)
      val g: Graphics2D = halfImg.createGraphics()
      val xOffset = if (direction.sideFor(this) == Side.Left) 0 else fullWidth - halfWidth
      g.drawImage(img, 0, 0, halfWidth, height, xOffset, 0, xOffset + halfWidth, height, null)
      g.dispose()
      halfImg
    }
  }

  def checkFile(file: File, encoding:Encoding): FileCheck =
    (file, Try(new CBZImages(file, encoding)))

  given imageReaderReleasable: Using.Releasable[ImageReader] with
    def release(r: ImageReader): Unit =
      r.dispose()

  private def getDimensions(zipFile: ZipFile, entry: ZipArchiveEntry): Option[Dimensions] =
    Using.Manager { use =>
      val in = use(zipFile.getInputStream(entry))
      val iis = use(ImageIO.createImageInputStream(in))
      val readers = ImageIO.getImageReaders(iis).asScala.to(LazyList)
      readers.flatMap { reader => tryReadDimensions(use, reader, iis, entry.getSize) }.headOption
    }.toOption.flatten

  private def tryReadDimensions(use:Using.Manager, reader:ImageReader, input:ImageInputStream, uncompressedSize:Long):Option[Dimensions] =
    Try {
      reader.setInput(input)
      val r = use(reader)
      (width = r.getWidth(0), height = r.getHeight(0), depth = totalBitDepth(r.getRawImageType(0)), size = uncompressedSize)
    }.toOption

  private def totalBitDepth(specifier: ImageTypeSpecifier): Int =
    (0 until specifier.getNumBands).map(specifier.getBitsPerBand).sum
}