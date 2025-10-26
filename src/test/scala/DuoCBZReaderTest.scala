package be.afront.reader

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class DuoCBZReaderTest extends AnyFlatSpec with Matchers {

  "DuoCBZReader main" should "exit with code 1 on not enough arguments" in {
      val pb = new ProcessBuilder("java", "-cp", System.getProperty("java.class.path"), "be.afront.reader.DuoCBZReader")
      val p = pb.start
      val exitCode = p.waitFor
      assert(1 === exitCode)
  }
}
