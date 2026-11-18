package studio.vyra.app.apk

import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import java.io.File
import java.security.MessageDigest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ApkInspector(private val context: Context) {
    private companion object {
        const val MAX_APK_BYTES = 500L * 1024L * 1024L
    }

    suspend fun inspect(uri: Uri): ApkInspection = withContext(Dispatchers.IO) {
        val resolver = context.contentResolver
        val displayName = resolver.query(uri, arrayOf(android.provider.OpenableColumns.DISPLAY_NAME), null, null, null)
            ?.use { cursor ->
                if (cursor.moveToFirst()) cursor.getString(0) else null
            } ?: "selected.apk"

        val tempFile = File.createTempFile("vyra-apk-", ".apk", context.cacheDir)
        try {
            val digest = MessageDigest.getInstance("SHA-256")
            resolver.openInputStream(uri)?.use { input ->
                tempFile.outputStream().buffered().use { output ->
                    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                    var totalBytes = 0L
                    while (true) {
                        val count = input.read(buffer)
                        if (count < 0) break
                        totalBytes += count
                        require(totalBytes <= MAX_APK_BYTES) {
                            "APK exceeds VYRA's 500 MB inspection limit."
                        }
                        digest.update(buffer, 0, count)
                        output.write(buffer, 0, count)
                    }
                    require(totalBytes > 0L) { "The selected APK file is empty." }
                }
            } ?: throw IllegalArgumentException("The selected file could not be opened.")

            val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                PackageManager.GET_SIGNING_CERTIFICATES
            } else {
                @Suppress("DEPRECATION")
                PackageManager.GET_SIGNATURES
            }
            val packageInfo = context.packageManager.getPackageArchiveInfo(tempFile.absolutePath, flags)
                ?: throw IllegalArgumentException("This file is not a readable Android APK.")

            val appInfo = packageInfo.applicationInfo
                ?: throw IllegalArgumentException("The APK does not contain application metadata.")
            appInfo.sourceDir = tempFile.absolutePath
            appInfo.publicSourceDir = tempFile.absolutePath

            val signers = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                packageInfo.signingInfo?.apkContentsSigners?.map { signature ->
                    sha256(signature.toByteArray())
                }.orEmpty()
            } else {
                @Suppress("DEPRECATION")
                packageInfo.signatures?.map { signature -> sha256(signature.toByteArray()) }.orEmpty()
            }

            val versionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                packageInfo.longVersionCode
            } else {
                @Suppress("DEPRECATION")
                packageInfo.versionCode.toLong()
            }

            val minSdk = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) appInfo.minSdkVersion else null
            val targetSdk = appInfo.targetSdkVersion

            ApkInspection(
                fileName = displayName,
                packageName = packageInfo.packageName,
                appLabel = appInfo.loadLabel(context.packageManager).toString(),
                versionName = packageInfo.versionName,
                versionCode = versionCode,
                minSdk = minSdk,
                targetSdk = targetSdk,
                apkSha256 = digest.digest().toHex(),
                signerSha256 = signers,
            )
        } finally {
            tempFile.delete()
        }
    }

    private fun sha256(bytes: ByteArray): String =
        MessageDigest.getInstance("SHA-256").digest(bytes).toHex()

    private fun ByteArray.toHex(): String = joinToString("") { byte ->
        "%02x".format(byte)
    }
}
