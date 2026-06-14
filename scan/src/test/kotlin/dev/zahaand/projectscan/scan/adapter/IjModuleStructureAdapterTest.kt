package dev.zahaand.projectscan.scan.adapter

import com.intellij.openapi.roots.ModuleRootModificationUtil
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import org.jetbrains.jps.model.java.JavaSourceRootType

class IjModuleStructureAdapterTest : BasePlatformTestCase() {
    fun testGetPackageTreeRootPackagesAndSecondLevelSegments() {
        val placeholder = myFixture.addFileToProject("main/kotlin/.gitkeep", "")
        val srcRoot = placeholder.virtualFile.parent
        ModuleRootModificationUtil.updateModel(module) { model ->
            model.contentEntries.first().addSourceFolder(srcRoot, JavaSourceRootType.SOURCE)
        }

        myFixture.addFileToProject("main/kotlin/com/example/Main.kt", "package com.example\nclass Main")
        myFixture.addFileToProject("main/kotlin/com/util/Utils.kt", "package com.util\nclass Utils")
        srcRoot.refresh(false, true)

        val tree = IjModuleStructureAdapter(project).getPackageTree()

        assertTrue("com" in tree.rootPackages)
        assertTrue("com.example" in tree.secondLevelSegments)
        assertTrue("com.util" in tree.secondLevelSegments)
    }

    fun testGetPackageTreeEmptyWhenNoSourceRoot() {
        val tree = IjModuleStructureAdapter(project).getPackageTree()

        assertTrue(tree.rootPackages.isEmpty())
        assertTrue(tree.secondLevelSegments.isEmpty())
    }

    fun testGetPackageTreeSkipsDotPrefixedEntries() {
        val placeholder = myFixture.addFileToProject("main/kotlin/.gitkeep", "")
        val srcRoot = placeholder.virtualFile.parent
        ModuleRootModificationUtil.updateModel(module) { model ->
            model.contentEntries.first().addSourceFolder(srcRoot, JavaSourceRootType.SOURCE)
        }
        myFixture.addFileToProject("main/kotlin/com/example/Main.kt", "package com.example")
        srcRoot.refresh(false, true)

        val tree = IjModuleStructureAdapter(project).getPackageTree()

        assertTrue(tree.rootPackages.none { it.startsWith(".") })
        assertTrue("com" in tree.rootPackages)
    }

    fun testGetModulesReturnsEmptyWhenNoBuildSystemData() {
        val modules = IjModuleStructureAdapter(project).getModules()
        assertTrue(modules.isEmpty())
    }
}
