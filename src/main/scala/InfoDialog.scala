package be.afront.reader

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

import java.awt.{Dialog, Frame, TextArea}
import java.awt.event.{MouseAdapter, MouseEvent, WindowAdapter, WindowEvent}

class InfoDialog (owner: Frame, contents: String) extends Dialog(owner, "Info", true) {
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

  val textArea = new TextArea(contents, 20, 40, TextArea.SCROLLBARS_BOTH)
  textArea.setEditable(false)

  add(textArea)
  setSize(400, 300)

}