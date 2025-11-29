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

import java.util.{Locale, ResourceBundle}

class ResourceLookup(bundle:ResourceBundle) {

  def apply(key:ResourceKey):String =
    bundle.getString(key.description)
}

object ResourceLookup {

  def apply(locale:Locale) =
    new ResourceLookup(ResourceBundle.getBundle("be.afront.resources.ui", locale))

  enum Label(val description: String) extends ResourceKey {
    case Application extends Label("APPLICATION")
  }

  enum MenuKey(val description: String) extends ResourceKey {
    case File extends MenuKey("MENU_File")
    case Mode extends MenuKey("MENU_Mode")
    case Size extends MenuKey("MENU_Size")
    case Options extends MenuKey("MENU_Options")
  }

  enum MenuItemKey(val description: String) extends ResourceKey {
    case Open extends MenuItemKey("MENU_ITEM_Open")
    case RightToLeft extends MenuItemKey("MENU_ITEM_Right_To_Left")
    case PageNumbers extends MenuItemKey("MENU_ITEM_Page_Numbers")
  }

  enum MessageKey(val description: String) extends ResourceKey {
    case Copyright extends MessageKey("COPYRIGHT_MESSAGE")
    case About extends MessageKey("ABOUT_MESSAGE")
  }
}

