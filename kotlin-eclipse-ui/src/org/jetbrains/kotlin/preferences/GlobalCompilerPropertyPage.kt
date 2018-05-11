package org.jetbrains.kotlin.preferences

import org.eclipse.core.resources.IncrementalProjectBuilder.FULL_BUILD
import org.eclipse.core.resources.ResourcesPlugin
import org.eclipse.core.runtime.IProgressMonitor
import org.eclipse.swt.widgets.Composite
import org.eclipse.ui.IWorkbench
import org.eclipse.ui.IWorkbenchPreferencePage
import org.jetbrains.kotlin.core.preferences.CompilerPlugin
import org.jetbrains.kotlin.core.preferences.KotlinProperties
import org.jetbrains.kotlin.swt.builders.asView
import org.eclipse.core.runtime.preferences.InstanceScope

class GlobalCompilerPropertyPage: KotlinCompilerPropertyPage(), IWorkbenchPreferencePage {
    override fun init(workbench: IWorkbench?) {
    }

    override fun createContents(parent: Composite) =
            parent.asView.createOptionsControls().control

    override val kotlinProperties = KotlinProperties()

    override fun rebuildTask(monitor: IProgressMonitor?) {
        ResourcesPlugin.getWorkspace().build(FULL_BUILD, monitor)
    }

}