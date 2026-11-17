package studio.vyra.app.apk

import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import java.io.File
import java.io.FileOutputStream

sealed interface InstalledApkResult {
    data class Available(val uri: Uri, val label: String, val packageName: String) : InstalledApkResult
    data class Unavailable(val reason: String) : InstalledApkResult
}

/**
 * Copies an installed app's base APK into VYRA's private cache for inspection.
 * Split installs are rejected rather than silently exporting an incomplete APK.
 */
class InstalledAppApkProvider(private val context: Context) {
    fun copyBaseApk(packageName: String): InstalledApkResult {
        val appInfo = try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.packageManager.getApplicationInfo(packageName, PackageManager.ApplicationInfoFlags.of(0))
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getApplicationInfo(packageName, 0)
            }
        } catch (_: PackageManager.NameNotFoundException) {
            return InstalledApkResult.Unavailable("This app is not installed on this device.")
        } catch (error: Exception) {
            return InstalledApkResult.Unavailable(error.message ?: "Could not read the installed app.")
        }

        val splitPaths = appInfo.splitSourceDirs.orEmpty()
        if (splitPaths.isNotEmpty()) {
            return InstalledApkResult.Unavailable(
                "This app is installed as a split APK (${splitPaths.size} split part(s)). VYRA needs the complete split set; it will not use the base APK alone."
            )
        }

        val source = File(appInfo.sourceDir)
        if (!source.isFile || !source.canRead()) {
            return InstalledApkResult.Unavailable("The installed APK file cannot be read.")
        }

        return try {
            val directory = File(context.cacheDir, "installed-apks")
            if (!directory.exists() && !directory.mkdirs()) {
                return InstalledApkResult.Unavailable("Could not prepare VYRA's temporary APK cache.")
            }
            val safeName = packageName.replace(Regex("[^A-Za-z0-9._-]"), "_")
            val destination = File(directory, "$safeName-installed.apk")
            source.inputStream().use { input ->
                FileOutputStream(destination).use { output ->
                    input.copyTo(output)
                    output.fd.sync()
                }
            }
            val label = context.packageManager.getApplicationLabel(appInfo).toString()
            InstalledApkResult.Available(Uri.fromFile(destination), label, packageName)
        } catch (error: Exception) {
            InstalledApkResult.Unavailable(error.message ?: "Could not copy the installed APK.")
        }
    }
}
