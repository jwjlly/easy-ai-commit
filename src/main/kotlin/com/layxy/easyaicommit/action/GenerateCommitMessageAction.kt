package com.layxy.easyaicommit.action

import com.layxy.easyaicommit.EasyAiCommitBundle.message
import com.layxy.easyaicommit.change.ChangeContentCollector
import com.layxy.easyaicommit.client.CompatibleModelClient
import com.layxy.easyaicommit.icons.EasyAiCommitIcons
import com.layxy.easyaicommit.prompt.PromptTemplate
import com.layxy.easyaicommit.settings.ApiKeyStore
import com.layxy.easyaicommit.settings.EasyAiCommitSettingsService
import com.intellij.ide.ActivityTracker
import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.vcs.VcsDataKeys
import com.intellij.ui.AnimatedIcon
import com.intellij.vcs.commit.CommitWorkflowUi
import java.util.concurrent.ConcurrentHashMap

class GenerateCommitMessageAction : AnAction(), DumbAware {
    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

    override fun update(event: AnActionEvent) {
        val project = event.project
        val workflowUi = event.getData(VcsDataKeys.COMMIT_WORKFLOW_UI)
        val generating = project != null && generatingProjects.contains(project)

        event.presentation.isVisible = project != null && workflowUi != null
        event.presentation.isEnabled = project != null && workflowUi != null && !generating
        event.presentation.icon = if (generating) AnimatedIcon.Default.INSTANCE else EasyAiCommitIcons.Logo
        event.presentation.description = if (generating) {
            message("action.com.layxy.easyaicommit.GenerateCommitMessage.generating")
        } else {
            message("action.com.layxy.easyaicommit.GenerateCommitMessage.description")
        }
    }

    override fun actionPerformed(event: AnActionEvent) {
        val project = event.project ?: return
        val workflowUi = event.getData(VcsDataKeys.COMMIT_WORKFLOW_UI) ?: return
        if (!generatingProjects.add(project)) return

        val includedChanges = workflowUi.getIncludedChanges()
        val includedUnversioned = workflowUi.getIncludedUnversionedFiles()
        val hasIncludedFiles = includedChanges.isNotEmpty() || includedUnversioned.isNotEmpty()
        val changes = if (hasIncludedFiles) includedChanges else workflowUi.getDisplayedChanges()
        val unversioned = if (hasIncludedFiles) includedUnversioned else workflowUi.getDisplayedUnversionedFiles()

        if (changes.isEmpty() && unversioned.isEmpty()) {
            finishGeneration(project, workflowUi)
            notifyError(
                project,
                message("generation.error.noChanges.title"),
                message("generation.error.noChanges.content"),
            )
            return
        }

        val settings = EasyAiCommitSettingsService.getInstance().snapshot()
        val profile = settings.defaultProfile
        if (profile == null) {
            finishGeneration(project, workflowUi)
            notifyError(
                project,
                message("generation.error.noModel.title"),
                message("generation.error.noModel.content"),
            )
            return
        }

        workflowUi.commitMessageUi.startLoading()
        ActivityTracker.getInstance().inc()

        object : Task.Backgroundable(project, message("generation.task.title"), true) {
            private var generatedMessage: String = ""

            override fun run(indicator: ProgressIndicator) {
                indicator.isIndeterminate = true
                indicator.text = message("generation.progress.readingChanges")
                indicator.checkCanceled()

                val apiKey = ApiKeyStore.load(profile.id)
                if (apiKey.isBlank()) {
                    error(message("generation.error.apiKeyMissing", profile.name))
                }

                val payload = ChangeContentCollector.collect(
                    project = project,
                    changes = changes,
                    unversionedFiles = unversioned,
                    maxCharacters = settings.maxDiffCharacters,
                    indicator = indicator,
                )
                indicator.checkCanceled()
                indicator.text = message("generation.progress.calling", profile.name)

                val prompt = PromptTemplate.render(profile, payload.files, payload.content)
                generatedMessage = CompatibleModelClient.generate(profile, apiKey, prompt, indicator)
            }

            override fun onSuccess() {
                if (project.isDisposed) return
                workflowUi.commitMessageUi.setText(generatedMessage)
                workflowUi.commitMessageUi.focus()
            }

            override fun onThrowable(error: Throwable) {
                if (project.isDisposed) return
                notifyError(
                    project,
                    message("generation.error.failed.title"),
                    error.message ?: message("generation.error.requestFailed"),
                )
            }

            override fun onFinished() {
                finishGeneration(project, workflowUi)
            }
        }.queue()
    }

    private fun finishGeneration(project: Project, workflowUi: CommitWorkflowUi) {
        runCatching { workflowUi.commitMessageUi.stopLoading() }
        generatingProjects.remove(project)
        ActivityTracker.getInstance().inc()
    }

    private fun notifyError(project: Project, title: String, content: String) {
        NotificationGroupManager.getInstance()
            .getNotificationGroup("Easy AI Commit")
            .createNotification(title, content, NotificationType.ERROR)
            .notify(project)
    }

    companion object {
        private val generatingProjects = ConcurrentHashMap.newKeySet<Project>()
    }
}
