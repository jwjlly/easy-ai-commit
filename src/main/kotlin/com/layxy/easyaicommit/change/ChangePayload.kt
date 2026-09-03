package com.layxy.easyaicommit.change

data class ChangePayload(
    val files: List<String>,
    val content: String,
    val truncated: Boolean,
)

