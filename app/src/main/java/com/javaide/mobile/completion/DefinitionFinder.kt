package com.javaide.mobile.completion

import org.eclipse.jdt.internal.compiler.ASTVisitor
import org.eclipse.jdt.internal.compiler.Compiler
import org.eclipse.jdt.internal.compiler.DefaultErrorHandlingPolicies
import org.eclipse.jdt.internal.compiler.ICompilerRequestor
import org.eclipse.jdt.internal.compiler.ast.MessageSend
import org.eclipse.jdt.internal.compiler.ast.SingleNameReference
import org.eclipse.jdt.internal.compiler.ast.SingleTypeReference
import org.eclipse.jdt.internal.compiler.batch.CompilationUnit
import org.eclipse.jdt.internal.compiler.batch.FileSystem
import org.eclipse.jdt.internal.compiler.classfmt.ClassFileConstants
import org.eclipse.jdt.internal.compiler.env.ICompilationUnit
import org.eclipse.jdt.internal.compiler.impl.CompilerOptions
import org.eclipse.jdt.internal.compiler.lookup.Binding
import org.eclipse.jdt.internal.compiler.lookup.BlockScope
import org.eclipse.jdt.internal.compiler.lookup.FieldBinding
import org.eclipse.jdt.internal.compiler.lookup.LocalVariableBinding
import org.eclipse.jdt.internal.compiler.lookup.MethodBinding
import org.eclipse.jdt.internal.compiler.lookup.ReferenceBinding
import org.eclipse.jdt.internal.compiler.lookup.SourceTypeBinding
import org.eclipse.jdt.internal.compiler.problem.DefaultProblemFactory
import java.io.File

/** Where a Go to Definition jump should land. */
sealed class DefinitionTarget {
    /** [offset] is a char index into the file currently being edited. */
    data class SameFile(val offset: Int) : DefinitionTarget()

    /** The declaring type isn't in this file -- [simpleName] is what the caller looks up by name
     * among the project's other .java files (see ClassRenamer.findDeclaration). */
    data class OtherProjectFile(val simpleName: String) : DefinitionTarget()
}

/**
 * Resolves the symbol under the cursor (a local variable, field, method call, or type reference)
 * to where it's declared -- ECJ-backed, like DiagnosticsEngine/SemanticCompletionEngine.
 *
 * Because only the active file is compiled (the rest of the classpath is already-compiled .class
 * binaries, same architecture as those two engines), ECJ's own isBinaryBinding() flag is exactly
 * the same-file/cross-file split: a binding is "source" only if it was declared in the one file
 * just compiled, so we can jump straight to its exact declaration offset; everything else is
 * binary, and we can only recover *which* file to open (via [ReferenceBinding.getFileName] path,
 * to tell a class the project already compiled apart from a class loaded from android.jar/libJars,
 * which has no project source to jump to at all) -- not a precise line.
 *
 * Not handled (falls through to null, "no definition found"): dotted field access like `h.field`
 * or `this.field` (ECJ's QualifiedNameReference/FieldReference chain resolution) -- only bare
 * identifiers, method calls, and type names are resolved.
 */
object DefinitionFinder {

    fun find(
        androidJar: File,
        fullText: String,
        cursor: Int,
        fileName: String,
        projectClassesDir: File? = null,
        libJars: List<File> = emptyList()
    ): DefinitionTarget? {
        val classpath = (
            listOfNotNull(
                androidJar.absolutePath,
                projectClassesDir?.takeIf { it.isDirectory }?.absolutePath
            ) + libJars.map { it.absolutePath }
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

            var found: DefinitionTarget? = null
            unit.traverse(object : ASTVisitor() {
                override fun visit(ref: SingleNameReference, scope: BlockScope): Boolean {
                    if (found == null && cursor in ref.sourceStart..ref.sourceEnd) {
                        found = resolveBinding(ref.binding, projectClassesDir)
                    }
                    return true
                }

                override fun visit(messageSend: MessageSend, scope: BlockScope): Boolean {
                    val start = messageSend.nameSourceStart()
                    val end = start + messageSend.selector.size - 1
                    if (found == null && cursor in start..end) {
                        found = resolveBinding(messageSend.binding, projectClassesDir)
                    }
                    return true
                }

                override fun visit(typeRef: SingleTypeReference, scope: BlockScope): Boolean {
                    if (found == null && cursor in typeRef.sourceStart..typeRef.sourceEnd) {
                        found = resolveBinding(typeRef.resolvedType, projectClassesDir)
                    }
                    return true
                }
            }, unit.scope)

            found
        } catch (e: Exception) {
            null
        } finally {
            nameEnv.cleanup()
        }
    }

    private fun resolveBinding(binding: Binding?, projectClassesDir: File?): DefinitionTarget? = when (binding) {
        is LocalVariableBinding -> DefinitionTarget.SameFile(binding.declaration.sourceStart)
        is FieldBinding -> fromDeclaringClass(binding.declaringClass, binding.sourceField()?.sourceStart, projectClassesDir)
        is MethodBinding -> fromDeclaringClass(
            binding.declaringClass,
            binding.sourceMethod()?.let { binding.sourceStart() },
            projectClassesDir
        )
        is ReferenceBinding -> fromDeclaringClass(binding, sameFileTypeOffset(binding), projectClassesDir)
        else -> null
    }

    private fun sameFileTypeOffset(rb: ReferenceBinding): Int? =
        (rb as? SourceTypeBinding)?.scope?.referenceContext?.sourceStart

    private fun fromDeclaringClass(rb: ReferenceBinding, sameFileOffset: Int?, projectClassesDir: File?): DefinitionTarget? {
        if (!rb.isBinaryBinding() && sameFileOffset != null) {
            return DefinitionTarget.SameFile(sameFileOffset)
        }
        if (projectClassesDir == null) return null
        val path = rb.getFileName()?.let { String(it) } ?: return null
        if (!path.startsWith(projectClassesDir.absolutePath)) return null
        return DefinitionTarget.OtherProjectFile(String(rb.sourceName))
    }
}
