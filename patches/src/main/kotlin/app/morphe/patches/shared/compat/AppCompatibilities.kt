package app.morphe.patches.shared.compat

import app.morphe.patcher.patch.AppTarget
import app.morphe.patcher.patch.Compatibility

@Suppress("MemberVisibilityCanBePrivate")
internal object AppCompatibilities {

    fun tiktokany(): Array<Compatibility> = arrayOf(
        Compatibility(
            name = "TikTok",
            packageName = "com.zhiliaoapp.musically",
            appIconColor = 0xFE2C55,
        ),
    )

    fun tiktok4383(): Array<Compatibility> = arrayOf(
        Compatibility(
            name = "TikTok",
            packageName = "com.zhiliaoapp.musically",
            appIconColor = 0xFE2C55,
            targets = listOf(AppTarget("43.8.3")),
        ),
    )

}
