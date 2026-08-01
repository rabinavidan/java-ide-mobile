package com.javaide.mobile.completion

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File

/**
 * Exercises against the real bundled android.jar (same file the app compiles against), not a
 * fake/fixture jar, so the results reflect the actual index the app builds at runtime.
 */
class ImportIndexTest {

    private lateinit var androidJar: File

    @Before
    fun setUp() {
        val path = System.getProperty("android.jar.path")
            ?: error("android.jar.path system property not set; see app/build.gradle.kts")
        androidJar = File(path)
        check(androidJar.isFile) { "android.jar not found at $path" }
    }

    @Test
    fun singleCandidateResolvesExactly() {
        val candidates = ImportIndex.candidatesFor(androidJar, "ArrayList")

        assertEquals(listOf("java.util.ArrayList"), candidates)
    }

    @Test
    fun anotherCommonSingleCandidateResolvesExactly() {
        val candidates = ImportIndex.candidatesFor(androidJar, "List")

        assertEquals(listOf("java.util.List"), candidates)
    }

    @Test
    fun genuinelyAmbiguousNameReturnsMultipleCandidates() {
        // Camera exists in both android.hardware and android.graphics -- a real ambiguity in
        // android.jar, not a hypothetical one, which is exactly why the app prompts to disambiguate.
        val candidates = ImportIndex.candidatesFor(androidJar, "Camera")

        assertTrue(candidates.contains("android.hardware.Camera"))
        assertTrue(candidates.contains("android.graphics.Camera"))
        assertEquals(2, candidates.size)
    }

    @Test
    fun unknownSimpleNameReturnsNoCandidates() {
        val candidates = ImportIndex.candidatesFor(androidJar, "ThisClassDoesNotExistAnywhere")

        assertTrue(candidates.isEmpty())
    }

    @Test
    fun nestedClassesAreNotIndexed() {
        // "Entry" only exists in android.jar as nested types (java.util.Map$Entry,
        // java.security.KeyStore$Entry, ...), never as a top-level class -- deliberately out of
        // scope (see ImportIndex's kdoc): entries with '$' are skipped since importing them needs
        // "import a.b.Outer.Inner;" syntax, not a plain top-level import. If nested entries leaked
        // into the index, this would return non-empty results instead.
        val candidates = ImportIndex.candidatesFor(androidJar, "Entry")

        assertTrue(candidates.isEmpty())
    }

    @Test
    fun repeatedLookupsAgreeAfterTheIndexIsCached() {
        val first = ImportIndex.candidatesFor(androidJar, "ArrayList")
        val second = ImportIndex.candidatesFor(androidJar, "ArrayList")

        assertEquals(first, second)
    }
}
