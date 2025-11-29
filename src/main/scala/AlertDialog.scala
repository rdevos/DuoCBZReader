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

import java.awt.{Color, Dialog, Frame, Graphics}
import java.awt.event.{MouseAdapter, MouseEvent, WindowAdapter, WindowEvent}


class AlertDialog(owner: Frame, line1: String, line2:String) extends Dialog(owner, "Alert", true) {
  addMouseListener(new MouseAdapter {
    override def mouseClicked(e: MouseEvent): Unit = {
      dispose()
    }
  })

  addWindowListener(new WindowAdapter {
    override def windowClosing(e: WindowEvent): Unit = {
      dispose()
    }
  })

  setSize(550, 100)

  override def paint(g: Graphics): Unit = {
    super.paint(g)
    g.drawString(line1, 20, 50)
    g.setColor(Color.RED)
    g.drawString(line2, 20, 80)
  }
}