package com.karumi.shot.json

import com.karumi.shot.domain.Dimension
import com.karumi.shot.domain.Screenshot
import com.karumi.shot.domain.model.Folder
import com.karumi.shot.domain.model.ScreenshotsSuite
import org.json4s.*
import org.json4s.native.JsonMethods.*

object ScreenshotsComposeSuiteJsonParser {

  implicit val formats: DefaultFormats.type = DefaultFormats

  def parseScreenshotSuite(
      metadataJson: String,
      screenshotsFolder: Folder,
      temporalScreenshotsFolder: Folder,
      screenshotsTemporalBuildPath: Folder
  ): ScreenshotsSuite = {

    val json = parse(metadataJson)

    parseScreenshots(
      json,
      screenshotsFolder,
      temporalScreenshotsFolder,
      screenshotsTemporalBuildPath
    )
  }

  private def parseScreenshots(
      json: JValue,
      screenshotsFolder: Folder,
      temporalScreenshotsFolder: Folder,
      screenshotsTemporalBuildPath: Folder
  ): ScreenshotsSuite = {
    val JArray(composeScreenshots) = (json \ "screenshots").extract[JArray]

    composeScreenshots
      .map(parseScreenshot)
      .map(
        mapComposeScreenshot(
          _,
          screenshotsFolder,
          temporalScreenshotsFolder,
          screenshotsTemporalBuildPath
        )
      )
  }


  private def parseScreenshot(jsonNode: JsonAST.JValue): ComposeScreenshot = {
    val name: String = (jsonNode \ "name").extract[String]
    val testClassName: String = (jsonNode \ "testClassName").extract[String]
    val testName: String = (jsonNode \ "testName").extract[String]

    ComposeScreenshot(name, testClassName, testName)
  }

  private def mapComposeScreenshot(
      screenshot: ComposeScreenshot,
      screenshotsFolder: String,
      temporalScreenshotsFolder: String,
      screenshotsTemporalBuildPath: String
  ): Screenshot = {
    val name = screenshot.name
    Screenshot(
      name = name,
      recordedScreenshotPath = screenshotsFolder + name + ".png",
      temporalScreenshotPath = screenshotsTemporalBuildPath + "/" + name + ".png",
      testClass = screenshot.testClassName,
      testName = screenshot.testName,
      tilesDimension = Dimension(0, 0),
      viewHierarchy = "",
      absoluteFileNames = Seq(),
      relativeFileNames = Seq(),
      recordedPartsPaths = Seq(temporalScreenshotsFolder + "/" + name + ".png"),
      screenshotDimension = Dimension(0, 0)
    )
  }
}

case class ComposeScreenshot(name: String, testClassName: String, testName: String)
case class ComposeScreenshotSuite(screenshots: List[ComposeScreenshot])
