package com.yalantis.ucrop;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.PorterDuff;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.yalantis.ucrop.callback.BitmapCropCallback;
import com.yalantis.ucrop.model.AspectRatio;
import com.yalantis.ucrop.util.SelectedStateListDrawable;
import com.yalantis.ucrop.view.CropImageView;
import com.yalantis.ucrop.view.GestureCropImageView;
import com.yalantis.ucrop.view.OverlayView;
import com.yalantis.ucrop.view.TransformImageView;
import com.yalantis.ucrop.view.UCropView;
import com.yalantis.ucrop.view.widget.AspectRatioTextView;
import com.yalantis.ucrop.view.widget.HorizontalProgressWheelView;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import androidx.annotation.ColorInt;
import androidx.annotation.IdRes;
import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.transition.AutoTransition;
import androidx.transition.Transition;
import androidx.transition.TransitionManager;

import static android.app.Activity.RESULT_OK;

@SuppressWarnings("ConstantConditions")
public class UCropFragment extends Fragment {

    public static final int DEFAULT_COMPRESS_QUALITY = 90;
    public static final Bitmap.CompressFormat DEFAULT_COMPRESS_FORMAT = Bitmap.CompressFormat.JPEG;

    public static final int NONE = 0;
    public static final int SCALE = 1;
    public static final int ROTATE = 2;
    public static final int ALL = 3;

    @IntDef({NONE, SCALE, ROTATE, ALL})
    @Retention(RetentionPolicy.SOURCE)
    public @interface GestureTypes {
    }

    public static final String TAG = "UCropFragment";

    private static final long CONTROLS_ANIMATION_DURATION = 50;
    private static final int TABS_COUNT = 3;
    private static final int SCALE_WIDGET_SENSITIVITY_COEFFICIENT = 15000;
    private static final int ROTATE_WIDGET_SENSITIVITY_COEFFICIENT = 42;
    private static final int BRIGHTNESS_WIDGET_SENSITIVITY_COEFFICIENT = 3;
    private static final int CONTRAST_WIDGET_SENSITIVITY_COEFFICIENT = 4;
    private static final int SATURATION_WIDGET_SENSITIVITY_COEFFICIENT = 3;
    private static final int SHARPNESS_WIDGET_SENSITIVITY_COEFFICIENT = 400;
    private UCropFragmentCallback callback;

    private int mActiveControlsWidgetColor;

    @ColorInt
    private int mRootViewBackgroundColor;
    private int mLogoColor;

    private boolean mShowBottomControls;

    private Transition mControlsTransition;

    private UCropView mUCropView;
    private GestureCropImageView mGestureCropImageView;
    private OverlayView mOverlayView;
    private ViewGroup mWrapperStateAspectRatio, mWrapperStateRotate, mWrapperStateScale,
            mWrapperStateBrightness, mWrapperStateContrast, mWrapperStateSaturation,
            mWrapperStateSharpness;
    private ViewGroup mLayoutAspectRatio, mLayoutRotate, mLayoutScale,
            mLayoutBrightnessBar, mLayoutContrastBar, mLayoutSaturationBar,
            mLayoutSharpnessBar;
    private List<ViewGroup> mCropAspectRatioViews = new ArrayList<>();
    private TextView mTextViewRotateAngle, mTextViewScalePercent,
            mTextViewBrightness, mTextViewContrast, mTextViewSaturation,
            mTextViewSharpness;
    private View mBlockingView;

    private Bitmap.CompressFormat mCompressFormat = DEFAULT_COMPRESS_FORMAT;
    private int mCompressQuality = DEFAULT_COMPRESS_QUALITY;
    private int[] mAllowedGestures = new int[]{SCALE, ROTATE, ALL};

    static {
        AppCompatDelegate.setCompatVectorFromResourcesEnabled(true);
    }

    public static UCropFragment newInstance(Bundle uCrop) {
        UCropFragment fragment = new UCropFragment();
        fragment.setArguments(uCrop);
        return fragment;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (getParentFragment() instanceof UCropFragmentCallback)
            callback = (UCropFragmentCallback) getParentFragment();
        else if (context instanceof UCropFragmentCallback)
            callback = (UCropFragmentCallback) context;
        else
            throw new IllegalArgumentException(context.toString()
                    + " must implement UCropFragmentCallback");
    }

    public void setCallback(UCropFragmentCallback callback) {
        this.callback = callback;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.ucrop_fragment_photobox, container, false);

        Bundle args = getArguments();

        setupViews(rootView, args);
        setImageData(args);
        setInitialState();
        addBlockingView(rootView);

