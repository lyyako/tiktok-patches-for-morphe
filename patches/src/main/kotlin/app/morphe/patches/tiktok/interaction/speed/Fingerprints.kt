/*
 * Forked from:
 * https://gitlab.com/ReVanced/revanced-patches/-/blob/main/patches/src/main/kotlin/app/revanced/patches/tiktok/interaction/speed/Fingerprints.kt
 */
package app.morphe.patches.tiktok.interaction.speed

import app.morphe.patcher.Fingerprint

internal object GetSpeedFingerprint : Fingerprint(
    custom = { method, classDef ->
        classDef.type == "Lcom/ss/android/ugc/aweme/feed/panel/BaseListFragmentPanel;" &&
            method.name == "onFeedSpeedSelectedEvent"
    },
)
