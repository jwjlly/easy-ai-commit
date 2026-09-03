package com.layxy.easyaicommit

import java.util.Properties
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class EasyAiCommitBundleTest {
    @Test
    fun `every localized bundle contains all default keys`() {
        val defaults = load("messages/EasyAiCommitBundle.properties")
        val localizedBundles = listOf(
            "messages/EasyAiCommitBundle_zh_CN.properties",
            "messages/EasyAiCommitBundle_ja.properties",
            "messages/EasyAiCommitBundle_ko.properties",
        )

        localizedBundles.forEach { path ->
            val localized = load(path)
            assertEquals(defaults.stringPropertyNames(), localized.stringPropertyNames(), path)
            assertTrue(localized.values.all { it.toString().isNotBlank() }, path)
        }
    }

    @Test
    fun `auto output labels contain only the resolved language`() {
        val expected = mapOf(
            "messages/EasyAiCommitBundle.properties" to "Auto ({0})",
            "messages/EasyAiCommitBundle_zh_CN.properties" to "自动（{0}）",
            "messages/EasyAiCommitBundle_ja.properties" to "自動（{0}）",
            "messages/EasyAiCommitBundle_ko.properties" to "자동({0})",
        )

        expected.forEach { (path, value) ->
            assertEquals(value, load(path).getProperty("output.language.auto"), path)
        }
    }

    @Test
    fun `compatible protocol terms remain English in every bundle`() {
        val bundles = mapOf(
            "messages/EasyAiCommitBundle.properties" to "Custom",
            "messages/EasyAiCommitBundle_zh_CN.properties" to "自定义",
            "messages/EasyAiCommitBundle_ja.properties" to "カスタム",
            "messages/EasyAiCommitBundle_ko.properties" to "사용자 지정",
        )

        bundles.forEach { (path, customLabel) ->
            val bundle = load(path)
            assertFalse(bundle.containsKey("protocol.openaiCompatible"), path)
            assertFalse(bundle.containsKey("protocol.anthropicCompatible"), path)
            assertFalse(bundle.containsKey("provider.custom.openaiCompatible"), path)
            assertFalse(bundle.containsKey("provider.custom.anthropicCompatible"), path)
            assertEquals(customLabel, bundle.getProperty("provider.custom"), path)
            assertTrue(
                bundle.getProperty("client.error.openaiMissingMessage").contains("OpenAI Compatible"),
                path,
            )
            assertTrue(
                bundle.getProperty("client.error.anthropicMissingContent")
                    .contains("Anthropic Compatible"),
                path,
            )
        }
    }

    @Test
    fun `action tooltip describes ai assisted commit message generation in every bundle`() {
        val expected = mapOf(
            "messages/EasyAiCommitBundle.properties" to "Generate a commit message with AI",
            "messages/EasyAiCommitBundle_zh_CN.properties" to "AI辅助生成提交信息",
            "messages/EasyAiCommitBundle_ja.properties" to
                "AI を使用してコミットメッセージを生成します",
            "messages/EasyAiCommitBundle_ko.properties" to
                "AI를 사용하여 커밋 메시지를 생성합니다",
        )

        expected.forEach { (path, tooltip) ->
            assertEquals(
                tooltip,
                load(path).getProperty(
                    "action.com.layxy.easyaicommit.GenerateCommitMessage.description",
                ),
                path,
            )
        }
    }

    private fun load(path: String): Properties = Properties().also { properties ->
        val stream = requireNotNull(javaClass.classLoader.getResourceAsStream(path)) { path }
        stream.bufferedReader(Charsets.UTF_8).use(properties::load)
    }
}
