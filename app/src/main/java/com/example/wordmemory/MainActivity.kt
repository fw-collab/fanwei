package com.example.wordmemory

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.example.wordmemory.databinding.ActivityMainBinding

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

        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.navHostFragment) as NavHostFragment

        binding.bottomNav.setupWithNavController(navHostFragment.navController)
    }
}
