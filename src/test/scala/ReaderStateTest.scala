package be.afront.reader

import CBZImages.Direction.{LeftToRight, RightToLeft}
import ReaderState.Mode.{SingleEvenOdd, SingleOddEven}
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class ReaderStateTest  extends AnyFlatSpec with Matchers{

  "calculating page numbers for even odd with an even number n" should "return n and n+1" in {
    assert(PartialState.calcPage(0, 0, LeftToRight, SingleEvenOdd) == 0)
    assert(PartialState.calcPage(0, 1, LeftToRight, SingleEvenOdd) == 1)

    assert(PartialState.calcPage(0, 0, RightToLeft, SingleEvenOdd) == 1)
    assert(PartialState.calcPage(0, 1, RightToLeft, SingleEvenOdd) == 0)
  }

  "calculating page numbers for even odd with an odd number n" should "return n-1 and n" in {
    assert(PartialState.calcPage(1, 0, LeftToRight, SingleEvenOdd) == 0)
    assert(PartialState.calcPage(1, 1, LeftToRight, SingleEvenOdd) == 1)

    assert(PartialState.calcPage(1, 0, RightToLeft, SingleEvenOdd) == 1)
    assert(PartialState.calcPage(1, 1, RightToLeft, SingleEvenOdd) == 0)
  }

  "calculating page numbers for odd even with an even number n" should "return n-1 and n" in {
    assert(PartialState.calcPage(0, 0, LeftToRight, SingleOddEven) == -1)
    assert(PartialState.calcPage(0, 1, LeftToRight, SingleOddEven) == 0)

    assert(PartialState.calcPage(0, 0, RightToLeft, SingleOddEven) == 0)
    assert(PartialState.calcPage(0, 1, RightToLeft, SingleOddEven) == -1)
  }

  "calculating page numbers for odd even with an odd number n" should "return n and n+1" in {
    assert(PartialState.calcPage(1, 0, LeftToRight, SingleOddEven) == 1)
    assert(PartialState.calcPage(1, 1, LeftToRight, SingleOddEven) == 2)

    assert(PartialState.calcPage(1, 0, RightToLeft, SingleOddEven) == 2)
    assert(PartialState.calcPage(1, 1, RightToLeft, SingleOddEven) == 1)
  }


}
