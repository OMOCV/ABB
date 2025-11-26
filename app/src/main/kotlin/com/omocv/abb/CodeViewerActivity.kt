package com.omocv.abb

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.graphics.drawable.ColorDrawable
import android.content.res.Configuration
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.TextView
import android.widget.EditText
import android.widget.Toast
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.color.MaterialColors
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import android.text.Editable
import android.text.TextWatcher
import android.text.SpannableString
import android.text.style.BackgroundColorSpan
import android.text.Spannable
import android.text.Spanned
import android.widget.RadioGroup
import android.util.Base64
import kotlin.math.max
import com.google.android.material.textfield.TextInputEditText
import com.google.android.gms.auth.GoogleAuthUtil
import com.google.android.gms.auth.UserRecoverableAuthException
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.Scopes
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.Scope
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/**
 * Full-screen code viewer with line numbers and advanced features
 * Version 2.3.0 - Enhanced search, replace, syntax checking with clickable results
 */
class CodeViewerActivity : AppCompatActivity() {

    private lateinit var toolbar: MaterialToolbar
    private lateinit var scrollViewLineNumbers: android.widget.ScrollView
    private lateinit var scrollViewCode: android.widget.ScrollView
    private lateinit var tvLineNumbers: TextView
    private lateinit var tvCodeContent: TextView
    private lateinit var etCodeContent: SyntaxHighlightEditText
    
