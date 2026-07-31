package com.javaide.mobile.completion

import org.eclipse.jdt.core.compiler.CharOperation
import org.eclipse.jdt.internal.compiler.ASTVisitor
import org.eclipse.jdt.internal.compiler.Compiler
import org.eclipse.jdt.internal.compiler.DefaultErrorHandlingPolicies
import org.eclipse.jdt.internal.compiler.ICompilerRequestor
import org.eclipse.jdt.internal.compiler.ast.MessageSend
import org.eclipse.jdt.internal.compiler.ast.SingleNameReference
import org.eclipse.jdt.internal.compiler.batch.CompilationUnit
import org.eclipse.jdt.internal.compiler.batch.FileSystem
import org.eclipse.jdt.internal.compiler.classfmt.ClassFileConstants
import org.eclipse.jdt.internal.compiler.env.ICompilationUnit
import org.eclipse.jdt.internal.compiler.impl.CompilerOptions
import org.eclipse.jdt.internal.compiler.lookup.BlockScope
import org.eclipse.jdt.internal.compiler.lookup.ReferenceBinding
import org.eclipse.jdt.internal.compiler.lookup.Scope
import org.eclipse.jdt.internal.compiler.lookup.TypeBinding
import org.eclipse.jdt.internal.compiler.problem.DefaultProblemFactory
import java.io.File

enum class SemanticKind { FIELD, METHOD, LOCAL }

data class SemanticSuggestion(val name: String, val kind: SemanticKind, val detail: String)

data class SemanticCompletionResult(val prefixLength: Int, val suggestions: List<SemanticSuggestion>)

/**
 * Resolves the file currently being edited with ECJ (against android.jar, the file's own
 * declarations, and optionally the rest of the project's already-compiled classes) to offer real
 * dot-member and in-scope completions, instead of sora-editor's default plain-text word matching.
 *
 * Cross-file symbols (types/methods declared in *other* .java files of the same project) resolve
 * only if [projectClassesDir] is given and contains their compiled .class files -- i.e. as of the
 * last time the project was compiled (seeded on editor open, refreshed by Build/Run), not live
 * across unsaved edits in other files. A dangling prefix (e.g. "list.a" or "it" while typing)
 * isn't valid Java on its own, so the prefix is swapped for a placeholder token that *is* valid
 * (a no-arg method call for dot-completion, a bare identifier for scope completion) before
 * asking ECJ to resolve it; the resulting bindings are then discarded and only used to look up
 * the receiver's members / the enclosing scope's locals and fields.
 */
object SemanticCompletionEngine {

    private const val PLACEHOLDER = "zzzCompletionPlaceholderZzz"
    private val PLACEHOLDER_CHARS = PLACEHOLDER.toCharArray()

    fun complete(
        androidJar: File,
        fullText: String,
        cursor: Int,
        fileName: String,
        projectClassesDir: File? = null
    ): SemanticCompletionResult {
        var prefixStart = cursor
        while (prefixStart > 0 && Character.isJavaIdentifierPart(fullText[prefixStart - 1])) {
            prefixStart--
        }
        val prefix = fullText.substring(prefixStart, cursor)
        val prefixLength = prefix.length
        val dotMode = prefixStart > 0 && fullText[prefixStart - 1] == '.'

        val modified = fullText.substring(0, prefixStart) +
            PLACEHOLDER + (if (dotMode) "()" else "") +
            fullText.substring(cursor)

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
            val sourceUnit: ICompilationUnit = CompilationUnit(modified.toCharArray(), fileName, "UTF-8")
            val unit = compiler.resolve(sourceUnit, true, false, false)
            val out = LinkedHashMap<String, SemanticSuggestion>()

            if (dotMode) {
                var receiverType: TypeBinding? = null
                unit.traverse(object : ASTVisitor() {
                    override fun visit(messageSend: MessageSend, scope: BlockScope): Boolean {
                        if (CharOperation.equals(messageSend.selector, PLACEHOLDER_CHARS)) {
                            receiverType = messageSend.actualReceiverType
                        }
                        return true
                    }
                }, unit.scope)

                val type = receiverType
                if (type is ReferenceBinding && !type.isBaseType) {
                    collectMembers(type, prefix, out)
                }
            } else {
                var found: BlockScope? = null
                unit.traverse(object : ASTVisitor() {
                    override fun visit(ref: SingleNameReference, scope: BlockScope): Boolean {
                        if (CharOperation.equals(ref.token, PLACEHOLDER_CHARS)) {
                            found = scope
                        }
                        return true
                    }
                }, unit.scope)

                var scope: Scope? = found
                while (scope is BlockScope) {
                    val locals = scope.locals
                    if (locals != null) {
                        for (i in 0 until scope.localIndex) {
                            val lv = locals[i] ?: continue
                            val name = String(lv.name)
                            if (matches(name, prefix)) {
                                out.putIfAbsent("var:$name", SemanticSuggestion(name, SemanticKind.LOCAL, String(lv.type.readableName())))
                            }
                        }
                    }
                    scope = scope.parent
                }
                found?.enclosingSourceType()?.let { collectMembers(it, prefix, out) }
            }

            SemanticCompletionResult(prefixLength, out.values.toList())
        } catch (e: Exception) {
            SemanticCompletionResult(prefixLength, emptyList())
        } finally {
            nameEnv.cleanup()
        }
    }

    private fun collectMembers(startType: ReferenceBinding, prefix: String, out: LinkedHashMap<String, SemanticSuggestion>) {
        var type: ReferenceBinding? = startType
        var guard = 0
        while (type != null && guard++ < 50) {
            for (fb in type.availableFields().orEmpty()) {
                val name = String(fb.name)
                if (matches(name, prefix)) {
                    out.putIfAbsent("field:$name", SemanticSuggestion(name, SemanticKind.FIELD, String(fb.type.readableName())))
                }
            }
            for (mb in type.availableMethods().orEmpty()) {
                if (mb.isConstructor) continue
                val name = String(mb.selector)
                if (matches(name, prefix)) {
                    val key = "method:$name:${mb.parameters?.size ?: 0}"
                    out.putIfAbsent(key, SemanticSuggestion(name, SemanticKind.METHOD, String(mb.returnType.readableName())))
                }
            }
            for (iface in type.superInterfaces().orEmpty()) {
                collectMembers(iface, prefix, out)
            }
            type = type.superclass()
        }
    }

    private fun matches(name: String, prefix: String) = prefix.isEmpty() || name.startsWith(prefix)
}
