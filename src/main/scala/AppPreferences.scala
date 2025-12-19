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

import java.io.{ByteArrayInputStream, ByteArrayOutputStream, ObjectInputStream, ObjectOutputStream}
import java.util.prefs.Preferences
import scala.util.{Try, Using}

object AppPreferences {

  private def KEY = "BULK"
  
  def saveObject(prefs: Preferences, obj: Serializable): Unit = {
    Using(new ByteArrayOutputStream()) { baos =>
      Using(new ObjectOutputStream(baos)) { oos =>
        oos.writeObject(obj)
      }
      prefs.putByteArray(KEY, baos.toByteArray)
    }.recover { case ex: Exception =>
      ex.printStackTrace()
    }
    prefs.flush()
  }

  def loadObject[T](prefs: Preferences): Option[T] = {
    val bytes = prefs.getByteArray(KEY, null)
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
}
