package com.example.wordmemory.ui.wrongbook

import android.content.DialogInterface
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
import kotlinx.coroutines.launch
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
