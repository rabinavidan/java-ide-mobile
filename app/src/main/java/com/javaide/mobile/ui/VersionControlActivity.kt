package com.javaide.mobile.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.javaide.mobile.R
import com.javaide.mobile.data.Logger
import com.javaide.mobile.databinding.ActivityVersionControlBinding
import com.javaide.mobile.databinding.ItemConflictBinding
import com.javaide.mobile.vcs.GitCredentials
import com.javaide.mobile.vcs.GitOperationResult
import com.javaide.mobile.vcs.GitRepository
import com.javaide.mobile.vcs.PgpKeyManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/** A single-screen git dashboard for one project: status, commit (optionally signed), push/pull, log. */
class VersionControlActivity : BaseActivity() {

    companion object {
        const val EXTRA_PROJECT_PATH = "extra_project_path"
        private const val DEFAULT_AUTHOR_NAME = "JavaIDE User"
        private const val DEFAULT_AUTHOR_EMAIL = "javaide@localhost"
    }

    private lateinit var binding: ActivityVersionControlBinding
    private lateinit var projectDir: File
    private lateinit var adapter: CommitLogAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityVersionControlBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val path = intent.getStringExtra(EXTRA_PROJECT_PATH) ?: error("Missing $EXTRA_PROJECT_PATH")
        projectDir = File(path)
        title = projectDir.name

        adapter = CommitLogAdapter()
        binding.recyclerCommits.layoutManager = LinearLayoutManager(this)
        binding.recyclerCommits.adapter = adapter

        binding.buttonSwitchBranch.setOnClickListener { showBranchDialog() }
        binding.buttonViewDiff.setOnClickListener { showDiffDialog() }
        binding.buttonCommit.setOnClickListener { performCommit() }
        binding.buttonViewKey.setOnClickListener { showSigningKeyDialog() }
        binding.buttonPush.setOnClickListener { promptCredentialsAndRun(isPush = true) }
        binding.buttonPull.setOnClickListener { promptCredentialsAndRun(isPush = false) }
        binding.buttonAbortMerge.setOnClickListener { confirmAbortMerge() }

