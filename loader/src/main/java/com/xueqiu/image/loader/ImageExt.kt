package com.xueqiu.image.loader

import android.widget.AbsListView
import android.widget.ListView
import androidx.recyclerview.widget.RecyclerView

fun ListView.setAutoPauseOnScroll() {
    this.setOnScrollListener(object : AbsListView.OnScrollListener {
        override fun onScrollStateChanged(view: AbsListView?, scrollState: Int) {
            if (scrollState == AbsListView.OnScrollListener.SCROLL_STATE_IDLE) {
                ImageLoader.resume()
            } else {
                ImageLoader.pause()
            }
        }

        override fun onScroll(view: AbsListView,
                              firstVisibleItem: Int,
                              visibleItemCount: Int,
                              totalItemCount: Int) {
            // do nothing
        }

    })
}

fun RecyclerView.setAutoPauseOnScroll() {
    this.addOnScrollListener(object : RecyclerView.OnScrollListener() {
        override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
            if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                ImageLoader.resume()
            } else {
                ImageLoader.pause()
            }
        }
    })
}