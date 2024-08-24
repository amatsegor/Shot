package com.karumi.shot.reports
import com.karumi.shot.domain.{ScreenshotsComparisonResult, ShotFolder}
import com.karumi.shot.domain.model.{AppId, ScreenshotsSuite}

trait ExecutionReporter {

  def generateRecordReport(
      appId: AppId,
      screenshots: ScreenshotsSuite,
      shotFolder: ShotFolder
  ): Unit

  def generateVerificationReport(
                                  appId: AppId,
                                  comparision: ScreenshotsComparisonResult,
                                  shotFolder: ShotFolder,
                                  showOnlyFailingTestsInReports: Boolean = false
  ): Unit
}
