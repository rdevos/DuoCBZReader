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
package state

import state.RecentStates.EMPTY

import java.io.File

case class RecentStates(states:List[RecentState]) extends Serializable {

  def clear:RecentStates =
    EMPTY

  def nonEmpty:Boolean =
    states.nonEmpty

  def filesWereOpened(newlyOpened: RecentState):RecentStates = 
    RecentStates((newlyOpened :: states.filter(_ != newlyOpened)).take(10))

  def foreach(f:RecentState => Unit):Unit =
    states.foreach(f)

  def updateWhere(files:List[File], updated:PersistedReaderState):RecentStates =
      RecentStates(states.map(rs =>  if (rs.files == files) RecentState(files, updated) else rs))
}

object RecentStates {

  def EMPTY: RecentStates =
    RecentStates(List())
}
