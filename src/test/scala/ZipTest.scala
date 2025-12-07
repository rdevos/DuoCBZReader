package be.afront.reader

import ReaderState.Encoding.{CP932, DEFAULT}
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

import java.io.File
import scala.util.Using

class ZipTest extends AnyFlatSpec with Matchers {

  "CBZImages using default encoding" should "contain garbled entry name" in {
    val resource = getClass.getResource("/small.zip")
    Using(new CBZImages(new File(resource.toURI), DEFAULT)) {
      cbz => assert(cbz.entries.head === "????l.png")
    }.get
  }

  "CBZImages using correct encoding" should "contain correct entry name" in {
    val resource = getClass.getResource("/small.zip")
    Using(new CBZImages(new File(resource.toURI), CP932)) {
      cbz => assert(cbz.entries.head === "ああl.png")
    }.get
  }
}
