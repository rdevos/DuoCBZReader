package be.afront.reader

import ReaderState.{Encoding, Mode, Size}

import CBZImages.Direction


case class PersistedReaderState(
     mode: Mode,
     currentPages: List[Int],
     zoomLevel: Int,
     hs: Double,
     vs: Double,
     size: Size,
     direction:Direction,
     encoding: Encoding,
     showPageNumbers:Boolean) {
}
