package com.example.wordmemory.ui.wordlist

import android.Manifest
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.wordmemory.R
import com.example.wordmemory.data.local.entity.Word
import com.example.wordmemory.databinding.DialogWordEditBinding
import com.example.wordmemory.databinding.FragmentWordListBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch

/**
 * 单词本列表页。
 *
 * 负责：
 * - RecyclerView 展示 Room 中的单词
 * - 下拉刷新
 * - 新增/编辑弹窗
 * - 空态和加载态切换
 *
 * 不包含复习算法，不直接操作 DAO。
 */
class WordListFragment : Fragment() {

    private var _binding: FragmentWordListBinding? = null
    private val binding get() = _binding!!

    private val viewModel: WordListViewModel by viewModels {
        WordListViewModel.factory(requireContext())
    }

    private val reminderViewModel: ReminderViewModel by viewModels {
        ReminderViewModel.factory(requireContext())
    }

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        reminderViewModel.onPermissionResult(granted)
        if (!granted) {
            Toast.makeText(
                requireContext(),
                R.string.notification_permission_off,
                Toast.LENGTH_LONG,
            ).show()
        }
    }

    private lateinit var adapter: WordListAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentWordListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = WordListAdapter(
            onEdit = { word -> showWordDialog(word) },
            onDelete = { word -> confirmDelete(word) },
        )
        binding.wordRecycler.layoutManager = LinearLayoutManager(requireContext())
        binding.wordRecycler.adapter = adapter

        binding.swipeRefresh.setColorSchemeResources(
            R.color.blue_primary,
            R.color.orange_primary,
        )
        binding.swipeRefresh.setOnRefreshListener { viewModel.refresh() }
        binding.fabAdd.setOnClickListener { showWordDialog(null) }
        binding.btnEmptyAdd.setOnClickListener { showWordDialog(null) }
        binding.btnNotificationSettings.setOnClickListener { openNotificationSettings() }

        // 页面创建时确保每日提醒已调度；ViewModel + 唯一任务名双重防重复
        reminderViewModel.ensureReminderScheduled()
        askNotificationPermissionIfNeeded()

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
                launch {
                    reminderViewModel.uiState.collect { state -> renderReminder(state) }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        reminderViewModel.refresh()
    }

    private fun render(state: WordListUiState) {
        adapter.submitList(state.words)
        binding.tvCount.text = getString(R.string.word_list_count, state.words.size)

        val hasWords = state.words.isNotEmpty()
        binding.contentContainer.isVisible = hasWords
        binding.loadingContainer.isVisible = state.isLoading && !hasWords
        binding.emptyContainer.isVisible = !state.isLoading && !hasWords
        binding.swipeRefresh.isRefreshing = state.isRefreshing
    }

    /**
     * 首页提醒条：
     * - 通知权限开启且有到期单词：显示数字；
     * - 通知权限关闭：显示降级文案和“去开启”按钮；
     * - 无到期且权限正常：隐藏。
     */
    private fun renderReminder(state: ReminderUiState) {
        val show = !state.notificationsEnabled || state.dueCount > 0
        binding.reminderCard.isVisible = show

        val hasDue = state.dueCount > 0
        binding.tvReminderBadge.isVisible = hasDue
        binding.tvReminderBadge.text = if (state.dueCount > 99) "99+" else state.dueCount.toString()

        binding.tvReminderTitle.text = if (hasDue) {
            getString(R.string.reminder_due_format, state.dueCount)
        } else {
            getString(R.string.reminder_title)
        }
        binding.tvReminderFallback.isVisible = !state.notificationsEnabled
        binding.btnNotificationSettings.isVisible = !state.notificationsEnabled
    }

    /** Android 13+ 先解释再请求；同一次安装只弹一次解释框。 */
    private fun askNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return

        val granted = ContextCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.POST_NOTIFICATIONS,
        ) == PackageManager.PERMISSION_GRANTED
        if (granted) return

        val prefs = requireContext().getSharedPreferences(
            "reminder_prefs",
            Context.MODE_PRIVATE,
        )
        if (prefs.getBoolean("notification_permission_prompted", false)) return
        prefs.edit().putBoolean("notification_permission_prompted", true).apply()

        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.notification_permission_title)
            .setMessage(R.string.notification_permission_message)
            .setNegativeButton(R.string.notification_permission_later, null)
            .setPositiveButton(R.string.notification_permission_allow) { _, _ ->
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
            .show()
    }

    /** 权限被拒后的降级入口：直接打开本应用的系统通知设置。 */
    private fun openNotificationSettings() {
        val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                putExtra(Settings.EXTRA_APP_PACKAGE, requireContext().packageName)
            }
        } else {
            Intent(
                Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.parse("package:${requireContext().packageName}"),
            )
        }
        startActivity(intent)
    }

    /**
     * 新增或编辑单词。
     * existing 为 null 时是新增，否则是编辑。
     */
    private fun showWordDialog(existing: Word?) {
        val dialogBinding = DialogWordEditBinding.inflate(layoutInflater)

        if (existing != null) {
            dialogBinding.etSpelling.setText(existing.spelling)
            dialogBinding.etPhonetic.setText(existing.phonetic)
            dialogBinding.etMeaning.setText(existing.meaning)
        }

        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setTitle(
                if (existing == null) R.string.word_add_title else R.string.word_edit_title
            )
            .setView(dialogBinding.root)
            .setNegativeButton(R.string.word_cancel, null)
            .setPositiveButton(R.string.word_save, null)
            .create()

        dialog.setOnShowListener {
            dialog.getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener {
                val spelling = dialogBinding.etSpelling.text?.toString()?.trim().orEmpty()
                val phonetic = dialogBinding.etPhonetic.text?.toString()?.trim().orEmpty()
                val meaning = dialogBinding.etMeaning.text?.toString()?.trim().orEmpty()

                var valid = true
                if (spelling.isEmpty()) {
                    dialogBinding.tilSpelling.error = getString(R.string.word_error_spelling)
                    valid = false
                } else {
                    dialogBinding.tilSpelling.error = null
                }
                if (meaning.isEmpty()) {
                    dialogBinding.tilMeaning.error = getString(R.string.word_error_meaning)
                    valid = false
                } else {
                    dialogBinding.tilMeaning.error = null
                }
                if (!valid) return@setOnClickListener

                if (existing == null) {
                    viewModel.addWord(spelling, phonetic, meaning)
                } else {
                    viewModel.updateWord(
                        existing.copy(
                            spelling = spelling,
                            phonetic = phonetic,
                            meaning = meaning,
                        )
                    )
                }
                dialog.dismiss()
            }
        }
        dialog.show()
    }

    private fun confirmDelete(word: Word) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.word_delete_title)
            .setMessage(getString(R.string.word_delete_message, word.spelling))
            .setNegativeButton(R.string.word_cancel, null)
            .setPositiveButton(R.string.word_delete_confirm) { _, _ ->
                viewModel.deleteWord(word)
            }
            .show()
    }

    override fun onDestroyView() {
        binding.wordRecycler.adapter = null
        _binding = null
        super.onDestroyView()
    }
}
