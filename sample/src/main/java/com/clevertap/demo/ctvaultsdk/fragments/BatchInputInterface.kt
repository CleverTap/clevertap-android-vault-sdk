package com.clevertap.demo.ctvaultsdk.fragments

import com.clevertap.demo.ctvaultsdk.MainActivity

/**
 * Interface for batch input fragments to communicate with MainActivity
 */
interface BatchInputInterface {
    /**
     * Called when data type changes
     */
    fun onDataTypeChanged(dataType: MainActivity.DataType)

    /**
     * Gets the current batch data for tokenization
     * @return List of values as Any, or null if no data available
     */
    fun getBatchData(): List<Any>?

    /**
     * Gets tokens for batch detokenization
     * @return List of tokens, or null if no tokens available
     */
    fun getTokensForDetokenization(): List<String>?

    /**
     * Called when the tab becomes visible
     */
    fun onTabSelected()

    /**
     * Called when the tab becomes invisible
     */
    fun onTabUnselected()

    /**
     * Clears all input data in this tab
     */
    fun clearData()
}