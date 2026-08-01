package com.javaide.mobile.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LogcatReaderTest {

    @Test
    fun parseBriefLineExtractsLevelTagPidAndMessage() {
        val entry = LogcatReader.parseBriefLine("I/ActivityManager( 1234): Displayed com.example/.MainActivity")

        assertEquals('I', entry?.level)
        assertEquals("ActivityManager", entry?.tag)
        assertEquals("Displayed com.example/.MainActivity", entry?.message)
    }

    @Test
    fun parseBriefLineHandlesTightlyPackedPidAndDottedTag() {
        val entry = LogcatReader.parseBriefLine("W/System.err( 555): java.lang.NullPointerException")

        assertEquals('W', entry?.level)
        assertEquals("System.err", entry?.tag)
        assertEquals("java.lang.NullPointerException", entry?.message)
    }

    @Test
    fun parseBriefLineReturnsNullForNonLogLines() {
        assertNull(LogcatReader.parseBriefLine("--------- beginning of main"))
        assertNull(LogcatReader.parseBriefLine(""))
    }

    @Test
    fun findPidMatchesExactPackageName() {
        val psOutput = """
            USER           PID  PPID     VSZ    RSS WCHAN            ADDR S NAME
            u0_a123       5678   456  123456  65432 SyS_epoll_wait      0 S com.example.myapp
            u0_a125       5680   456  123456  65432 SyS_epoll_wait      0 S com.other.app
        """.trimIndent()

        assertEquals("5678", LogcatReader.findPid(psOutput, "com.example.myapp"))
        assertEquals("5680", LogcatReader.findPid(psOutput, "com.other.app"))
    }

    @Test
    fun findPidMatchesSecondaryProcessSuffix() {
        val psOutput = """
            USER           PID  PPID     VSZ    RSS WCHAN            ADDR S NAME
            u0_a124       5679   456  123456  65432 SyS_epoll_wait      0 S com.example.myapp:remote
        """.trimIndent()

        assertEquals("5679", LogcatReader.findPid(psOutput, "com.example.myapp"))
    }

    @Test
    fun findPidReturnsNullWhenPackageIsNotRunning() {
        val psOutput = """
            USER           PID  PPID     VSZ    RSS WCHAN            ADDR S NAME
            u0_a123       5678   456  123456  65432 SyS_epoll_wait      0 S com.example.myapp
        """.trimIndent()

        assertNull(LogcatReader.findPid(psOutput, "com.notfound.app"))
    }

    @Test
    fun findPidDoesNotMatchAPackageNamePrefix() {
        val psOutput = """
            USER           PID  PPID     VSZ    RSS WCHAN            ADDR S NAME
            u0_a123       5678   456  123456  65432 SyS_epoll_wait      0 S com.example.myapp2
        """.trimIndent()

        assertNull(LogcatReader.findPid(psOutput, "com.example.myapp"))
    }
}
