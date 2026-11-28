package com.example.wordmemory.ui.wrongbook

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.example.wordmemory.R
import com.example.wordmemory.databinding.FragmentWrongDetailBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 错题详情页。
 *
 * 展示单词、释义、答错次数和最近答错时间；
 * 支持把该单词的全部错题记录移出错题本。
 */
class WrongDetailFragment : Fragment() {

    private var _binding: FragmentWrongDetailBinding? = null
    private val binding get() = _binding!!

    private val wordId: Long by lazy {
        requireArguments().getLong("wordId", -1L)
    }

    private val viewModel: WrongDetailViewModel by viewModels {
        WrongDetailViewModel.factory(requireContext(), wordId)
    }

    private val timeFormatter = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
    private var navigatedBack = false
    private var isSharing = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentWrongDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }
        binding.btnRemove.setOnClickListener { confirmRemove() }
        binding.btnShare.setOnClickListener { shareCard() }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.uiState.collect { state -> render(state) }
                }
                launch {
                    viewModel.events.collect { message ->
                        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    private fun render(state: WrongDetailUiState) {
        if (state.removed && !navigatedBack) {
            navigatedBack = true
            findNavController().navigateUp()
            return
        }

        val item = state.item
        binding.contentContainer.isVisible = item != null
        binding.loadingContainer.isVisible = state.isLoading && item == null
        binding.emptyContainer.isVisible = !state.isLoading && item == null

        binding.btnRemove.isEnabled = item != null && !state.isRemoving
        binding.btnShare.isEnabled = item != null && !isSharing
        binding.btnShare.text = getString(
            if (isSharing) R.string.wrong_sharing else R.string.wrong_share
        )

        if (item != null) {
            binding.tvWord.text = item.spelling
            binding.tvPhonetic.text = item.phonetic.ifBlank { "—" }
            binding.tvMeaning.text = item.meaning
            binding.tvWrongCountValue.text = getString(
                R.string.wrong_count_badge,
                item.wrongCount,
            )
            binding.tvLastWrongValue.text = timeFormatter.format(Date(item.lastWrongAt))
        }
    }

    /**
     * 生成卡片位图并保存到 cache，然后交给系统分享。
     * 位图生成和文件写入都在后台线程，避免阻塞主线程。
     */
    private fun shareCard() {
        val item = viewModel.uiState.value.item ?: return
        if (isSharing) return

        val appContext = requireContext().applicationContext
        isSharing = true
        binding.btnShare.isEnabled = false
        binding.btnShare.text = getString(R.string.wrong_sharing)

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val bitmap = withContext(Dispatchers.Default) {
                    WrongCardGenerator.generate(item)
                }
                val uri = withContext(Dispatchers.IO) {
                    WrongCardGenerator.saveToCache(appContext, bitmap, item.wordId)
                }
                shareUri(uri)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Toast.makeText(
                    appContext,
                    R.string.wrong_share_failed,
                    Toast.LENGTH_SHORT,
                ).show()
            } finally {
                isSharing = false
                if (_binding != null) {
                    binding.btnShare.isEnabled = true
                    binding.btnShare.text = getString(R.string.wrong_share)
                }
            }
        }
    }

    private fun shareUri(uri: android.net.Uri) {
        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            clipData = ClipData.newRawUri("wrong_card", uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        try {
            startActivity(
                Intent.createChooser(sendIntent, getString(R.string.wrong_share_chooser))
            )
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(
                requireContext(),
                R.string.wrong_share_no_app,
                Toast.LENGTH_SHORT,
            ).show()
        } catch (e: Exception) {
            Toast.makeText(
                requireContext(),
                R.string.wrong_share_failed,
                Toast.LENGTH_SHORT,
            ).show()
        }
    }

    private fun confirmRemove() {
        val item = viewModel.uiState.value.item ?: return
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.wrong_remove)
            .setMessage(getString(R.string.wrong_remove_message, item.spelling))
            .setNegativeButton(R.string.word_cancel, null)
            .setPositiveButton(R.string.wrong_remove_confirm) { _: DialogInterface, _: Int ->
                viewModel.removeFromWrongBook()
            }
            .show()
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}
