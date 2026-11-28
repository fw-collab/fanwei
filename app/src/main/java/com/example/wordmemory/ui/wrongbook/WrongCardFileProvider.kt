package com.example.wordmemory.ui.wrongbook

import androidx.core.content.FileProvider

/**
 * 错题卡片专用 FileProvider。
 *
 * 使用独立 Provider 类，避免和 data_board_module 中 CSV 导出的 FileProvider
 * 在 Manifest 合并时发生 authorities / meta-data 冲突。
 */
class WrongCardFileProvider : FileProvider()
