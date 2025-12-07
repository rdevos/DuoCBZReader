package be.afront.reader

import ReaderState.Encoding.{CP932, DEFAULT, LATIN1}

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

import java.io.File
import scala.util.Using

class ZipTest extends AnyFlatSpec with Matchers {

  "jap CBZImages using default encoding" should "contain garbled entry name" in {
    val resource = getClass.getResource("/cp932.zip")
    Using(new CBZImages(new File(resource.toURI), DEFAULT)) {
      cbz => assert(cbz.entries.head === "????l.png")
    }.get
  }

  "jap CBZImages using correct encoding" should "contain correct entry name" in {
    val resource = getClass.getResource("/cp932.zip")
    Using(new CBZImages(new File(resource.toURI), CP932)) {
      cbz => assert(cbz.entries.head === "ああl.png")
    }.get
  }

  "euro CBZImages using default encoding" should "contain garbled entry name" in {
    val resource = getClass.getResource("/8859-1.zip")
    Using(new CBZImages(new File(resource.toURI), DEFAULT)) {
      cbz => assert(cbz.entries.head === "????l.png")
    }.get
  }

  "euro CBZImages using correct encoding" should "contain correct entry name" in {
    val resource = getClass.getResource("/8859-1.zip")
    Using(new CBZImages(new File(resource.toURI), LATIN1)) {
      cbz => assert(cbz.entries.head === "ÀÁàál.png")
    }.get
  }
}