        ensureRepoThenRefresh()
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }

    override fun onResume() {
        super.onResume()
        if (GitRepository.isRepo(projectDir)) refresh()
    }

    private fun ensureRepoThenRefresh() {
        if (GitRepository.isRepo(projectDir)) {
            refresh()
            return
        }
        AlertDialog.Builder(this)
            .setTitle(R.string.dialog_init_repo_title)
            .setMessage(R.string.dialog_init_repo_message)
            .setPositiveButton(R.string.action_initialize) { _, _ ->
                lifecycleScope.launch {
                    val result = withContext(Dispatchers.IO) { GitRepository.init(projectDir) }
                    if (result.success) refresh() else Toast.makeText(this@VersionControlActivity, result.message, Toast.LENGTH_LONG).show()
                }
            }
            .setNegativeButton(R.string.dialog_cancel) { _, _ -> finish() }
            .setCancelable(false)
            .show()
    }

    private fun refresh() {
        lifecycleScope.launch {
            val branchesResult = withContext(Dispatchers.IO) { GitRepository.branches(projectDir) }
            branchesResult.onSuccess { (current, _) ->
                binding.textBranch.text = getString(R.string.label_branch, current)
            }

            val statusResult = withContext(Dispatchers.IO) { GitRepository.status(projectDir) }
            statusResult.onSuccess { status ->
                binding.textStatus.text = if (status.isClean) {
                    getString(R.string.status_clean)
                } else {
                    buildString {
                        status.added.forEach { appendLine("A  $it") }
                        status.changed.forEach { appendLine("M  $it") }
                        status.removed.forEach { appendLine("D  $it") }
                        status.modified.forEach { appendLine("M  $it") }
                        status.missing.forEach { appendLine("!  $it") }
                        status.untracked.forEach { appendLine("?  $it") }
                        status.conflicting.forEach { appendLine("C  $it") }
                    }.trimEnd()
                }
                updateConflictsSection(status.conflicting)
            }

            val logResult = withContext(Dispatchers.IO) { GitRepository.log(projectDir) }
            logResult.onSuccess { commits -> adapter.submitList(commits) }
        }
    }

    /** Files with unresolved conflict markers, from the last successful status() call. */
    private fun updateConflictsSection(conflicting: Set<String>) {
        binding.containerConflictItems.removeAllViews()
        if (conflicting.isEmpty()) {
            binding.layoutConflicts.visibility = View.GONE
            return
        }
        binding.layoutConflicts.visibility = View.VISIBLE
        binding.textConflictsHeader.text = getString(R.string.label_conflicts, conflicting.size)
        conflicting.sorted().forEach { path ->
            val itemBinding = ItemConflictBinding.inflate(layoutInflater, binding.containerConflictItems, false)
            itemBinding.textConflictFile.text = path
            itemBinding.buttonOpenConflict.setOnClickListener {
                val intent = Intent(this, EditorActivity::class.java)
                intent.putExtra(EditorActivity.EXTRA_FILE_PATH, File(projectDir, path).absolutePath)
                intent.putExtra(EditorActivity.EXTRA_PROJECT_PATH, projectDir.absolutePath)
                startActivity(intent)
            }
            itemBinding.buttonMarkResolved.setOnClickListener {
                lifecycleScope.launch {
                    val result = withContext(Dispatchers.IO) { GitRepository.markResolved(projectDir, path) }
                    onOperationDone("resolve", result)
                    refresh()
                }
            }
            binding.containerConflictItems.addView(itemBinding.root)
        }
    }

    private fun confirmAbortMerge() {
        AlertDialog.Builder(this)
            .setTitle(R.string.dialog_abort_merge_title)
            .setMessage(R.string.dialog_abort_merge_message)
            .setPositiveButton(R.string.action_abort) { _, _ ->
                lifecycleScope.launch {
                    val result = withContext(Dispatchers.IO) { GitRepository.abortMerge(projectDir) }
                    onOperationDone("abort merge", result)
                    refresh()
                }
            }
            .setNegativeButton(R.string.dialog_cancel, null)
            .show()
    }

    private fun performCommit() {
        val message = binding.editCommitMessage.text?.toString()?.trim().orEmpty()
        if (message.isEmpty()) {
            binding.editCommitMessage.error = getString(R.string.error_commit_message_required)
            return
        }
        val sign = binding.checkSignCommit.isChecked
        lifecycleScope.launch {
            withContext(Dispatchers.IO) { GitRepository.addAll(projectDir) }
            val result = withContext(Dispatchers.IO) {
                GitRepository.commit(
                    context = this@VersionControlActivity,
                    projectDir = projectDir,
                    message = message,
                    authorName = DEFAULT_AUTHOR_NAME,
                    authorEmail = DEFAULT_AUTHOR_EMAIL,
                    sign = sign
                )
            }
            onOperationDone("commit", result)
            if (result.success) {
                binding.editCommitMessage.text?.clear()
                refresh()
            }
        }
    }

    private fun promptCredentialsAndRun(isPush: Boolean) {
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 24, 48, 24)
        }
        val remoteInput = EditText(this).apply { hint = getString(R.string.hint_remote_url) }
        val usernameInput = EditText(this).apply { hint = getString(R.string.hint_username) }
        val tokenInput = EditText(this).apply {
            hint = getString(R.string.hint_token)
            inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
        }
        container.addView(remoteInput)
        container.addView(usernameInput)
        container.addView(tokenInput)

        AlertDialog.Builder(this)
            .setTitle(R.string.dialog_credentials_title)
            .setView(container)
            .setPositiveButton(if (isPush) R.string.action_push else R.string.action_pull) { _, _ ->
                val remoteUrl = remoteInput.text?.toString()?.trim().orEmpty()
                val username = usernameInput.text?.toString()?.trim().orEmpty()
                val token = tokenInput.text?.toString()?.trim().orEmpty()
                val credentials = if (username.isNotEmpty() && token.isNotEmpty()) GitCredentials(username, token) else null
                runPushOrPull(isPush, remoteUrl, credentials)
            }
            .setNegativeButton(R.string.dialog_cancel, null)
            .show()
    }

    private fun runPushOrPull(isPush: Boolean, remoteUrl: String, credentials: GitCredentials?) {
        lifecycleScope.launch {
            val result = withContext(Dispatchers.IO) {
                if (isPush) {
                    GitRepository.push(projectDir, remoteUrl, credentials)
                } else {
                    GitRepository.pull(projectDir, credentials)
                }
            }
            onOperationDone(if (isPush) "push" else "pull", result)
            // Always refresh, even on failure -- a conflicting pull reports success=false but
            // still needs the conflicts section to appear so the user can act on it.
            refresh()
        }
    }

    private fun onOperationDone(category: String, result: GitOperationResult) {
        Toast.makeText(this, result.message, Toast.LENGTH_LONG).show()
        if (result.success) {
            Logger.info(this, "vcs", "$category succeeded for '${projectDir.name}'")
        } else {
            Logger.warn(this, "vcs", "$category failed for '${projectDir.name}': ${result.message}")
        }
    }

    private fun showBranchDialog() {
        lifecycleScope.launch {
            val result = withContext(Dispatchers.IO) { GitRepository.branches(projectDir) }
            result.onSuccess { (current, names) ->
                val container = LinearLayout(this@VersionControlActivity).apply {
                    orientation = LinearLayout.VERTICAL
                    setPadding(48, 24, 48, 24)
                }
                val existingList = TextView(this@VersionControlActivity).apply {
                    text = names.joinToString("\n") { if (it == current) "* $it" else "  $it" }
                    setTextIsSelectable(true)
                }
                val input = EditText(this@VersionControlActivity).apply {
                    hint = getString(R.string.dialog_branch_title)
                }
                container.addView(existingList)
                container.addView(input)

                AlertDialog.Builder(this@VersionControlActivity)
                    .setTitle(R.string.dialog_branch_title)
                    .setView(container)
                    .setPositiveButton(R.string.action_switch_branch) { _, _ ->
                        val name = input.text?.toString()?.trim().orEmpty()
                        if (name.isNotEmpty()) switchBranch(name, createNew = !names.contains(name))
                    }
                    .setNegativeButton(R.string.dialog_cancel, null)
                    .show()
            }
        }
    }

    private fun switchBranch(name: String, createNew: Boolean) {
        lifecycleScope.launch {
            val result = withContext(Dispatchers.IO) { GitRepository.checkout(projectDir, name, createNew) }
            onOperationDone("checkout", result)
            if (result.success) refresh()
        }
    }

    private fun showDiffDialog() {
        lifecycleScope.launch {
            val result = withContext(Dispatchers.IO) { GitRepository.diff(projectDir) }
            val text = result.getOrElse { it.message ?: it.toString() }
            showScrollableTextDialog(R.string.title_diff, text)
        }
    }

    private fun showSigningKeyDialog() {
        lifecycleScope.launch {
            val armored = withContext(Dispatchers.IO) { PgpKeyManager.armoredPublicKey(this@VersionControlActivity) }
            val dialogView = buildScrollableTextView(armored)
            AlertDialog.Builder(this@VersionControlActivity)
                .setTitle(R.string.dialog_signing_key_title)
                .setView(dialogView)
                .setPositiveButton(R.string.action_copy) { _, _ ->
                    val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    clipboard.setPrimaryClip(ClipData.newPlainText("PGP public key", armored))
                    Toast.makeText(this@VersionControlActivity, R.string.msg_key_copied, Toast.LENGTH_SHORT).show()
                }
                .setNegativeButton(R.string.dialog_cancel, null)
                .show()
        }
    }

    private fun showScrollableTextDialog(titleRes: Int, text: String) {
        AlertDialog.Builder(this)
            .setTitle(titleRes)
            .setView(buildScrollableTextView(text.ifBlank { " " }))
            .setPositiveButton(android.R.string.ok, null)
            .show()
    }

    private fun buildScrollableTextView(text: String): View {
        val textView = TextView(this).apply {
            this.text = text
            typeface = android.graphics.Typeface.MONOSPACE
            textSize = 12f
            setTextIsSelectable(true)
            setPadding(48, 24, 48, 24)
        }
        return ScrollView(this).apply { addView(textView) }
    }
}
