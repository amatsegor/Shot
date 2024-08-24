package com.karumi.shot.screenshots

import com.karumi.shot.Resources
import com.karumi.shot.domain.Dimension
import com.karumi.shot.domain.Screenshot
import com.karumi.shot.domain.model.ScreenshotsSuite
import com.karumi.shot.ui.Console
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should

import java.net.URL

class ScreenshotsComparatorTest extends AnyFlatSpec with should.Matchers with Resources {

  private val sampleImage                     = getResourceURL("images/sample.png").getPath
  private val sampleImageScrambled            = getResourceURL("images/sample-scrambled.png").getPath
  private val sampleImageWithTinyModification = getResourceURL("images/sample-tinymodified.png").getPath

  it should "not match image with same pixels but different order" in {
    val comparator = new ScreenshotsComparator()
    val screenshot = Screenshot(
      "test",
      sampleImage,
      sampleImageScrambled,
      "SomeClass",
      "ShoudFail",
      Dimension(768, 1280),
      null,
      null,
      null,
      List(sampleImageScrambled),
      Dimension(768, 1280)
    )
    val suite: ScreenshotsSuite = List(screenshot)

    val result = comparator.compare(screenshots = suite,
                                    tolerance = 0.01,
                                    parallelThreads = 2,
                                    console = new Console)

    result.hasErrors shouldBe true
  }

  it should "consider quasi-identical images as equal" in {
    val comparator = new ScreenshotsComparator()
    val screenshot = Screenshot(
      "test",
      sampleImage,
      sampleImageWithTinyModification,
      "SomeClass",
      "ShoudFail",
      Dimension(768, 1280),
      null,
      null,
      null,
      List(sampleImageWithTinyModification),
      Dimension(768, 1280)
    )
    val suite: ScreenshotsSuite = List(screenshot)

    val result = comparator.compare(screenshots = suite,
                                    tolerance = 0.1,
                                    parallelThreads = 2,
                                    console = new Console)

    result.hasErrors shouldBe false
  }

  it should "perform parallel comparison of 100 image pairs faster than sequential" in {
    val comparator = new ScreenshotsComparator()
    val screenshot = Screenshot(
      "test",
      sampleImage,
      sampleImageScrambled,
      "SomeClass",
      "ShoudFail",
      Dimension(768, 1280),
      null,
      null,
      null,
      List(sampleImageScrambled),
      Dimension(768, 1280)
    )

    val console = new Console

    val suite: ScreenshotsSuite = List.fill(100) {
      screenshot
    }

    val (_, parDuration) = measure {
      comparator.compare(screenshots = suite,
                         tolerance = 0.01,
                         parallelThreads = 2,
                         console = console)
    }

    val (_, seqDuration) = measure {
      comparator.compare(screenshots = suite,
                         tolerance = 0.01,
                         parallelThreads = 1,
                         console = console)
    }

    println(s"Parallel duration: $parDuration ms")
    println(s"Sequential duration: $seqDuration ms")

    parDuration should be < seqDuration
  }

  private def getResourceURL(path: String): URL = getClass.getClassLoader.getResource(path)

  private def measure[T](block: => T): (T, Long) = {
    val startTime = System.currentTimeMillis()
    val result    = block
    val endTime   = System.currentTimeMillis()
    val duration  = endTime - startTime
    (result, duration)
  }
}
