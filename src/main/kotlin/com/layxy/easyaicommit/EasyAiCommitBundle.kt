package com.layxy.easyaicommit

import com.intellij.DynamicBundle
import org.jetbrains.annotations.Nls
import org.jetbrains.annotations.NonNls
import org.jetbrains.annotations.PropertyKey

@NonNls
private const val BUNDLE = "messages.EasyAiCommitBundle"

object EasyAiCommitBundle {
    private val instance = DynamicBundle(EasyAiCommitBundle::class.java, BUNDLE)

    @Nls
    fun message(
        @PropertyKey(resourceBundle = BUNDLE) key: String,
        vararg params: Any,
    ): String = instance.getMessage(key, *params)
}
