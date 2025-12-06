package com.yalantis.ucrop.view

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import com.yalantis.ucrop.R
import androidx.core.view.isVisible


/**
 * Created by krokyze on 03/12/2017.
 */
class ControlLayout @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0) : HorizontalScrollView(context, attrs, defStyleAttr) {

    private val linearLayout: LinearLayout = LinearLayout(context)

    private val minChildWidth: Int by lazy { context.resources.getDimensionPixelSize(R.dimen.ucrop_control_min_width) }

    init {
        linearLayout.layoutParams = ViewGroup.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.MATCH_PARENT)
        linearLayout.orientation = LinearLayout.HORIZONTAL
        linearLayout.isBaselineAligned = false
        addView(linearLayout)
    }

    override fun addView(child: View?) {
        if (child == linearLayout) {
            super.addView(child)
        } else {
            linearLayout.addView(child)
        }
    }

    override fun addView(child: View?, index: Int) {
        if (child == linearLayout) {
            super.addView(child, index)
        } else {
            linearLayout.addView(child, index)
        }
    }

    override fun addView(child: View?, params: ViewGroup.LayoutParams?) {
        if (child == linearLayout) {
            super.addView(child, params)
        } else {
            linearLayout.addView(child, LinearLayout.LayoutParams(params))
        }
    }

    override fun addView(child: View?, index: Int, params: ViewGroup.LayoutParams?) {
        if (child == linearLayout) {
            super.addView(child, index, params)
        } else {
            linearLayout.addView(child, index, LinearLayout.LayoutParams(params))
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)

        val width = MeasureSpec.getSize(widthMeasureSpec)
        val childCount = (0 until linearLayout.childCount)
            .map { linearLayout.getChildAt(it) }
            .count { it.isVisible }

        var childWidth = minChildWidth

        if (childCount * minChildWidth < width) {
            childWidth = width / childCount
        }

        (0 until linearLayout.childCount)
                .map { linearLayout.getChildAt(it) }
                .forEach { it.layoutParams.width = childWidth }
    }
}