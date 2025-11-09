package com.omocv.abb

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import android.text.Editable
import android.text.TextWatcher
import android.widget.EditText
import android.graphics.Color
import android.text.SpannableString
import android.text.style.BackgroundColorSpan
import android.text.Spanned

/**
 * Full-screen code viewer with line numbers and advanced features
 */
class CodeViewerActivity : AppCompatActivity() {

    private lateinit var toolbar: MaterialToolbar
    private lateinit var tvLineNumbers: TextView
    private lateinit var tvCodeContent: TextView
    
    private val syntaxHighlighter = ABBSyntaxHighlighter()
    private lateinit var fileName: String
    private lateinit var fileContent: String
    private var currentSearchQuery = ""
    
    companion object {
        private const val EXTRA_FILE_NAME = "file_name"
        private const val EXTRA_FILE_CONTENT = "file_content"
        private const val PREFS_NAME = "ABBPrefs"
        private const val KEY_THEME_MODE = "theme_mode"
        private const val KEY_BOOKMARKS = "bookmarks_"
        
        fun newIntent(context: Context, fileName: String, fileContent: String): Intent {
            return Intent(context, CodeViewerActivity::class.java).apply {
                putExtra(EXTRA_FILE_NAME, fileName)
                putExtra(EXTRA_FILE_CONTENT, fileContent)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Apply saved theme
        applySavedTheme()
        
        setContentView(R.layout.activity_code_viewer)
        
        fileName = intent.getStringExtra(EXTRA_FILE_NAME) ?: "Unknown"
        fileContent = intent.getStringExtra(EXTRA_FILE_CONTENT) ?: ""
        
        initViews()
        displayContent()
    }

    private fun initViews() {
        toolbar = findViewById(R.id.toolbar)
        tvLineNumbers = findViewById(R.id.tvLineNumbers)
        tvCodeContent = findViewById(R.id.tvCodeContent)
        
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = fileName
        
        toolbar.setNavigationOnClickListener {
            finish()
        }
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
        val highlightedContent = syntaxHighlighter.highlight(fileContent)
        tvCodeContent.text = highlightedContent
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_code_viewer, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_search -> {
                showSearchDialog()
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

    private fun searchCode(query: String) {
        currentSearchQuery = query
        if (query.isEmpty()) {
            displayContent()
            return
        }
        
        val lines = fileContent.lines()
        val matches = mutableListOf<Int>()
        
        lines.forEachIndexed { index, line ->
            if (line.contains(query, ignoreCase = true)) {
                matches.add(index + 1)
            }
        }
        
        if (matches.isEmpty()) {
            Toast.makeText(this, getString(R.string.no_matches_found), Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, getString(R.string.found_matches, matches.size), Toast.LENGTH_SHORT).show()
            highlightSearchResults(query)
        }
    }

    private fun highlightSearchResults(query: String) {
        val spannable = SpannableString(fileContent)
        var index = fileContent.indexOf(query, 0, ignoreCase = true)
        
        while (index >= 0) {
            spannable.setSpan(
                BackgroundColorSpan(Color.YELLOW),
                index,
                index + query.length,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
            index = fileContent.indexOf(query, index + 1, ignoreCase = true)
        }
        
        val highlightedContent = syntaxHighlighter.highlight(fileContent)
        tvCodeContent.text = highlightedContent
    }

    private fun exportCode() {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, fileContent)
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
                scrollToLine(lineNumber)
            }
            .setNegativeButton(getString(R.string.close), null)
            .show()
    }

    private fun scrollToLine(lineNumber: Int) {
        // Calculate scroll position
        val lines = fileContent.lines()
        if (lineNumber > 0 && lineNumber <= lines.size) {
            Toast.makeText(this, getString(R.string.jumped_to_line, lineNumber), Toast.LENGTH_SHORT).show()
        }
    }

    private fun formatCode() {
        // Simple code formatting: fix indentation
        val lines = fileContent.lines()
        val formatted = StringBuilder()
        var indentLevel = 0
        
        lines.forEach { line ->
            val trimmed = line.trim()
            
            // Decrease indent for end keywords
            if (trimmed.startsWith("ENDMODULE", ignoreCase = true) ||
                trimmed.startsWith("ENDPROC", ignoreCase = true) ||
                trimmed.startsWith("ENDFUNC", ignoreCase = true) ||
                trimmed.startsWith("ENDTRAP", ignoreCase = true) ||
                trimmed.startsWith("ENDIF", ignoreCase = true) ||
                trimmed.startsWith("ENDFOR", ignoreCase = true) ||
                trimmed.startsWith("ENDWHILE", ignoreCase = true) ||
                trimmed.startsWith("ENDTEST", ignoreCase = true)) {
                indentLevel = maxOf(0, indentLevel - 1)
            }
            
            // Add indented line
            val indent = "    ".repeat(indentLevel)
            formatted.append(indent).append(trimmed).append("\n")
            
            // Increase indent for start keywords
            if (trimmed.startsWith("MODULE", ignoreCase = true) ||
                trimmed.startsWith("PROC", ignoreCase = true) ||
                trimmed.startsWith("FUNC", ignoreCase = true) ||
                trimmed.startsWith("TRAP", ignoreCase = true) ||
                trimmed.startsWith("IF", ignoreCase = true) ||
                trimmed.startsWith("FOR", ignoreCase = true) ||
                trimmed.startsWith("WHILE", ignoreCase = true) ||
                trimmed.startsWith("TEST", ignoreCase = true)) {
                indentLevel++
            }
        }
        
        fileContent = formatted.toString()
        displayContent()
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
        val errors = mutableListOf<String>()
        val lines = fileContent.lines()
        
        var moduleCount = 0
        var endModuleCount = 0
        var procCount = 0
        var endProcCount = 0
        
        lines.forEachIndexed { index, line ->
            val trimmed = line.trim()
            
            when {
                trimmed.startsWith("MODULE", ignoreCase = true) -> moduleCount++
                trimmed.startsWith("ENDMODULE", ignoreCase = true) -> endModuleCount++
                trimmed.startsWith("PROC", ignoreCase = true) -> procCount++
                trimmed.startsWith("ENDPROC", ignoreCase = true) -> endProcCount++
            }
        }
        
        if (moduleCount != endModuleCount) {
            errors.add(getString(R.string.module_mismatch, moduleCount, endModuleCount))
        }
        if (procCount != endProcCount) {
            errors.add(getString(R.string.proc_mismatch, procCount, endProcCount))
        }
        
        if (errors.isEmpty()) {
            Toast.makeText(this, getString(R.string.no_syntax_errors), Toast.LENGTH_SHORT).show()
        } else {
            MaterialAlertDialogBuilder(this)
                .setTitle(getString(R.string.syntax_errors))
                .setMessage(errors.joinToString("\n"))
                .setPositiveButton(getString(R.string.ok), null)
                .show()
        }
    }
}
