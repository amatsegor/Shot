package com.karumi.shot.domain

import com.karumi.shot.domain.model.FilePath
import com.karumi.shot.domain.model.ScreenshotComparisonErrors
import com.karumi.shot.domain.model.ScreenshotsSuite

object model {
  type ScreenshotsSuite           = Seq[Screenshot]
  type FilePath                   = String
  type Folder                     = String
  type AppId                      = String
  type ScreenshotComparisonErrors = Seq[ScreenshotComparisonError]
}

object Config {
  val defaultTolerance: Double         = 0.0
  val shotConfiguration: String        = "shotDependencies"
  val androidDependencyMode: FilePath  = "androidTestImplementation"
  val androidDependencyGroup: String   = "com.karumi"
  val androidDependencyName: String    = "shot-android"
  val androidDependencyVersion: String = "6.1.0"
  val androidDependency: FilePath =
    s"$androidDependencyGroup:$androidDependencyName:$androidDependencyVersion"

  val androidPluginName: FilePath = "com.android.application"

  def defaultInstrumentationTestTask(flavor: Option[String], buildType: String): String =
    s"connected${flavor.getOrElse("").capitalize}${buildType.capitalize}AndroidTest"

  def composerInstrumentationTestTask(flavor: Option[String], buildType: String) =
    s"test${flavor.getOrElse("").capitalize}${buildType.capitalize}Composer"

  val defaultPackageTestApkTask: String = "packageDebugAndroidTest"

  val defaultTaskName: String = "executeScreenshotTests"
}

case class Screenshot(
    name: String,
    recordedScreenshotPath: String,
    temporalScreenshotPath: String,
    testClass: String,
    testName: String,
    tilesDimension: Dimension,
    viewHierarchy: FilePath,
    absoluteFileNames: Seq[FilePath],
    relativeFileNames: Seq[FilePath],
    recordedPartsPaths: Seq[FilePath],
    screenshotDimension: Dimension
) {
  val fileName: String =
    temporalScreenshotPath.substring(
      temporalScreenshotPath.lastIndexOf("/") + 1,
      temporalScreenshotPath.length
    )

  def getDiffScreenshotPath(basePath: String): String =
    s"${basePath}diff_$fileName"

}

case class Dimension(width: Int, height: Int) {
  val isZero: Boolean = width == 0 && height == 0

  override def toString: String = s"${width}x$height"
}

sealed trait ScreenshotComparisonError {
  def errorScreenshot: Screenshot =
    this match {
      case ScreenshotNotFound(screenshot)             => screenshot
      case DifferentScreenshots(screenshot, _)        => screenshot
      case DifferentImageDimensions(screenshot, _, _) => screenshot
    }
}

case class ScreenshotNotFound(screenshot: Screenshot) extends ScreenshotComparisonError

case class DifferentScreenshots(screenshot: Screenshot, base64Diff: Option[String] = None)
    extends ScreenshotComparisonError

case class DifferentImageDimensions(
    screenshot: Screenshot,
    originalDimension: Dimension,
    newDimension: Dimension
) extends ScreenshotComparisonError

case class ScreenshotsComparisonResult(
    errors: ScreenshotComparisonErrors = Seq(),
    screenshots: ScreenshotsSuite = Seq()
) {
  val hasErrors: Boolean                = errors.nonEmpty
  val errorScreenshots: Seq[Screenshot] = errors.map(_.errorScreenshot)
  val correctScreenshots: Seq[Screenshot] =
    screenshots.filterNot(errorScreenshots.contains(_))
}
