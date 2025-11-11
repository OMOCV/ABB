package com.omocv.abb

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.TextView
import android.widget.EditText
import android.widget.Toast
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import android.text.Editable
import android.text.TextWatcher
import android.graphics.Color
import android.text.SpannableString
import android.text.style.BackgroundColorSpan
import android.text.Spanned
import android.widget.RadioGroup

/**
 * Full-screen code viewer with line numbers and advanced features
 * Version 2.3.0 - Enhanced search, replace, syntax checking with clickable results
 */
class CodeViewerActivity : AppCompatActivity() {

    private lateinit var toolbar: MaterialToolbar
    private lateinit var tvLineNumbers: TextView
    private lateinit var tvCodeContent: TextView
    private lateinit var etCodeContent: SyntaxHighlightEditText
    
    private val syntaxHighlighter = ABBSyntaxHighlighter()
    private val abbParser = ABBParser()
    private lateinit var fileName: String
    private var fileContent: String = ""
    private var originalContent: String = ""
    private var currentSearchQuery = ""
    private var isEditMode = false
    private var hasUnsavedChanges = false
    private var currentProgramFile: ABBProgramFile? = null
    
    companion object {
        private const val EXTRA_FILE_NAME = "file_name"
        private const val EXTRA_FILE_CONTENT = "file_content"
        private const val PREFS_NAME = "ABBPrefs"
        private const val KEY_THEME_MODE = "theme_mode"
        private const val KEY_BOOKMARKS = "bookmarks_"
        private const val KEY_REAL_TIME_CHECK = "real_time_syntax_check"
        
        fun newIntent(context: Context, fileName: String, fileContent: String): Intent {
            return Intent(context, CodeViewerActivity::class.java).apply {
                putExtra(EXTRA_FILE_NAME, fileName)
                putExtra(EXTRA_FILE_CONTENT, fileContent)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        try {
            // Apply saved theme
            applySavedTheme()
            
            setContentView(R.layout.activity_code_viewer)
            
            fileName = intent.getStringExtra(EXTRA_FILE_NAME) ?: "Unknown"
            fileContent = intent.getStringExtra(EXTRA_FILE_CONTENT) ?: ""
            originalContent = fileContent
            
            // Parse the file content to get routines info
            try {
                val tempFile = java.io.File(cacheDir, fileName)
                tempFile.writeText(fileContent)
                currentProgramFile = abbParser.parseFile(tempFile)
                tempFile.delete()
            } catch (e: Exception) {
                android.util.Log.e("CodeViewerActivity", "Error parsing file", e)
            }
            
            initViews()
            displayContent()
            setupRealTimeSyntaxCheck()
        } catch (e: Exception) {
            // Log the error and show a user-friendly message
            android.util.Log.e("CodeViewerActivity", "Error in onCreate", e)
            Toast.makeText(
                this,
                getString(R.string.failed_to_load_code_viewer),
                Toast.LENGTH_LONG
            ).show()
            finish()
        }
    }

    private fun initViews() {
        toolbar = findViewById(R.id.toolbar)
        tvLineNumbers = findViewById(R.id.tvLineNumbers)
        tvCodeContent = findViewById(R.id.tvCodeContent)
        etCodeContent = findViewById(R.id.etCodeContent)
        
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = fileName
        
        toolbar.setNavigationOnClickListener {
            handleBackPressed()
        }
    }
    
    private fun handleBackPressed() {
        if (hasUnsavedChanges) {
            MaterialAlertDialogBuilder(this)
                .setTitle(getString(R.string.unsaved_changes))
                .setMessage(getString(R.string.discard_changes))
                .setPositiveButton(getString(R.string.save)) { _, _ ->
                    saveChanges()
                    finish()
                }
                .setNegativeButton(getString(R.string.discard_changes)) { _, _ ->
                    finish()
                }
                .setNeutralButton(getString(R.string.cancel), null)
                .show()
        } else {
            finish()
        }
    }
    
    override fun onBackPressed() {
        handleBackPressed()
    }

    private fun displayContent() {
        val lines = fileContent.lines()
        val lineCount = lines.size
        
        // Generate line numbers
        val lineNumbers = StringBuilder()
        for (i in 1..lineCount) {
            lineNumbers.append("$i\n")
        }
        tvLineNumbers.text = lineNumbers.toString()
        
        // Apply syntax highlighting
        if (isEditMode) {
            etCodeContent.setText(fileContent)
        } else {
            val highlightedContent = syntaxHighlighter.highlight(fileContent)
            tvCodeContent.text = highlightedContent
        }
    }
    
    private fun toggleEditMode() {
        isEditMode = !isEditMode
        
        if (isEditMode) {
            // Switch to edit mode
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
        
        // Update menu
        invalidateOptionsMenu()
    }
    
    private fun saveChanges() {
        if (isEditMode) {
            fileContent = etCodeContent.text.toString()
        }
        originalContent = fileContent
        hasUnsavedChanges = false
        Toast.makeText(this, getString(R.string.code_saved), Toast.LENGTH_SHORT).show()
        
        // Update display
        displayContent()
    }
    
    private fun setupRealTimeSyntaxCheck() {
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val enabled = prefs.getBoolean(KEY_REAL_TIME_CHECK, false)
        
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
    
    private fun showReplaceDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_replace, null)
        val etSearchText = dialogView.findViewById<EditText>(R.id.etSearchText)
        val etReplaceText = dialogView.findViewById<EditText>(R.id.etReplaceText)
        val rgReplaceScope = dialogView.findViewById<RadioGroup>(R.id.rgReplaceScope)
        
        MaterialAlertDialogBuilder(this)
            .setTitle(getString(R.string.replace_code))
            .setView(dialogView)
            .setPositiveButton(getString(R.string.replace)) { _, _ ->
                val searchText = etSearchText.text.toString()
                val replaceText = etReplaceText.text.toString()
                
                if (searchText.isNotEmpty()) {
                    val scope = when (rgReplaceScope.checkedRadioButtonId) {
                        R.id.rbReplaceInRoutine -> {
                            // Show routine selection dialog
                            showRoutineSelectionDialog(searchText, replaceText)
                            return@setPositiveButton
                        }
                        R.id.rbReplaceInModule -> "module"
                        else -> "all"
                    }
                    replaceCode(searchText, replaceText, scope, null)
                }
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .show()
    }
    
    private fun showRoutineSelectionDialog(searchText: String, replaceText: String) {
        val routines = currentProgramFile?.routines ?: emptyList()
        
        if (routines.isEmpty()) {
            Toast.makeText(this, getString(R.string.no_routines_selected), Toast.LENGTH_SHORT).show()
            replaceCode(searchText, replaceText, "all", null)
            return
        }
        
        val dialogView = layoutInflater.inflate(R.layout.dialog_routine_selection, null)
        val rvRoutines = dialogView.findViewById<RecyclerView>(R.id.rvRoutines)
        val btnSelectAll = dialogView.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnSelectAll)
        val btnDeselectAll = dialogView.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnDeselectAll)
        
        rvRoutines.layoutManager = LinearLayoutManager(this)
        val adapter = RoutineSelectionAdapter(routines)
        rvRoutines.adapter = adapter
        
        btnSelectAll.setOnClickListener {
            adapter.selectAll()
        }
        
        btnDeselectAll.setOnClickListener {
            adapter.deselectAll()
        }
        
        MaterialAlertDialogBuilder(this)
            .setTitle(getString(R.string.select_routines))
            .setView(dialogView)
            .setPositiveButton(getString(R.string.replace)) { _, _ ->
                val selectedRoutines = adapter.getSelectedRoutines()
                if (selectedRoutines.isEmpty()) {
                    Toast.makeText(this, getString(R.string.no_routines_selected), Toast.LENGTH_SHORT).show()
                } else {
                    replaceCode(searchText, replaceText, "routine", selectedRoutines)
                }
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .show()
    }
    
    private fun replaceCode(searchText: String, replaceText: String, scope: String, selectedRoutines: List<ABBRoutine>?) {
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
                        for (lineIdx in routine.startLine..routine.endLine.coerceAtMost(lines.size - 1)) {
                            if (lineIdx < lines.size) {
                                val line = lines[lineIdx]
                                if (line.contains(searchText, ignoreCase = true)) {
                                    val regex = Regex.escape(searchText).toRegex(RegexOption.IGNORE_CASE)
                                    count += regex.findAll(line).count()
                                    lines[lineIdx] = line.replace(searchText, replaceText, ignoreCase = true)
                                }
                            }
                        }
                    }
                    
                    content = lines.joinToString("\n")
                }
            }
            "module" -> {
                // Replace in current module (simplified - replaces in visible content)
                val regex = Regex.escape(searchText).toRegex(RegexOption.IGNORE_CASE)
                count = regex.findAll(content).count()
                content = content.replace(searchText, replaceText, ignoreCase = true)
            }
        }
        
        fileContent = content
        hasUnsavedChanges = true
        
        if (isEditMode) {
            etCodeContent.setText(content)
        } else {
            displayContent()
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
            jumpToLine(result.lineNumber)
            dialog.dismiss()
        }
        
        dialog.show()
    }
    
    private fun jumpToLine(lineNumber: Int) {
        val content = if (isEditMode) etCodeContent.text.toString() else fileContent
        val lines = content.lines()
        if (lineNumber > 0 && lineNumber <= lines.size) {
            // Calculate the character position of the line
            var charPosition = 0
            for (i in 0 until lineNumber - 1) {
                charPosition += lines[i].length + 1 // +1 for newline
            }
            
            if (isEditMode) {
                // Set cursor to the line in edit mode
                etCodeContent.setSelection(charPosition.coerceAtMost(etCodeContent.text.length))
                etCodeContent.requestFocus()
            }
            
            Toast.makeText(this, getString(R.string.jumped_to_line, lineNumber), Toast.LENGTH_SHORT).show()
        }
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
        
        val items = bookmarks.sorted().map { getString(R.string.line_number, it) }.toTypedArray()
        
        MaterialAlertDialogBuilder(this)
            .setTitle(getString(R.string.bookmarks))
            .setItems(items) { _, which ->
                val lineNumber = bookmarks.sorted()[which].toInt()
                jumpToLine(lineNumber)
            }
            .setNegativeButton(getString(R.string.close), null)
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

    private fun toggleTheme() {
        val currentMode = AppCompatDelegate.getDefaultNightMode()
        val newMode = if (currentMode == AppCompatDelegate.MODE_NIGHT_YES) {
            AppCompatDelegate.MODE_NIGHT_NO
        } else {
            AppCompatDelegate.MODE_NIGHT_YES
        }
        
        // Save preference
        getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putInt(KEY_THEME_MODE, newMode)
            .apply()
        
        // Apply theme
        AppCompatDelegate.setDefaultNightMode(newMode)
        recreate()
    }

    private fun applySavedTheme() {
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val mode = prefs.getInt(KEY_THEME_MODE, AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        AppCompatDelegate.setDefaultNightMode(mode)
    }

    private fun checkSyntax() {
        val content = if (isEditMode) etCodeContent.text.toString() else fileContent
        val errors = abbParser.validateSyntax(content)
        
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
            jumpToLine(error.lineNumber)
            dialog.dismiss()
        }
        
        dialog.show()
    }
}
