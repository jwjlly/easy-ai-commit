package com.layxy.easyaicommit.model

enum class ModelProtocol(val displayName: String) {
    OPENAI_COMPATIBLE("OpenAI Compatible"),
    ANTHROPIC_COMPATIBLE("Anthropic Compatible");

    override fun toString(): String = displayName

    companion object {
        fun fromStored(value: String): ModelProtocol = entries.firstOrNull { it.name == value }
            ?: OPENAI_COMPATIBLE
    }
}
