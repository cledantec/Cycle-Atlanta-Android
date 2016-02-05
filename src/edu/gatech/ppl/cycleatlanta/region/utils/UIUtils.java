/*
 * Copyright (C) 2010-2013 Paul Watts (paulcwatts@gmail.com)
 * and individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package edu.gatech.ppl.cycleatlanta.region.utils;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.os.Build;
import android.view.View;

/**
 * A class containing utility methods related to the user interface
 */
public final class UIUtils {

    private static final String TAG = "UIHelp";

    /**
     * Returns true if the activity is still active and dialogs can be managed (i.e., displayed
     * or dismissed), or false if it is
     * not
     *
     * @param activity Activity to check for displaying/dismissing a dialog
     * @return true if the activity is still active and dialogs can be managed, or false if it is
     * not
     */
    public static boolean canManageDialog(Activity activity) {
        if (activity == null) {
            return false;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            return !activity.isFinishing() && !activity.isDestroyed();
        } else {
            return !activity.isFinishing();
        }
    }

    /**
     * Returns true if the context is an Activity and is still active and dialogs can be managed
     * (i.e., displayed or dismissed) OR the context is not an Activity, or false if the Activity
     * is
     * no longer active.
     *
     * NOTE: We really shouldn't display dialogs from a Service - a notification is a better way
     * to communicate with the user.
     *
     * @param context Context to check for displaying/dismissing a dialog
     * @return true if the context is an Activity and is still active and dialogs can be managed
     * (i.e., displayed or dismissed) OR the context is not an Activity, or false if the Activity
     * is
     * no longer active
     */
    public static boolean canManageDialog(Context context) {
        if (context == null) {
            return false;
        }

        if (context instanceof Activity) {
            return canManageDialog((Activity) context);
        } else {
            // We really shouldn't be displaying dialogs from a Service, but if for some reason we
            // need to do this, we don't have any way of checking whether its possible
            return true;
        }
    }

    /**
     * Returns true if the API level supports animating Views using ViewPropertyAnimator, false if
     * it doesn't
     *
     * @return true if the API level supports animating Views using ViewPropertyAnimator, false if
     * it doesn't
     */
    public static boolean canAnimateViewModern() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR1;
    }

    /**
     * Returns true if the API level supports canceling existing animations via the
     * ViewPropertyAnimator, and false if it does not
     *
     * @return true if the API level supports canceling existing animations via the
     * ViewPropertyAnimator, and false if it does not
     */
    public static boolean canCancelAnimation() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH;
    }

    /**
     * Returns true if the API level supports our Arrival Info Style B (sort by route) views, false
     * if it does not.  See #350 and #275.
     *
     * @return true if the API level supports our Arrival Info Style B (sort by route) views, false
     * if it does not
     */
    public static boolean canSupportArrivalInfoStyleB() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH;
    }

    /**
     * Shows a view, using animation if the platform supports it
     *
     * @param v                 View to show
     * @param animationDuration duration of animation
     */
    @TargetApi(14)
    public static void showViewWithAnimation(final View v, int animationDuration) {
        // If we're on a legacy device, show the view without the animation
        if (!canAnimateViewModern()) {
            showViewWithoutAnimation(v);
            return;
        }

        if (v.getVisibility() == View.VISIBLE && v.getAlpha() == 1) {
            // View is already visible and not transparent, return without doing anything
            return;
        }

        v.clearAnimation();
        if (canCancelAnimation()) {
            v.animate().cancel();
        }

        if (v.getVisibility() != View.VISIBLE) {
            // Set the content view to 0% opacity but visible, so that it is visible
            // (but fully transparent) during the animation.
            v.setAlpha(0f);
            v.setVisibility(View.VISIBLE);
        }

        // Animate the content view to 100% opacity, and clear any animation listener set on the view.
        v.animate()
                .alpha(1f)
                .setDuration(animationDuration)
                .setListener(null);
    }

    /**
     * Shows a view without using animation
     *
     * @param v View to show
     */
    public static void showViewWithoutAnimation(final View v) {
        if (v.getVisibility() == View.VISIBLE) {
            // View is already visible, return without doing anything
            return;
        }
        v.setVisibility(View.VISIBLE);
    }

    /**
     * Hides a view, using animation if the platform supports it
     *
     * @param v                 View to hide
     * @param animationDuration duration of animation
     */
    @TargetApi(14)
    public static void hideViewWithAnimation(final View v, int animationDuration) {
        // If we're on a legacy device, hide the view without the animation
        if (!canAnimateViewModern()) {
            hideViewWithoutAnimation(v);
            return;
        }

        if (v.getVisibility() == View.GONE) {
            // View is already gone, return without doing anything
            return;
        }

        v.clearAnimation();
        if (canCancelAnimation()) {
            v.animate().cancel();
        }

        // Animate the view to 0% opacity. After the animation ends, set its visibility to GONE as
        // an optimization step (it won't participate in layout passes, etc.)
        v.animate()
                .alpha(0f)
                .setDuration(animationDuration)
                .setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        v.setVisibility(View.GONE);
                    }
                });
    }

    /**
     * Hides a view without using animation
     *
     * @param v View to hide
     */
    public static void hideViewWithoutAnimation(final View v) {
        if (v.getVisibility() == View.GONE) {
            // View is already gone, return without doing anything
            return;
        }
        // Hide the view without animation
        v.setVisibility(View.GONE);
    }
}