        return rootView;
    }


    public void setupViews(View view, Bundle args) {
        mActiveControlsWidgetColor = args.getInt(UCrop.Options.EXTRA_UCROP_COLOR_CONTROLS_WIDGET_ACTIVE, ContextCompat.getColor(getContext(), R.color.ucrop_color_widget_active));
        mLogoColor = args.getInt(UCrop.Options.EXTRA_UCROP_LOGO_COLOR, ContextCompat.getColor(getContext(), R.color.ucrop_color_default_logo));
        mShowBottomControls = !args.getBoolean(UCrop.Options.EXTRA_HIDE_BOTTOM_CONTROLS, false);
        mRootViewBackgroundColor = args.getInt(UCrop.Options.EXTRA_UCROP_ROOT_VIEW_BACKGROUND_COLOR, ContextCompat.getColor(getContext(), R.color.ucrop_color_crop_background));

        initiateRootViews(view);
        callback.loadingProgress(true);

        if (mShowBottomControls) {

            ViewGroup wrapper = view.findViewById(R.id.controls_wrapper);
            wrapper.setVisibility(View.VISIBLE);
            LayoutInflater.from(getContext()).inflate(R.layout.ucrop_controls, wrapper, true);

            mControlsTransition = new AutoTransition();
            mControlsTransition.setDuration(CONTROLS_ANIMATION_DURATION);

            mWrapperStateAspectRatio = view.findViewById(R.id.state_aspect_ratio);
            mWrapperStateAspectRatio.setOnClickListener(mStateClickListener);
            mWrapperStateRotate = view.findViewById(R.id.state_rotate);
            mWrapperStateRotate.setOnClickListener(mStateClickListener);
            mWrapperStateScale = view.findViewById(R.id.state_scale);
            mWrapperStateScale.setOnClickListener(mStateClickListener);

            mWrapperStateBrightness = view.findViewById(R.id.state_brightness);
            mWrapperStateBrightness.setOnClickListener(mStateClickListener);
            mWrapperStateContrast = view.findViewById(R.id.state_contrast);
            mWrapperStateContrast.setOnClickListener(mStateClickListener);
            mWrapperStateSaturation = view.findViewById(R.id.state_saturation);
            mWrapperStateSaturation.setOnClickListener(mStateClickListener);
            mWrapperStateSharpness = view.findViewById(R.id.state_sharpness);
            mWrapperStateSharpness.setOnClickListener(mStateClickListener);

            mLayoutAspectRatio = view.findViewById(R.id.layout_aspect_ratio);
            mLayoutRotate = view.findViewById(R.id.layout_rotate_wheel);
            mLayoutScale = view.findViewById(R.id.layout_scale_wheel);
            mLayoutBrightnessBar = view.findViewById(R.id.layout_brightness_bar);
            mLayoutContrastBar = view.findViewById(R.id.layout_contrast_bar);
            mLayoutSaturationBar = view.findViewById(R.id.layout_saturation_bar);
            mLayoutSharpnessBar = view.findViewById(R.id.layout_sharpness_bar);

            setupAspectRatioWidget(args, view);
            setupRotateWidget(view);
            setupScaleWidget(view);
            setupStatesWrapper(view);
            setupBrightnessWidget(view);
            setupContrastWidget(view);
            setupSaturationWidget(view);
            setupSharpnessWidget(view);
        } else {
            RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) view.findViewById(R.id.ucrop_frame).getLayoutParams();
            params.bottomMargin = 0;
            view.findViewById(R.id.ucrop_frame).requestLayout();
        }
    }

    private void setImageData(@NonNull Bundle bundle) {
        Uri inputUri = bundle.getParcelable(UCrop.EXTRA_INPUT_URI);
        Uri outputUri = bundle.getParcelable(UCrop.EXTRA_OUTPUT_URI);
        processOptions(bundle);

        if (inputUri != null && outputUri != null) {
            try {
                mGestureCropImageView.setImageUri(inputUri, outputUri);
            } catch (Exception e) {
                callback.onCropFinish(getError(e));
            }
        } else {
            callback.onCropFinish(getError(new NullPointerException(getString(R.string.ucrop_error_input_data_is_absent))));
        }
    }

    /**
     * This method extracts {@link com.yalantis.ucrop.UCrop.Options #optionsBundle} from incoming bundle
     * and setups fragment, {@link OverlayView} and {@link CropImageView} properly.
     */
    @SuppressWarnings("deprecation")
    private void processOptions(@NonNull Bundle bundle) {
        // Bitmap compression options
        String compressionFormatName = bundle.getString(UCrop.Options.EXTRA_COMPRESSION_FORMAT_NAME);
        Bitmap.CompressFormat compressFormat = null;
        if (!TextUtils.isEmpty(compressionFormatName)) {
            compressFormat = Bitmap.CompressFormat.valueOf(compressionFormatName);
        }
        mCompressFormat = (compressFormat == null) ? DEFAULT_COMPRESS_FORMAT : compressFormat;

        mCompressQuality = bundle.getInt(UCrop.Options.EXTRA_COMPRESSION_QUALITY, UCropActivity.DEFAULT_COMPRESS_QUALITY);

        // Gestures options
        int[] allowedGestures = bundle.getIntArray(UCrop.Options.EXTRA_ALLOWED_GESTURES);
        if (allowedGestures != null && allowedGestures.length == TABS_COUNT) {
            mAllowedGestures = allowedGestures;
        }

        // Crop image view options
        mGestureCropImageView.setMaxBitmapSize(bundle.getInt(UCrop.Options.EXTRA_MAX_BITMAP_SIZE, CropImageView.DEFAULT_MAX_BITMAP_SIZE));
        mGestureCropImageView.setMaxScaleMultiplier(bundle.getFloat(UCrop.Options.EXTRA_MAX_SCALE_MULTIPLIER, CropImageView.DEFAULT_MAX_SCALE_MULTIPLIER));
        mGestureCropImageView.setImageToWrapCropBoundsAnimDuration(bundle.getInt(UCrop.Options.EXTRA_IMAGE_TO_CROP_BOUNDS_ANIM_DURATION, CropImageView.DEFAULT_IMAGE_TO_CROP_BOUNDS_ANIM_DURATION));

        // Overlay view options
        mOverlayView.setFreestyleCropEnabled(bundle.getBoolean(UCrop.Options.EXTRA_FREE_STYLE_CROP, OverlayView.DEFAULT_FREESTYLE_CROP_MODE != OverlayView.FREESTYLE_CROP_MODE_DISABLE));

        mOverlayView.setDimmedColor(bundle.getInt(UCrop.Options.EXTRA_DIMMED_LAYER_COLOR, getResources().getColor(R.color.ucrop_color_default_dimmed)));
        mOverlayView.setCircleDimmedLayer(bundle.getBoolean(UCrop.Options.EXTRA_CIRCLE_DIMMED_LAYER, OverlayView.DEFAULT_CIRCLE_DIMMED_LAYER));

        mOverlayView.setShowCropFrame(bundle.getBoolean(UCrop.Options.EXTRA_SHOW_CROP_FRAME, OverlayView.DEFAULT_SHOW_CROP_FRAME));
        mOverlayView.setCropFrameColor(bundle.getInt(UCrop.Options.EXTRA_CROP_FRAME_COLOR, getResources().getColor(R.color.ucrop_color_default_crop_frame)));
        mOverlayView.setCropFrameStrokeWidth(bundle.getInt(UCrop.Options.EXTRA_CROP_FRAME_STROKE_WIDTH, getResources().getDimensionPixelSize(R.dimen.ucrop_default_crop_frame_stoke_width)));

        mOverlayView.setShowCropGrid(bundle.getBoolean(UCrop.Options.EXTRA_SHOW_CROP_GRID, OverlayView.DEFAULT_SHOW_CROP_GRID));
        mOverlayView.setCropGridRowCount(bundle.getInt(UCrop.Options.EXTRA_CROP_GRID_ROW_COUNT, OverlayView.DEFAULT_CROP_GRID_ROW_COUNT));
        mOverlayView.setCropGridColumnCount(bundle.getInt(UCrop.Options.EXTRA_CROP_GRID_COLUMN_COUNT, OverlayView.DEFAULT_CROP_GRID_COLUMN_COUNT));
        mOverlayView.setCropGridColor(bundle.getInt(UCrop.Options.EXTRA_CROP_GRID_COLOR, getResources().getColor(R.color.ucrop_color_default_crop_grid)));
        mOverlayView.setCropGridStrokeWidth(bundle.getInt(UCrop.Options.EXTRA_CROP_GRID_STROKE_WIDTH, getResources().getDimensionPixelSize(R.dimen.ucrop_default_crop_grid_stoke_width)));

        // Aspect ratio options
        float aspectRatioX = bundle.getFloat(UCrop.EXTRA_ASPECT_RATIO_X, -1);
        float aspectRatioY = bundle.getFloat(UCrop.EXTRA_ASPECT_RATIO_Y, -1);

        int aspectRationSelectedByDefault = bundle.getInt(UCrop.Options.EXTRA_ASPECT_RATIO_SELECTED_BY_DEFAULT, 0);
        ArrayList<AspectRatio> aspectRatioList = bundle.getParcelableArrayList(UCrop.Options.EXTRA_ASPECT_RATIO_OPTIONS);

        if (aspectRatioX >= 0 && aspectRatioY >= 0) {
            if (mWrapperStateAspectRatio != null) {
                mWrapperStateAspectRatio.setVisibility(View.GONE);
            }
            float targetAspectRatio = aspectRatioX / aspectRatioY;
            mGestureCropImageView.setTargetAspectRatio(Float.isNaN(targetAspectRatio) ? CropImageView.SOURCE_IMAGE_ASPECT_RATIO : targetAspectRatio);
        } else if (aspectRatioList != null && aspectRationSelectedByDefault < aspectRatioList.size()) {
            float targetAspectRatio = aspectRatioList.get(aspectRationSelectedByDefault).getAspectRatioX() / aspectRatioList.get(aspectRationSelectedByDefault).getAspectRatioY();
            mGestureCropImageView.setTargetAspectRatio(Float.isNaN(targetAspectRatio) ? CropImageView.SOURCE_IMAGE_ASPECT_RATIO : targetAspectRatio);
        } else {
            mGestureCropImageView.setTargetAspectRatio(CropImageView.SOURCE_IMAGE_ASPECT_RATIO);
        }

        // Result bitmap max size options
        int maxSizeX = bundle.getInt(UCrop.EXTRA_MAX_SIZE_X, 0);
        int maxSizeY = bundle.getInt(UCrop.EXTRA_MAX_SIZE_Y, 0);

        if (maxSizeX > 0 && maxSizeY > 0) {
            mGestureCropImageView.setMaxResultImageSizeX(maxSizeX);
            mGestureCropImageView.setMaxResultImageSizeY(maxSizeY);
        }

        mWrapperStateBrightness.setVisibility(bundle.getBoolean(UCrop.Options.EXTRA_BRIGHTNESS, true) ? View.VISIBLE : View.GONE);
        mWrapperStateContrast.setVisibility(bundle.getBoolean(UCrop.Options.EXTRA_CONTRAST, true) ? View.VISIBLE : View.GONE);
        mWrapperStateSaturation.setVisibility(bundle.getBoolean(UCrop.Options.EXTRA_SATURATION, true) ? View.VISIBLE : View.GONE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1 && bundle.getBoolean(UCrop.Options.EXTRA_SHARPNESS, true)) {
            mWrapperStateSharpness.setVisibility(View.VISIBLE);
        } else {
            mWrapperStateSharpness.setVisibility(View.GONE);
        }
    }

    private void initiateRootViews(View view) {
        mUCropView = view.findViewById(R.id.ucrop);
        mGestureCropImageView = mUCropView.getCropImageView();
        mOverlayView = mUCropView.getOverlayView();

        mGestureCropImageView.setTransformImageListener(mImageListener);

        ((ImageView) view.findViewById(R.id.image_view_logo)).setColorFilter(mLogoColor, PorterDuff.Mode.SRC_ATOP);

        view.findViewById(R.id.ucrop_frame).setBackgroundColor(mRootViewBackgroundColor);
    }

    private TransformImageView.TransformImageListener mImageListener = new TransformImageView.TransformImageListener() {
        @Override
        public void onRotate(float currentAngle) {
            setAngleText(currentAngle);
        }

        @Override
        public void onScale(float currentScale) {
            setScaleText(currentScale);
        }

        @Override
        public void onBrightness(float currentBrightness) {
            setBrightnessText(currentBrightness);
        }

        @Override
        public void onContrast(float currentContrast) {
            setContrastText(currentContrast);
        }

        @Override
        public void onSaturation(float currentSaturation) {
            setSaturationText(currentSaturation);
        }

        @Override
        public void onSharpness(float currentSharpness) {
            setSharpnessText(currentSharpness);
        }

        @Override
        public void onLoadComplete() {
            mUCropView.animate().alpha(1).setDuration(300).setInterpolator(new AccelerateInterpolator());
            mBlockingView.setClickable(false);
            callback.loadingProgress(false);
        }

        @Override
        public void onLoadFailure(@NonNull Exception e) {
            callback.onCropFinish(getError(e));
        }

    };

    private void setupStatesWrapper(View view) {
        ImageView stateScaleImageView = view.findViewById(R.id.image_view_state_scale);
        ImageView stateRotateImageView = view.findViewById(R.id.image_view_state_rotate);
        ImageView stateAspectRatioImageView = view.findViewById(R.id.image_view_state_aspect_ratio);
        ImageView stateBrightnessImageView = view.findViewById(R.id.image_view_state_brightness);
        ImageView stateContrastImageView = view.findViewById(R.id.image_view_state_contrast);
        ImageView stateSaturationImageView = view.findViewById(R.id.image_view_state_saturation);
        ImageView stateSharpnessImageView = view.findViewById(R.id.image_view_state_sharpness);

        stateScaleImageView.setImageDrawable(new SelectedStateListDrawable(stateScaleImageView.getDrawable(), mActiveControlsWidgetColor));
        stateRotateImageView.setImageDrawable(new SelectedStateListDrawable(stateRotateImageView.getDrawable(), mActiveControlsWidgetColor));
        stateAspectRatioImageView.setImageDrawable(new SelectedStateListDrawable(stateAspectRatioImageView.getDrawable(), mActiveControlsWidgetColor));
        stateBrightnessImageView.setImageDrawable(new SelectedStateListDrawable(stateBrightnessImageView.getDrawable(), mActiveControlsWidgetColor));
        stateContrastImageView.setImageDrawable(new SelectedStateListDrawable(stateContrastImageView.getDrawable(), mActiveControlsWidgetColor));
        stateSaturationImageView.setImageDrawable(new SelectedStateListDrawable(stateSaturationImageView.getDrawable(), mActiveControlsWidgetColor));
        stateSharpnessImageView.setImageDrawable(new SelectedStateListDrawable(stateSharpnessImageView.getDrawable(), mActiveControlsWidgetColor));
    }

    private void setupAspectRatioWidget(@NonNull Bundle bundle, View view) {
        int aspectRationSelectedByDefault = bundle.getInt(UCrop.Options.EXTRA_ASPECT_RATIO_SELECTED_BY_DEFAULT, 0);
        ArrayList<AspectRatio> aspectRatioList = bundle.getParcelableArrayList(UCrop.Options.EXTRA_ASPECT_RATIO_OPTIONS);

        if (aspectRatioList == null || aspectRatioList.isEmpty()) {
            aspectRationSelectedByDefault = 2;

            aspectRatioList = new ArrayList<>();
            aspectRatioList.add(new AspectRatio(null, 1, 1));
            aspectRatioList.add(new AspectRatio(null, 3, 4));
            aspectRatioList.add(new AspectRatio(getString(R.string.ucrop_label_original).toUpperCase(),
                    CropImageView.SOURCE_IMAGE_ASPECT_RATIO, CropImageView.SOURCE_IMAGE_ASPECT_RATIO));
            aspectRatioList.add(new AspectRatio(null, 3, 2));
            aspectRatioList.add(new AspectRatio(null, 16, 9));
        }

        LinearLayout wrapperAspectRatioList = view.findViewById(R.id.layout_aspect_ratio);

        FrameLayout wrapperAspectRatio;
        AspectRatioTextView aspectRatioTextView;
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT);
        lp.weight = 1;
        for (AspectRatio aspectRatio : aspectRatioList) {
            wrapperAspectRatio = (FrameLayout) getLayoutInflater().inflate(R.layout.ucrop_aspect_ratio, null);
            wrapperAspectRatio.setLayoutParams(lp);
            aspectRatioTextView = ((AspectRatioTextView) wrapperAspectRatio.getChildAt(0));
            aspectRatioTextView.setActiveColor(mActiveControlsWidgetColor);
            aspectRatioTextView.setAspectRatio(aspectRatio);

            wrapperAspectRatioList.addView(wrapperAspectRatio);
            mCropAspectRatioViews.add(wrapperAspectRatio);
        }

        mCropAspectRatioViews.get(aspectRationSelectedByDefault).setSelected(true);

        for (ViewGroup cropAspectRatioView : mCropAspectRatioViews) {
            cropAspectRatioView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mGestureCropImageView.setTargetAspectRatio(
                            ((AspectRatioTextView) ((ViewGroup) v).getChildAt(0)).getAspectRatio(v.isSelected()));
                    mGestureCropImageView.setImageToWrapCropBounds();
                    if (!v.isSelected()) {
                        for (ViewGroup cropAspectRatioView : mCropAspectRatioViews) {
                            cropAspectRatioView.setSelected(cropAspectRatioView == v);
                        }
                    }
                }
            });
        }
    }

    private void setupRotateWidget(View view) {
        mTextViewRotateAngle = view.findViewById(R.id.text_view_rotate);
        ((HorizontalProgressWheelView) view.findViewById(R.id.rotate_scroll_wheel))
                .setScrollingListener(new HorizontalProgressWheelView.ScrollingListener() {
                    @Override
                    public void onScroll(float delta, float totalDistance) {
                        mGestureCropImageView.postRotate(delta / ROTATE_WIDGET_SENSITIVITY_COEFFICIENT);
                    }

                    @Override
                    public void onScrollEnd() {
                        mGestureCropImageView.setImageToWrapCropBounds();
                    }

                    @Override
                    public void onScrollStart() {
                        mGestureCropImageView.cancelAllAnimations();
                    }
                });

        ((HorizontalProgressWheelView) view.findViewById(R.id.rotate_scroll_wheel)).setMiddleLineColor(mActiveControlsWidgetColor);


        view.findViewById(R.id.wrapper_reset_rotate).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                resetRotation();
            }
        });
        view.findViewById(R.id.wrapper_rotate_by_angle).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                rotateByAngle(90);
            }
        });
        setAngleTextColor(mActiveControlsWidgetColor);
    }

    private void setupScaleWidget(View view) {
        mTextViewScalePercent = view.findViewById(R.id.text_view_scale);
        ((HorizontalProgressWheelView) view.findViewById(R.id.scale_scroll_wheel))
                .setScrollingListener(new HorizontalProgressWheelView.ScrollingListener() {
                    @Override
                    public void onScroll(float delta, float totalDistance) {
                        if (delta > 0) {
                            mGestureCropImageView.zoomInImage(mGestureCropImageView.getCurrentScale()
                                    + delta * ((mGestureCropImageView.getMaxScale() - mGestureCropImageView.getMinScale()) / SCALE_WIDGET_SENSITIVITY_COEFFICIENT));
                        } else {
                            mGestureCropImageView.zoomOutImage(mGestureCropImageView.getCurrentScale()
                                    + delta * ((mGestureCropImageView.getMaxScale() - mGestureCropImageView.getMinScale()) / SCALE_WIDGET_SENSITIVITY_COEFFICIENT));
                        }
                    }

                    @Override
                    public void onScrollEnd() {
                        mGestureCropImageView.setImageToWrapCropBounds();
                    }

                    @Override
                    public void onScrollStart() {
                        mGestureCropImageView.cancelAllAnimations();
                    }
                });
        ((HorizontalProgressWheelView) view.findViewById(R.id.scale_scroll_wheel)).setMiddleLineColor(mActiveControlsWidgetColor);

        setScaleTextColor(mActiveControlsWidgetColor);
    }

    private void setupBrightnessWidget(View view) {
        mTextViewBrightness = view.findViewById(R.id.text_view_brightness);
        ((HorizontalProgressWheelView) view.findViewById(R.id.brightness_scroll_wheel))
                .setScrollingListener(new HorizontalProgressWheelView.ScrollingListener() {
                    @Override
                    public void onScroll(float delta, float totalDistance) {
                        mGestureCropImageView.postBrightness(delta / BRIGHTNESS_WIDGET_SENSITIVITY_COEFFICIENT);
                    }

                    @Override
                    public void onScrollEnd() {
                        mGestureCropImageView.setImageToWrapCropBounds();
                    }

                    @Override
                    public void onScrollStart() {
                        mGestureCropImageView.cancelAllAnimations();
                    }
                });

        ((HorizontalProgressWheelView) view.findViewById(R.id.brightness_scroll_wheel)).setMiddleLineColor(mActiveControlsWidgetColor);
    }

    private void setupContrastWidget(View view) {
        mTextViewContrast = view.findViewById(R.id.text_view_contrast);
        ((HorizontalProgressWheelView) view.findViewById(R.id.contrast_scroll_wheel))
                .setScrollingListener(new HorizontalProgressWheelView.ScrollingListener() {
                    @Override
                    public void onScroll(float delta, float totalDistance) {
                        mGestureCropImageView.postContrast(delta / CONTRAST_WIDGET_SENSITIVITY_COEFFICIENT);
                    }

                    @Override
                    public void onScrollEnd() {
                        mGestureCropImageView.setImageToWrapCropBounds();
                    }

                    @Override
                    public void onScrollStart() {
                        mGestureCropImageView.cancelAllAnimations();
                    }
                });

        ((HorizontalProgressWheelView) view.findViewById(R.id.contrast_scroll_wheel)).setMiddleLineColor(mActiveControlsWidgetColor);
    }

    private void setupSaturationWidget(View view) {
        mTextViewSaturation = view.findViewById(R.id.text_view_saturation);
        ((HorizontalProgressWheelView) view.findViewById(R.id.saturation_scroll_wheel))
                .setScrollingListener(new HorizontalProgressWheelView.ScrollingListener() {
                    @Override
                    public void onScroll(float delta, float totalDistance) {
                        mGestureCropImageView.postSaturation(delta / SATURATION_WIDGET_SENSITIVITY_COEFFICIENT);
                    }

                    @Override
                    public void onScrollEnd() {
                        mGestureCropImageView.setImageToWrapCropBounds();
                    }

                    @Override
                    public void onScrollStart() {
                        mGestureCropImageView.cancelAllAnimations();
                    }
                });

        ((HorizontalProgressWheelView) view.findViewById(R.id.saturation_scroll_wheel)).setMiddleLineColor(mActiveControlsWidgetColor);
    }

    private void setupSharpnessWidget(View view) {
        mTextViewSharpness = view.findViewById(R.id.text_view_sharpness);
        ((HorizontalProgressWheelView) view.findViewById(R.id.sharpness_scroll_wheel))
                .setScrollingListener(new HorizontalProgressWheelView.ScrollingListener() {
                    @Override
                    public void onScroll(float delta, float totalDistance) {
                        mGestureCropImageView.postSharpness(delta / SHARPNESS_WIDGET_SENSITIVITY_COEFFICIENT);
                    }

                    @Override
                    public void onScrollEnd() {
                        mGestureCropImageView.setImageToWrapCropBounds();
                    }

                    @Override
                    public void onScrollStart() {
                        mGestureCropImageView.cancelAllAnimations();
                    }
                });

        ((HorizontalProgressWheelView) view.findViewById(R.id.sharpness_scroll_wheel)).setMiddleLineColor(mActiveControlsWidgetColor);
    }

    private void setAngleText(float angle) {
        if (mTextViewRotateAngle != null) {
            mTextViewRotateAngle.setText(String.format(Locale.getDefault(), "%.1f°", angle));
        }
    }

    private void setAngleTextColor(int textColor) {
        if (mTextViewRotateAngle != null) {
            mTextViewRotateAngle.setTextColor(textColor);
        }
    }

    private void setScaleText(float scale) {
        if (mTextViewScalePercent != null) {
            mTextViewScalePercent.setText(String.format(Locale.getDefault(), "%d%%", (int) (scale * 100)));
        }
    }

    private void setBrightnessText(float brightness) {
        if (mTextViewBrightness != null) {
            mTextViewBrightness.setText(String.format(Locale.getDefault(), "%d", (int) brightness));
        }
    }

    private void setContrastText(float contrast) {
        if (mTextViewContrast != null) {
            mTextViewContrast.setText(String.format(Locale.getDefault(), "%d", (int) contrast));
        }
    }

    private void setSaturationText(float saturation) {
        if (mTextViewSaturation != null) {
            mTextViewSaturation.setText(String.format(Locale.getDefault(), "%d", (int) saturation));
        }
    }

    private void setSharpnessText(float sharpness) {
        if (mTextViewSharpness != null) {
            mTextViewSharpness.setText(String.format(Locale.getDefault(), "%d", (int) sharpness));
        }
    }

    private void setScaleTextColor(int textColor) {
        if (mTextViewScalePercent != null) {
            mTextViewScalePercent.setTextColor(textColor);
        }
    }

    private void resetRotation() {
        mGestureCropImageView.postRotate(-mGestureCropImageView.getCurrentAngle());
        mGestureCropImageView.setImageToWrapCropBounds();
    }

    private void rotateByAngle(int angle) {
        mGestureCropImageView.postRotate(angle);
        mGestureCropImageView.setImageToWrapCropBounds();
    }

    private final View.OnClickListener mStateClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (!v.isSelected()) {
                setWidgetState(v.getId());
            }
        }
    };

    private void setInitialState() {
        if (mShowBottomControls) {
            if (mWrapperStateAspectRatio.getVisibility() == View.VISIBLE) {
                setWidgetState(R.id.state_aspect_ratio);
            } else {
                setWidgetState(R.id.state_scale);
            }
        } else {
            setAllowedGestures(0);
        }
    }

    private void setWidgetState(@IdRes int stateViewId) {
        if (!mShowBottomControls) return;

        mWrapperStateAspectRatio.setSelected(stateViewId == R.id.state_aspect_ratio);
        mWrapperStateRotate.setSelected(stateViewId == R.id.state_rotate);
        mWrapperStateScale.setSelected(stateViewId == R.id.state_scale);
        mWrapperStateBrightness.setSelected(stateViewId == R.id.state_brightness);
        mWrapperStateContrast.setSelected(stateViewId == R.id.state_contrast);
        mWrapperStateSaturation.setSelected(stateViewId == R.id.state_saturation);
        mWrapperStateSharpness.setSelected(stateViewId == R.id.state_sharpness);

        mLayoutAspectRatio.setVisibility(stateViewId == R.id.state_aspect_ratio ? View.VISIBLE : View.GONE);
        mLayoutRotate.setVisibility(stateViewId == R.id.state_rotate ? View.VISIBLE : View.GONE);
        mLayoutScale.setVisibility(stateViewId == R.id.state_scale ? View.VISIBLE : View.GONE);
        mLayoutBrightnessBar.setVisibility(stateViewId == R.id.state_brightness ? View.VISIBLE : View.GONE);
        mLayoutContrastBar.setVisibility(stateViewId == R.id.state_contrast ? View.VISIBLE : View.GONE);
        mLayoutSaturationBar.setVisibility(stateViewId == R.id.state_saturation ? View.VISIBLE : View.GONE);
        mLayoutSharpnessBar.setVisibility(stateViewId == R.id.state_sharpness ? View.VISIBLE : View.GONE);

        changeSelectedTab(stateViewId);

        if (stateViewId == R.id.state_brightness || stateViewId == R.id.state_contrast
                || stateViewId == R.id.state_saturation || stateViewId == R.id.state_sharpness
                || stateViewId == R.id.state_scale) {
            setAllowedGestures(0);
        } else if (stateViewId == R.id.state_rotate) {
            setAllowedGestures(1);
        } else {
            setAllowedGestures(2);
        }
    }

    private void changeSelectedTab(int stateViewId) {
        if (getView() != null) {
            TransitionManager.beginDelayedTransition((ViewGroup) getView().findViewById(R.id.ucrop_photobox), mControlsTransition);
        }
        mWrapperStateScale.findViewById(R.id.text_view_scale).setVisibility(stateViewId == R.id.state_scale ? View.VISIBLE : View.GONE);
        mWrapperStateAspectRatio.findViewById(R.id.text_view_crop).setVisibility(stateViewId == R.id.state_aspect_ratio ? View.VISIBLE : View.GONE);
        mWrapperStateRotate.findViewById(R.id.text_view_rotate).setVisibility(stateViewId == R.id.state_rotate ? View.VISIBLE : View.GONE);
        mWrapperStateBrightness.findViewById(R.id.text_view_brightness).setVisibility(stateViewId == R.id.state_brightness ? View.VISIBLE : View.GONE);
        mWrapperStateContrast.findViewById(R.id.text_view_contrast).setVisibility(stateViewId == R.id.state_contrast ? View.VISIBLE : View.GONE);
        mWrapperStateSaturation.findViewById(R.id.text_view_saturation).setVisibility(stateViewId == R.id.state_saturation ? View.VISIBLE : View.GONE);
        mWrapperStateSharpness.findViewById(R.id.text_view_sharpness).setVisibility(stateViewId == R.id.state_sharpness ? View.VISIBLE : View.GONE);
    }

    private void setAllowedGestures(int tab) {
        mGestureCropImageView.setScaleEnabled(mAllowedGestures[tab] == ALL || mAllowedGestures[tab] == SCALE);
        mGestureCropImageView.setRotateEnabled(mAllowedGestures[tab] == ALL || mAllowedGestures[tab] == ROTATE);
    }

    /**
     * Adds view that covers everything below the Toolbar.
     * When it's clickable - user won't be able to click/touch anything below the Toolbar.
     * Need to block user input while loading and cropping an image.
     */
    private void addBlockingView(View view) {
        if (mBlockingView == null) {
            mBlockingView = new View(getContext());
            RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
            mBlockingView.setLayoutParams(lp);
            mBlockingView.setClickable(true);
        }

        ((RelativeLayout) view.findViewById(R.id.ucrop_photobox)).addView(mBlockingView);
    }

    public void cropAndSaveImage() {
        mBlockingView.setClickable(true);
        callback.loadingProgress(true);

        mGestureCropImageView.cropAndSaveImage(mCompressFormat, mCompressQuality, new BitmapCropCallback() {

            @Override
            public void onBitmapCropped(@NonNull Uri resultUri, int offsetX, int offsetY, int imageWidth, int imageHeight) {
                callback.onCropFinish(getResult(resultUri, mGestureCropImageView.getTargetAspectRatio(), offsetX, offsetY, imageWidth, imageHeight));
                callback.loadingProgress(false);
            }

            @Override
            public void onCropFailure(@NonNull Throwable t) {
                callback.onCropFinish(getError(t));
            }
        });
    }

    protected UCropResult getResult(Uri uri, float resultAspectRatio, int offsetX, int offsetY, int imageWidth, int imageHeight) {
        return new UCropResult(RESULT_OK, new Intent()
                .putExtra(UCrop.EXTRA_OUTPUT_URI, uri)
                .putExtra(UCrop.EXTRA_OUTPUT_CROP_ASPECT_RATIO, resultAspectRatio)
                .putExtra(UCrop.EXTRA_OUTPUT_IMAGE_WIDTH, imageWidth)
                .putExtra(UCrop.EXTRA_OUTPUT_IMAGE_HEIGHT, imageHeight)
                .putExtra(UCrop.EXTRA_OUTPUT_OFFSET_X, offsetX)
                .putExtra(UCrop.EXTRA_OUTPUT_OFFSET_Y, offsetY)
        );
    }

    protected UCropResult getError(Throwable throwable) {
        return new UCropResult(UCrop.RESULT_ERROR, new Intent().putExtra(UCrop.EXTRA_ERROR, throwable));
    }

    public class UCropResult {

        public int mResultCode;
        public Intent mResultData;

        public UCropResult(int resultCode, Intent data) {
            mResultCode = resultCode;
            mResultData = data;
        }

    }

}

