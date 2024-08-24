package com.karumi.shot.screenshots

import com.karumi.shot.domain._
import com.karumi.shot.domain.model.ScreenshotsSuite
import com.karumi.shot.ui.Console
import com.sksamuel.scrimage.ImmutableImage

import java.io.File
import java.util.concurrent.ForkJoinPool
import scala.collection.parallel.CollectionConverters.seqIsParallelizable
import scala.collection.parallel.ForkJoinTaskSupport
import scala.math.BigDecimal.RoundingMode

class ScreenshotsComparator {

  private val differencePercentageScale = 5

  def compare(screenshots: ScreenshotsSuite,
              tolerance: Double,
              parallelThreads: Int,
              console: Console): ScreenshotsComparisonResult = {

    if (parallelThreads <= 0) {
      throw new IllegalArgumentException("The number of parallel threads must be greater than 0")
    }

    val errors = if (parallelThreads > 1) {
      console.showWarning(
        "Comparing screenshots in a parallel mode, using " + parallelThreads + " threads")

      val parScreenshots = screenshots.par
      parScreenshots.tasksupport = new ForkJoinTaskSupport(new ForkJoinPool(parallelThreads))
      parScreenshots.flatMap(compareScreenshot(_, tolerance, console)).toList
    } else {
      console.showWarning("Comparing screenshots in a sequential mode")
      screenshots.flatMap(compareScreenshot(_, tolerance, console)).toList
    }

    ScreenshotsComparisonResult(errors, screenshots)
  }

  private def compareScreenshot(
      screenshot: Screenshot,
      tolerance: Double,
      console: Console
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
        } else if (imagesAreDifferent(screenshot, oldScreenshot, newScreenshot, tolerance, console)) {
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
      tolerance: Double,
      console: Console
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
        console.showWarning(
          s"⚠️   Shot warning: Screenshot named ${screenshot.name} ${Console.GREEN}passed${Console.YELLOW} with difference of $percentageOutOf100%, because tolerance is $tolerance%"
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
