package com.example.wordmemory

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.example.wordmemory.databinding.ActivityMainBinding
import com.example.wordmemory.notification.NotificationHelper

/**
 * 主 App 导航骨架。
 *
 * 只负责：
 * - 绑定 NavHostFragment
 * - 让 BottomNavigationView 跟随 NavController
 *
 * 不在这里做数据库访问、不写业务逻辑。
 */
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 提前创建通知通道，保证通知设置页能看到“复习提醒”
        NotificationHelper.createChannel(this)

        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.navHostFragment) as NavHostFragment

        binding.bottomNav.setupWithNavController(navHostFragment.navController)
    }
}
