package com.example.wordmemory.core.ui

import android.content.Context
import android.widget.Toast
import androidx.annotation.StringRes

/**
 * 统一 Toast 入口，所有页面使用同一时长和应用 Context，避免持有 Activity。
 */
object ToastUtils {

    fun show(context: Context, message: CharSequence) {
        Toast.makeText(context.applicationContext, message, Toast.LENGTH_SHORT).show()
    }

    fun show(context: Context, @StringRes messageRes: Int) {
        Toast.makeText(context.applicationContext, messageRes, Toast.LENGTH_SHORT).show()
    }
}
