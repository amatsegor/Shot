package com.karumi.shot.screenshots

import com.karumi.shot.domain._
import com.karumi.shot.domain.model.ScreenshotsSuite
import com.karumi.shot.ui.Console
import com.sksamuel.scrimage.ImmutableImage

import java.io.File
import scala.math.BigDecimal.RoundingMode

class ScreenshotsComparator {

  private val differencePercentageScale = 5

  def compare(screenshots: ScreenshotsSuite,
              tolerance: Double,
              console: Console): ScreenshotsComparisionResult = {
    console.showWarning("Comparing screenshots in a sequential mode, this could take a while...")

    val errors = screenshots.flatMap(compareScreenshot(_, tolerance)).toList
    ScreenshotsComparisionResult(errors, screenshots)
  }

  private def compareScreenshot(
      screenshot: Screenshot,
      tolerance: Double
  ): Option[ScreenshotComparisonError] = {
    val recordedScreenshotFile = new File(screenshot.recordedScreenshotPath)

    if (!recordedScreenshotFile.exists()) {
      Some(ScreenshotNotFound(screenshot))
    } else {
      val oldScreenshot = ImmutableImage.loader().fromFile(recordedScreenshotFile)
      val newScreenshot = ScreenshotComposer.composeNewScreenshot(screenshot)

      try {
        if (!haveSameDimensions(newScreenshot, oldScreenshot)) {
          val originalDimension = Dimension(oldScreenshot.width, oldScreenshot.height)
          val newDimension      = Dimension(newScreenshot.width, newScreenshot.height)
          Some(DifferentImageDimensions(screenshot, originalDimension, newDimension))
        } else if (imagesAreDifferent(screenshot, oldScreenshot, newScreenshot, tolerance)) {
          Some(DifferentScreenshots(screenshot))
        } else {
          None
        }
      } finally {
        oldScreenshot.awt.flush()
        newScreenshot.awt.flush()
      }
    }
  }

  private def imagesAreDifferent(
      screenshot: Screenshot,
      oldScreenshot: ImmutableImage,
      newScreenshot: ImmutableImage,
      tolerance: Double
  ): Boolean = {
    if (oldScreenshot == newScreenshot) {
      false
    } else {
      val oldScreenshotPixels = oldScreenshot.pixels
      val newScreenshotPixels = newScreenshot.pixels

      val differentPixels = oldScreenshotPixels
        .zip(newScreenshotPixels)
        .filter { case (a, b) => a != b }

      val percentageOfDifferentPixels = differentPixels.length.toDouble / oldScreenshotPixels.length.toDouble
      val percentageOutOf100 = BigDecimal(percentageOfDifferentPixels * 100.0)
        .setScale(differencePercentageScale, RoundingMode.UP)

      val imagesAreDifferent        = percentageOutOf100 > tolerance
      val imagesAreConsideredEquals = !imagesAreDifferent
      if (imagesAreConsideredEquals && tolerance != Config.defaultTolerance) {
        val screenshotName = screenshot.name
        println(
          Console.YELLOW + s"⚠️   Shot warning: There are some pixels changed in the screenshot named $screenshotName, but we consider the comparison correct because tolerance is configured to $tolerance % and the percentage of different pixels is $percentageOutOf100 %" + Console.RESET
        )
      }

      imagesAreDifferent
    }
  }

  private def haveSameDimensions(
      newScreenshot: ImmutableImage,
      recordedScreenshot: ImmutableImage
  ): Boolean =
    newScreenshot.width == recordedScreenshot.width && newScreenshot.height == recordedScreenshot.height

}
