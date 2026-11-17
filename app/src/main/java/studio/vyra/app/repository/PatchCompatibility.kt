package studio.vyra.app.repository

import studio.vyra.app.apk.ApkInspection

object PatchCompatibility {
    fun matches(patch: PatchEntry, apk: ApkInspection): Boolean {
        if (patch.targetPackage != apk.packageName) return false
        if (patch.minVersionCode != null && apk.versionCode < patch.minVersionCode) return false
        if (patch.maxVersionCode != null && apk.versionCode > patch.maxVersionCode) return false
        if (patch.targetVersions.isNotEmpty() && apk.versionName !in patch.targetVersions) return false
        return true
    }
}
