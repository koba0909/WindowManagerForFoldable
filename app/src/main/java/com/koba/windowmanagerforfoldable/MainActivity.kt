package com.koba.windowmanagerforfoldable

import android.annotation.SuppressLint
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.core.util.Consumer
import androidx.window.WindowLayoutInfo
import androidx.window.WindowManager
import com.koba.windowmanagerforfoldable.databinding.ActivityMainBinding
import java.util.concurrent.Executor

class MainActivity : AppCompatActivity() {
    private lateinit var wm: WindowManager
    private lateinit var binding: ActivityMainBinding
    private val layoutStateChangeCallback = Consumer<WindowLayoutInfo> { newLayoutInfo ->
        printLayoutStateChange(newLayoutInfo)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        wm = WindowManager(this).apply {
            registerLayoutChangeCallback(
                runOnUiThreadExecutor(),
                layoutStateChangeCallback
            )
        }

        initView()
    }

    @SuppressLint("SetTextI18n")
    private fun initView() {
        binding.run {
            windowMetrics.text =
                "CurrentWindowMetrics: ${wm.currentWindowMetrics.bounds.flattenToString()}\n" +
                        "MaximumWindowMetrics: ${wm.maximumWindowMetrics.bounds.flattenToString()}"
        }
    }

    private fun printLayoutStateChange(newLayoutInfo: WindowLayoutInfo) {
        binding.layoutChange.text = newLayoutInfo.toString()

        binding.configurationChanged.text = if(newLayoutInfo.displayFeatures.size > 0) {
            "Spanned across displays"
        }else {
            "One logic/physical display - unspanned"
        }
    }

    private fun runOnUiThreadExecutor(): Executor {
        val handler = Handler(Looper.getMainLooper())
        return Executor { handler.post(it) }
    }
}