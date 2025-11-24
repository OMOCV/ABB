package com.omocv.abb

import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.documentfile.provider.DocumentFile
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.chip.Chip
import com.google.android.material.dialog.MaterialAlertDialogBuilder

/**
 * Centralized hub for surfacing the requested roadmap features in-app.
 * Some items (like auto-complete) are already available, while others
 * provide stub interactions and discovery to show the planned surface area.
 */
class FeatureHubActivity : AppCompatActivity() {

    private lateinit var featureList: RecyclerView
    private lateinit var projectCard: MaterialCardView
    private lateinit var openProjectButton: MaterialButton
    private lateinit var projectStatus: TextView
    private lateinit var projectDetails: TextView

    private val projectTreeLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        uri?.let { handleProjectTree(it) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_feature_hub)

        initToolbar()
        initProjectSection()
        initFeatureList()
    }

    private fun initToolbar() {
        val toolbar: Toolbar = findViewById(R.id.featureHubToolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { finish() }
    }

    private fun initProjectSection() {
        projectCard = findViewById(R.id.projectBrowserCard)
        openProjectButton = findViewById(R.id.btnOpenProject)
        projectStatus = findViewById(R.id.tvProjectStatus)
        projectDetails = findViewById(R.id.tvProjectDetails)

        openProjectButton.setOnClickListener {
            try {
                projectTreeLauncher.launch(null)
            } catch (e: Exception) {
                Toast.makeText(this, getString(R.string.feature_hub_open_project_error), Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun initFeatureList() {
        featureList = findViewById(R.id.rvFeatureList)
        featureList.layoutManager = LinearLayoutManager(this)

        val features = listOf(
            FeatureCard(
                title = getString(R.string.feature_autocomplete),
                description = getString(R.string.feature_autocomplete_desc),
                status = FeatureStatus.Available,
                onClick = {
                    showInfoDialog(
                        getString(R.string.feature_autocomplete),
                        getString(R.string.feature_autocomplete_dialog)
                    )
                }
            ),
            FeatureCard(
                title = getString(R.string.feature_fold),
                description = getString(R.string.feature_fold_desc),
                status = FeatureStatus.Planned,
                onClick = { showComingSoonToast() }
            ),
            FeatureCard(
                title = getString(R.string.feature_project_browser),
                description = getString(R.string.feature_project_browser_desc),
                status = FeatureStatus.InProgress,
                onClick = {
                    projectCard.requestFocus()
                    showInfoDialog(
                        getString(R.string.feature_project_browser),
                        getString(R.string.feature_project_browser_dialog)
                    )
                }
            ),
            FeatureCard(
                title = getString(R.string.feature_refactor),
                description = getString(R.string.feature_refactor_desc),
                status = FeatureStatus.Planned,
                onClick = { showComingSoonToast() }
            ),
            FeatureCard(
                title = getString(R.string.feature_vcs),
                description = getString(R.string.feature_vcs_desc),
                status = FeatureStatus.Planned,
                onClick = { showComingSoonToast() }
            ),
            FeatureCard(
                title = getString(R.string.feature_cloud_sync),
                description = getString(R.string.feature_cloud_sync_desc),
                status = FeatureStatus.Planned,
                onClick = { showComingSoonToast() }
            ),
            FeatureCard(
                title = getString(R.string.feature_collab),
                description = getString(R.string.feature_collab_desc),
                status = FeatureStatus.Planned,
                onClick = { showComingSoonToast() }
            ),
            FeatureCard(
                title = getString(R.string.feature_simulator),
                description = getString(R.string.feature_simulator_desc),
                status = FeatureStatus.Planned,
                onClick = { showComingSoonToast() }
            ),
            FeatureCard(
                title = getString(R.string.feature_robot_connect),
                description = getString(R.string.feature_robot_connect_desc),
                status = FeatureStatus.Planned,
                onClick = { showComingSoonToast() }
            ),
            FeatureCard(
                title = getString(R.string.feature_upload_download),
                description = getString(R.string.feature_upload_download_desc),
                status = FeatureStatus.Planned,
                onClick = { showComingSoonToast() }
            ),
            FeatureCard(
                title = getString(R.string.feature_ai_agent),
                description = getString(R.string.feature_ai_agent_desc),
                status = FeatureStatus.Planned,
                onClick = { showComingSoonToast() }
            ),
            FeatureCard(
                title = getString(R.string.feature_mcp),
                description = getString(R.string.feature_mcp_desc),
                status = FeatureStatus.Planned,
                onClick = { showComingSoonToast() }
            )
        )

        featureList.adapter = FeatureHubAdapter(features)
    }

    private fun handleProjectTree(uri: Uri) {
        val tree = DocumentFile.fromTreeUri(this, uri) ?: return
        val fileNames = mutableListOf<String>()
        walkProjectTree(tree, fileNames)

        projectStatus.visibility = View.VISIBLE
        projectDetails.visibility = View.VISIBLE

        projectStatus.text = getString(R.string.feature_project_browser_status, fileNames.size)
        projectDetails.text = fileNames.joinToString(separator = "\n") { "• $it" }

        if (fileNames.isEmpty()) {
            Toast.makeText(this, getString(R.string.feature_project_browser_empty), Toast.LENGTH_SHORT).show()
        } else {
            MaterialAlertDialogBuilder(this)
                .setTitle(getString(R.string.feature_project_browser))
                .setMessage(fileNames.joinToString("\n"))
                .setPositiveButton(android.R.string.ok, null)
                .show()
        }
    }

    private fun walkProjectTree(node: DocumentFile, files: MutableList<String>, depth: Int = 0) {
        if (depth > 2) return  // Avoid deep recursion for performance

        if (node.isDirectory) {
            node.listFiles().forEach { child ->
                walkProjectTree(child, files, depth + 1)
            }
        } else {
            node.name?.let { files.add(it) }
        }
    }

    private fun showComingSoonToast() {
        Toast.makeText(this, getString(R.string.feature_hub_coming_soon), Toast.LENGTH_SHORT).show()
    }

    private fun showInfoDialog(title: String, message: String) {
        MaterialAlertDialogBuilder(this)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton(android.R.string.ok, null)
            .show()
    }
}

data class FeatureCard(
    val title: String,
    val description: String,
    val status: FeatureStatus,
    val onClick: () -> Unit
)

enum class FeatureStatus { Available, InProgress, Planned }

private class FeatureHubAdapter(
    private val features: List<FeatureCard>
) : RecyclerView.Adapter<FeatureHubViewHolder>() {

    override fun onCreateViewHolder(parent: android.view.ViewGroup, viewType: Int): FeatureHubViewHolder {
        val view = android.view.LayoutInflater.from(parent.context)
            .inflate(R.layout.item_feature_card, parent, false)
        return FeatureHubViewHolder(view)
    }

    override fun getItemCount(): Int = features.size

    override fun onBindViewHolder(holder: FeatureHubViewHolder, position: Int) {
        holder.bind(features[position])
    }
}

private class FeatureHubViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    private val card: MaterialCardView = itemView.findViewById(R.id.featureCard)
    private val title: TextView = itemView.findViewById(R.id.tvFeatureTitle)
    private val description: TextView = itemView.findViewById(R.id.tvFeatureDescription)
    private val statusChip: Chip = itemView.findViewById(R.id.chipStatus)

    fun bind(feature: FeatureCard) {
        title.text = feature.title
        description.text = feature.description

        when (feature.status) {
            FeatureStatus.Available -> {
                statusChip.text = itemView.context.getString(R.string.feature_status_available)
                statusChip.setChipBackgroundColorResource(R.color.featureStatusAvailable)
            }
            FeatureStatus.InProgress -> {
                statusChip.text = itemView.context.getString(R.string.feature_status_in_progress)
                statusChip.setChipBackgroundColorResource(R.color.featureStatusInProgress)
            }
            FeatureStatus.Planned -> {
                statusChip.text = itemView.context.getString(R.string.feature_status_planned)
                statusChip.setChipBackgroundColorResource(R.color.featureStatusPlanned)
            }
        }

        card.setOnClickListener { feature.onClick.invoke() }
    }
}
