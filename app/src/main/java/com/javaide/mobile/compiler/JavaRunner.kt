package com.javaide.mobile.compiler

import dalvik.system.InMemoryDexClassLoader
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.PrintStream
import java.io.PrintWriter
import java.io.StringWriter
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Method
import java.lang.reflect.Modifier
import java.nio.ByteBuffer

data class RunResult(val success: Boolean, val output: String)

/**
 * Loads a project's classes.dex in-process and runs whichever class has a
 * `public static void main(String[])`, capturing stdout/stderr. Meant for quick practice
 * snippets (algorithms, collections, streams, ...) without building a full APK.
 *
 * The practice code runs directly inside this app's process, so a hostile or broken
 * snippet (infinite loop, crash) can hang or crash the IDE itself — acceptable for
 * running one's own practice code, but not a sandbox.
 */
object JavaRunner {

    fun run(classesDir: File, dexFile: File, optimizedDir: File): RunResult {
        if (!dexFile.isFile) {
            return RunResult(success = false, output = "No classes.dex found; compile/dex step must have failed.")
        }

        // Android 10+ blocks DexClassLoader from loading writable DEX files (security restriction).
        // InMemoryDexClassLoader bypasses this by loading DEX directly from a ByteBuffer.
        val dexBuffer = ByteBuffer.wrap(dexFile.readBytes())
        val classLoader = InMemoryDexClassLoader(dexBuffer, JavaRunner::class.java.classLoader)

        val (mainClass, mainMethod) = findMain(classesDir, classLoader)
            ?: return RunResult(success = false, output = "No class with a public static void main(String[]) method was found.")

        val originalOut = System.out
        val originalErr = System.err
        val buffer = ByteArrayOutputStream()
        val capture = PrintStream(buffer, true, "UTF-8")
        System.setOut(capture)
        System.setErr(capture)

        return try {
            mainMethod.invoke(null, arrayOf<String>())
            capture.flush()
            RunResult(success = true, output = buffer.toString("UTF-8").ifBlank { "(no output)" })
        } catch (e: InvocationTargetException) {
            capture.flush()
            val stackTrace = StringWriter().also { (e.cause ?: e).printStackTrace(PrintWriter(it)) }
            RunResult(success = false, output = buffer.toString("UTF-8") + "\n" + stackTrace)
        } catch (e: Exception) {
            capture.flush()
            RunResult(success = false, output = buffer.toString("UTF-8") + "\nFailed to run ${mainClass.name}: ${e.message}")
        } finally {
            System.setOut(originalOut)
            System.setErr(originalErr)
        }
    }

    private fun findMain(classesDir: File, classLoader: ClassLoader): Pair<Class<*>, Method>? {
        val candidateNames = classesDir.walkTopDown()
            .filter { it.isFile && it.extension == "class" && !it.name.contains('$') }
            .map { it.relativeTo(classesDir).path.removeSuffix(".class").replace(File.separatorChar, '.') }
            .sortedByDescending { it.substringAfterLast('.') == "Main" }

        for (name in candidateNames) {
            val clazz = try {
                Class.forName(name, false, classLoader)
            } catch (e: Throwable) {
                continue
            }
            val method = clazz.declaredMethods.firstOrNull {
                it.name == "main" &&
                    Modifier.isStatic(it.modifiers) &&
                    Modifier.isPublic(it.modifiers) &&
                    it.parameterTypes.size == 1 &&
                    it.parameterTypes[0] == Array<String>::class.java
            }
            if (method != null) return clazz to method
        }
        return null
    }
}
