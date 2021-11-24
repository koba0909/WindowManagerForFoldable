package com.koba.windowmanagerforfoldable

import android.annotation.SuppressLint
import android.graphics.Rect
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.TypedValue
import android.view.View
import androidx.constraintlayout.widget.ConstraintSet
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
            alignViewToDeviceFeatureBoundaries(newLayoutInfo)
            "Spanned across displays"
        }else {
            "One logic/physical display - unspanned"
        }
    }

    private fun alignViewToDeviceFeatureBoundaries(newLayoutInfo: WindowLayoutInfo) {
        val constraintLayout = binding.constraintLayout
        val set = ConstraintSet()
        set.clone(constraintLayout)

        val rect = newLayoutInfo.displayFeatures[0].bounds

        val height = (rect.bottom - rect.top).coerceAtLeast(2)
        val width = (rect.right - rect.left).coerceAtLeast(2)

        // 접히는 부분 표시를 위한 사이즈, 제약 설정
        set.constrainHeight(
            R.id.device_feature,
            height
        )

        set.constrainWidth(
            R.id.device_feature,
            width
        )

        set.connect(
            R.id.device_feature, ConstraintSet.START,
            R.id.constraint_layout, ConstraintSet.START, 0
        )

        set.connect(
            R.id.device_feature, ConstraintSet.TOP,
            R.id.constraint_layout, ConstraintSet.TOP, 0
        )

        // rect.top == 0 인 경우 수직으로 접히는 일반적인 폴더블(ex. 갤럭시 폴더블)
        // 아닌 경우는 수평적으로 나뉜 폴더블(ex. 갤럭시 z플립)
        if (rect.top == 0) {
            set.setMargin(R.id.device_feature, ConstraintSet.START, rect.left)

            set.connect(
                R.id.layout_change, ConstraintSet.END,
                R.id.device_feature, ConstraintSet.START, 0
            )
        } else {
            val statusBarHeight = calculateStatusBarHeight()
            val toolBarHeight = calculateToolbarHeight()

            set.setMargin(
                R.id.device_feature, ConstraintSet.TOP,
                rect.top - statusBarHeight - toolBarHeight
            )
            set.connect(
                R.id.layout_change, ConstraintSet.TOP,
                R.id.device_feature, ConstraintSet.BOTTOM, 0
            )
        }

        set.setVisibility(R.id.device_feature, View.VISIBLE)
        set.applyTo(constraintLayout)
    }

    private fun calculateToolbarHeight(): Int {
        val typedValue = TypedValue()
        return if (theme.resolveAttribute(android.R.attr.actionBarSize, typedValue, true)) {
            TypedValue.complexToDimensionPixelSize(typedValue.data, resources.displayMetrics)
        } else {
            0
        }
    }

    private fun calculateStatusBarHeight(): Int {
        val rect = Rect()
        window.decorView.getWindowVisibleDisplayFrame(rect)
        return rect.top
    }

    private fun runOnUiThreadExecutor(): Executor {
        val handler = Handler(Looper.getMainLooper())
        return Executor { handler.post(it) }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        wm.unregisterLayoutChangeCallback(layoutStateChangeCallback)
    }
}