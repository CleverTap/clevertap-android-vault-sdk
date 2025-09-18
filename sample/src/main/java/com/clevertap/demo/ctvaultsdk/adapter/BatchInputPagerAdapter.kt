package com.clevertap.demo.ctvaultsdk.adapter

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.clevertap.demo.ctvaultsdk.fragments.ImportDataFragment
import com.clevertap.demo.ctvaultsdk.fragments.ManualInputFragment
import com.clevertap.demo.ctvaultsdk.fragments.QuickTestFragment

/**
 * Adapter for the batch input tabs
 */
class BatchInputPagerAdapter(fragmentActivity: FragmentActivity) : FragmentStateAdapter(fragmentActivity) {

    companion object {
        const val TAB_QUICK_TEST = 0
        const val TAB_MANUAL_INPUT = 1
        const val TAB_IMPORT = 2
        const val TAB_COUNT = 3

        val TAB_TITLES = arrayOf(
            "⚡ Quick Test",
            "✏️ Manual Input",
            "📂 Import"
        )
    }

    override fun getItemCount(): Int = TAB_COUNT

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            TAB_QUICK_TEST -> QuickTestFragment()
            TAB_MANUAL_INPUT -> ManualInputFragment()
            TAB_IMPORT -> ImportDataFragment()
            else -> throw IllegalArgumentException("Invalid tab position: $position")
        }
    }
}