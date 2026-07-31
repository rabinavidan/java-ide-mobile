package com.javaide.mobile.completion

import org.eclipse.jdt.core.compiler.CategorizedProblem
import org.eclipse.jdt.core.compiler.IProblem
import org.eclipse.jdt.internal.compiler.Compiler
import org.eclipse.jdt.internal.compiler.DefaultErrorHandlingPolicies
import org.eclipse.jdt.internal.compiler.ICompilerRequestor
import org.eclipse.jdt.internal.compiler.batch.CompilationUnit
import org.eclipse.jdt.internal.compiler.batch.FileSystem
import org.eclipse.jdt.internal.compiler.classfmt.ClassFileConstants
import org.eclipse.jdt.internal.compiler.env.ICompilationUnit
import org.eclipse.jdt.internal.compiler.impl.CompilerOptions
import org.eclipse.jdt.internal.compiler.problem.DefaultProblemFactory
import java.io.File

enum class DiagnosticSeverity { WARNING, ERROR }

/**
 * [start]/[end] are char offsets into the source text; [end] is exclusive.
 * [unresolvedTypeName] is set when this is ECJ's "X cannot be resolved to a type" problem
 * (IProblem.UndefinedType) -- the simple name X, for the Fix Imports quick fix to look up.
 */
data class DiagnosticIssue(
    val start: Int,
    val end: Int,
    val severity: DiagnosticSeverity,
    val message: String,
    val unresolvedTypeName: String? = null
)

/**
 * Resolves the file currently being edited with ECJ -- the real, unmodified text this time (no
 * placeholder substitution, unlike SemanticCompletionEngine) -- and reports its compile problems
 * for inline error/warning markers. Uses the same classpath (android.jar + optionally the
 * project's compiled classes) as completion, so it shares the same cross-file scope/limitations.
 */
object DiagnosticsEngine {

    fun analyze(
        androidJar: File,
        fullText: String,
        fileName: String,
        projectClassesDir: File? = null
    ): List<DiagnosticIssue> {
        val classpath = listOfNotNull(
            androidJar.absolutePath,
            projectClassesDir?.takeIf { it.isDirectory }?.absolutePath
        ).toTypedArray()
        val nameEnv = FileSystem(classpath, arrayOf(), "UTF-8")
        val options = CompilerOptions()
        options.complianceLevel = ClassFileConstants.JDK1_8
        options.sourceLevel = ClassFileConstants.JDK1_8
        options.targetJDK = ClassFileConstants.JDK1_8
        val requestor = ICompilerRequestor { }
        val compiler = Compiler(
            nameEnv,
            DefaultErrorHandlingPolicies.proceedWithAllProblems(),
            options,
            requestor,
            DefaultProblemFactory()
        )

        return try {
            val sourceUnit: ICompilationUnit = CompilationUnit(fullText.toCharArray(), fileName, "UTF-8")
            val unit = compiler.resolve(sourceUnit, true, true, false)
            val problems: Array<CategorizedProblem>? = unit.compilationResult.allProblems
            problems.orEmpty()
                .filter { it.sourceStart >= 0 && it.sourceEnd >= it.sourceStart }
                .map { problem ->
                    DiagnosticIssue(
                        start = problem.sourceStart,
                        end = (problem.sourceEnd + 1).coerceAtMost(fullText.length),
                        severity = if (problem.isError) DiagnosticSeverity.ERROR else DiagnosticSeverity.WARNING,
                        message = problem.message,
                        // getID(): Kotlin's synthetic-property naming for all-caps getters like
                        // getID() is unreliable, so call it explicitly rather than via `.id`.
                        unresolvedTypeName = if (problem.getID() == IProblem.UndefinedType) {
                            problem.arguments?.firstOrNull()
                        } else {
                            null
                        }
                    )
                }
        } catch (e: Exception) {
            emptyList()
        } finally {
            nameEnv.cleanup()
        }
    }
}
