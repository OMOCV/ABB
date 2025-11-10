package com.omocv.abb

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.io.File

/**
 * Activity to display crash report information
 * Shows crash log path and allows user to view or share the log
 */
class CrashReportActivity : AppCompatActivity() {

    companion object {
        private const val EXTRA_LOG_PATH = "log_path"
        
        fun newIntent(context: Context, logPath: String): Intent {
            return Intent(context, CrashReportActivity::class.java).apply {
                putExtra(EXTRA_LOG_PATH, logPath)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val logPath = intent.getStringExtra(EXTRA_LOG_PATH) ?: ""
        
        // Show crash dialog
        showCrashDialog(logPath)
    }

    private fun showCrashDialog(logPath: String) {
        val message = if (logPath.isNotEmpty()) {
            getString(R.string.crash_message_with_log, logPath)
        } else {
            getString(R.string.crash_message_no_log)
        }
        
        val builder = MaterialAlertDialogBuilder(this)
            .setTitle(getString(R.string.crash_title))
            .setMessage(message)
            .setCancelable(false)
            .setPositiveButton(getString(R.string.close_app)) { _, _ ->
                finishAndExit()
            }
        
        if (logPath.isNotEmpty()) {
            builder.setNeutralButton(getString(R.string.view_log)) { _, _ ->
                viewLogFile(logPath)
            }
            builder.setNegativeButton(getString(R.string.share_log)) { _, _ ->
                shareLogFile(logPath)
            }
        }
        
        builder.show()
    }

    private fun viewLogFile(logPath: String) {
        try {
            val logContent = File(logPath).readText()
            
            val dialogView = layoutInflater.inflate(android.R.layout.select_dialog_item, null)
            val textView = TextView(this).apply {
                text = logContent
                setPadding(32, 32, 32, 32)
                textSize = 12f
                setTextIsSelectable(true)
            }
            
            MaterialAlertDialogBuilder(this)
                .setTitle(getString(R.string.crash_log))
                .setView(textView)
                .setPositiveButton(getString(R.string.ok)) { _, _ ->
                    finishAndExit()
                }
                .setCancelable(false)
                .show()
                
        } catch (e: Exception) {
            MaterialAlertDialogBuilder(this)
                .setTitle(getString(R.string.error))
                .setMessage(getString(R.string.failed_to_read_log))
                .setPositiveButton(getString(R.string.ok)) { _, _ ->
                    finishAndExit()
                }
                .setCancelable(false)
                .show()
        }
    }

    private fun shareLogFile(logPath: String) {
        try {
            val logFile = File(logPath)
            val logContent = logFile.readText()
            
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, logContent)
                putExtra(Intent.EXTRA_SUBJECT, getString(R.string.crash_log_subject))
            }
            startActivity(Intent.createChooser(intent, getString(R.string.share_log)))
            finishAndExit()
            
        } catch (e: Exception) {
            MaterialAlertDialogBuilder(this)
                .setTitle(getString(R.string.error))
                .setMessage(getString(R.string.failed_to_share_log))
                .setPositiveButton(getString(R.string.ok)) { _, _ ->
                    finishAndExit()
                }
                .setCancelable(false)
                .show()
        }
    }

    private fun finishAndExit() {
        finishAffinity()
        System.exit(0)
    }
}