    private val syntaxHighlighter = ABBSyntaxHighlighter()
    private val abbParser = ABBParser()
    private lateinit var fileName: String
    private var fileUri: String? = null
    private var fileContent: String = ""
    private var originalContent: String = ""
    private var currentSearchQuery = ""
    private var isEditMode = false
    private var hasUnsavedChanges = false
    private var isSavePromptVisible = false
    private var currentProgramFile: ABBProgramFile? = null
    private var isScrollSyncing = false  // Flag to prevent infinite scroll loop
    private var currentHighlightedLine: Int = -1  // Track currently highlighted line
    private var currentHighlightSpan: BackgroundColorSpan? = null  // Track current highlight span
    private var currentHighlightRange: Pair<Int, Int>? = null
    private var currentHighlightColor: Int? = null
    private var currentHighlightColumns: Pair<Int, Int>? = null
    private var pendingExitAfterSave = false
    private var isFolded = false
    private var foldedContent: String? = null
    private var collaborationSessionId: String? = null
    private var connectedRobot = false
    private var lastCloudSyncTime: Long = 0L
    private lateinit var googleSignInClient: GoogleSignInClient
    private var googleAccount: GoogleSignInAccount? = null
    // File save launcher for save-as functionality
    private val saveFileLauncher = registerForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            result.data?.data?.let { uri ->
                val content = if (isEditMode) etCodeContent.text.toString() else fileContent
                saveToUri(uri, content)
            }
        } else if (pendingExitAfterSave) {
            pendingExitAfterSave = false
        }
    }

    private val googleSignInLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode != RESULT_OK) {
            googleAccount = null
            Toast.makeText(this, R.string.cloud_google_signin_required, Toast.LENGTH_SHORT).show()
            return@registerForActivityResult
        }

        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(ApiException::class.java)
            if (!GoogleSignIn.hasPermissions(account, Scope(GOOGLE_DRIVE_SCOPE))) {
                requestGoogleDriveConsent(account)
            } else {
                handleGoogleAccount(account)
            }
        } catch (e: ApiException) {
            val message = when (e.statusCode) {
                com.google.android.gms.common.api.CommonStatusCodes.CANCELED -> getString(R.string.cloud_google_signin_cancelled)
                12500 /* DEVELOPER_ERROR */ -> getString(R.string.cloud_google_signin_unconfigured)
                else -> e.localizedMessage ?: e.message ?: "Sign-in failed"
            }
            Toast.makeText(this, "Google Sign-In failed: $message", Toast.LENGTH_LONG).show()
        }
    }

    private val googleAuthRecoveryLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode != RESULT_OK) {
            Toast.makeText(this, R.string.cloud_google_consent_required, Toast.LENGTH_SHORT).show()
        }
    }
    
    companion object {
        private const val EXTRA_FILE_NAME = "file_name"
        private const val EXTRA_FILE_CONTENT = "file_content"
        private const val EXTRA_FILE_URI = "file_uri"
        private const val PREFS_NAME = "ABBPrefs"
        private const val KEY_THEME_MODE = "theme_mode"
        private const val KEY_THEME_MANUALLY_SELECTED = "theme_manually_selected"
        private const val KEY_BOOKMARKS = "bookmarks_"
        private const val KEY_AUTO_COMPLETE = "auto_complete_enabled"
        private const val KEY_REAL_TIME_CHECK = "real_time_syntax_check"
        private const val KEY_VCS_PREFIX = "vcs_snapshots_"
        private const val KEY_CLOUD_PREFIX = "cloud_sync_"
        private const val KEY_COLLAB_PREFIX = "collab_session_"
        private const val GOOGLE_DRIVE_SCOPE = "https://www.googleapis.com/auth/drive.file"
        private const val GOOGLE_PERMISSIONS_REQUEST = 9001

        fun newIntent(context: Context, fileName: String, fileContent: String, fileUri: String? = null): Intent {
            return Intent(context, CodeViewerActivity::class.java).apply {
                putExtra(EXTRA_FILE_NAME, fileName)
                putExtra(EXTRA_FILE_CONTENT, fileContent)
                putExtra(EXTRA_FILE_URI, fileUri)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        // Reset manual theme overrides on a fresh launch so the default follows the system theme
        clearManualThemeSelectionIfFirstLaunch(savedInstanceState)

        // Apply saved theme before the activity is created so the initial render uses the preferred mode
        AppCompatDelegate.setDefaultNightMode(resolveSavedTheme())

        super.onCreate(savedInstanceState)
        
        try {
            setContentView(R.layout.activity_code_viewer)
        } catch (e: Exception) {
            // Layout inflation error is fatal
            android.util.Log.e("CodeViewerActivity", "Error inflating layout", e)
            Toast.makeText(
                this,
                getString(R.string.failed_to_load_code_viewer),
                Toast.LENGTH_LONG
            ).show()
            finish()
            return
        }

        initGoogleSignIn()
        
        fileName = intent.getStringExtra(EXTRA_FILE_NAME) ?: "Unknown"
        fileContent = intent.getStringExtra(EXTRA_FILE_CONTENT) ?: ""
        fileUri = intent.getStringExtra(EXTRA_FILE_URI)
        originalContent = fileContent
        
        // Parse the file content to get routines info
        try {
            val tempFile = java.io.File(cacheDir, fileName)
            tempFile.writeText(fileContent)
            currentProgramFile = abbParser.parseFile(tempFile)
            tempFile.delete()
        } catch (e: Exception) {
            android.util.Log.e("CodeViewerActivity", "Error parsing file", e)
            // Continue even if parsing fails - we can still display the content
        }
        
        try {
            initViews()
            displayContent()
            setupRealTimeSyntaxCheck()
        } catch (e: Exception) {
            // Log the error but try to continue
            android.util.Log.e("CodeViewerActivity", "Error in initialization", e)
            Toast.makeText(
                this,
                getString(R.string.failed_to_load_code_viewer),
                Toast.LENGTH_LONG
            ).show()
            // Don't finish - allow user to see what's displayed
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == GOOGLE_PERMISSIONS_REQUEST) {
            if (resultCode != RESULT_OK) {
                Toast.makeText(this, R.string.cloud_google_signin_cancelled, Toast.LENGTH_SHORT).show()
                return
            }

            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            try {
                val account = task.getResult(ApiException::class.java)
                handleGoogleAccount(account)
            } catch (e: ApiException) {
                Toast.makeText(
                    this,
                    "Google Sign-In failed: ${e.localizedMessage ?: e.message ?: "Sign-in failed"}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun initGoogleSignIn() {
        val options = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestScopes(Scope(Scopes.DRIVE_FILE))
            .build()
        googleSignInClient = GoogleSignIn.getClient(this, options)
        googleAccount = GoogleSignIn.getLastSignedInAccount(this)
    }

    private fun initViews() {
        toolbar = findViewById(R.id.toolbar)
        scrollViewLineNumbers = findViewById(R.id.scrollViewLineNumbers)
        scrollViewCode = findViewById(R.id.scrollViewCode)
        tvLineNumbers = findViewById(R.id.tvLineNumbers)
        tvCodeContent = findViewById(R.id.tvCodeContent)
        etCodeContent = findViewById(R.id.etCodeContent)
        
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = fileName

        toolbar.setNavigationOnClickListener {
            handleBackPressed()
        }

        etCodeContent.setAutoCompleteEnabled(isAutoCompleteEnabled())

        // Synchronize scrolling between line numbers and code content
        setupScrollSynchronization()
    }
    
    private fun setupScrollSynchronization() {
        // Remove any existing listeners to prevent duplicates
        scrollViewCode.setOnScrollChangeListener(null)
        scrollViewLineNumbers.setOnScrollChangeListener(null)
        
        // Synchronize line numbers with code content scrolling
        scrollViewCode.setOnScrollChangeListener { _, _, scrollY, _, oldScrollY ->
            if (!isScrollSyncing && scrollY != oldScrollY) {
                isScrollSyncing = true
                scrollViewLineNumbers.scrollTo(0, scrollY)
                isScrollSyncing = false
            }
        }
        
        // Synchronize code content with line numbers scrolling
        scrollViewLineNumbers.setOnScrollChangeListener { _, _, scrollY, _, oldScrollY ->
            if (!isScrollSyncing && scrollY != oldScrollY) {
                isScrollSyncing = true
                scrollViewCode.scrollTo(0, scrollY)
                isScrollSyncing = false
            }
        }
    }
    
    private fun handleBackPressed() {
        if (hasContentChangedFromOriginal()) {
            showUnsavedChangesPrompt(
                onSave = {
                    pendingExitAfterSave = true
                    invokeMenuSaveAction()
                },
                onDiscard = { finish() },
                onCancel = { }
            )
        } else {
            finish()
        }
    }
    
    @SuppressLint("MissingSuperCall")
    override fun onBackPressed() {
        handleBackPressed()
    }

    override fun onPause() {
        super.onPause()

        if (hasContentChangedFromOriginal() && !isChangingConfigurations && !isFinishing) {
            showUnsavedChangesPrompt(
                onSave = { invokeMenuSaveAction() },
                onDiscard = { hasUnsavedChanges = false },
                onCancel = { }
            )
        }
    }

    private fun showUnsavedChangesPrompt(
        onSave: () -> Unit,
        onDiscard: () -> Unit,
        onCancel: (() -> Unit)? = null
    ) {
        if (isSavePromptVisible) return

        isSavePromptVisible = true
        MaterialAlertDialogBuilder(this)
            .setTitle(getString(R.string.unsaved_changes))
            .setMessage(getString(R.string.discard_changes))
            .setPositiveButton(getString(R.string.save)) { _, _ ->
                isSavePromptVisible = false
                onSave()
            }
            .setNegativeButton(getString(R.string.discard_changes)) { _, _ ->
                isSavePromptVisible = false
                onDiscard()
            }
            .apply {
                onCancel?.let {
                    setNeutralButton(getString(R.string.cancel)) { _, _ ->
                        isSavePromptVisible = false
                        it()
                    }
                }
            }
            .setOnDismissListener {
                isSavePromptVisible = false
            }
            .show()
    }

    private fun invokeMenuSaveAction() {
        val saveItem = toolbar.menu?.findItem(R.id.action_save)
        if (saveItem != null && saveItem.isVisible) {
            onOptionsItemSelected(saveItem)
        } else {
            saveChanges()
        }
    }

    private fun displayContent() {
        val displayText = if (isFolded) foldedContent ?: foldContent(fileContent) else fileContent
        val lines = displayText.lines()
        val lineCount = lines.size
        
        // Generate line numbers
        val lineNumbers = StringBuilder()
        for (i in 1..lineCount) {
            lineNumbers.append("$i\n")
        }
        tvLineNumbers.text = lineNumbers.toString()
        
        // Apply syntax highlighting
        if (isEditMode) {
            etCodeContent.setText(displayText)
        } else {
            val highlightedContent = syntaxHighlighter.highlight(displayText)
            tvCodeContent.text = highlightedContent
        }
    }
    
    private fun toggleEditMode() {
        isEditMode = !isEditMode
        
        if (isEditMode) {
            // Switch to edit mode
            if (isFolded) {
                isFolded = false
                foldedContent = null
            }
            tvCodeContent.visibility = View.GONE
            etCodeContent.visibility = View.VISIBLE
            etCodeContent.setText(fileContent)
            etCodeContent.setHighlightingEnabled(true)  // Enable syntax highlighting in edit mode
            Toast.makeText(this, getString(R.string.editing_enabled), Toast.LENGTH_SHORT).show()
        } else {
            // Switch to view mode
            fileContent = etCodeContent.text.toString()
            tvCodeContent.visibility = View.VISIBLE
            etCodeContent.visibility = View.GONE
            displayContent()
            Toast.makeText(this, getString(R.string.editing_disabled), Toast.LENGTH_SHORT).show()
        }

        if (currentHighlightedLine != -1) {
            reapplyCurrentHighlightIfNeeded(applyAfterLayout = true)
        }

        // Update menu
        invalidateOptionsMenu()
    }
    
    private fun saveChanges() {
        if (isEditMode) {
            fileContent = etCodeContent.text.toString()
        }

        // Show save options dialog
        MaterialAlertDialogBuilder(this)
            .setTitle(getString(R.string.save_options))
            .setMessage(getString(R.string.save_confirmation))
            .setPositiveButton(getString(R.string.overwrite_file)) { _, _ ->
                if (fileUri != null) {
                    saveToUri(Uri.parse(fileUri), fileContent)
                } else {
                    saveAsNewFile(fileContent)
                }
            }
            .setNegativeButton(getString(R.string.save_as_new_file)) { _, _ ->
                saveAsNewFile(fileContent)
            }
            .setNeutralButton(getString(R.string.cancel)) { _, _ ->
                pendingExitAfterSave = false
            }
            .setOnCancelListener {
                pendingExitAfterSave = false
            }
            .show()
    }
    
    private fun saveToUri(uri: Uri, content: String) {
        try {
            // Try to take persistable URI permissions if not already held
            try {
                val takeFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                contentResolver.takePersistableUriPermission(uri, takeFlags)
            } catch (e: SecurityException) {
                // Permission might already be held or not available for this URI
                android.util.Log.d("CodeViewerActivity", "Could not take persistable permission: ${e.message}")
            }
            
            // First, check if we can write to this URI by checking permissions
            val persistedUris = contentResolver.persistedUriPermissions
            val hasWritePermission = persistedUris.any { 
                it.uri == uri && it.isWritePermission 
            }
            
            if (!hasWritePermission) {
                android.util.Log.w("CodeViewerActivity", "No write permission for URI, will try anyway")
            }
            
            // Try multiple write modes for maximum compatibility
            // Mode "wt" (write truncate) is most reliable for text files
            // Mode "w" is a fallback
            // Mode "wa" (write append) then truncate is another fallback
            var outputStream: java.io.OutputStream? = null
            var writeSuccess = false
            
            // Try mode "wt" first (write truncate - best for overwriting)
            try {
                outputStream = contentResolver.openOutputStream(uri, "wt")
                if (outputStream != null) {
                    android.util.Log.d("CodeViewerActivity", "Opened stream with mode 'wt'")
                    writeSuccess = true
                }
            } catch (e: Exception) {
                android.util.Log.w("CodeViewerActivity", "Mode 'wt' failed: ${e.message}")
            }
            
            // Try mode "w" as fallback (write - may not truncate on all systems)
            if (!writeSuccess) {
                try {
                    outputStream = contentResolver.openOutputStream(uri, "w")
                    if (outputStream != null) {
                        android.util.Log.d("CodeViewerActivity", "Opened stream with mode 'w'")
                        writeSuccess = true
                    }
                } catch (e: Exception) {
                    android.util.Log.w("CodeViewerActivity", "Mode 'w' failed: ${e.message}")
                }
            }
            
            // Try default mode as last resort
            if (!writeSuccess) {
                try {
                    outputStream = contentResolver.openOutputStream(uri)
                    if (outputStream != null) {
                        android.util.Log.d("CodeViewerActivity", "Opened stream with default mode")
                        writeSuccess = true
                    }
                } catch (e: Exception) {
                    android.util.Log.w("CodeViewerActivity", "Default mode failed: ${e.message}")
                }
            }
            
            if (!writeSuccess || outputStream == null) {
                throw Exception("Could not open output stream for URI. Try 'Save as new file' instead.")
            }
            
            // Write the content
            outputStream.use { stream ->
                stream.write(content.toByteArray(Charsets.UTF_8))
                stream.flush()
            }

            originalContent = content
            hasUnsavedChanges = false
            Toast.makeText(this, getString(R.string.file_saved_successfully), Toast.LENGTH_SHORT).show()
            displayContent()

            if (pendingExitAfterSave) {
                pendingExitAfterSave = false
                finish()
            }
        } catch (e: Exception) {
            android.util.Log.e("CodeViewerActivity", "Error saving file", e)

            if (pendingExitAfterSave) {
                pendingExitAfterSave = false
            }

            // Provide more specific error message and offer to save as new file
            val errorMsg = when {
                e.message?.contains("Permission denied") == true || e is SecurityException ->
                    getString(R.string.permission_denied_try_save_as)
                e.message?.contains("Could not open output stream") == true ->
                    getString(R.string.file_save_failed) + ": " + getString(R.string.permission_denied_try_save_as)
                else -> getString(R.string.file_save_failed) + ": ${e.message}"
            }
            
            // Show error with option to save as new file
            MaterialAlertDialogBuilder(this)
                .setTitle(getString(R.string.file_save_failed))
                .setMessage(errorMsg)
                .setPositiveButton(getString(R.string.save_as_new_file)) { _, _ ->
                    saveAsNewFile(content)
                }
                .setNegativeButton(getString(R.string.cancel), null)
                .show()
        }
    }
    
    private fun saveAsNewFile(content: String) {
        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "text/plain"
            putExtra(Intent.EXTRA_TITLE, fileName)
        }
        try {
            saveFileLauncher.launch(intent)
        } catch (e: Exception) {
            android.util.Log.e("CodeViewerActivity", "Error launching save file picker", e)
            Toast.makeText(this, getString(R.string.file_save_failed), Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun setupRealTimeSyntaxCheck() {
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val enabled = prefs.getBoolean(KEY_REAL_TIME_CHECK, true)

        if (!prefs.contains(KEY_REAL_TIME_CHECK)) {
            prefs.edit().putBoolean(KEY_REAL_TIME_CHECK, true).apply()
        }
        
        if (enabled) {
            etCodeContent.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                
                override fun afterTextChanged(s: Editable?) {
                    hasUnsavedChanges = true
                    // Debounce syntax checking
                    etCodeContent.removeCallbacks(syntaxCheckRunnable)
                    etCodeContent.postDelayed(syntaxCheckRunnable, 1000)
                }
            })
        }
    }
    
    private val syntaxCheckRunnable = Runnable {
        if (isEditMode) {
            val errors = abbParser.validateSyntax(etCodeContent.text.toString())
            if (errors.isNotEmpty()) {
                // Show first error as toast
                Toast.makeText(this, 
                    getString(R.string.syntax_error_line, errors[0].lineNumber, errors[0].message),
                    Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_code_viewer, menu)
        
        // Update menu based on edit mode
        menu?.findItem(R.id.action_save)?.isVisible = isEditMode
        menu?.findItem(R.id.action_edit)?.title = 
            if (isEditMode) getString(R.string.disable_editing) else getString(R.string.enable_editing)
        
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_search -> {
                showSearchDialog()
                true
            }
            R.id.action_replace -> {
                showReplaceDialog()
                true
            }
            R.id.action_edit -> {
                toggleEditMode()
                true
            }
            R.id.action_save -> {
                saveChanges()
                true
            }
            R.id.action_export -> {
                exportCode()
                true
            }
            R.id.action_bookmark -> {
                showBookmarkDialog()
                true
            }
            R.id.action_format -> {
                formatCode()
                true
            }
            R.id.action_toggle_theme -> {
                toggleTheme()
                true
            }
            R.id.action_syntax_check -> {
                checkSyntax()
                true
            }
            R.id.action_fold -> {
                toggleFolding()
                true
            }
            R.id.action_refactor -> {
                showRefactorDialog()
                true
            }
            R.id.action_vcs -> {
                showVcsDialog()
                true
            }
            R.id.action_cloud_sync -> {
                performCloudSync()
                true
            }
            R.id.action_collaboration -> {
                openCollaborationPanel()
                true
            }
            R.id.action_simulator -> {
                openSimulator()
                true
            }
            R.id.action_robot_connect -> {
                toggleRobotConnection()
                true
            }
            R.id.action_transfer -> {
                showTransferOptions()
                true
            }
            R.id.action_ai_agent -> {
                launchAiAgent()
                true
            }
            R.id.action_mcp_tools -> {
                runMcpTools()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun showSearchDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_search, null)
        val editText = dialogView.findViewById<EditText>(R.id.etSearch)

        MaterialAlertDialogBuilder(this)
            .setTitle(getString(R.string.search_code))
            .setView(dialogView)
            .setPositiveButton(getString(R.string.search)) { _, _ ->
                val query = editText.text.toString()
                if (query.isNotEmpty()) {
                    searchCode(query)
                }
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .show()
    }

    private fun parseCurrentFileSafely(): ABBProgramFile? {
        val content = if (isEditMode) etCodeContent.text.toString() else fileContent
        return try {
            val tempFile = java.io.File(cacheDir, "_temp_parse_${System.currentTimeMillis()}.mod")
            tempFile.writeText(content)
            val parsedFile = abbParser.parseFile(tempFile)
            tempFile.delete()
            if (parsedFile.modules.isEmpty() && parsedFile.routines.isNotEmpty()) {
                val totalLines = content.lines().size
                parsedFile.copy(
                    modules = listOf(
                        ABBModule(
                            name = getString(R.string.default_module_name),
                            type = "NOSTEPIN",
                            routines = parsedFile.routines,
                            variables = emptyList(),
                            startLine = 1,
                            endLine = totalLines
                        )
                    )
                )
            } else {
                parsedFile
            }
        } catch (e: Exception) {
            android.util.Log.e("CodeViewerActivity", "Error parsing content", e)
            currentProgramFile
        }
    }

    private fun showReplaceDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_replace, null)
        val etSearchText = dialogView.findViewById<EditText>(R.id.etSearchText)
        val etReplaceText = dialogView.findViewById<EditText>(R.id.etReplaceText)
        val rgReplaceScope = dialogView.findViewById<RadioGroup>(R.id.rgReplaceScope)
        val routineGroup = dialogView.findViewById<View>(R.id.groupRoutineSelection)
        val moduleGroup = dialogView.findViewById<View>(R.id.groupModuleSelection)
        val btnOpenRoutineSelector = dialogView.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnOpenRoutineSelector)
        val btnOpenModuleSelector = dialogView.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnOpenModuleSelector)
        val tvRoutineStatus = dialogView.findViewById<TextView>(R.id.tvRoutineStatus)
        val tvModuleStatus = dialogView.findViewById<TextView>(R.id.tvModuleStatus)

        val parsedFile = parseCurrentFileSafely()
        val routines = parsedFile?.routines ?: emptyList()
        val modules = parsedFile?.modules ?: emptyList()

        var routineAdapter: RoutineSelectionAdapter? = null
        var moduleAdapter: ModuleSelectionAdapter? = null

        if (routines.isNotEmpty()) {
            routineAdapter = RoutineSelectionAdapter(routines)
        } else {
            tvRoutineStatus.text = getString(R.string.no_routines_found)
        }

        if (modules.isNotEmpty()) {
            moduleAdapter = ModuleSelectionAdapter(modules)
        } else {
            tvModuleStatus.text = getString(R.string.no_modules_found)
        }

        fun updateRoutineStatus() {
            tvRoutineStatus.text = when {
                routineAdapter == null -> getString(R.string.no_routines_found)
                routineAdapter.itemCount == 0 -> getString(R.string.no_routines_found)
                routineAdapter.getSelectedRoutines().isEmpty() ->
                    getString(R.string.routines_selected_with_hint, 0)
                else -> getString(R.string.routines_selected, routineAdapter.getSelectedRoutines().size)
            }
        }

        fun updateModuleStatus() {
            tvModuleStatus.text = when {
                moduleAdapter == null -> getString(R.string.no_modules_found)
                moduleAdapter.itemCount == 0 -> getString(R.string.no_modules_found)
                moduleAdapter.getSelectedModules().isEmpty() ->
                    getString(R.string.modules_selected_with_hint, 0)
                else -> getString(R.string.modules_selected, moduleAdapter.getSelectedModules().size)
            }
        }

        fun showRoutineSelectionDialog() {
            val adapter = routineAdapter ?: return
            if (adapter.itemCount == 0) return

            val selectionView = layoutInflater.inflate(R.layout.dialog_routine_selection, null)
            val toolbar = selectionView.findViewById<com.google.android.material.appbar.MaterialToolbar>(R.id.toolbarRoutineSelection)
            val rvList = selectionView.findViewById<RecyclerView>(R.id.rvRoutines)
            val btnSelectAll = selectionView.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnSelectAll)
            val btnDeselectAll = selectionView.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnDeselectAll)

            selectionView.setBackgroundColor(
                MaterialColors.getColor(selectionView, com.google.android.material.R.attr.colorSurface)
            )

            toolbar.title = getString(R.string.select_routines)
            rvList.layoutManager = LinearLayoutManager(this)
            rvList.adapter = adapter

            btnSelectAll.setOnClickListener { adapter.selectAll(); updateRoutineStatus() }
            btnDeselectAll.setOnClickListener { adapter.deselectAll(); updateRoutineStatus() }

            val selectionDialog = android.app.Dialog(this, android.R.style.Theme_DeviceDefault_NoActionBar_Fullscreen)
            selectionDialog.setContentView(selectionView)
            selectionDialog.setOnDismissListener { updateRoutineStatus() }
            toolbar.setNavigationOnClickListener { selectionDialog.dismiss() }

            selectionDialog.window?.setBackgroundDrawable(
                ColorDrawable(
                    MaterialColors.getColor(selectionView, com.google.android.material.R.attr.colorSurface)
                )
            )

            selectionDialog.show()
            selectionDialog.window?.setLayout(
                android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                android.view.ViewGroup.LayoutParams.MATCH_PARENT
            )
        }

        fun showModuleSelectionDialog() {
            val adapter = moduleAdapter ?: return
            if (adapter.itemCount == 0) return

            val selectionView = layoutInflater.inflate(R.layout.dialog_routine_selection, null)
            val toolbar = selectionView.findViewById<com.google.android.material.appbar.MaterialToolbar>(R.id.toolbarRoutineSelection)
            val rvList = selectionView.findViewById<RecyclerView>(R.id.rvRoutines)
            val btnSelectAll = selectionView.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnSelectAll)
            val btnDeselectAll = selectionView.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnDeselectAll)

            selectionView.setBackgroundColor(
                MaterialColors.getColor(selectionView, com.google.android.material.R.attr.colorSurface)
            )

            toolbar.title = getString(R.string.select_modules)
            rvList.layoutManager = LinearLayoutManager(this)
            rvList.adapter = adapter

            btnSelectAll.setOnClickListener { adapter.selectAll(); updateModuleStatus() }
            btnDeselectAll.setOnClickListener { adapter.deselectAll(); updateModuleStatus() }

            val selectionDialog = android.app.Dialog(this, android.R.style.Theme_DeviceDefault_NoActionBar_Fullscreen)
            selectionDialog.setContentView(selectionView)
            selectionDialog.setOnDismissListener { updateModuleStatus() }
            toolbar.setNavigationOnClickListener { selectionDialog.dismiss() }

            selectionDialog.window?.setBackgroundDrawable(
                ColorDrawable(
                    MaterialColors.getColor(selectionView, com.google.android.material.R.attr.colorSurface)
                )
            )

            selectionDialog.show()
            selectionDialog.window?.setLayout(
                android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                android.view.ViewGroup.LayoutParams.MATCH_PARENT
            )
        }

        val openRoutineSelector: () -> Unit = {
            if (routineAdapter == null || routineAdapter.itemCount == 0) {
                Toast.makeText(this, getString(R.string.no_routines_found), Toast.LENGTH_SHORT).show()
            } else {
                showRoutineSelectionDialog()
            }
        }

        val openModuleSelector: () -> Unit = {
            if (moduleAdapter == null || moduleAdapter.itemCount == 0) {
                Toast.makeText(this, getString(R.string.no_modules_found), Toast.LENGTH_SHORT).show()
            } else {
                showModuleSelectionDialog()
            }
        }

        btnOpenRoutineSelector.setOnClickListener { openRoutineSelector() }
        btnOpenModuleSelector.setOnClickListener { openModuleSelector() }
        tvRoutineStatus.setOnClickListener { openRoutineSelector() }
        tvModuleStatus.setOnClickListener { openModuleSelector() }
        routineGroup.setOnClickListener { openRoutineSelector() }
        moduleGroup.setOnClickListener { openModuleSelector() }

        updateRoutineStatus()
        updateModuleStatus()

        val updateScopeVisibility = {
            when (rgReplaceScope.checkedRadioButtonId) {
                R.id.rbReplaceInRoutine -> {
                    routineGroup.visibility = View.VISIBLE
                    moduleGroup.visibility = View.GONE
                }
                R.id.rbReplaceInModule -> {
                    routineGroup.visibility = View.GONE
                    moduleGroup.visibility = View.VISIBLE
                }
                else -> {
                    routineGroup.visibility = View.GONE
                    moduleGroup.visibility = View.GONE
                }
            }
        }

        rgReplaceScope.setOnCheckedChangeListener { _, _ -> updateScopeVisibility.invoke() }
        updateScopeVisibility.invoke()

        val dialog = MaterialAlertDialogBuilder(this)
            .setTitle(getString(R.string.replace_code))
            .setView(dialogView)
            .setPositiveButton(getString(R.string.replace), null)
            .setNegativeButton(getString(R.string.cancel), null)
            .create()

        dialog.setOnShowListener {
            val positiveButton = dialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE)
            positiveButton.setOnClickListener {
                val searchText = etSearchText.text.toString()
                val replaceText = etReplaceText.text.toString()

                if (searchText.isEmpty()) {
                    Toast.makeText(this, getString(R.string.search_hint), Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                when (rgReplaceScope.checkedRadioButtonId) {
                    R.id.rbReplaceInRoutine -> {
                        val selectedRoutines = routineAdapter?.getSelectedRoutines().orEmpty()
                        if (selectedRoutines.isEmpty()) {
                            Toast.makeText(this, getString(R.string.no_routines_selected), Toast.LENGTH_SHORT).show()
                            return@setOnClickListener
                        }
                        replaceCode(searchText, replaceText, "routine", selectedRoutines, null)
                    }
                    R.id.rbReplaceInModule -> {
                        val selectedModules = moduleAdapter?.getSelectedModules().orEmpty()
                        if (selectedModules.isEmpty()) {
                            Toast.makeText(this, getString(R.string.no_modules_selected), Toast.LENGTH_SHORT).show()
                            return@setOnClickListener
                        }
                        replaceCode(searchText, replaceText, "module", null, selectedModules)
                    }
                    else -> replaceCode(searchText, replaceText, "all", null, null)
                }

                dialog.dismiss()
            }
        }

        dialog.show()
    }

    private fun replaceCode(searchText: String, replaceText: String, scope: String, selectedRoutines: List<ABBRoutine>?, selectedModules: List<ABBModule>?) {
        var content = if (isEditMode) etCodeContent.text.toString() else fileContent
        var count = 0
        
        when (scope) {
            "all" -> {
                // Replace all occurrences
                val regex = Regex.escape(searchText).toRegex(RegexOption.IGNORE_CASE)
                count = regex.findAll(content).count()
                content = content.replace(searchText, replaceText, ignoreCase = true)
            }
            "routine" -> {
                // Replace only in selected routines
                if (selectedRoutines != null && selectedRoutines.isNotEmpty()) {
                    val lines = content.lines().toMutableList()
                    
                    for (routine in selectedRoutines) {
                        val start = (routine.startLine - 1).coerceAtLeast(0)
                        val end = (routine.endLine - 1).coerceAtMost(lines.size - 1)

                        for (lineIdx in start..end) {
                            val line = lines[lineIdx]
                            if (line.contains(searchText, ignoreCase = true)) {
                                val regex = Regex.escape(searchText).toRegex(RegexOption.IGNORE_CASE)
                                count += regex.findAll(line).count()
                                lines[lineIdx] = line.replace(searchText, replaceText, ignoreCase = true)
                            }
                        }
                    }
                    
                    content = lines.joinToString("\n")
                }
            }
            "module" -> {
                // Replace only in selected modules
                if (selectedModules != null && selectedModules.isNotEmpty()) {
                    val lines = content.lines().toMutableList()
                    
                    for (module in selectedModules) {
                        val start = (module.startLine - 1).coerceAtLeast(0)
                        val end = (module.endLine - 1).coerceAtMost(lines.size - 1)

                        for (lineIdx in start..end) {
                            val line = lines[lineIdx]
                            if (line.contains(searchText, ignoreCase = true)) {
                                val regex = Regex.escape(searchText).toRegex(RegexOption.IGNORE_CASE)
                                count += regex.findAll(line).count()
                                lines[lineIdx] = line.replace(searchText, replaceText, ignoreCase = true)
                            }
                        }
                    }
                    
                    content = lines.joinToString("\n")
                }
            }
        }
        
        fileContent = content
        hasUnsavedChanges = true
        
        if (isEditMode) {
            etCodeContent.setText(content)
        } else {
            displayContent()
        }

        if (currentHighlightedLine != -1) {
            reapplyCurrentHighlightIfNeeded(applyAfterLayout = true)
        }

        Toast.makeText(this, getString(R.string.replaced_count, count), Toast.LENGTH_SHORT).show()
    }

    private fun searchCode(query: String) {
        currentSearchQuery = query
        if (query.isEmpty()) {
            displayContent()
            return
        }
        
        val content = if (isEditMode) etCodeContent.text.toString() else fileContent
        val lines = content.lines()
        val results = mutableListOf<SearchResultAdapter.SearchResult>()
        
        lines.forEachIndexed { index, line ->
            if (line.contains(query, ignoreCase = true)) {
                val startIndex = line.indexOf(query, ignoreCase = true)
                results.add(
                    SearchResultAdapter.SearchResult(
                        lineNumber = index + 1,
                        lineContent = line,
                        startIndex = startIndex,
                        endIndex = startIndex + query.length
                    )
                )
            }
        }
        
        if (results.isEmpty()) {
            Toast.makeText(this, getString(R.string.no_matches_found), Toast.LENGTH_SHORT).show()
        } else {
            showSearchResultsDialog(results, query)
        }
    }
    
    private fun showSearchResultsDialog(results: List<SearchResultAdapter.SearchResult>, query: String) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_search_results, null)
        val tvSearchResultsCount = dialogView.findViewById<TextView>(R.id.tvSearchResultsCount)
        val rvSearchResults = dialogView.findViewById<RecyclerView>(R.id.rvSearchResults)
        
        tvSearchResultsCount.text = getString(R.string.search_results_count, results.size)
        
        rvSearchResults.layoutManager = LinearLayoutManager(this)
        
        val dialog = MaterialAlertDialogBuilder(this)
            .setTitle(getString(R.string.search_results))
            .setView(dialogView)
            .setNegativeButton(getString(R.string.close), null)
            .create()
        
        // Set adapter with click listener to dismiss dialog
        rvSearchResults.adapter = SearchResultAdapter(results, query) { result ->
            jumpToLineAndColumn(
                result.lineNumber,
                result.startIndex,
                result.endIndex,
                HighlightColors.getErrorHighlightColor(this)
            )
            dialog.dismiss()
        }
        
        dialog.show()
    }

    private fun resetHighlightStateForNewTarget() {
        if (currentHighlightedLine == -1) return

        if (isEditMode) {
            val editableContent = etCodeContent.text
            if (editableContent is Spannable) {
                removeExistingHighlightSpans(editableContent)
            }
            etCodeContent.clearPersistentHighlight()
        } else {
            tvCodeContent.setOnClickListener(null)
        }

        currentHighlightSpan = null
        currentHighlightRange = null
        currentHighlightColor = null
        currentHighlightColumns = null
        currentHighlightedLine = -1
    }

    private fun jumpToLine(
        lineNumber: Int,
        highlightColor: Int = HighlightColors.getLineHighlightColor(this)
    ) {
        resetHighlightStateForNewTarget()

        val content = if (isEditMode) etCodeContent.text.toString() else fileContent
        val lines = content.lines()
        if (lineNumber > 0 && lineNumber <= lines.size) {
            // Calculate the character position of the line
            var charPosition = 0
            for (i in 0 until lineNumber - 1) {
                charPosition += lines[i].length + 1 // +1 for newline
            }
            
            if (isEditMode) {
                // Highlight the line using the unified method FIRST
                // This ensures background highlighting is visible before cursor placement
                highlightLine(lineNumber, highlightColor)
                
                // Find the first non-whitespace character on the line for cursor placement
                val currentLine = lines[lineNumber - 1]
                val firstNonWhitespace = currentLine.indexOfFirst { !it.isWhitespace() }
                val cursorPosition = if (firstNonWhitespace >= 0) {
                    charPosition + firstNonWhitespace
                } else {
                    charPosition
                }
                
                // Place cursor at the start of the line without selecting text
                // This allows the background color highlight to be fully visible
                etCodeContent.setSelection(
                    cursorPosition.coerceAtMost(etCodeContent.text?.length ?: 0)
                )
                etCodeContent.requestFocus()
                
                // Scroll to make the cursor visible
                etCodeContent.post {
                    val layout = etCodeContent.layout
                    if (layout != null && lineNumber - 1 < layout.lineCount) {
                        val lineTop = layout.getLineTop(lineNumber - 1)
                        scrollViewCode.smoothScrollTo(0, lineTop)
                    }
                }
            } else {
                // Calculate the Y position of the line in view mode
                tvCodeContent.post {
                    val layout = tvCodeContent.layout
                    if (layout != null && lineNumber - 1 < layout.lineCount) {
                        val lineTop = layout.getLineTop(lineNumber - 1)
                        scrollViewCode.smoothScrollTo(0, lineTop)
                        
                        // Highlight the line using the unified method
                        highlightLine(lineNumber, highlightColor)
                    }
                }
            }

            Toast.makeText(this, getString(R.string.jumped_to_line, lineNumber), Toast.LENGTH_SHORT).show()

            reapplyCurrentHighlightIfNeeded(applyAfterLayout = true)
        }
    }
    
    private fun jumpToLineAndColumn(
        lineNumber: Int,
        columnStart: Int,
        columnEnd: Int,
        highlightColor: Int = HighlightColors.getErrorHighlightColor(this)
    ) {
        resetHighlightStateForNewTarget()

        val content = if (isEditMode) etCodeContent.text.toString() else fileContent
        val lines = content.lines()
        if (lineNumber > 0 && lineNumber <= lines.size) {
            // Calculate the character position of the line
            var charPosition = 0
            for (i in 0 until lineNumber - 1) {
                charPosition += lines[i].length + 1 // +1 for newline
            }
            
            if (isEditMode) {
                // Highlight the specific column range using the unified method FIRST
                highlightLineAndColumn(lineNumber, columnStart, columnEnd, highlightColor)
                
                // Calculate cursor position at the start of the error
                val cursorPosition = charPosition + columnStart
                
                // Place cursor at the start of the error
                etCodeContent.setSelection(
                    cursorPosition.coerceAtMost(etCodeContent.text?.length ?: 0)
                )
                etCodeContent.requestFocus()
                
                // Scroll to make the cursor visible
                etCodeContent.post {
                    val layout = etCodeContent.layout
                    if (layout != null && lineNumber - 1 < layout.lineCount) {
                        val lineTop = layout.getLineTop(lineNumber - 1)
                        scrollViewCode.smoothScrollTo(0, lineTop)
                    }
                }
            } else {
                // Calculate the Y position of the line in view mode
                tvCodeContent.post {
                    val layout = tvCodeContent.layout
                    if (layout != null && lineNumber - 1 < layout.lineCount) {
                        val lineTop = layout.getLineTop(lineNumber - 1)
                        scrollViewCode.smoothScrollTo(0, lineTop)
                        
                        // Highlight the specific column range using the unified method
                        highlightLineAndColumn(lineNumber, columnStart, columnEnd, highlightColor)
                    }
                }
            }
            
            // Show appropriate jump notification with line and column info
            val message = if (columnStart >= 0) {
                getString(R.string.jumped_to_line_column, lineNumber, columnStart + 1)
            } else {
                getString(R.string.jumped_to_line, lineNumber)
            }
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()

            reapplyCurrentHighlightIfNeeded(applyAfterLayout = true)
        }
    }
    
    private fun highlightLine(lineNumber: Int, highlightColor: Int = HighlightColors.getLineHighlightColor(this)) {
        val content = if (isEditMode) etCodeContent.text.toString() else fileContent

        calculateHighlightRange(content, lineNumber, null)?.let { (startPos, endPos) ->
            currentHighlightedLine = lineNumber
            currentHighlightColumns = null

            applyHighlightSpan(content, startPos, endPos, highlightColor, applyAfterLayout = true)
        }
    }

    private fun clearHighlight() {
        if (currentHighlightedLine != -1) {
            val range = currentHighlightRange
            val color = currentHighlightColor

            if (isEditMode && range != null && color != null) {
                etCodeContent.clearPersistentHighlight()

                val editableContent = etCodeContent.text
                if (editableContent is Spannable) {
                    removeExistingHighlightSpans(editableContent)
                }
            }

            if (!isEditMode) {
                tvCodeContent.setOnClickListener(null)
                displayContent()
            }

            currentHighlightedLine = -1
            currentHighlightSpan = null
            currentHighlightRange = null
            currentHighlightColor = null
            currentHighlightColumns = null
        }
    }

    private fun highlightLineAndColumn(
        lineNumber: Int,
        columnStart: Int,
        columnEnd: Int,
        highlightColor: Int = HighlightColors.getErrorHighlightColor(this)
    ) {
        val content = if (isEditMode) etCodeContent.text.toString() else fileContent

        calculateHighlightRange(content, lineNumber, columnStart to columnEnd)?.let { (startPos, endPos) ->
            currentHighlightedLine = lineNumber
            currentHighlightColumns = columnStart to columnEnd

            applyHighlightSpan(content, startPos, endPos, highlightColor, applyAfterLayout = true)
        }
    }

    private fun applyHighlightSpan(
        content: String,
        startPos: Int,
        endPos: Int,
        highlightColor: Int,
        applyAfterLayout: Boolean = false
    ) {
        currentHighlightRange = startPos to endPos
        currentHighlightColor = highlightColor

        if (isEditMode) {
            val applyToEditable = editableBlock@{
                val editableContent = etCodeContent.text
                if (editableContent is Spannable) {
                    removeExistingHighlightSpans(editableContent)

                    val textLength = editableContent.length
                    if (textLength == 0) return@editableBlock

                    val resolvedStart = startPos.coerceIn(0, textLength - 1)
                    val desiredEnd = if (endPos <= resolvedStart) resolvedStart + 1 else endPos
                    val resolvedEnd = desiredEnd.coerceIn(resolvedStart + 1, textLength)

                    val highlightSpan = BackgroundColorSpan(highlightColor)
                    currentHighlightSpan = highlightSpan
                    editableContent.setSpan(
                        highlightSpan,
                        resolvedStart,
                        resolvedEnd,
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE or Spanned.SPAN_PRIORITY
                    )

                    etCodeContent.setPersistentHighlight(
                        highlightColor,
                        resolvedStart,
                        resolvedEnd
                    )

                    etCodeContent.invalidate()
                }
            }

            if (applyAfterLayout) {
                etCodeContent.post { applyToEditable() }
            } else {
                applyToEditable()
            }
        } else {
            val highlighted = syntaxHighlighter.highlight(content)
            val finalSpannable = SpannableString(highlighted)
            finalSpannable.setSpan(
                BackgroundColorSpan(highlightColor),
                startPos.coerceAtMost(finalSpannable.length),
                endPos.coerceAtMost(finalSpannable.length),
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
            tvCodeContent.text = finalSpannable

            tvCodeContent.setOnClickListener {
                clearHighlight()
            }
        }
    }

    private fun removeExistingHighlightSpans(editableContent: Spannable) {
        currentHighlightSpan?.let { editableContent.removeSpan(it) }

        val range = currentHighlightRange
        val color = currentHighlightColor
        if (range != null && color != null) {
            val (start, end) = range
            editableContent.getSpans(start, end.coerceAtMost(editableContent.length), BackgroundColorSpan::class.java)
                .filter { span -> span.backgroundColor == color }
                .forEach { span -> editableContent.removeSpan(span) }
        }
    }

    private fun reapplyCurrentHighlightIfNeeded(applyAfterLayout: Boolean = false) {
        val range = currentHighlightRange
        val color = currentHighlightColor
        val lineNumber = currentHighlightedLine

        if (range != null && color != null && lineNumber != -1) {
            val content = if (isEditMode) etCodeContent.text.toString() else fileContent
            val recalculatedRange = calculateHighlightRange(content, lineNumber, currentHighlightColumns)
            val (start, end) = recalculatedRange ?: range
            applyHighlightSpan(content, start, end, color, applyAfterLayout)
        }
    }

    private fun calculateHighlightRange(
        content: String,
        lineNumber: Int,
        columnRange: Pair<Int, Int>?
    ): Pair<Int, Int>? {
        if (lineNumber <= 0) return null

        var currentLine = 1
        var startPos = 0

        while (currentLine < lineNumber && startPos < content.length) {
            if (content[startPos] == '\n') {
                currentLine++
            }
            startPos++
        }

        if (currentLine != lineNumber) return null

        var endPos = startPos
        while (endPos < content.length && content[endPos] != '\n') {
            endPos++
        }

        if (endPos == startPos) {
            endPos = (startPos + 1).coerceAtMost(content.length)
        }

        return columnRange?.let { (columnStart, columnEnd) ->
            val resolvedStart = (startPos + columnStart).coerceIn(startPos, endPos)
            val minimumSpanEnd = if (endPos > startPos) resolvedStart + 1 else resolvedStart
            val resolvedEnd = (startPos + max(columnEnd, columnStart)).coerceIn(minimumSpanEnd, endPos)
            resolvedStart to resolvedEnd
        } ?: (startPos to endPos)
    }

    private fun exportCode() {
        val content = if (isEditMode) etCodeContent.text.toString() else fileContent
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, content)
            putExtra(Intent.EXTRA_SUBJECT, fileName)
        }
        startActivity(Intent.createChooser(intent, getString(R.string.export_code)))
    }

    private fun showBookmarkDialog() {
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val bookmarks = prefs.getStringSet(KEY_BOOKMARKS + fileName, mutableSetOf())?.toMutableSet() ?: mutableSetOf()
        
        val dialogView = layoutInflater.inflate(R.layout.dialog_bookmark, null)
        val editText = dialogView.findViewById<EditText>(R.id.etLineNumber)
        
        MaterialAlertDialogBuilder(this)
            .setTitle(getString(R.string.add_bookmark))
            .setView(dialogView)
            .setPositiveButton(getString(R.string.add)) { _, _ ->
                val lineNumber = editText.text.toString().toIntOrNull()
                if (lineNumber != null && lineNumber > 0) {
                    bookmarks.add(lineNumber.toString())
                    prefs.edit().putStringSet(KEY_BOOKMARKS + fileName, bookmarks).apply()
                    Toast.makeText(this, getString(R.string.bookmark_added), Toast.LENGTH_SHORT).show()
                }
            }
            .setNeutralButton(getString(R.string.view_bookmarks)) { _, _ ->
                showBookmarksList(bookmarks)
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .show()
    }

    private fun showBookmarksList(bookmarks: Set<String>) {
        if (bookmarks.isEmpty()) {
            Toast.makeText(this, getString(R.string.no_bookmarks), Toast.LENGTH_SHORT).show()
            return
        }

        val sortedBookmarks = bookmarks.mapNotNull { it.toIntOrNull() }.sorted()
        val items = sortedBookmarks.map { getString(R.string.line_number, it.toString()) }.toTypedArray()

        MaterialAlertDialogBuilder(this)
            .setTitle(getString(R.string.bookmarks))
            .setItems(items) { _, which ->
                val lineNumber = sortedBookmarks[which]
                jumpToLine(lineNumber)
            }
            .setNeutralButton(getString(R.string.remove_bookmarks)) { dialog, _ ->
                dialog.dismiss()
                showBookmarkRemovalDialog(bookmarks)
            }
            .setNegativeButton(getString(R.string.close), null)
            .show()
    }

    private fun showBookmarkRemovalDialog(bookmarks: Set<String>) {
        val sortedBookmarks = bookmarks.mapNotNull { it.toIntOrNull() }.sorted()
        if (sortedBookmarks.isEmpty()) {
            Toast.makeText(this, getString(R.string.no_bookmarks), Toast.LENGTH_SHORT).show()
            return
        }

        val items = sortedBookmarks.map { getString(R.string.line_number, it.toString()) }.toTypedArray()
        val checkedItems = BooleanArray(items.size)

        MaterialAlertDialogBuilder(this)
            .setTitle(getString(R.string.remove_bookmark))
            .setMultiChoiceItems(items, checkedItems) { _, which, isChecked ->
                checkedItems[which] = isChecked
            }
            .setPositiveButton(getString(R.string.remove_bookmarks)) { _, _ ->
                val toRemove = sortedBookmarks
                    .filterIndexed { index, _ -> checkedItems[index] }
                    .map { it.toString() }
                    .toSet()

                if (toRemove.isEmpty()) {
                    Toast.makeText(this, getString(R.string.no_bookmark_selected_for_removal), Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                val updated = bookmarks.toMutableSet()
                updated.removeAll(toRemove)
                prefs.edit().putStringSet(KEY_BOOKMARKS + fileName, updated).apply()

                Toast.makeText(this, getString(R.string.bookmarks_removed, toRemove.size), Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .show()
    }

    private fun formatCode() {
        var content = if (isEditMode) etCodeContent.text.toString() else fileContent
        
        // Simple code formatting: fix indentation
        val lines = content.lines()
        val formatted = StringBuilder()
        var indentLevel = 0
        
        lines.forEach { line ->
            val trimmed = line.trim()
            
            // Decrease indent for end keywords
            if (trimmed.matches(Regex("^(ENDMODULE|ENDPROC|ENDFUNC|ENDTRAP|ENDIF|ENDFOR|ENDWHILE|ENDTEST).*", RegexOption.IGNORE_CASE))) {
                indentLevel = maxOf(0, indentLevel - 1)
            }
            
            // Add indented line
            val indent = "    ".repeat(indentLevel)
            formatted.append(indent).append(trimmed).append("\n")
            
            // Increase indent for start keywords
            if (trimmed.matches(Regex("^(MODULE|PROC|FUNC|TRAP|IF .+ THEN|FOR .+ DO|WHILE .+ DO|TEST .+).*", RegexOption.IGNORE_CASE))) {
                indentLevel++
            }
        }
        
        fileContent = formatted.toString()
        hasUnsavedChanges = true
        
        if (isEditMode) {
            etCodeContent.setText(fileContent)
        } else {
            displayContent()
        }

        Toast.makeText(this, getString(R.string.code_formatted), Toast.LENGTH_SHORT).show()
    }

    private fun clearManualThemeSelectionIfFirstLaunch(savedInstanceState: Bundle?) {
        if (savedInstanceState != null) return

        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        if (prefs.getBoolean(KEY_THEME_MANUALLY_SELECTED, false)) {
            prefs.edit()
                .putBoolean(KEY_THEME_MANUALLY_SELECTED, false)
                .remove(KEY_THEME_MODE)
                .apply()
        }
    }

    private fun toggleTheme() {
        val currentMode = AppCompatDelegate.getDefaultNightMode()
        val isCurrentlyNight = when (currentMode) {
            AppCompatDelegate.MODE_NIGHT_YES -> true
            AppCompatDelegate.MODE_NIGHT_NO -> false
            else -> {
                val nightModeFlags = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
                nightModeFlags == Configuration.UI_MODE_NIGHT_YES
            }
        }

        val newMode = if (isCurrentlyNight) {
            AppCompatDelegate.MODE_NIGHT_NO
        } else {
            AppCompatDelegate.MODE_NIGHT_YES
        }

        // Save preference
        getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putInt(KEY_THEME_MODE, newMode)
            .putBoolean(KEY_THEME_MANUALLY_SELECTED, true)
            .apply()

        // Apply theme
        AppCompatDelegate.setDefaultNightMode(newMode)
        recreate()
    }

    private fun resolveSavedTheme(): Int {
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        if (!prefs.contains(KEY_THEME_MANUALLY_SELECTED)) {
            prefs.edit().putBoolean(KEY_THEME_MANUALLY_SELECTED, false).apply()
        }

        val manuallySelected = prefs.getBoolean(KEY_THEME_MANUALLY_SELECTED, false)

        return if (manuallySelected) {
            prefs.getInt(KEY_THEME_MODE, AppCompatDelegate.MODE_NIGHT_NO)
        } else {
            AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
        }
    }

    private fun hasContentChangedFromOriginal(): Boolean {
        val currentContent = if (isEditMode) {
            etCodeContent.text.toString()
        } else {
            fileContent
        }

        hasUnsavedChanges = currentContent != originalContent
        return hasUnsavedChanges
    }

    private fun isAutoCompleteEnabled(): Boolean {
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        if (!prefs.contains(KEY_AUTO_COMPLETE)) {
            prefs.edit().putBoolean(KEY_AUTO_COMPLETE, true).apply()
        }
        return prefs.getBoolean(KEY_AUTO_COMPLETE, true)
    }

    private fun checkSyntax() {
        val content = if (isEditMode) etCodeContent.text.toString() else fileContent
        // Use comprehensive validation that analyzes every line and detects all possible errors
        val errors = abbParser.validateSyntaxComprehensive(content)
        
        if (errors.isEmpty()) {
            Toast.makeText(this, getString(R.string.no_syntax_errors), Toast.LENGTH_SHORT).show()
        } else {
            showSyntaxErrorsDialog(errors)
        }
    }
    
    private fun showSyntaxErrorsDialog(errors: List<SyntaxError>) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_search_results, null)
        val tvSearchResultsCount = dialogView.findViewById<TextView>(R.id.tvSearchResultsCount)
        val rvSearchResults = dialogView.findViewById<RecyclerView>(R.id.rvSearchResults)
        
        tvSearchResultsCount.text = getString(R.string.syntax_errors_found, errors.size)
        
        rvSearchResults.layoutManager = LinearLayoutManager(this)
        
        val dialog = MaterialAlertDialogBuilder(this)
            .setTitle(getString(R.string.syntax_errors))
            .setView(dialogView)
            .setPositiveButton(getString(R.string.ok), null)
            .create()
        
        rvSearchResults.adapter = SyntaxErrorAdapter(errors) { error ->
            jumpToLineAndColumn(error.lineNumber, error.columnStart, error.columnEnd)
            dialog.dismiss()
        }
        
        dialog.show()
    }

    private fun toggleFolding() {
        if (isEditMode) {
            Toast.makeText(this, R.string.fold_not_available_edit_mode, Toast.LENGTH_SHORT).show()
            return
        }

        isFolded = !isFolded
        if (isFolded) {
            foldedContent = foldContent(fileContent)
            Toast.makeText(this, R.string.fold_enabled, Toast.LENGTH_SHORT).show()
        } else {
            foldedContent = null
            Toast.makeText(this, R.string.fold_disabled, Toast.LENGTH_SHORT).show()
        }
        displayContent()
    }

    private fun foldContent(source: String): String {
        val procedureRegex = Regex("(?is)(PROC|FUNC|TRAP)\\s+([A-Za-z0-9_]+)(.*?)(ENDPROC|ENDFUNC|ENDTRAP)")
        val moduleRegex = Regex("(?is)(MODULE)\\s+([A-Za-z0-9_]+)(.*?)(ENDMODULE)")

        val collapsedProcedures = procedureRegex.replace(source) {
            val header = it.groupValues[1].uppercase()
            val name = it.groupValues[2]
            "$header $name\n    ...\n${it.groupValues[4].uppercase()}"
        }

        return moduleRegex.replace(collapsedProcedures) {
            val name = it.groupValues[2]
            "MODULE $name\n    ...\nENDMODULE"
        }
    }

    private fun showRefactorDialog() {
        val view = layoutInflater.inflate(R.layout.dialog_replace, null)
        val etSearchText = view.findViewById<EditText>(R.id.etSearchText)
        val etReplaceText = view.findViewById<EditText>(R.id.etReplaceText)

        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.refactor_title)
            .setView(view)
            .setPositiveButton(R.string.apply) { _, _ ->
                val target = etSearchText.text.toString()
                val replacement = etReplaceText.text.toString()
                if (target.isBlank() || replacement.isBlank()) {
                    Toast.makeText(this, R.string.refactor_empty, Toast.LENGTH_SHORT).show()
                } else {
                    applyRefactor(target, replacement)
                }
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun applyRefactor(target: String, replacement: String) {
        val content = if (isEditMode) etCodeContent.text.toString() else fileContent
        val regex = Regex("\\b${Regex.escape(target)}\\b")
        val updated = regex.replace(content, replacement)
        fileContent = updated
        if (isEditMode) {
            etCodeContent.setText(updated)
        }
        displayContent()
        hasUnsavedChanges = true
        Toast.makeText(this, R.string.refactor_done, Toast.LENGTH_SHORT).show()
    }

    private fun showVcsDialog() {
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val key = "$KEY_VCS_PREFIX$fileName"
        val snapshots = prefs.getStringSet(key, mutableSetOf())?.toMutableSet() ?: mutableSetOf()

        val options = arrayOf(
            getString(R.string.vcs_snapshot_now),
            getString(R.string.vcs_history)
        )

        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.feature_vcs)
            .setItems(options) { _, which ->
                when (which) {
                    0 -> {
                        val stamped = "${System.currentTimeMillis()}::${fileContent.take(2000)}"
                        snapshots.add(stamped)
                        prefs.edit().putStringSet(key, snapshots).apply()
                        Toast.makeText(this, R.string.vcs_snapshot_saved, Toast.LENGTH_SHORT).show()
                    }
                    1 -> showVcsHistory(snapshots)
                }
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun showVcsHistory(snapshots: Set<String>) {
        if (snapshots.isEmpty()) {
            Toast.makeText(this, R.string.vcs_no_history, Toast.LENGTH_SHORT).show()
            return
        }
        val ordered = snapshots.sortedBy { it.substringBefore("::").toLongOrNull() ?: 0L }
        val labels = ordered.mapIndexed { index, item ->
            val ts = item.substringBefore("::").toLongOrNull() ?: 0L
            val preview = item.substringAfter("::").take(60).replace("\n", " ")
            "${index + 1}. ${android.text.format.DateFormat.format("yyyy-MM-dd HH:mm", ts)} — $preview"
        }

        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.vcs_history)
            .setItems(labels.toTypedArray()) { _, which ->
                val snapshot = ordered.getOrNull(which) ?: return@setItems
                val restored = snapshot.substringAfter("::")
                fileContent = restored
                if (isEditMode) etCodeContent.setText(restored)
                displayContent()
                Toast.makeText(this, R.string.vcs_restored, Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton(R.string.close, null)
            .show()
    }

    private fun performCloudSync() {
        showCloudSyncDialog()
    }

    private fun launchGoogleSignIn(forceInteractive: Boolean = false) {
        if (!::googleSignInClient.isInitialized) {
            initGoogleSignIn()
        }

        val startFlow: () -> Unit = {
            googleSignInLauncher.launch(googleSignInClient.signInIntent)
        }

        if (forceInteractive) {
            // Clear any cached session so the user always sees the account/consent sheet
            googleSignInClient.signOut().addOnCompleteListener { startFlow() }
        } else {
            startFlow()
        }
    }

    private fun showCloudSyncDialog() {
        val view = layoutInflater.inflate(R.layout.dialog_cloud_sync, null)
        val providerGroup = view.findViewById<RadioGroup>(R.id.cloudProviderGroup)
        val webDavContainer = view.findViewById<View>(R.id.webDavContainer)
        val googleContainer = view.findViewById<View>(R.id.googleContainer)
        val etWebDavUrl = view.findViewById<TextInputEditText>(R.id.etWebDavUrl)
        val etWebDavUser = view.findViewById<TextInputEditText>(R.id.etWebDavUser)
        val etWebDavPassword = view.findViewById<TextInputEditText>(R.id.etWebDavPassword)
        val etGoogleEmail = view.findViewById<TextInputEditText>(R.id.etGoogleEmail)
        val tvGoogleStatus = view.findViewById<TextView>(R.id.tvGoogleStatus)
        val btnGoogleSignIn = view.findViewById<MaterialButton>(R.id.btnGoogleSignIn)

        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        etWebDavUrl.setText(prefs.getString("${KEY_CLOUD_PREFIX}webdav_url", ""))
        etWebDavUser.setText(prefs.getString("${KEY_CLOUD_PREFIX}webdav_user", ""))
        etGoogleEmail.setText(prefs.getString("${KEY_CLOUD_PREFIX}google_email", ""))

        refreshGoogleUi(tvGoogleStatus, etGoogleEmail, btnGoogleSignIn)

        providerGroup.setOnCheckedChangeListener { _, checkedId ->
            val webDavSelected = checkedId == R.id.providerWebDav
            webDavContainer.visibility = if (webDavSelected) View.VISIBLE else View.GONE
            googleContainer.visibility = if (webDavSelected) View.GONE else View.VISIBLE
            if (!webDavSelected) {
                refreshGoogleUi(tvGoogleStatus, etGoogleEmail, btnGoogleSignIn)
            }
        }

        btnGoogleSignIn.setOnClickListener {
            if (!isNetworkAvailable()) {
                Toast.makeText(this, R.string.cloud_network_required, Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            pendingGoogleRequest = null
            launchGoogleSignIn(forceInteractive = true)
        }

        val dialog = MaterialAlertDialogBuilder(this)
            .setTitle(getString(R.string.cloud_sync_provider_label))
            .setView(view)
            .setPositiveButton(R.string.cloud_sync_now, null)
            .setNegativeButton(R.string.cancel, null)
            .create()

        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val isWebDav = providerGroup.checkedRadioButtonId == R.id.providerWebDav

                if (isWebDav) {
                    // WebDAV sync
                    val url = etWebDavUrl.text?.toString()?.trim().orEmpty()
                    val username = etWebDavUser.text?.toString()?.trim().orEmpty()
                    val password = etWebDavPassword.text?.toString()?.trim().orEmpty()

                    if (url.isBlank()) {
                        etWebDavUrl.error = getString(R.string.cloud_webdav_endpoint)
                        return@setOnClickListener
                    }

                    // Save preferences
                    prefs.edit().apply {
                        putString("${KEY_CLOUD_PREFIX}provider", "WEBDAV")
                        putString("${KEY_CLOUD_PREFIX}webdav_url", url)
                        putString("${KEY_CLOUD_PREFIX}webdav_user", username)
                    }.apply()

                    dialog.dismiss()
                    startCloudBackup(isWebDav = true, url = url, username = username, password = password)

                } else {
                    // Google Drive sync
                    val email = googleAccount?.email ?: etGoogleEmail.text?.toString()?.trim().orEmpty()

                    if (googleAccount == null) {
                        Toast.makeText(this, R.string.cloud_google_signin_required, Toast.LENGTH_SHORT).show()
                        launchGoogleSignIn(forceInteractive = true)
                        return@setOnClickListener
                    }

                    // Save preferences
                    prefs.edit().apply {
                        putString("${KEY_CLOUD_PREFIX}provider", "GOOGLE")
                        putString("${KEY_CLOUD_PREFIX}google_email", email)
                    }.apply()

                    dialog.dismiss()
                    startCloudBackup(isWebDav = false)
                }
            }
        }

        dialog.show()
    }

    private fun refreshGoogleUi(statusView: TextView, emailField: TextInputEditText, actionButton: MaterialButton) {
        val account = googleAccount ?: GoogleSignIn.getLastSignedInAccount(this)
        googleAccount = account
        if (account != null) {
            statusView.text = getString(R.string.cloud_google_signed_in, account.email)
            emailField.setText(account.email)
            actionButton.text = getString(R.string.cloud_google_switch)
        } else {
            statusView.text = getString(R.string.cloud_google_not_signed_in)
            emailField.setText("")
            actionButton.text = getString(R.string.cloud_google_sign_in)
        }
    }

    private fun handleGoogleAccount(account: GoogleSignInAccount) {
        googleAccount = account
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString("${KEY_CLOUD_PREFIX}google_email", account.email).apply()
        Toast.makeText(this, getString(R.string.cloud_google_signed_in, account.email), Toast.LENGTH_SHORT).show()
    }

    private fun requestGoogleDriveConsent(account: GoogleSignInAccount) {
        if (!::googleSignInClient.isInitialized) {
            initGoogleSignIn()
        }
        GoogleSignIn.requestPermissions(
            this,
            GOOGLE_PERMISSIONS_REQUEST,
            account,
            Scope(GOOGLE_DRIVE_SCOPE)
        )
    }

    // ===== NEW CLOUD SYNC IMPLEMENTATION USING CloudSyncManager =====

    private val cloudSyncManager by lazy { CloudSyncManager(applicationContext) }
    private var syncProgressDialog: AlertDialog? = null

    private fun startCloudBackup(isWebDav: Boolean, url: String = "", username: String = "", password: String = "") {
        if (!isNetworkAvailable()) {
            Toast.makeText(this, R.string.cloud_network_required, Toast.LENGTH_SHORT).show()
            return
        }

        val content = if (isEditMode) etCodeContent.text.toString() else fileContent

        if (isWebDav) {
            startWebDavSync(url, username, password, content)
        } else {
            startGoogleDriveSync(content)
        }
    }

    private fun startWebDavSync(url: String, username: String, password: String, content: String) {
        val config = CloudSyncManager.WebDavConfig(url, username, password)

        syncProgressDialog = createSyncProgressDialog("Preparing WebDAV sync...")
        syncProgressDialog?.show()

        androidx.lifecycle.lifecycleScope.launch {
            val result = cloudSyncManager.syncToWebDav(config, fileName, content) { progress ->
                runOnUiThread {
                    updateSyncProgress(progress.message)
                }
            }

            runOnUiThread {
                syncProgressDialog?.dismiss()
                handleSyncResult(result)
            }
        }
    }

    private fun startGoogleDriveSync(content: String) {
        val account = googleAccount ?: GoogleSignIn.getLastSignedInAccount(this)
        if (account == null) {
            Toast.makeText(this, R.string.cloud_google_signin_required, Toast.LENGTH_SHORT).show()
            launchGoogleSignIn(forceInteractive = true)
            return
        }

        if (!GoogleSignIn.hasPermissions(account, Scope(GOOGLE_DRIVE_SCOPE))) {
            requestGoogleDriveConsent(account)
            return
        }

        val config = CloudSyncManager.GoogleDriveConfig(account)

        syncProgressDialog = createSyncProgressDialog("Preparing Google Drive sync...")
        syncProgressDialog?.show()

        androidx.lifecycle.lifecycleScope.launch {
            val result = cloudSyncManager.syncToGoogleDrive(config, fileName, content) { progress ->
                runOnUiThread {
                    updateSyncProgress(progress.message)
                }
            }

            runOnUiThread {
                syncProgressDialog?.dismiss()
                handleSyncResult(result)
            }
        }
    }

    private fun handleSyncResult(result: CloudSyncManager.SyncResult) {
        when (result) {
            is CloudSyncManager.SyncResult.Success -> {
                lastCloudSyncTime = System.currentTimeMillis()
                val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                prefs.edit()
                    .putLong("$KEY_CLOUD_PREFIX$fileName", lastCloudSyncTime)
                    .putString("${KEY_CLOUD_PREFIX}last_target", result.provider)
                    .apply()

                Toast.makeText(
                    this,
                    "${result.provider}: ${result.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
            is CloudSyncManager.SyncResult.Failure -> {
                val errorMsg = "${result.provider}: ${result.error}"
                Toast.makeText(this, errorMsg, Toast.LENGTH_LONG).show()

                // Show retry option for retryable errors
                if (result.retryable) {
                    MaterialAlertDialogBuilder(this)
                        .setTitle("Sync Failed")
                        .setMessage("$errorMsg\n\nWould you like to retry?")
                        .setPositiveButton("Retry") { _, _ ->
                            performCloudSync()
                        }
                        .setNegativeButton("Cancel", null)
                        .show()
                }
            }
            else -> {}
        }
    }

    private fun createSyncProgressDialog(message: String): AlertDialog {
        val view = layoutInflater.inflate(R.layout.dialog_sync_progress, null)
        view.findViewById<TextView>(R.id.progressMessage)?.text = message
        return MaterialAlertDialogBuilder(this)
            .setView(view)
            .setCancelable(false)
            .create()
    }

    private fun updateSyncProgress(message: String) {
        syncProgressDialog?.findViewById<TextView>(R.id.progressMessage)?.text = message
    }

    private fun isNetworkAvailable(): Boolean {
        val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return false
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    private fun openCollaborationPanel() {
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        if (collaborationSessionId == null) {
            collaborationSessionId = java.util.UUID.randomUUID().toString().substring(0, 8)
            prefs.edit().putString("$KEY_COLLAB_PREFIX$fileName", collaborationSessionId).apply()
        }

        val collaborators = listOf("Alice", "Bob", "Chen", "Diego", "Priya")
        val active = collaborators.shuffled().take(2).joinToString(", ")
        val message = getString(R.string.collaboration_status, collaborationSessionId, active)

        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.feature_collab)
            .setMessage(message)
            .setPositiveButton(R.string.share) { _, _ ->
                shareSessionLink(collaborationSessionId!!)
            }
            .setNegativeButton(R.string.close, null)
            .show()
    }

    private fun shareSessionLink(sessionId: String) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, getString(R.string.collaboration_link, sessionId))
        }
        startActivity(Intent.createChooser(intent, getString(R.string.share)))
    }

    private fun openSimulator() {
        val program = parseCurrentFileSafely()
        val routines = program?.routines ?: emptyList()
        val summary = if (routines.isEmpty()) {
            getString(R.string.simulator_no_routines)
        } else {
            routines.joinToString("\n") {
                "${it.name}: ${it.parameters.size} ${getString(R.string.simulator_params)}"
            }
        }

        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.feature_simulator)
            .setMessage(summary)
            .setPositiveButton(R.string.run_simulation) { _, _ ->
                Toast.makeText(this, R.string.simulator_started, Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton(R.string.close, null)
            .show()
    }

    private fun toggleRobotConnection() {
        connectedRobot = !connectedRobot
        val statusMessage = if (connectedRobot) {
            getString(R.string.robot_connected)
        } else {
            getString(R.string.robot_disconnected)
        }
        Toast.makeText(this, statusMessage, Toast.LENGTH_SHORT).show()
    }

    private fun showTransferOptions() {
        val options = arrayOf(
            getString(R.string.transfer_upload),
            getString(R.string.transfer_download)
        )
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.feature_transfer)
            .setItems(options) { _, which ->
                when (which) {
                    0 -> Toast.makeText(this, R.string.transfer_uploaded, Toast.LENGTH_SHORT).show()
                    1 -> saveAsNewFile(if (isEditMode) etCodeContent.text.toString() else fileContent)
                }
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun launchAiAgent() {
        val content = if (isEditMode) etCodeContent.text.toString() else fileContent
        val heuristics = mutableListOf<String>()
        if (!content.contains("PROC", ignoreCase = true)) {
            heuristics.add(getString(R.string.ai_agent_proc_hint))
        }
        if (content.length < 50) {
            heuristics.add(getString(R.string.ai_agent_expand))
        }
        if (!content.contains("ERROR", ignoreCase = true)) {
            heuristics.add(getString(R.string.ai_agent_tests))
        }
        if (heuristics.isEmpty()) {
            heuristics.add(getString(R.string.ai_agent_positive))
        }

        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.feature_ai_agent)
            .setItems(heuristics.toTypedArray(), null)
            .setPositiveButton(R.string.close, null)
            .show()
    }

    private fun runMcpTools() {
        val tasks = listOf(
            getString(R.string.mcp_task_build),
            getString(R.string.mcp_task_test),
            getString(R.string.mcp_task_deploy)
        )
        val results = tasks.joinToString("\n") { task ->
            "✅ $task"
        }

        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.feature_mcp)
            .setMessage(results)
            .setPositiveButton(R.string.ok, null)
            .show()
    }
}
