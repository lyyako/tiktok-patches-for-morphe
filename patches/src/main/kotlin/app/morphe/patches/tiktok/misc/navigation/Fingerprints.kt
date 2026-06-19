package app.morphe.patches.tiktok.misc.navigation

import app.morphe.patcher.Fingerprint

internal object HomeTabAbilityListFingerprint : Fingerprint(
    definingClass = "Lcom/ss/android/ugc/aweme/main/assems/tabs/TabAbilityAssem;",
    name = "eT1",
    returnType = "Ljava/util/List;",
    parameters = listOf("Z"),
)

internal object BottomTabBuildListFingerprint : Fingerprint(
    definingClass = "LX/0tBq;",
    name = "LJJL",
    returnType = "V",
    parameters = listOf("Ljava/util/List;"),
)
