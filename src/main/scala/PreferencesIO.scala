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

import state.{AggregatePersistedState, AppPreferences, RecentState, RecentStates}

import java.io.{ByteArrayInputStream, ByteArrayOutputStream, ObjectInputStream, ObjectOutputStream}
import java.util.prefs.Preferences
import scala.util.{Try, Using}

object PreferencesIO {

  private def KEY_PREFIX = "BULK_v4"
  
  def saveObject(prefs: Preferences, key:String, obj: Serializable): Unit = {
    Using(new ByteArrayOutputStream()) { baos =>
      Using(new ObjectOutputStream(baos)) { oos =>
        oos.writeObject(obj)
      }
      prefs.putByteArray(key, baos.toByteArray)
    }.recover { case ex: Exception =>
      ex.printStackTrace()
    }
    prefs.flush()
  }

  def loadObject[T](prefs: Preferences, key:String): Option[T] = {
    val bytes = prefs.getByteArray(key, null)
    if (bytes == null || bytes.isEmpty) {
      None
    } else {
      val result: Try[Try[T]] = Using(new ByteArrayInputStream(bytes)) { bais =>
        Using(new ObjectInputStream(bais)) { ois =>
          ois.readObject().asInstanceOf[T]
        }
      }
      result.flatten.toOption
    }
  }

  def prefs: Preferences = Preferences.userNodeForPackage(classOf[DuoCBZReader.type])

  def load: Option[AggregatePersistedState] = {
    loadObject[AppPreferences](prefs, KEY_PREFIX).map(appPrefs => new AggregatePersistedState(
      appPrefs, new RecentStates(Range(0, 10).flatMap(ix => loadObject[RecentState](prefs, KEY_PREFIX+"_"+ix)).toList)))
  }

  def save(toPersist:AggregatePersistedState):Unit = {
    saveObject(prefs, KEY_PREFIX, toPersist.preferences)
    toPersist.recentStates.states.zipWithIndex.foreach((s, ix) => saveObject(prefs, KEY_PREFIX+"_"+ix, s))
  }
}
