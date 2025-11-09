package com.omocv.abb

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import androidx.recyclerview.widget.RecyclerView
import android.widget.TextView
import java.io.File

class MainActivity : AppCompatActivity() {
    
    companion object {
        private const val PREFS_NAME = "ABBPrefs"
        private const val KEY_RECENT_FILES = "recent_files"
        private const val MAX_RECENT_FILES = 10
    }

    private lateinit var btnSelectFile: MaterialButton
    private lateinit var tvFileName: TextView
    private lateinit var tvFileType: TextView
    private lateinit var tvContent: TextView
    private lateinit var cardModules: MaterialCardView
    private lateinit var cardRoutines: MaterialCardView
    private lateinit var cardContent: MaterialCardView
    private lateinit var rvModules: RecyclerView
    private lateinit var rvRoutines: RecyclerView

    private val abbParser = ABBParser()
    private val syntaxHighlighter = ABBSyntaxHighlighter()

    private var currentProgramFile: ABBProgramFile? = null

    // File picker launcher
    private val filePickerLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { handleSelectedFile(it) }
    }

    // Permission launcher
    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.all { it.value }
        if (allGranted) {
            openFilePicker()
        } else {
            Toast.makeText(this, "需要存储权限才能读取文件", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initViews()
        setupListeners()
        
        // Handle intent if opened from file manager
        handleIntent(intent)
    }
    
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_open_file -> {
                checkPermissionAndOpenFile()
                true
            }
            R.id.action_recent_files -> {
                showRecentFiles()
                true
            }
            R.id.action_open_folder -> {
                openFolderBrowser()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun initViews() {
        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)

        btnSelectFile = findViewById(R.id.btnSelectFile)
        tvFileName = findViewById(R.id.tvFileName)
        tvFileType = findViewById(R.id.tvFileType)
        tvContent = findViewById(R.id.tvContent)
        cardModules = findViewById(R.id.cardModules)
        cardRoutines = findViewById(R.id.cardRoutines)
        cardContent = findViewById(R.id.cardContent)
        rvModules = findViewById(R.id.rvModules)
        rvRoutines = findViewById(R.id.rvRoutines)

        rvModules.layoutManager = LinearLayoutManager(this)
        rvRoutines.layoutManager = LinearLayoutManager(this)
    }

    private fun setupListeners() {
        btnSelectFile.setOnClickListener {
            checkPermissionAndOpenFile()
        }
    }

    private fun handleIntent(intent: Intent) {
        if (intent.action == Intent.ACTION_VIEW) {
            intent.data?.let { uri ->
                handleSelectedFile(uri)
            }
        }
    }

    private fun checkPermissionAndOpenFile() {
        // For Android 6.0 to 12, check READ_EXTERNAL_STORAGE permission
        // For Android 13+, we rely on SAF which doesn't require runtime permissions
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && Build.VERSION.SDK_INT <= Build.VERSION_CODES.S_V2) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.READ_EXTERNAL_STORAGE
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                openFilePicker()
            } else {
                permissionLauncher.launch(
                    arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
                )
            }
        } else {
            // Android 5.x or Android 13+ - use SAF directly
            openFilePicker()
        }
    }

    private fun openFilePicker() {
        try {
            filePickerLauncher.launch("*/*")
        } catch (e: Exception) {
            Toast.makeText(this, "无法打开文件选择器: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun handleSelectedFile(uri: Uri) {
        try {
            currentFileUri = uri
            // Read file from URI
            val inputStream = contentResolver.openInputStream(uri)
            val content = inputStream?.bufferedReader()?.use { it.readText() } ?: ""
            inputStream?.close()

            // Get filename from URI
            val fileName = uri.lastPathSegment ?: "unknown"
            val fileExtension = fileName.substringAfterLast(".", "")

            // Check if it's a supported ABB file
            if (fileExtension.lowercase() !in setOf("mod", "prg", "sys")) {
                Toast.makeText(this, "不支持的文件格式。支持的格式: .mod, .prg, .sys", Toast.LENGTH_LONG).show()
                return
            }

            // Create a temporary file for parsing
            val tempFile = File.createTempFile("abb_temp", ".$fileExtension", cacheDir)
            tempFile.writeText(content)

            // Parse the file
            val programFile = abbParser.parseFile(tempFile)
            currentProgramFile = programFile

            // Display the results
            displayProgramFile(programFile)

            // Clean up temp file
            tempFile.delete()

        } catch (e: Exception) {
            Toast.makeText(this, "文件读取错误: ${e.message}", Toast.LENGTH_LONG).show()
            e.printStackTrace()
        }
    }

    private fun displayProgramFile(programFile: ABBProgramFile) {
        // Display file name
        tvFileName.text = programFile.fileName
        tvFileType.text = "文件类型: ${programFile.fileType.uppercase()}"
        tvFileType.visibility = View.VISIBLE

        // Display modules
        if (programFile.modules.isNotEmpty()) {
            cardModules.visibility = View.VISIBLE
            val moduleElements = programFile.modules.map { module ->
                CodeElementAdapter.CodeElement(
                    name = module.name,
                    description = "类型: ${module.type}, 例行程序: ${module.routines.size}, 变量: ${module.variables.size}"
                )
            }
            rvModules.adapter = CodeElementAdapter(moduleElements) { element ->
                Toast.makeText(this, "模块: ${element.name}", Toast.LENGTH_SHORT).show()
            }
        } else {
            cardModules.visibility = View.GONE
        }

        // Display routines
        if (programFile.routines.isNotEmpty()) {
            cardRoutines.visibility = View.VISIBLE
            val routineElements = programFile.routines.map { routine ->
                CodeElementAdapter.CodeElement(
                    name = routine.name,
                    description = "类型: ${routine.type}, 参数: ${routine.parameters.size}, 行: ${routine.startLine}-${routine.endLine}"
                )
            }
            rvRoutines.adapter = CodeElementAdapter(routineElements) { element ->
                // Find the routine and highlight its content
                val routine = programFile.routines.find { it.name == element.name }
                routine?.let {
                    displayRoutineContent(programFile.content, it)
                }
            }
        } else {
            cardRoutines.visibility = View.GONE
        }

        // Display content with syntax highlighting
        cardContent.visibility = View.VISIBLE
        val highlightedContent = syntaxHighlighter.highlight(programFile.content)
        tvContent.text = highlightedContent
        
        // Add button to view in full screen
        tvContent.setOnClickListener {
            openCodeViewer(programFile.fileName, programFile.content)
        }
        
        // Add file to recent files
        addToRecentFiles(programFile.fileName, currentFileUri?.toString() ?: "")
    }

    private fun openCodeViewer(fileName: String, content: String) {
        val intent = CodeViewerActivity.newIntent(this, fileName, content)
        startActivity(intent)
    }

    private fun addToRecentFiles(fileName: String, fileUri: String) {
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val recentFiles = prefs.getStringSet(KEY_RECENT_FILES, mutableSetOf())?.toMutableSet() ?: mutableSetOf()
        
        // Format: fileName|||fileUri
        val entry = "$fileName|||$fileUri"
        recentFiles.add(entry)
        
        // Keep only the last MAX_RECENT_FILES
        if (recentFiles.size > MAX_RECENT_FILES) {
            val list = recentFiles.toList()
            recentFiles.clear()
            recentFiles.addAll(list.takeLast(MAX_RECENT_FILES))
        }
        
        prefs.edit().putStringSet(KEY_RECENT_FILES, recentFiles).apply()
    }

    private fun showRecentFiles() {
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val recentFiles = prefs.getStringSet(KEY_RECENT_FILES, mutableSetOf())?.toList() ?: emptyList()
        
        if (recentFiles.isEmpty()) {
            Toast.makeText(this, getString(R.string.no_recent_files), Toast.LENGTH_SHORT).show()
            return
        }
        
        val fileNames = recentFiles.map { it.split("|||")[0] }.toTypedArray()
        
        MaterialAlertDialogBuilder(this)
            .setTitle(getString(R.string.recent_files))
            .setItems(fileNames) { _, which ->
                val entry = recentFiles[which]
                val parts = entry.split("|||")
                if (parts.size >= 2) {
                    val uri = Uri.parse(parts[1])
                    handleSelectedFile(uri)
                }
            }
            .setNegativeButton(getString(R.string.close), null)
            .show()
    }

    private fun openFolderBrowser() {
        Toast.makeText(this, getString(R.string.feature_coming_soon), Toast.LENGTH_SHORT).show()
    }
    
    private var currentFileUri: Uri? = null

    private fun displayRoutineContent(fullContent: String, routine: ABBRoutine) {
        val lines = fullContent.lines()
        val routineContent = lines.subList(
            routine.startLine.coerceAtLeast(0),
            (routine.endLine + 1).coerceAtMost(lines.size)
        ).joinToString("\n")

        val highlightedContent = syntaxHighlighter.highlight(routineContent)
        tvContent.text = highlightedContent

        // Scroll to content card
        cardContent.requestFocus()
        Toast.makeText(this, "显示例行程序: ${routine.name}", Toast.LENGTH_SHORT).show()
    }
}
