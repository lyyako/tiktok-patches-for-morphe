/*
 * Forked from:
 * https://github.com/ReVanced/revanced-patches/blob/377d4e15016296b45d809697f7f69bce74badd3a/extensions/tiktok/src/main/java/app/revanced/extension/tiktok/settings/TikTokActivityHook.java
 */

package app.morphe.extension.tiktok.settings;

import android.content.Intent;
import android.preference.PreferenceFragment;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

import app.morphe.extension.tiktok.settings.preference.TikTokPreferenceFragment;

import com.bytedance.ies.ugc.aweme.commercialize.compliance.personalization.AdPersonalizationActivity;

/**
 * Hooks AdPersonalizationActivity to inject a custom {@link TikTokPreferenceFragment}.
 */
@SuppressWarnings({"deprecation", "NewApi", "unused"})
public class TikTokActivityHook {
    private static final String SETTINGS_ACTION = "morphe_settings";

    /***
     * Initialize the settings menu.
     * @param base The activity to initialize the settings menu on.
     * @return Whether the settings menu should be initialized.
     */
    public static boolean initialize(AdPersonalizationActivity base) {
        Intent intent = base.getIntent();
        if (!SETTINGS_ACTION.equals(intent.getAction())) {
            return false;
        }

        SettingsStatus.load();

        LinearLayout linearLayout = new LinearLayout(base);
        linearLayout.setLayoutParams(new LinearLayout.LayoutParams(-1, -1));
        linearLayout.setOrientation(LinearLayout.VERTICAL);
        linearLayout.setFitsSystemWindows(true);
        linearLayout.setTransitionGroup(true);

        FrameLayout fragment = new FrameLayout(base);
        fragment.setLayoutParams(new FrameLayout.LayoutParams(-1, -1));
        int fragmentId = View.generateViewId();
        fragment.setId(fragmentId);

        linearLayout.addView(fragment);
        base.setContentView(linearLayout);

        PreferenceFragment preferenceFragment = new TikTokPreferenceFragment();
        base.getFragmentManager().beginTransaction().replace(fragmentId, preferenceFragment).commit();

        return true;
    }
}

