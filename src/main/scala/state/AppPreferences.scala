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

package state

import state.AppPreferences.PreferenceKey

import AppPreferences.PreferenceKey.AutoRestore

case class AppPreferences(autoRestore:Boolean) {
  
  def changePreference(key:PreferenceKey, newValue:Boolean): AppPreferences =
    key match {
      case AutoRestore => copy(autoRestore = newValue)
    }
    
}

object AppPreferences {
  
  val DEFAULT = new AppPreferences(false)

  enum PreferenceKey {
    case AutoRestore
  }
}
