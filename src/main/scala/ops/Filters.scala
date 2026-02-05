/*
  Copyright 2026 Paul Janssens - All rights reserved

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
package ops

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
