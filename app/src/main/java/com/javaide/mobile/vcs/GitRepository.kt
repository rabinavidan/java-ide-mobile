package com.javaide.mobile.vcs

import android.content.Context
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.api.MergeResult
import org.eclipse.jgit.api.ResetCommand
import org.eclipse.jgit.lib.BranchConfig
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider
import java.io.ByteArrayOutputStream
import java.io.File

data class GitOperationResult(val success: Boolean, val message: String)

data class GitStatusInfo(
    val added: Set<String>,
    val changed: Set<String>,
    val removed: Set<String>,
    val modified: Set<String>,
    val missing: Set<String>,
    val untracked: Set<String>,
    val conflicting: Set<String>
) {
    val isClean: Boolean
        get() = added.isEmpty() && changed.isEmpty() && removed.isEmpty() &&
            modified.isEmpty() && missing.isEmpty() && untracked.isEmpty() && conflicting.isEmpty()
}

data class GitCommitInfo(val hash: String, val author: String, val time: Long, val message: String)

/** Username + a GitHub personal access token (or equivalent). SSH auth is out of scope for now. */
data class GitCredentials(val username: String, val token: String)

/**
 * Thin wrapper around JGit's porcelain API for the operations the Version Control screen needs.
 * Each call opens the repo, performs the operation, and closes it again -- JGit's Git/Repository
 * objects hold native file handles, so they aren't kept open across calls.
 */
object GitRepository {

    fun isRepo(projectDir: File): Boolean = File(projectDir, ".git").exists()

    fun init(projectDir: File): GitOperationResult = runCatching {
        Git.init().setDirectory(projectDir).call().close()
    }.toResult("Initialized empty repository")

    fun status(projectDir: File): Result<GitStatusInfo> = runCatching {
        Git.open(projectDir).use { git ->
            val status = git.status().call()
            GitStatusInfo(
                added = status.added,
                changed = status.changed,
                removed = status.removed,
                modified = status.modified,
                missing = status.missing,
                untracked = status.untracked,
                conflicting = status.conflicting
            )
        }
    }

    /** Stages everything: new/modified files plus deletions (git add . && git add -u). */
    fun addAll(projectDir: File): GitOperationResult = runCatching {
        Git.open(projectDir).use { git ->
            git.add().addFilepattern(".").call()
            git.add().addFilepattern(".").setUpdate(true).call()
        }
    }.toResult("Staged all changes")

    fun commit(
        context: Context,
        projectDir: File,
        message: String,
        authorName: String,
        authorEmail: String,
        sign: Boolean
    ): GitOperationResult = runCatching {
        Git.open(projectDir).use { git ->
            val command = git.commit()
                .setMessage(message)
                .setAuthor(authorName, authorEmail)
                .setCommitter(authorName, authorEmail)
                .setSign(sign)
            if (sign) {
                command.setSigner(GitCommitSigner(context))
            }
            command.call()
        }
    }.toResult("Committed")

    fun log(projectDir: File, maxCount: Int = 50): Result<List<GitCommitInfo>> = runCatching {
        Git.open(projectDir).use { git ->
            git.log().setMaxCount(maxCount).call().map { commit ->
                GitCommitInfo(
                    hash = commit.name,
                    author = commit.authorIdent.name,
                    time = commit.commitTime * 1000L,
                    message = commit.shortMessage
                )
            }
        }
    }

    /** Unified diff of the working tree against the index (unstaged changes). */
    fun diff(projectDir: File): Result<String> = runCatching {
        Git.open(projectDir).use { git ->
            val out = ByteArrayOutputStream()
            git.diff().setOutputStream(out).call()
            out.toString(Charsets.UTF_8.name())
        }
    }

    /** Returns the current branch name plus all local branch names. */
    fun branches(projectDir: File): Result<Pair<String, List<String>>> = runCatching {
        Git.open(projectDir).use { git ->
            val current = git.repository.branch
            val names = git.branchList().call().map { it.name.removePrefix("refs/heads/") }
            current to names
        }
    }

    fun checkout(projectDir: File, branchName: String, createNew: Boolean): GitOperationResult = runCatching {
        Git.open(projectDir).use { git ->
            git.checkout().setName(branchName).setCreateBranch(createNew).call()
        }
    }.toResult("Switched to $branchName")

    fun push(projectDir: File, remoteUrl: String, credentials: GitCredentials?): GitOperationResult = runCatching {
        Git.open(projectDir).use { git ->
            val command = git.push().setRemote(remoteUrl)
            credentials?.let { command.setCredentialsProvider(UsernamePasswordCredentialsProvider(it.username, it.token)) }
            command.call().joinToString("\n") { it.messages }
        }
    }.toResult("Pushed")

    /**
     * JGit doesn't throw on a conflicting merge -- pull() returns normally with a PullResult
     * whose merge status is CONFLICTING, leaving real git conflict markers in the working tree.
     * So unlike the other operations here, success/failure has to be read from the result content,
     * not just "did an exception happen".
     */
    fun pull(projectDir: File, credentials: GitCredentials?): GitOperationResult = runCatching {
        Git.open(projectDir).use { git ->
            val command = git.pull()
                // Force merge mode — never rebase. Rebase leaves mergeResult null, which
                // makes conflict detection impossible without parsing rebaseResult instead.
                .setRebase(BranchConfig.BranchRebaseMode.NONE)
            credentials?.let { command.setCredentialsProvider(UsernamePasswordCredentialsProvider(it.username, it.token)) }
            command.call()
        }
    }.fold(
        onSuccess = { result ->
            when {
                result.isSuccessful -> GitOperationResult(true, "Pulled")
                result.mergeResult?.mergeStatus == MergeResult.MergeStatus.CONFLICTING -> {
                    val files = result.mergeResult?.conflicts?.keys.orEmpty().joinToString(", ")
                    GitOperationResult(false, "Pull resulted in conflicts in: $files")
                }
                else -> GitOperationResult(false, "Pull did not complete: ${result.mergeResult?.mergeStatus}")
            }
        },
        onFailure = { GitOperationResult(false, it.message ?: it.toString()) }
    )

    /** Stages a single conflicted file once the user has hand-resolved its conflict markers. */
    fun markResolved(projectDir: File, filePath: String): GitOperationResult = runCatching {
        Git.open(projectDir).use { git -> git.add().addFilepattern(filePath).call() }
    }.toResult("Marked resolved")

    /** Bails out of an in-progress conflicted merge, discarding the partial merge state. */
    fun abortMerge(projectDir: File): GitOperationResult = runCatching {
        Git.open(projectDir).use { git -> git.reset().setMode(ResetCommand.ResetType.HARD).setRef("HEAD").call() }
    }.toResult("Merge aborted")

    fun clone(remoteUrl: String, targetDir: File, credentials: GitCredentials?): GitOperationResult = runCatching {
        val command = Git.cloneRepository().setURI(remoteUrl).setDirectory(targetDir)
        credentials?.let { command.setCredentialsProvider(UsernamePasswordCredentialsProvider(it.username, it.token)) }
        command.call().close()
    }.toResult("Cloned")

    private fun <T> Result<T>.toResult(successMessage: String): GitOperationResult = fold(
        onSuccess = { GitOperationResult(true, successMessage) },
        onFailure = { GitOperationResult(false, it.message ?: it.toString()) }
    )
}
