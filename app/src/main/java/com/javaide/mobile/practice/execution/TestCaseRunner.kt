package com.javaide.mobile.practice.execution

import dalvik.system.DexClassLoader
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.PrintStream
import java.io.PrintWriter
import java.io.StringWriter
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Method
import java.lang.reflect.Modifier
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

/**
 * Executes a compiled exercise's `main()` once per test case, feeding the test case's [input][
 * com.javaide.mobile.practice.model.ExerciseTestCase.input] in via stdin and capturing stdout and
 * stderr separately (Milestone 4). Runs in-process on the same `DexClassLoader` + reflection
 * pattern as [com.javaide.mobile.compiler.JavaRunner] — including the same "not a sandbox"
 * caveat — but is intentionally a separate implementation rather than a shared one, so this
 * milestone's additions can't affect the existing, shipped Run flow for normal Java Console
 * projects.
 *
 * **Timeout is best-effort.** [run] returns as soon as [timeoutMs] elapses, so a caller is never
 * blocked indefinitely — but there is no safe way to forcibly kill a JVM thread from the outside.
 * A genuinely hung test's background thread keeps running (and, since `System.out`/`err`/`in` are
 * process-global, may still be pointed at buffers this method already returned) until it either
 * finishes on its own or the process is killed. This mirrors the limitation
 * [com.javaide.mobile.compiler.JavaRunner] already documents for the general case; it's not a
 * regression introduced here.
 */
object TestCaseRunner {

    private val executor = Executors.newCachedThreadPool { runnable ->
        Thread(runnable, "test-case-runner").apply { isDaemon = true }
    }

    fun run(classesDir: File, dexFile: File, optimizedDir: File, input: String, timeoutMs: Long): RawExecution {
        if (!dexFile.isFile) {
            return RawExecution(
                stdout = "",
                stderr = "No classes.dex found; compile/dex step must have failed.",
                executionTimeMs = 0,
                threw = true,
                timedOut = false
            )
        }
        optimizedDir.mkdirs()

        val classLoader = DexClassLoader(
            dexFile.absolutePath,
            optimizedDir.absolutePath,
            null,
            TestCaseRunner::class.java.classLoader
        )

        val (mainClass, mainMethod) = findMain(classesDir, classLoader)
            ?: return RawExecution(
                stdout = "",
                stderr = "No class with a public static void main(String[]) method was found.",
                executionTimeMs = 0,
                threw = true,
                timedOut = false
            )

        val future = executor.submit<RawExecution> { execute(mainClass.name, mainMethod, input) }

        return try {
            future.get(timeoutMs, TimeUnit.MILLISECONDS)
        } catch (e: TimeoutException) {
            RawExecution(
                stdout = "",
                stderr = "",
                executionTimeMs = timeoutMs,
                threw = false,
                timedOut = true
            )
        }
    }

    private fun execute(mainClassName: String, mainMethod: Method, input: String): RawExecution {
        val originalOut = System.out
        val originalErr = System.err
        val originalIn = System.`in`
        val outBuffer = ByteArrayOutputStream()
        val errBuffer = ByteArrayOutputStream()

        System.setOut(PrintStream(outBuffer, true, "UTF-8"))
        System.setErr(PrintStream(errBuffer, true, "UTF-8"))
        System.setIn(ByteArrayInputStream(input.toByteArray(Charsets.UTF_8)))

        val startNanos = System.nanoTime()
        return try {
            mainMethod.invoke(null, arrayOf<String>())
            RawExecution(
                stdout = outBuffer.toString("UTF-8"),
                stderr = errBuffer.toString("UTF-8"),
                executionTimeMs = elapsedMs(startNanos),
                threw = false,
                timedOut = false
            )
        } catch (e: InvocationTargetException) {
            val stackTrace = StringWriter().also { (e.cause ?: e).printStackTrace(PrintWriter(it)) }
            RawExecution(
                stdout = outBuffer.toString("UTF-8"),
                stderr = (errBuffer.toString("UTF-8") + "\n" + stackTrace).trim(),
                executionTimeMs = elapsedMs(startNanos),
                threw = true,
                timedOut = false
            )
        } catch (e: Throwable) {
            RawExecution(
                stdout = outBuffer.toString("UTF-8"),
                stderr = "Failed to run $mainClassName: ${e.message}",
                executionTimeMs = elapsedMs(startNanos),
                threw = true,
                timedOut = false
            )
        } finally {
            System.setOut(originalOut)
            System.setErr(originalErr)
            System.setIn(originalIn)
        }
    }

    private fun elapsedMs(startNanos: Long): Long = (System.nanoTime() - startNanos) / 1_000_000

    // Deliberately duplicated from com.javaide.mobile.compiler.JavaRunner.findMain rather than
    // shared, per this class's doc: keeps the existing Run flow completely unaffected by this
    // milestone's additions.
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
