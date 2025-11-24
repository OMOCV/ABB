package com.omocv.abb

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.documentfile.provider.DocumentFile
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton

/**
 * Lightweight SAF-backed browser for multi-file projects.
 */
class ProjectBrowserActivity : AppCompatActivity() {

    private lateinit var toolbar: MaterialToolbar
    private lateinit var btnPickFolder: MaterialButton
    private lateinit var tvFolder: TextView
    private lateinit var rvFiles: RecyclerView

    private val folderPicker = registerForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        uri?.let { loadFolder(it) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_project_browser)

        toolbar = findViewById(R.id.projectBrowserToolbar)
        btnPickFolder = findViewById(R.id.btnPickFolder)
        tvFolder = findViewById(R.id.tvFolderPath)
        rvFiles = findViewById(R.id.rvProjectFiles)

        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        rvFiles.layoutManager = LinearLayoutManager(this)

        btnPickFolder.setOnClickListener {
            try {
                folderPicker.launch(null)
            } catch (e: Exception) {
                Toast.makeText(this, R.string.feature_hub_open_project_error, Toast.LENGTH_LONG).show()
            }
        }

        intent.data?.let { loadFolder(it) }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                finish()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun loadFolder(uri: Uri) {
        val tree = DocumentFile.fromTreeUri(this, uri) ?: run {
            Toast.makeText(this, R.string.feature_project_browser_empty, Toast.LENGTH_SHORT).show()
            return
        }

        tvFolder.text = tree.uri.path
        tvFolder.visibility = View.VISIBLE

        val files = mutableListOf<DocumentFile>()
        collectFiles(tree, files)

        if (files.isEmpty()) {
            Toast.makeText(this, R.string.feature_project_browser_empty, Toast.LENGTH_SHORT).show()
        }

        rvFiles.adapter = ProjectFileAdapter(files) { file ->
            openFile(file)
        }
    }

    private fun collectFiles(node: DocumentFile, files: MutableList<DocumentFile>, depth: Int = 0) {
        if (depth > 3) return
        if (node.isDirectory) {
            node.listFiles().forEach { child -> collectFiles(child, files, depth + 1) }
        } else if (node.isFile) {
            files.add(node)
        }
    }

    private fun openFile(file: DocumentFile) {
        if (!file.isFile) return
        try {
            val input = contentResolver.openInputStream(file.uri)
            val content = input?.bufferedReader()?.use { it.readText() } ?: ""
            input?.close()

            val intent = CodeViewerActivity.newIntent(
                this,
                file.name ?: getString(R.string.unknown_file),
                content,
                file.uri.toString()
            )
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(this, R.string.feature_hub_open_project_error, Toast.LENGTH_LONG).show()
        }
    }
}

private class ProjectFileAdapter(
    private val files: List<DocumentFile>,
    private val onClick: (DocumentFile) -> Unit
) : RecyclerView.Adapter<ProjectFileViewHolder>() {

    override fun onCreateViewHolder(parent: android.view.ViewGroup, viewType: Int): ProjectFileViewHolder {
        val view = android.view.LayoutInflater.from(parent.context)
            .inflate(R.layout.item_project_file, parent, false)
        return ProjectFileViewHolder(view)
    }

    override fun onBindViewHolder(holder: ProjectFileViewHolder, position: Int) {
        holder.bind(files[position], onClick)
    }

    override fun getItemCount(): Int = files.size
}

private class ProjectFileViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    private val title: TextView = itemView.findViewById(R.id.tvProjectFileName)
    private val subtitle: TextView = itemView.findViewById(R.id.tvProjectFilePath)

    fun bind(file: DocumentFile, onClick: (DocumentFile) -> Unit) {
        title.text = file.name ?: itemView.context.getString(R.string.unknown_file)
        subtitle.text = file.uri.lastPathSegment ?: ""
        itemView.setOnClickListener { onClick(file) }
    }
}
