package com.sas.lostandfound;

import android.app.Activity;
import android.graphics.Color;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.RelativeLayout;
import android.widget.LinearLayout;

import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.ColorUtils;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.DisplayCutoutCompat;
import androidx.core.graphics.Insets;

import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.tabs.TabLayout;

/**
 * Utility class to implement smooth, scroll-based header color transitions
 * mimicking the Telegram X dynamic header behavior.
 */
public class HeaderColorHelper {

    /**
     * Default setup: Redesigns the header for a clean, modern off-white light theme.
     * Transitions from #F8FAFC (soft slate off-white) to #E2E8F0 (soft slate grey) on scroll,
     * using dark status bar icons and dark text/actions.
     */
    public static void setup(Activity activity, AppBarLayout appBarLayout, Toolbar toolbar) {
        if (activity == null || appBarLayout == null) return;
        
        int resolvedColor = Color.WHITE;
        android.util.TypedValue typedValue = new android.util.TypedValue();
        if (activity.getTheme().resolveAttribute(com.google.android.material.R.attr.colorSurface, typedValue, true)) {
            resolvedColor = typedValue.data;
        } else if (activity.getTheme().resolveAttribute(android.R.attr.windowBackground, typedValue, true)) {
            resolvedColor = typedValue.data;
        }
        
        // Dynamically calculate luminance to set the light status bar flag (dark text/icons for light background)
        boolean lightStatusBar = ColorUtils.calculateLuminance(resolvedColor) > 0.5;
        
        setup(activity, appBarLayout, resolvedColor, resolvedColor, lightStatusBar);
    }

    /**
     * Overloaded setup method for custom colors and status bar light/dark icon settings.
     */
    public static void setup(Activity activity, AppBarLayout appBarLayout, int startColor, int endColor, boolean lightStatusBar) {
        if (activity == null || appBarLayout == null) return;

        appBarLayout.setLiftOnScroll(false);

        // Determine the text color based on the background brightness (lightStatusBar flag)
        int textColor = lightStatusBar ? Color.parseColor("#0F172A") : Color.WHITE;

        // Dynamically style all headers and their contents recursively
        styleHeaderContents(appBarLayout, textColor);

        // Safely set light/dark status bar icon colors without affecting system layout visibility flags
        androidx.core.view.WindowInsetsControllerCompat controller = 
            androidx.core.view.WindowCompat.getInsetsController(activity.getWindow(), activity.getWindow().getDecorView());
        if (controller != null) {
            controller.setAppearanceLightStatusBars(lightStatusBar);
        }

        // Move the text and icons container slightly upward to improve alignment and visual appearance
        float density = activity.getResources().getDisplayMetrics().density;
        float translationY = -12 * density;

        // Ensure no cutout/notch obstruction by dynamically adjusting top padding based on system bar & cutout insets
        ViewCompat.setOnApplyWindowInsetsListener(appBarLayout, (v, insets) -> {
            Insets systemInsets = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            DisplayCutoutCompat cutout = insets.getDisplayCutout();

            int statusBarHeight = systemInsets.top;
            int cutoutHeight = 0;
            if (cutout != null) {
                cutoutHeight = cutout.getSafeInsetTop();
            }

            int topPadding = Math.max(statusBarHeight, cutoutHeight);
            int extraPadding = (int) Math.abs(translationY);

            // Set the top padding of the AppBarLayout to match the status bar/cutout height + extra padding
            v.setPadding(v.getPaddingLeft(), topPadding + extraPadding, v.getPaddingRight(), v.getPaddingBottom());
            
            return insets;
        });

        // Apply initial startColor to the header and status bar
        appBarLayout.setBackgroundColor(startColor);
        activity.getWindow().setStatusBarColor(startColor);
        appBarLayout.setElevation(0f);

        // Apply the upward translation to the header contents
        for (int i = 0; i < appBarLayout.getChildCount(); i++) {
            View child = appBarLayout.getChildAt(i);
            if (!child.getClass().getSimpleName().equals("TabLayout")) {
                child.setTranslationY(translationY);
            }
        }

        // Add dynamic hairline bottom separator if none exists
        if (!hasSeparator(appBarLayout, density)) {
            View separator = new View(activity);
            AppBarLayout.LayoutParams separatorParams = new AppBarLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                (int) Math.max(1f, 1f * density)
            );
            separatorParams.setScrollFlags(0); // static locking
            separator.setLayoutParams(separatorParams);
            int dividerColor = lightStatusBar ? Color.parseColor("#E2E8F0") : Color.parseColor("#2D2D2D");
            separator.setBackgroundColor(dividerColor);
            
            // On User Dashboard (where TabLayout exists), do not shift the separator line up too much.
            // A translation of 0f keeps it positioned cleanly under the tabs.
            float separatorTranslation = hasTabLayout(appBarLayout) ? 0f : translationY;
            separator.setTranslationY(separatorTranslation); 
            
            appBarLayout.addView(separator);
        }

