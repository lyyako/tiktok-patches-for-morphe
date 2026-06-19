/*
 * Forked from:
 * https://gitlab.com/ReVanced/revanced-patches/-/blob/main/patches/src/main/kotlin/app/revanced/patches/tiktok/feedfilter/Fingerprints.kt
 */
package app.morphe.patches.tiktok.feedfilter

import app.morphe.patcher.Fingerprint
import com.android.tools.smali.dexlib2.AccessFlags

internal object FeedItemListGetItemsFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC),
    returnType = "Ljava/util/List;",
    custom = { method, classDef ->
        classDef.type == "Lcom/ss/android/ugc/aweme/feed/model/FeedItemList;" &&
            method.name == "getItems" &&
            method.parameterTypes.isEmpty()
    },
)

internal object FollowFeedFingerprint : Fingerprint(
    definingClass = "LX/0NkW;",
    name = "LIZ",
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.STATIC),
    returnType = "Lcom/ss/android/ugc/aweme/follow/presenter/FollowFeedList;",
    parameters = listOf("LX/0NkV;", "LX/0tGv;"),
)

internal object TakoAiFeedButtonSetVisibleFingerprint : Fingerprint(
    definingClass = "Lcom/ss/android/ugc/aweme/feed/assem/tikbot/TakoAssem;",
    name = "rn",
    returnType = "V",
    parameters = listOf("Z"),
)

internal object TakoAiFeedButtonBindFingerprint : Fingerprint(
    definingClass = "Lcom/ss/android/ugc/aweme/feed/assem/tikbot/TakoAssem;",
    name = "rm",
    returnType = "V",
    parameters = listOf("Landroid/view/View;"),
)
