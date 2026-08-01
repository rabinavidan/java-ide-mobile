package com.javaide.mobile.vcs

import org.eclipse.jgit.api.Git
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

/**
 * Exercises GitRepository's Context-free functions against real JGit repositories in temp
 * directories -- real git plumbing, not mocks. Repo setup that needs a commit uses raw JGit
 * directly (not GitRepository.commit(), which needs an Android Context for optional GPG signing
 * -- out of scope for these plain-JVM unit tests) with signing explicitly disabled.
 */
class GitRepositoryTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private fun rawCommit(dir: File, message: String) {
        Git.open(dir).use { git ->
            git.add().addFilepattern(".").call()
            git.commit()
                .setMessage(message)
                .setSign(false)
                .setAuthor("Test", "test@example.com")
                .setCommitter("Test", "test@example.com")
                .call()
        }
    }

    @Test
    fun isRepoReflectsPresenceOfGitDirectory() {
        val dir = tempFolder.newFolder("Plain")
        assertFalse(GitRepository.isRepo(dir))

        val result = GitRepository.init(dir)

        assertTrue(result.success)
        assertTrue(GitRepository.isRepo(dir))
    }

    @Test
    fun statusReportsUntrackedThenCleanAfterCommitThenModified() {
        val dir = tempFolder.newFolder("StatusRepo")
        GitRepository.init(dir)
        val file = File(dir, "Main.java").apply { writeText("class Main {}") }

        val untracked = GitRepository.status(dir).getOrThrow()
        assertTrue(untracked.untracked.contains("Main.java"))
        assertFalse(untracked.isClean)

        rawCommit(dir, "initial")
        val clean = GitRepository.status(dir).getOrThrow()
        assertTrue(clean.isClean)

        file.writeText("class Main { void x() {} }")
        val modified = GitRepository.status(dir).getOrThrow()
        assertTrue(modified.modified.contains("Main.java"))
        assertFalse(modified.isClean)
    }

    @Test
    fun addAllStagesNewFiles() {
        val dir = tempFolder.newFolder("AddAllRepo")
        GitRepository.init(dir)
        File(dir, "Main.java").writeText("class Main {}")

        val result = GitRepository.addAll(dir)

        assertTrue(result.success)
        val status = GitRepository.status(dir).getOrThrow()
        assertTrue(status.added.contains("Main.java"))
    }

    @Test
    fun logReturnsCommitsNewestFirst() {
        val dir = tempFolder.newFolder("LogRepo")
        GitRepository.init(dir)
        File(dir, "Main.java").writeText("v1")
        rawCommit(dir, "first")
        File(dir, "Main.java").writeText("v2")
        rawCommit(dir, "second")

        val commits = GitRepository.log(dir).getOrThrow()

        assertEquals(2, commits.size)
        assertEquals("second", commits[0].message)
        assertEquals("first", commits[1].message)
    }

    @Test
    fun diffShowsUnstagedChangesToATrackedFile() {
        val dir = tempFolder.newFolder("DiffRepo")
        GitRepository.init(dir)
        val file = File(dir, "Main.java").apply { writeText("class Main {}\n") }
        rawCommit(dir, "initial")
        file.writeText("class Main { void x() {} }\n")

        val diff = GitRepository.diff(dir).getOrThrow()

        assertTrue(diff.contains("Main.java"))
    }

    @Test
    fun branchesListsCurrentAndAllAfterCheckout() {
        val dir = tempFolder.newFolder("BranchRepo")
        GitRepository.init(dir)
        File(dir, "Main.java").writeText("class Main {}")
        rawCommit(dir, "initial")

        val checkoutResult = GitRepository.checkout(dir, "feature", createNew = true)
        assertTrue(checkoutResult.success)

        val (current, names) = GitRepository.branches(dir).getOrThrow()
        assertEquals("feature", current)
        assertTrue(names.contains("feature"))
        assertTrue(names.any { it == "master" || it == "main" })
    }

    @Test
    fun pushThenCloneRoundTripsFileContent() {
        val bareDir = tempFolder.newFolder("Bare")
        Git.init().setDirectory(bareDir).setBare(true).call().close()

        val workDir = tempFolder.newFolder("Work")
        GitRepository.init(workDir)
        File(workDir, "Main.java").writeText("class Main {}")
        rawCommit(workDir, "initial")

        val pushResult = GitRepository.push(workDir, bareDir.absolutePath, credentials = null)
        assertTrue(pushResult.success)

        val cloneDir = File(tempFolder.root, "Clone")
        val cloneResult = GitRepository.clone(bareDir.absolutePath, cloneDir, credentials = null)

        assertTrue(cloneResult.success)
        assertEquals("class Main {}", File(cloneDir, "Main.java").readText())
    }

    @Test
    fun pullDetectsConflictWithRealMarkersInWorkingTree() {
        val (_, localDir) = setUpDivergedClone()

        val pullResult = GitRepository.pull(localDir, credentials = null)

        assertFalse(pullResult.success)
        assertTrue(pullResult.message.contains("shared.txt"))
        val status = GitRepository.status(localDir).getOrThrow()
        assertTrue(status.conflicting.contains("shared.txt"))
        val content = File(localDir, "shared.txt").readText()
        assertTrue(content.contains("<<<<<<<"))
        assertTrue(content.contains("======="))
        assertTrue(content.contains(">>>>>>>"))
    }

    @Test
    fun markResolvedStagesTheConflictedFile() {
        val (_, localDir) = setUpDivergedClone()
        GitRepository.pull(localDir, credentials = null)
        File(localDir, "shared.txt").writeText("resolved\n")

        val result = GitRepository.markResolved(localDir, "shared.txt")

        assertTrue(result.success)
        val status = GitRepository.status(localDir).getOrThrow()
        assertFalse(status.conflicting.contains("shared.txt"))
    }

    @Test
    fun abortMergeRevertsWorkingTreeAndClearsConflict() {
        val (_, localDir) = setUpDivergedClone()
        GitRepository.pull(localDir, credentials = null)

        val result = GitRepository.abortMerge(localDir)

        assertTrue(result.success)
        assertFalse(File(localDir, ".git/MERGE_HEAD").exists())
        val status = GitRepository.status(localDir).getOrThrow()
        assertTrue(status.isClean)
        assertEquals("local-change", File(localDir, "shared.txt").readText().trim())
    }

    /** A remote repo plus a local clone of it, each with a differing edit to "shared.txt". */
    private fun setUpDivergedClone(): Pair<File, File> {
        val remoteDir = tempFolder.newFolder("Remote")
        Git.init().setDirectory(remoteDir).call().use { git ->
            File(remoteDir, "shared.txt").writeText("base\n")
            git.add().addFilepattern(".").call()
            git.commit().setMessage("base").setSign(false)
                .setAuthor("Test", "test@example.com").setCommitter("Test", "test@example.com").call()
        }

        val localDir = File(tempFolder.root, "Local")
        Git.cloneRepository().setURI(remoteDir.absolutePath).setDirectory(localDir).call().close()

        Git.open(remoteDir).use { git ->
            File(remoteDir, "shared.txt").writeText("remote-change\n")
            git.add().addFilepattern(".").call()
            git.commit().setMessage("remote change").setSign(false)
                .setAuthor("Test", "test@example.com").setCommitter("Test", "test@example.com").call()
        }

        Git.open(localDir).use { git ->
            File(localDir, "shared.txt").writeText("local-change\n")
            git.add().addFilepattern(".").call()
            git.commit().setMessage("local change").setSign(false)
                .setAuthor("Test", "test@example.com").setCommitter("Test", "test@example.com").call()
        }

        return remoteDir to localDir
    }
}