        appBarLayout.addOnOffsetChangedListener((layout, verticalOffset) -> {
            int scrollRange = layout.getTotalScrollRange();
            if (scrollRange <= 0) return;

            float percentage = (float) Math.abs(verticalOffset) / (float) scrollRange;
            percentage = Math.max(0f, Math.min(1f, percentage));

            int currentColor = ColorUtils.blendARGB(startColor, endColor, percentage);

            layout.setBackgroundColor(currentColor);
            activity.getWindow().setStatusBarColor(currentColor);

            float elevation = percentage * 8 * density;
            layout.setElevation(elevation);
        });
    }

    /**
     * Overloaded setup method for when a Toolbar reference is not available or handled separately.
     */
    public static void setup(Activity activity, AppBarLayout appBarLayout) {
        setup(activity, appBarLayout, null);
    }

    /**
     * Recursively traverses and styles all child views inside the header to match the theme color.
     */
    private static void styleHeaderContents(View view, int textColor) {
        if (view == null) return;

        // Skip specific views that have their own custom branding or styling
        int id = view.getId();
        if (id == R.id.btnSignIn || id == R.id.ivLogo || id == R.id.ivHeaderAvatar) {
            return;
        }

        if (view instanceof TabLayout) {
            TabLayout tabLayout = (TabLayout) view;
            boolean lightTheme = textColor == Color.parseColor("#0F172A");
            int normalColor = lightTheme ? Color.parseColor("#6B7280") : Color.parseColor("#A1A1AA");
            tabLayout.setTabTextColors(normalColor, textColor);
            return;
        }

        if (view instanceof TextView) {
            TextView tv = (TextView) view;
            boolean lightTheme = textColor == Color.parseColor("#0F172A");
            int viewId = tv.getId();
            boolean isSubtitle = false;
            if (viewId != -1 && viewId > 0) {
                try {
                    String entryName = tv.getContext().getResources().getResourceEntryName(viewId).toLowerCase();
                    if (entryName.contains("subtitle") || entryName.contains("status")) {
                        isSubtitle = true;
                    }
                } catch (Exception ignored) {}
            }

            if (isSubtitle) {
                tv.setTextColor(lightTheme ? Color.parseColor("#475569") : Color.parseColor("#94A3B8"));
            } else {
                tv.setTextColor(textColor);
            }
            tv.setTypeface(android.graphics.Typeface.create("sans-serif-medium", android.graphics.Typeface.BOLD));
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                tv.setLetterSpacing(0.01f);
            }

            // Center header titles programmatically
            float sizeInSp = tv.getTextSize() / tv.getContext().getResources().getDisplayMetrics().scaledDensity;
            boolean isTitleId = false;
            if (viewId != -1 && viewId > 0) {
                try {
                    String entryName = tv.getContext().getResources().getResourceEntryName(viewId).toLowerCase();
                    if (entryName.contains("title") || entryName.contains("header")) {
                        isTitleId = true;
                    }
                } catch (android.content.res.Resources.NotFoundException e) {
                    // Ignore
                }
            }

            if ("centered".equals(tv.getTag())) {
                // Skip centering/gravity modifications entirely for views explicitly tagged
            } else if (viewId == R.id.tvHeaderTitle || isTitleId || sizeInSp >= 15f) {
                tv.setGravity(android.view.Gravity.CENTER);
                ViewGroup.LayoutParams params = tv.getLayoutParams();
                if (params instanceof Toolbar.LayoutParams) {
                    Toolbar.LayoutParams lp = (Toolbar.LayoutParams) params;
                    lp.gravity = android.view.Gravity.CENTER;
                    tv.setLayoutParams(lp);
                } else if (params instanceof RelativeLayout.LayoutParams) {
                    RelativeLayout.LayoutParams lp = (RelativeLayout.LayoutParams) params;
                    lp.addRule(RelativeLayout.CENTER_IN_PARENT, RelativeLayout.TRUE);
                    tv.setLayoutParams(lp);
                } else if (params instanceof LinearLayout.LayoutParams) {
                    LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams) params;
                    lp.gravity = android.view.Gravity.CENTER;
                    tv.setLayoutParams(lp);
                }

                // Absolutely center the TextView if it is inside a Toolbar
                Toolbar toolbar = findParentToolbar(tv);
                if (toolbar != null) {
                    centerTextViewAbsolutely(tv, toolbar);
                }
            }
        } else if (view instanceof ImageView) {
            ImageView iv = (ImageView) view;
            iv.setImageTintList(android.content.res.ColorStateList.valueOf(textColor));
        } else if (view instanceof ImageButton) {
            ImageButton ib = (ImageButton) view;
            ib.setImageTintList(android.content.res.ColorStateList.valueOf(textColor));
        } else if (view instanceof Toolbar) {
            Toolbar toolbar = (Toolbar) view;
            if (toolbar.getNavigationIcon() != null) {
                toolbar.getNavigationIcon().setTint(textColor);
            }
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                if (toolbar.getOverflowIcon() != null) {
                    toolbar.getOverflowIcon().setTint(textColor);
                }
            }

            // Adjust insets to allow clean absolute centering of content
            toolbar.setContentInsetsAbsolute(0, 0);
            toolbar.setContentInsetsRelative(0, 0);
            toolbar.setClipChildren(false);
            toolbar.setClipToPadding(false);

            // Style the children group recursively
            for (int i = 0; i < toolbar.getChildCount(); i++) {
                styleHeaderContents(toolbar.getChildAt(i), textColor);
            }
        } else if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            group.setClipChildren(false);
            group.setClipToPadding(false);
            for (int i = 0; i < group.getChildCount(); i++) {
                styleHeaderContents(group.getChildAt(i), textColor);
            }
        }
    }

    /**
     * Helper to recursively check if the AppBarLayout already has a hairline separator
     */
    private static boolean hasSeparator(ViewGroup viewGroup, float density) {
        if (viewGroup == null) return false;
        for (int i = 0; i < viewGroup.getChildCount(); i++) {
            View child = viewGroup.getChildAt(i);
            if (child instanceof ViewGroup) {
                if (hasSeparator((ViewGroup) child, density)) {
                    return true;
                }
            } else {
                ViewGroup.LayoutParams lp = child.getLayoutParams();
                if (lp != null && lp.height > 0 && lp.height <= 3 * density) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Helper to recursively check if the ViewGroup has a TabLayout child
     */
    private static boolean hasTabLayout(ViewGroup viewGroup) {
        if (viewGroup == null) return false;
        for (int i = 0; i < viewGroup.getChildCount(); i++) {
            View child = viewGroup.getChildAt(i);
            if (child instanceof TabLayout) {
                return true;
            }
            if (child instanceof ViewGroup) {
                if (hasTabLayout((ViewGroup) child)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static Toolbar findParentToolbar(View view) {
        android.view.ViewParent parent = view.getParent();
        while (parent != null) {
            if (parent instanceof Toolbar) {
                return (Toolbar) parent;
            }
            parent = parent.getParent();
        }
        return null;
    }

    private static void centerTextViewAbsolutely(final TextView tv, final Toolbar toolbar) {
        if (tv == null || toolbar == null) return;
        
        if ("centered".equals(tv.getTag())) {
            return;
        }
        tv.setTag("centered");
        
        tv.addOnLayoutChangeListener(new View.OnLayoutChangeListener() {
            @Override
            public void onLayoutChange(View v, int left, int top, int right, int bottom,
                                       int oldLeft, int oldTop, int oldRight, int oldBottom) {
                int toolbarWidth = toolbar.getWidth();
                if (toolbarWidth > 0) {
                    int[] tvLocation = new int[2];
                    tv.getLocationInWindow(tvLocation);
                    int[] toolbarLocation = new int[2];
                    toolbar.getLocationInWindow(toolbarLocation);
                    
                    int tvLeftInToolbar = tvLocation[0] - toolbarLocation[0];
                    float currentTransX = tv.getTranslationX();
                    float rawTvLeft = tvLeftInToolbar - currentTransX;
                    
                    float tvWidth = tv.getWidth();
                    float desiredLeft = (toolbarWidth - tvWidth) / 2f;
                    float newTransX = desiredLeft - rawTvLeft;
                    
                    tv.setTranslationX(newTransX);
                }
            }
        });
        
        tv.requestLayout();
    }
}
