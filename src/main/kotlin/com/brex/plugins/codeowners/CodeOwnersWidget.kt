package com.brex.plugins.codeowners

import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.FileEditorManagerEvent
import com.intellij.openapi.fileEditor.OpenFileDescriptor
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.ui.popup.ListPopup
import com.intellij.openapi.ui.popup.PopupStep
import com.intellij.openapi.ui.popup.util.BaseListPopupStep
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.openapi.wm.StatusBar
import com.intellij.openapi.wm.StatusBarWidget
import com.intellij.openapi.wm.impl.status.EditorBasedWidget
import com.intellij.refactoring.listeners.RefactoringEventData
import com.intellij.refactoring.listeners.RefactoringEventListener
import com.intellij.util.Consumer
import java.awt.event.MouseEvent
import java.nio.file.Path

class CodeOwnersWidget(project: Project) :
    EditorBasedWidget(project),
    StatusBarWidget.MultipleTextValuesPresentation,
    RefactoringEventListener {

    private var currentOrSelectedFile: VirtualFile? = null
    private var currentFilePath: String? = null
    private var currentFileRule: CodeOwnerRule? = null
    private val codeOwnersService: OwnedByAnnotationStrategy = OwnedByAnnotationStrategy(project)

    override fun install(statusBar: StatusBar) {
        super.install(statusBar)
        myConnection.subscribe(RefactoringEventListener.REFACTORING_EVENT_TOPIC, this)
    }

    override fun ID() = ID

    override fun getTooltipText() = "Click to show owner details"

    override fun getSelectedValue(): String {
        if (currentOrSelectedFile === null) return ""
        return getCurrentCodeOwnerRule()?.getOwner() ?: return "Owner: None"
    }

    override fun getClickConsumer(): Consumer<MouseEvent>? = null

    override fun getPresentation(): StatusBarWidget.WidgetPresentation = this

    /** Return a popup listing all code owners for a file */
    override fun getPopupStep(): ListPopup? {
        val owners = getCurrentCodeOwnerRule()
        if (owners === null) {
            return null
        }

        return JBPopupFactory.getInstance().createListPopup(
            object : BaseListPopupStep<String>("Code Owners", owners.getPopup()) {
                override fun onChosen(selectedValue: String?, finalChoice: Boolean): PopupStep<*>? {
                    goToOwner(owners)
                    return super.onChosen(selectedValue, finalChoice)
                }
            }
        )
    }

    /** Open the CODEOWNERS file, and navigate to the line which defines the owner of the current file */
    private fun goToOwner(owners : CodeOwnerRule) {
        val vf = VirtualFileManager.getInstance().findFileByNioPath(Path.of(owners.annotationLocation)) ?: return
        OpenFileDescriptor(project, vf, currentFileRule?.lineNumber ?: 0, 0).navigate(true)
    }

    /** Get CodeOwner rule for the currently opened file */
    private fun getCurrentCodeOwnerRule(): CodeOwnerRule? {
        // Reload CodeOwners if the current file has changed
        val file = currentOrSelectedFile ?: return null
        if (file.path != currentFilePath) {
            currentFilePath = file.path
            currentFileRule = codeOwnersService.getCodeOwner(file)
        }
        return currentFileRule
    }

    private fun update(file: VirtualFile?) {
        // TODO: In theory we should just be able to use this.selectedFile, but for some reason,
        // sometimes the EditorBasedWidget loses its ability to find the currently selected file.
        // Therefore, we allow editor events to pass in the switched-to file, which seems to be
        // more reliable.
        currentOrSelectedFile = file ?: selectedFile ?: currentOrSelectedFile
        myStatusBar.updateWidget(ID())
    }
    // Listen to Editor events and update the status bar when switching or renaming files
    override fun selectionChanged(event: FileEditorManagerEvent) = update(event.newFile)
    override fun undoRefactoring(refactoringId: String) = update(null)
    override fun refactoringDone(refactoringId: String, afterData: RefactoringEventData?) = update(null)
    override fun fileOpened(source: FileEditorManager, file: VirtualFile) { /* ignored */ }
    override fun refactoringStarted(refactoringId: String, beforeData: RefactoringEventData?) { /* ignored */ }
    override fun conflictsDetected(refactoringId: String, conflictsData: RefactoringEventData) { /* ignored */ }

    companion object {
        internal const val ID = "org.brex.plugins.codeowners.CodeOwnersWidget"
    }
}
