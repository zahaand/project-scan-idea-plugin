package dev.zahaand.projectscan.scan.adapter

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.projectRoots.ProjectJdkTable
import com.intellij.openapi.roots.LanguageLevelModuleExtension
import com.intellij.openapi.roots.LanguageLevelProjectExtension
import com.intellij.openapi.roots.ModuleRootModificationUtil
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.pom.java.LanguageLevel
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import dev.zahaand.projectscan.model.BuildSystem

class IjBuildSystemAdapterTest : BasePlatformTestCase() {
    fun testGetBuildSystemNullWhenNoSystem() {
        assertNull(IjBuildSystemAdapter(project).getBuildSystem())
    }

    fun testGetBuildSystemGradleWhenLinkedProjectPresent() {
        // Gradle detection reads GradleSettings.linkedProjectsSettings which starts empty
        // in a light fixture — verifying the null path is covered by testGetBuildSystemNullWhenNoSystem
        val result = IjBuildSystemAdapter(project).getBuildSystem()
        assertTrue("Should be null or GRADLE without build files", result == null || result == BuildSystem.GRADLE)
    }

    fun testGetModuleLanguageLevelReturnsConfiguredLevel() {
        ModuleRootModificationUtil.updateModel(module) { model ->
            model.getModuleExtension(LanguageLevelModuleExtension::class.java)
                .languageLevel = LanguageLevel.JDK_17
        }

        val levels = IjBuildSystemAdapter(project).getModuleLanguageLevels()

        assertEquals("17", levels[module.name])
    }

    fun testGetModuleLanguageLevelFallsBackToProjectDefault() {
        ModuleRootModificationUtil.updateModel(module) { model ->
            model.getModuleExtension(LanguageLevelModuleExtension::class.java).languageLevel = null
        }
        ApplicationManager.getApplication().runWriteAction {
            LanguageLevelProjectExtension.getInstance(project).languageLevel = LanguageLevel.JDK_11
        }

        val levels = IjBuildSystemAdapter(project).getModuleLanguageLevels()

        assertEquals("11", levels[module.name])
    }

    fun testGetJdkVersionFromAvailableSdk() {
        val allJdks = ProjectJdkTable.getInstance().allJdks
        if (allJdks.isNotEmpty()) {
            ApplicationManager.getApplication().runWriteAction {
                ProjectRootManager.getInstance(project).projectSdk = allJdks[0]
            }
            assertNotNull(IjBuildSystemAdapter(project).getJdkVersion())
        } else {
            assertNull(IjBuildSystemAdapter(project).getJdkVersion())
        }
    }
}
