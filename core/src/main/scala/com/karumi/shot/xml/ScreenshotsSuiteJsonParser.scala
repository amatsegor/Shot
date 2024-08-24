package com.karumi.shot.xml

import com.karumi.shot.domain.Dimension
import com.karumi.shot.domain.Screenshot
import com.karumi.shot.domain.model.FilePath
import com.karumi.shot.domain.model.Folder
import com.karumi.shot.domain.model.ScreenshotsSuite
import org.json4s.*
import org.json4s.native.JsonMethods.*

import scala.collection.mutable
import scala.collection.mutable.ListBuffer

object ScreenshotsSuiteJsonParser {

  implicit val formats: DefaultFormats.type = DefaultFormats

  def parseScreenshots(
      metadataJson: String,
      screenshotsFolder: Folder,
      temporalScreenshotsFolder: Folder,
      screenshotsTemporalBuildPath: Folder
  ): ScreenshotsSuite = {
    val json                    = parse(metadataJson)
    val JArray(jsonScreenshots) = json.extract[JArray]
    jsonScreenshots.map(
      parseScreenshot(_, screenshotsFolder, temporalScreenshotsFolder, screenshotsTemporalBuildPath)
    )
  }

  private def parseScreenshot(
      jsonNode: JValue,
      screenshotsFolder: Folder,
      temporalScreenshotsFolder: Folder,
      screenshotsTemporalBuildPath: Folder
  ): Screenshot = {
    val name                              = (jsonNode \ "name").extract[String]
    val recordedScreenshotPath            = screenshotsFolder + name + ".png"
    val temporalScreenshotPath            = screenshotsTemporalBuildPath + "/" + name + ".png"
    val testClass                         = (jsonNode \ "testClass").extract[String]
    val testName                          = (jsonNode \ "testName").extract[String]
    val tileWidth                         = (jsonNode \ "tileWidth").extract[Int]
    val tileHeight                        = (jsonNode \ "tileHeight").extract[Int]
    val viewHierarchy                     = (jsonNode \ "viewHierarchy").extract[String]
    val JArray(absoluteFileNamesFromJson) = (jsonNode \ "absoluteFilesNames").extract[JArray]
    val absoluteFileNames                 = ListBuffer[String]()
    absoluteFileNamesFromJson.foreach(value => {
      val fileName = value.extract[String]
      absoluteFileNames += (fileName + ".png")
    })
    val JArray(relativeFileNamesFromJson) = (jsonNode \ "relativeFileNames").extract[JArray]

    val relativeFileNames = ListBuffer[String]()
    relativeFileNamesFromJson.foreach(value => {
      val fileName = value.extract[String]
      relativeFileNames += (fileName + ".png")
    })

    val tilesDimension = Dimension(tileWidth.toInt, tileHeight.toInt)

    Screenshot(
      name,
      recordedScreenshotPath,
      temporalScreenshotPath,
      testClass,
      testName,
      tilesDimension,
      viewHierarchy,
      absoluteFileNames.toSeq,
      relativeFileNames.toSeq,
      relativeFileNames.map(temporalScreenshotsFolder + _).toSeq,
      Dimension(0, 0)
    )
  }

  def parseScreenshotSize(screenshot: Screenshot, viewHierarchyContent: String): Screenshot = {
    val json              = parse(viewHierarchyContent)
    val viewHierarchyNode = json \ "viewHierarchy"
    val screenshotLeft    = (viewHierarchyNode \ "left").extract[Int]
    val screenshotWidth   = (viewHierarchyNode \ "width").extract[Int]
    val screenshotTop     = (viewHierarchyNode \ "top").extract[Int]
    val screenshotHeight  = (viewHierarchyNode \ "height").extract[Int]
    screenshot.copy(
      screenshotDimension = Dimension(
        screenshotLeft + screenshotWidth,
        screenshotTop + screenshotHeight
      )
    )
  }

}
