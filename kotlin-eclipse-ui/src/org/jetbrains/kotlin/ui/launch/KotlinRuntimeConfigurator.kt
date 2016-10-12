/*******************************************************************************
 * Copyright 2000-2016 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 *******************************************************************************/
package org.jetbrains.kotlin.ui.launch

import org.eclipse.core.resources.IProject
import org.eclipse.mylyn.commons.ui.dialogs.AbstractNotificationPopup
import org.eclipse.swt.SWT
import org.eclipse.swt.layout.GridLayout
import org.eclipse.swt.widgets.Composite
import org.eclipse.swt.widgets.Display
import org.eclipse.swt.widgets.Label
import org.jetbrains.kotlin.core.utils.ProjectUtils
import org.jetbrains.kotlin.ui.gridData
import org.eclipse.swt.custom.StyledText
import org.jetbrains.kotlin.core.KotlinClasspathContainer
import org.jetbrains.kotlin.ui.gridData
import org.eclipse.swt.custom.StyleRange

class KotlinRuntimeConfigurator(private val project: IProject) : Runnable {
    companion object {
        @JvmStatic fun suggestForProject(project: IProject) {
            Display.getDefault().asyncExec(KotlinRuntimeConfigurator(project))
        }
    }

    override fun run() {
        if (ProjectUtils.hasKotlinRuntime(project)) return

        ProjectUtils.addKotlinRuntime(project)
        
        RuntimeNotificationPopup(Display.getDefault()).open()
    }
}

private class RuntimeNotificationPopup(display: Display) : AbstractNotificationPopup(display) {
    companion object {
        private val RUNTIME_JAR = KotlinClasspathContainer.LIB_RUNTIME_NAME.toJar()
        private val REFLECT_JAR = KotlinClasspathContainer.LIB_REFLECT_NAME.toJar()
        
        private fun String.toJar() = "$this.jar" 
    }
    
    init {
        setDelayClose(0)
    }
    
    override fun createContentArea(parent: Composite) {
        val parentLayout = GridLayout(1, true)
        
        parent.setLayout(parentLayout)
        parent.setLayoutData(gridData())
        
        StyledText(parent, SWT.LEFT).apply {
            setText("$RUNTIME_JAR, $REFLECT_JAR were added to the project classpath.")
            makeBold(RUNTIME_JAR, REFLECT_JAR)
        }
    }
    
    override fun getPopupShellTitle(): String = "Configure Kotlin in Project"
    
    private fun StyledText.makeBold(vararg strs: String) {
        val styleRanges = strs.mapNotNull { str ->
            val start = text.indexOf(str)
            if (start < 0) return@mapNotNull null

            StyleRange(start, str.length, foreground, background, SWT.BOLD)
        }
        
        setStyleRanges(styleRanges.toTypedArray())
    }
}