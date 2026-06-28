package dev.zahaand.projectscan.scan.adapter

import com.intellij.openapi.roots.ModuleRootModificationUtil
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import org.jetbrains.jps.model.java.JavaSourceRootType

class IjTestInfoAdapterTest : BasePlatformTestCase() {
    fun testGetTestSourceRootsReturnsProjectRelativePath() {
        val placeholder = myFixture.addFileToProject("test/kotlin/.gitkeep", "")
        val testSrcDir = placeholder.virtualFile.parent
        ModuleRootModificationUtil.updateModel(module) { model ->
            model.contentEntries.first().addSourceFolder(testSrcDir, JavaSourceRootType.TEST_SOURCE)
        }

        val roots = IjTestInfoAdapter(project).getTestSourceRoots()

        assertEquals(1, roots.size)
        assertFalse("Root path must be relative", roots[0].startsWith("/"))
        assertTrue("Root must end with test/kotlin", roots[0].endsWith("test/kotlin"))
    }

    fun testGetTestClassNamesReturnsKtAndJavaFilesOnly() {
        val placeholder = myFixture.addFileToProject("test/kotlin/.gitkeep", "")
        val testSrcDir = placeholder.virtualFile.parent
        ModuleRootModificationUtil.updateModel(module) { model ->
            model.contentEntries.first().addSourceFolder(testSrcDir, JavaSourceRootType.TEST_SOURCE)
        }

        myFixture.addFileToProject("test/kotlin/FooTest.kt", "class FooTest")
        myFixture.addFileToProject("test/kotlin/BarIT.java", "class BarIT {}")
        myFixture.addFileToProject("test/kotlin/TestData.xml", "<data/>")
        myFixture.addFileToProject("test/kotlin/schema.json", "{}")
        testSrcDir.refresh(false, true)

        val names = IjTestInfoAdapter(project).getTestClassNames()

        assertTrue("FooTest" in names)
        assertTrue("BarIT" in names)
        assertTrue("TestData" !in names)
        assertTrue("schema" !in names)
    }

    fun testGetTestClassNamesExcludesNonSourceFiles() {
        val placeholder = myFixture.addFileToProject("test/kotlin/.gitkeep", "")
        val testSrcDir = placeholder.virtualFile.parent
        ModuleRootModificationUtil.updateModel(module) { model ->
            model.contentEntries.first().addSourceFolder(testSrcDir, JavaSourceRootType.TEST_SOURCE)
        }
        myFixture.addFileToProject("test/kotlin/SomeTest.xml", "<config/>")
        testSrcDir.refresh(false, true)

        val names = IjTestInfoAdapter(project).getTestClassNames()

        assertTrue("SomeTest" !in names)
    }

    fun testGetCoverageThresholdNullForNonMavenProject() {
        assertNull(IjTestInfoAdapter(project).getCoverageThreshold())
    }
}
