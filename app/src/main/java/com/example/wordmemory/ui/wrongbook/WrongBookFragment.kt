package com.example.wordmemory.ui.wrongbook

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.wordmemory.R
import com.example.wordmemory.databinding.FragmentWrongBookBinding
import kotlinx.coroutines.launch

/**
 * 错题本列表页。
 *
 * 列表按单词聚合错题记录，点击进入详情；移除操作在详情页完成。
 */
class WrongBookFragment : Fragment() {

    private var _binding: FragmentWrongBookBinding? = null
    private val binding get() = _binding!!

    private val viewModel: WrongBookViewModel by viewModels {
        WrongBookViewModel.factory(requireContext())
    }

    private lateinit var adapter: WrongBookAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentWrongBookBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = WrongBookAdapter { item ->
            findNavController().navigate(
                R.id.action_wrongBook_to_wrongDetail,
                bundleOf("wordId" to item.wordId),
            )
        }
        binding.wrongRecycler.layoutManager = LinearLayoutManager(requireContext())
        binding.wrongRecycler.adapter = adapter

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

    private fun render(state: WrongBookUiState) {
        adapter.submitList(state.items)
        binding.tvCount.text = getString(R.string.wrong_book_count, state.items.size)

        val hasItems = state.items.isNotEmpty()
        binding.wrongRecycler.isVisible = hasItems
        binding.loadingContainer.isVisible = state.isLoading && !hasItems
        binding.emptyContainer.isVisible = !state.isLoading && !hasItems
    }

    override fun onDestroyView() {
        binding.wrongRecycler.adapter = null
        _binding = null
        super.onDestroyView()
    }
}
