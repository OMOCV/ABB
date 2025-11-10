package com.omocv.abb

import android.app.Application
import android.content.Context
import android.os.Build
import android.util.Log
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Custom Application class for ABB Robot Program Reader
 * Implements global crash handling with file logging
 */
class ABBApplication : Application() {

    companion object {
        private const val TAG = "ABBApplication"
        private const val CRASH_LOG_DIR = "crash_logs"
        private const val MAX_LOG_FILES = 10
        
        @Volatile
        private var crashLogPath: String? = null
        
        fun getLastCrashLogPath(): String? = crashLogPath
    }

    override fun onCreate() {
        super.onCreate()
        setupCrashHandler()
    }

    private fun setupCrashHandler() {
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                // Log the crash to file
                val logPath = logCrashToFile(throwable, thread)
                crashLogPath = logPath
                
                // Show crash dialog with log path
                showCrashDialog(logPath)
                
                // Give time for dialog to show
                Thread.sleep(2000)
                
            } catch (e: Exception) {
                Log.e(TAG, "Error handling crash", e)
            } finally {
                // Call the default handler to properly terminate the app
                defaultHandler?.uncaughtException(thread, throwable)
            }
        }
    }

    private fun logCrashToFile(throwable: Throwable, thread: Thread): String {
        val crashLogDir = File(getExternalFilesDir(null), CRASH_LOG_DIR)
        if (!crashLogDir.exists()) {
            crashLogDir.mkdirs()
        }

        // Clean up old log files
        cleanupOldLogFiles(crashLogDir)

        // Create new log file with timestamp
        val timestamp = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.getDefault()).format(Date())
        val logFile = File(crashLogDir, "crash_$timestamp.log")

        try {
            logFile.writer().use { writer ->
                // Write header information
                writer.write("=== ABB Robot Program Reader Crash Log ===\n")
                writer.write("Time: ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())}\n")
                writer.write("App Version: ${getAppVersion()}\n")
                writer.write("Device: ${Build.MANUFACTURER} ${Build.MODEL}\n")
                writer.write("Android Version: ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})\n")
                writer.write("Thread: ${thread.name}\n")
                writer.write("\n=== Stack Trace ===\n")
                
                // Write stack trace
                val sw = StringWriter()
                val pw = PrintWriter(sw)
                throwable.printStackTrace(pw)
                writer.write(sw.toString())
                
                // Write cause chain if present
                var cause = throwable.cause
                while (cause != null) {
                    writer.write("\n=== Caused By ===\n")
                    val causeSw = StringWriter()
                    val causePw = PrintWriter(causeSw)
                    cause.printStackTrace(causePw)
                    writer.write(causeSw.toString())
                    cause = cause.cause
                }
            }
            
            Log.i(TAG, "Crash log written to: ${logFile.absolutePath}")
            return logFile.absolutePath
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to write crash log", e)
            return ""
        }
    }

    private fun cleanupOldLogFiles(crashLogDir: File) {
        try {
            val logFiles = crashLogDir.listFiles { file ->
                file.isFile && file.name.startsWith("crash_") && file.name.endsWith(".log")
            }?.sortedByDescending { it.lastModified() } ?: return

            // Keep only the most recent MAX_LOG_FILES files
            if (logFiles.size > MAX_LOG_FILES) {
                logFiles.drop(MAX_LOG_FILES).forEach { file ->
                    file.delete()
                    Log.d(TAG, "Deleted old crash log: ${file.name}")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to cleanup old log files", e)
        }
    }

    private fun showCrashDialog(logPath: String) {
        try {
            // Start CrashReportActivity to show crash information
            val intent = CrashReportActivity.newIntent(this, logPath)
            intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to show crash dialog", e)
        }
    }

    private fun getAppVersion(): String {
        return try {
            val packageInfo = packageManager.getPackageInfo(packageName, 0)
            "${packageInfo.versionName} (${packageInfo.versionCode})"
        } catch (e: Exception) {
            "Unknown"
        }
    }
}
