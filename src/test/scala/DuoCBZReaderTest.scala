package be.afront.reader

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class DuoCBZReaderTest extends AnyFlatSpec with Matchers {

  "DuoCBZReader main" should "exit with code 1 on not enough arguments" in {
      val pb = new ProcessBuilder("java", "-cp", System.getProperty("java.class.path"), "be.afront.reader.DuoCBZReader",
      "800","1600")
      val p = pb.start
      val exitCode = p.waitFor
      assert(1 === exitCode)
  }

  "DuoCBZReader main" should "exit with code 2 on cbz file not present" in {
    val pb = new ProcessBuilder("java", "-cp", System.getProperty("java.class.path"), "be.afront.reader.DuoCBZReader",
      "800", "1600", "missing1.cbz", "missing2.cbz")
    val p = pb.start
    val exitCode = p.waitFor
    assert(2 === exitCode)
  }

}
