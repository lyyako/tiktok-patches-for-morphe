/*
 * Forked from:
 * https://gitlab.com/ReVanced/revanced-patches/-/blob/main/patches/src/main/kotlin/app/revanced/patches/tiktok/misc/settings/Fingerprints.kt
 */
package app.morphe.patches.tiktok.misc.settings

import app.morphe.patcher.Fingerprint

internal object AdPersonalizationActivityOnCreateFingerprint : Fingerprint(
    custom = { method, classDef ->
        classDef.type == "Lcom/bytedance/ies/ugc/aweme/commercialize/compliance/personalization/AdPersonalizationActivity;" &&
            method.name == "onCreate"
    },
)

internal object SettingsStatusLoadFingerprint : Fingerprint(
    custom = { method, classDef ->
        classDef.type == "Lapp/morphe/extension/tiktok/settings/SettingsStatus;" && method.name == "load"
    },
)

internal object SettingsComposeRowsFingerprint : Fingerprint(
    custom = { method, classDef ->
        classDef.type == "Lcom/ss/android/ugc/aweme/setting/ui/rvmpcompose/SettingsComposeRvmpFragment;" &&
            method.name == "XN" &&
            method.parameterTypes.size == 8
    },
)

internal object SupportGroupDefaultStateFingerprint : Fingerprint(
    custom = { method, classDef ->
        classDef.type == "Lcom/ss/android/ugc/aweme/setting/ui/rvmpcompose/group/support/SupportGroupVM;" &&
            method.name == "defaultState"
    },
)

internal object OpenDebugCellVmDefaultStateFingerprint : Fingerprint(
    custom = { method, classDef ->
        classDef.type == "Lcom/ss/android/ugc/aweme/setting/ui/rvmpcompose/group/support/cells/OpenDebugCellVM;" &&
            method.name == "defaultState"
    },
)
