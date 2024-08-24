package com.karumi.shot

import org.gradle.api.Project

class AdbPathExtractor {

    static String extractPath(Project project) {
        if (project.hasProperty('android')) {
            project.android.getAdbExe().toString()
        } else {
            throw new IllegalStateException("Project doesn't seem to be an Android one")
        }
    }
}
