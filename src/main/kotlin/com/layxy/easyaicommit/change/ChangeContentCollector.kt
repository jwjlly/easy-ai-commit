package com.layxy.easyaicommit.change

import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.project.Project
import com.intellij.openapi.vcs.FilePath
import com.intellij.openapi.vcs.VcsException
import com.intellij.openapi.vcs.changes.Change
import com.intellij.openapi.vcs.changes.ContentRevision
import com.intellij.openapi.vfs.VfsUtilCore
import java.nio.file.Paths
import kotlin.math.max

object ChangeContentCollector {
    fun collect(
        project: Project,
        changes: List<Change>,
        unversionedFiles: List<FilePath>,
        maxCharacters: Int,
        indicator: ProgressIndicator? = null,
    ): ChangePayload {
        val allPaths = buildList {
            changes.mapTo(this) { displayPath(project, it.afterRevision?.file ?: it.beforeRevision?.file) }
            unversionedFiles.mapTo(this) { displayPath(project, it) }
        }.distinct()

        val safeLimit = maxCharacters.coerceIn(5_000, 200_000)
        val perFileLimit = max(2_000, safeLimit / max(1, allPaths.size))
        val sections = mutableListOf<String>()
        var usedCharacters = 0
        var truncated = false

        fun appendSection(section: String) {
            val remaining = safeLimit - usedCharacters
            if (remaining <= 0) {
                truncated = true
                return
            }
            val value = if (section.length > remaining) {
                truncated = true
                truncateMiddle(section, remaining)
            } else {
                section
            }
            sections += value
            usedCharacters += value.length
        }

        changes.forEach { change ->
            indicator?.checkCanceled()
            val filePath = change.afterRevision?.file ?: change.beforeRevision?.file
            val path = displayPath(project, filePath)
            val before = readRevision(change.beforeRevision)
            val after = readRevision(change.afterRevision)
            val contentBudget = (perFileLimit - 160).coerceAtLeast(400)

            val body = when (change.type) {
                Change.Type.NEW -> "+++ Added file\n${renderContent(after, contentBudget)}"
                Change.Type.DELETED -> "--- Deleted file\n${renderContent(before, contentBudget)}"
                else -> {
                    val half = contentBudget / 2
                    "--- Before\n${renderContent(before, half)}\n+++ After\n${renderContent(after, half)}"
                }
            }
            appendSection("### ${change.type.name} $path\n$body")
        }

        unversionedFiles.forEach { filePath ->
            indicator?.checkCanceled()
            val path = displayPath(project, filePath)
            val virtualFile = filePath.virtualFile
            val content = when {
                virtualFile == null -> "[File is not available]"
                virtualFile.fileType.isBinary -> "[Binary file omitted]"
                else -> runCatching { VfsUtilCore.loadText(virtualFile) }
                    .getOrElse { "[Unable to read file: ${it.message.orEmpty()}]" }
            }
            appendSection("### NEW $path\n+++ Unversioned file\n${renderContent(content, perFileLimit)}")
        }

        if (truncated) {
            val marker = "\n\n[Change content truncated to $safeLimit characters]"
            if (usedCharacters + marker.length <= safeLimit) sections += marker
        }

        return ChangePayload(
            files = allPaths,
            content = sections.joinToString("\n\n"),
            truncated = truncated,
        )
    }

    private fun readRevision(revision: ContentRevision?): String? {
        if (revision == null) return null
        return try {
            revision.content
        } catch (error: VcsException) {
            "[Unable to read revision: ${error.message.orEmpty()}]"
        }
    }

    private fun renderContent(content: String?, limit: Int): String = when {
        content == null -> "[Binary or unavailable content omitted]"
        content.isEmpty() -> "[Empty file]"
        else -> truncateMiddle(content, limit)
    }

    internal fun truncateMiddle(value: String, limit: Int): String {
        if (value.length <= limit) return value
        if (limit < 80) return value.take(limit)
        val marker = "\n...[truncated]...\n"
        val remaining = limit - marker.length
        val head = remaining * 2 / 3
        return value.take(head) + marker + value.takeLast(remaining - head)
    }

    private fun displayPath(project: Project, filePath: FilePath?): String {
        if (filePath == null) return "unknown"
        val absolute = filePath.ioFile.toPath().normalize()
        val base = project.basePath?.let { runCatching { Paths.get(it).normalize() }.getOrNull() }
        return if (base != null && absolute.startsWith(base)) {
            base.relativize(absolute).toString()
        } else {
            filePath.path
        }
    }
}
