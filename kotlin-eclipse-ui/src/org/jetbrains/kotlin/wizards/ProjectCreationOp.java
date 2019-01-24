/*******************************************************************************
 * Copyright 2000-2014 JetBrains s.r.o.
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
package org.jetbrains.kotlin.wizards;

import static org.eclipse.ui.ide.undo.WorkspaceUndoUtil.getUIInfoAdapter;

import java.io.FileNotFoundException;
import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.ICommand;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.launching.JavaRuntime;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.ide.undo.CreateProjectOperation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.core.log.KotlinLogger;
import org.jetbrains.kotlin.core.utils.ProjectUtils;

public class ProjectCreationOp implements IRunnableWithProgress {

    private static final String SRC_FOLDER = "src";
    private static final String BIN_FOLDER = "bin";

    private final IProjectDescription projectDescription;
    private final String projectName;
    private final Shell shell;

    private OperationResult result = new OperationResult();
    private IJavaProject javaResult;

    public ProjectCreationOp(String projectName, String projectLocation, Shell shell) {
        projectDescription = buildProjectDescription(projectName, projectLocation);

        this.projectName = projectName;
        this.shell = shell;
    }

    public OperationResult getResult() {
        return result;
    }

    private IJavaProject getJavaResult() {
        if (javaResult == null) {
            try {
                javaResult = buildJavaProject(result.getProject());
            } catch (CoreException e) {
                KotlinLogger.logAndThrow(e);
            } catch (FileNotFoundException e) {
                KotlinLogger.logAndThrow(e);
            }
        }

        return javaResult;
    }

    @Override
    public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
        CreateProjectOperation operation = new CreateProjectOperation(projectDescription, projectName);
        try {
            PlatformUI.getWorkbench().getOperationSupport().getOperationHistory().execute(operation, monitor,
                    getUIInfoAdapter(shell));

            result.project = ResourcesPlugin.getWorkspace().getRoot().getProject(projectName);
            result.project.setDescription(projectDescription, new NullProgressMonitor());
            getJavaResult();
        } catch (ExecutionException | CoreException e) {
                KotlinLogger.logError(e);
                result.exception = e.getCause();
        }
    }

    private static IProjectDescription buildProjectDescription(String projectName, String projectLocation) {
        IWorkspace workspace = ResourcesPlugin.getWorkspace();

        IProjectDescription result = workspace.newProjectDescription(projectName);
        result.setComment(projectName);
        if (!workspace.getRoot().getLocation().toOSString().equals(projectLocation)) {
            result.setLocation(new Path(projectLocation));
        }

        ICommand command = result.newCommand();
        command.setBuilderName(JavaCore.BUILDER_ID);
        result.setBuildSpec(new ICommand[] { command });

        result.setNatureIds(new String[] { JavaCore.NATURE_ID });

        return result;
    }

    private static IJavaProject buildJavaProject(@NotNull IProject project) throws CoreException, FileNotFoundException {
        IJavaProject result = JavaCore.create(project);

        IFolder binFolder = project.getFolder(BIN_FOLDER);
        if (!binFolder.exists()) {
            binFolder.create(false, true, null);
        }
        result.setOutputLocation(binFolder.getFullPath(), null);

        IFolder srcFolder = project.getFolder(SRC_FOLDER);
        if (!srcFolder.exists()) {
            srcFolder.create(false, true, null);
        }

        result.setRawClasspath(new IClasspathEntry[] {
                JavaCore.newContainerEntry(new Path(JavaRuntime.JRE_CONTAINER)),
                JavaCore.newSourceEntry(result.getPackageFragmentRoot(srcFolder).getPath())
                }, null);

        ProjectUtils.addKotlinRuntime(project);

        return result;
    }

    class OperationResult {
        private IProject project = null;
        private Throwable exception = null;

        private OperationResult() {}

        public boolean isSuccess() {
            return exception == null;
        }

        IProject getProject() {
            return project;
        }

        Throwable getException() {
            return exception;
        }
    }
}
