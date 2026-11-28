package com.example.wordmemory.core.ui

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import androidx.core.view.isVisible
import com.example.wordmemory.core.ui.databinding.ViewStateLayoutBinding

/**
 * 通用页面状态容器。
 *
 * XML 中把业务内容作为 StateLayout 的子 View，StateLayout 会在上层覆盖
 * 加载态 / 空态 / 错误态；切到成功态时直接隐藏覆盖层。
 */
class StateLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : FrameLayout(context, attrs, defStyleAttr) {

    private val stateBinding = ViewStateLayoutBinding.inflate(
        LayoutInflater.from(context),
        this,
        true,
    )

    init {
        stateBinding.stateOverlay.isVisible = false
        stateBinding.loadingState.isVisible = false
        stateBinding.emptyState.isVisible = false
        stateBinding.errorState.isVisible = false
    }
    override fun onFinishInflate() {
        super.onFinishInflate()
        // 覆盖层在构造函数中先添加，业务内容后添加；这里保证状态层始终在最上面
        stateBinding.stateOverlay.bringToFront()
    }

    fun showLoading(message: CharSequence? = null) {
        stateBinding.tvLoading.text = message ?: context.getString(R.string.core_ui_loading)
        stateBinding.loadingState.isVisible = true
        stateBinding.emptyState.isVisible = false
        stateBinding.errorState.isVisible = false
        showOverlay()
    }

    fun showContent() {
        stateBinding.stateOverlay.isVisible = false
        stateBinding.loadingState.isVisible = false
        stateBinding.emptyState.isVisible = false
        stateBinding.errorState.isVisible = false
    }

    fun showEmpty(
        title: CharSequence,
        message: CharSequence,
        actionText: CharSequence? = null,
        onAction: (() -> Unit)? = null,
    ) {
        stateBinding.tvEmptyTitle.text = title
        stateBinding.tvEmptyMessage.text = message
        stateBinding.btnEmptyAction.isVisible = !actionText.isNullOrBlank() && onAction != null
        stateBinding.btnEmptyAction.text = actionText
        stateBinding.btnEmptyAction.setOnClickListener { onAction?.invoke() }

        stateBinding.loadingState.isVisible = false
        stateBinding.emptyState.isVisible = true
        stateBinding.errorState.isVisible = false
        showOverlay()
    }

    fun showError(message: CharSequence, onRetry: () -> Unit) {
        showError(
            title = context.getString(R.string.core_ui_error_title),
            message = message,
            retryText = context.getString(R.string.core_ui_retry),
            onRetry = onRetry,
        )
    }

    fun showError(
        title: CharSequence,
        message: CharSequence,
        retryText: CharSequence,
        onRetry: () -> Unit,
    ) {
        stateBinding.tvErrorTitle.text = title
        stateBinding.tvErrorMessage.text = message
        stateBinding.btnRetry.text = retryText
        stateBinding.btnRetry.setOnClickListener { onRetry.invoke() }

        stateBinding.loadingState.isVisible = false
        stateBinding.emptyState.isVisible = false
        stateBinding.errorState.isVisible = true
        showOverlay()
    }

    private fun showOverlay() {
        stateBinding.stateOverlay.isVisible = true
        stateBinding.stateOverlay.bringToFront()
    }
}
