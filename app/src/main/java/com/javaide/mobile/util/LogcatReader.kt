package com.javaide.mobile.util

/**
 * Best-effort reader for an installed app's logcat output, for the "View Logs" action after
 * Install/Launch. Android has restricted logcat to an app's own UID since API 16 (Jelly Bean) --
 * a regular, non-system app can only ever see log lines it produced itself, unless the device is
 * rooted, is an emulator/userdebug build, or otherwise relaxes that restriction. This shells out to
 * the real system `logcat`/`ps` binaries the same way any adb-less in-app logcat viewer does, and
 * is expected to come back empty on a locked-down production device -- that's a platform limitation,
 * not a bug in this reader.
 */
object LogcatReader {

    data class LogEntry(val level: Char, val tag: String, val message: String)

    sealed class Result {
        data class Entries(val entries: List<LogEntry>) : Result()
        /** No running process found for the package (already exited, never started, or this app
         * can't see other apps' processes at all on this device). */
        object NoProcessFound : Result()
    }

    fun read(packageName: String, maxLines: Int = 500): Result {
        val psOutput = runCommand(listOf("ps", "-A")) ?: return Result.NoProcessFound
        val pid = findPid(psOutput, packageName) ?: return Result.NoProcessFound

        val logOutput = runCommand(listOf("logcat", "-d", "-v", "brief", "--pid=$pid")).orEmpty()
        val entries = logOutput.lineSequence()
            .mapNotNull { parseBriefLine(it) }
            .toList()
            .takeLast(maxLines)
        return Result.Entries(entries)
    }

    /**
     * Finds the pid of the process whose `ps -A` NAME column exactly matches [packageName], or a
     * secondary process of it (e.g. "com.example:remote"). NAME is always the last whitespace-
     * separated column; PID is always the second.
     */
    fun findPid(psOutput: String, packageName: String): String? =
        psOutput.lineSequence()
            .mapNotNull { line ->
                val columns = line.trim().split(Regex("\\s+"))
                val name = columns.lastOrNull() ?: return@mapNotNull null
                val pid = columns.getOrNull(1) ?: return@mapNotNull null
                if (name == packageName || name.startsWith("$packageName:")) pid else null
            }
            .firstOrNull()

    /** Parses one `logcat -v brief` line, e.g. "I/ActivityManager( 1234): Displayed com.example". */
    fun parseBriefLine(line: String): LogEntry? {
        val match = BRIEF_LINE.matchEntire(line) ?: return null
        val (level, tag, _, message) = match.destructured
        return LogEntry(level[0], tag, message)
    }

    private fun runCommand(command: List<String>): String? = runCatching {
        val process = ProcessBuilder(command).redirectErrorStream(true).start()
        process.inputStream.bufferedReader().readText().also { process.waitFor() }
    }.getOrNull()

    private val BRIEF_LINE = Regex("""^([VDIWEF])/(.*)\(\s*(\d+)\):\s?(.*)$""")
}
