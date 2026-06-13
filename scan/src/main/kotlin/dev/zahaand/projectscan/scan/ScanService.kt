package dev.zahaand.projectscan.scan

import dev.zahaand.projectscan.model.CodeStyleInfo
import dev.zahaand.projectscan.model.LinterInfo
import dev.zahaand.projectscan.model.ScanResult
import dev.zahaand.projectscan.model.SectionResult
import dev.zahaand.projectscan.model.StackInfo
import dev.zahaand.projectscan.model.StructureInfo
import dev.zahaand.projectscan.model.TestInfo
import dev.zahaand.projectscan.scan.port.BuildSystemPort
import dev.zahaand.projectscan.scan.port.DependencyPort
import dev.zahaand.projectscan.scan.port.LinterConfigParser
import dev.zahaand.projectscan.scan.port.LinterPort
import dev.zahaand.projectscan.scan.port.ModuleStructurePort
import dev.zahaand.projectscan.scan.port.StyleSourcePort
import dev.zahaand.projectscan.scan.port.TestInfoPort

class ScanService(
    private val buildSystemPort: BuildSystemPort,
    private val dependencyPort: DependencyPort,
    private val styleSourcePort: StyleSourcePort,
    private val linterPort: LinterPort,
    private val linterConfigParsers: Map<String, LinterConfigParser>,
    private val testInfoPort: TestInfoPort,
    private val moduleStructurePort: ModuleStructurePort,
) {
    fun scan(): ScanResult {
        val stack: SectionResult<StackInfo> = try {
            TODO("StackCollector not yet implemented")
        } catch (e: Exception) {
            SectionResult.Error(e.message)
        }
        val codeStyle: SectionResult<CodeStyleInfo> = try {
            TODO("CodeStyleCollector not yet implemented")
        } catch (e: Exception) {
            SectionResult.Error(e.message)
        }
        val linters: SectionResult<LinterInfo> = try {
            TODO("LinterCollector not yet implemented")
        } catch (e: Exception) {
            SectionResult.Error(e.message)
        }
        val tests: SectionResult<TestInfo> = try {
            TODO("TestCollector not yet implemented")
        } catch (e: Exception) {
            SectionResult.Error(e.message)
        }
        val structure: SectionResult<StructureInfo> = try {
            TODO("StructureCollector not yet implemented")
        } catch (e: Exception) {
            SectionResult.Error(e.message)
        }
        return ScanResult(
            stack = stack,
            codeStyle = codeStyle,
            linters = linters,
            tests = tests,
            structure = structure,
        )
    }
}
