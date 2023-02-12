package com.github.retrofitx.android.utils

import android.graphics.Rect
import android.view.View
import android.widget.TextView
import androidx.annotation.DimenRes
import androidx.recyclerview.widget.RecyclerView
import com.github.retrofitx.android.R

class RecyclerViewVerticalSpaceDecoration(
    @DimenRes val marginTopDp: Int = R.dimen.margin_default
): RecyclerView.ItemDecoration() {
    override fun getItemOffsets(
        outRect: Rect,
        view: View,
        parent: RecyclerView,
        state: RecyclerView.State
    ) {
        outRect.top += parent.resources.getDimensionPixelSize(marginTopDp)
    }
}

class RecyclerViewHorizontalSpaceDecoration(
    @DimenRes val margin: Int = R.dimen.margin_default
): RecyclerView.ItemDecoration() {
    override fun getItemOffsets(
        outRect: Rect,
        view: View,
        parent: RecyclerView,
        state: RecyclerView.State
    ) {
        outRect.offset(parent.resources.getDimensionPixelSize(margin), 0)
    }
}

fun TextView.setTextIfChanged(text: String) {
    if (getText().toString() != text) {
        setTextKeepState(text)
    }
}