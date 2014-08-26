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
package org.jetbrains.kotlin.core.launch;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.IStatusHandler;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.launching.IJavaLaunchConfigurationConstants;
import org.eclipse.jdt.launching.JavaLaunchDelegate;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.cli.common.messages.CompilerMessageLocation;
import org.jetbrains.jet.cli.common.messages.CompilerMessageSeverity;
import org.jetbrains.jet.cli.common.messages.MessageCollector;
import org.jetbrains.jet.cli.jvm.K2JVMCompiler;
import org.jetbrains.jet.lang.resolve.name.FqName;
import org.jetbrains.kotlin.core.Activator;
import org.jetbrains.kotlin.core.builder.KotlinPsiManager;
import org.jetbrains.kotlin.core.log.KotlinLogger;
import org.jetbrains.kotlin.core.model.KotlinJavaManager;
import org.jetbrains.kotlin.core.utils.ProjectUtils;

import com.google.common.collect.Lists;

public class LaunchConfigurationDelegate extends JavaLaunchDelegate {
    
    private final static String KT_COMPILER_PATH = ProjectUtils.buildLibPath("kotlin-compiler");
    public final static String KT_JDK_ANNOTATIONS_PATH = ProjectUtils.buildLibPath("kotlin-jdk-annotations");
    
    private final CompilerOutputData compilerOutput = new CompilerOutputData();
    
    private boolean buildFailed = false;
    
    @Override
    public void launch(ILaunchConfiguration configuration, String mode, ILaunch launch, IProgressMonitor monitor) throws CoreException {
        String projectName = getJavaProjectName(configuration);
        
        if (projectName == null) {
            abort("Project name is invalid: " + projectName, null, IJavaLaunchConfigurationConstants.ERR_UNSPECIFIED_PROJECT);
            
            return;
        }
        
        List<IFile> projectFiles = KotlinPsiManager.INSTANCE.getFilesByProject(projectName);
        
        if (!compileKotlinFiles(projectFiles, configuration)) {
            IStatus status = new Status(IStatus.ERROR, Activator.PLUGIN_ID, 1, "", null);
            IStatusHandler handler = DebugPlugin.getDefault().getStatusHandler(status);
            
            if (handler != null) {
                handler.handleStatus(status, compilerOutput);
            }
            
            return;
        }
        
        super.launch(configuration, mode, launch, monitor);
    }
    
    @Override
    public String[] getClasspath(ILaunchConfiguration configuration) throws CoreException {
        String[] oldClasspath = super.getClasspath(configuration);
        String[] newClasspath = new String[oldClasspath.length + 1];
        System.arraycopy(oldClasspath, 0, newClasspath, 0, oldClasspath.length);
        
        newClasspath[oldClasspath.length] = KT_JDK_ANNOTATIONS_PATH;
        
        return newClasspath;
    }
    
    @Override
    public String verifyMainTypeName(ILaunchConfiguration configuration) throws CoreException {
        try {
            return getPackageClassName(configuration).toString();
        } catch (IllegalArgumentException e) {
            abort("File with main method not defined", null, IJavaLaunchConfigurationConstants.ERR_UNSPECIFIED_MAIN_TYPE);
        }
        
        return null;
    }
    
    @NotNull
    private FqName getPackageClassName(ILaunchConfiguration configuration) {
        try {
            String projectName = getJavaProjectName(configuration);
            String mainTypeName = getMainTypeName(configuration);
            
            assert projectName != null;
            assert mainTypeName != null;
            
            FqName mainClassName = new FqName(mainTypeName);
            for (IFile file : KotlinPsiManager.INSTANCE.getFilesByProject(projectName)) {
                if (ProjectUtils.hasMain(file) && ProjectUtils.createPackageClassName(file).equalsTo(mainClassName)) {
                    return mainClassName;
                }
            }
        } catch (CoreException e) {
            KotlinLogger.logError(e);
        }
        
        throw new IllegalArgumentException();
    }
    
    private void refreshInitData() {
        buildFailed = false;
        compilerOutput.clear();
    }
    
    private boolean compileKotlinFiles(@NotNull List<IFile> files, @NotNull ILaunchConfiguration configuration)
            throws CoreException {
        String[] command = configureBuildCommand(configuration);
        
        refreshInitData();
        try {
            Process buildProcess = Runtime.getRuntime().exec(command);
            parseCompilerOutput(buildProcess.getErrorStream());
            
            buildProcess.waitFor();
            
            if (buildFailed) {
                return false;
            }
        } catch (IOException | InterruptedException e) {
            KotlinLogger.logError(e);
            
            abort("Build error", null, 0);
        }
        
        return true;
    }
    
    private void parseCompilerOutput(InputStream inputStream) {
        MessageCollector messageCollector = new MessageCollector() {
            @Override
            public void report(@NotNull CompilerMessageSeverity messageSeverity, @NotNull String message,
                    @NotNull CompilerMessageLocation messageLocation) {
                if (CompilerMessageSeverity.ERROR.equals(messageSeverity)
                        || CompilerMessageSeverity.EXCEPTION.equals(messageSeverity)) {
                    buildFailed = true;
                }
                
                compilerOutput.add(messageSeverity, message, messageLocation);
            }
        };
        CompilerOutputParser.parseCompilerMessagesFromReader(messageCollector, new InputStreamReader(inputStream));
    }
    
    private String[] configureBuildCommand(ILaunchConfiguration configuration) throws CoreException {
        List<String> command = new ArrayList<String>();
        command.add("java");
        command.add("-cp");
        command.add(KT_COMPILER_PATH);
        command.add(K2JVMCompiler.class.getCanonicalName());
        command.add("-kotlinHome");
        command.add(ProjectUtils.KT_HOME);
        command.add("-tags");
        
        StringBuilder classPath = new StringBuilder();
        String pathSeparator = System.getProperty("path.separator");
        
        IJavaProject javaProject = getJavaProject(configuration);
        for (File srcDirectory : ProjectUtils.getSrcDirectories(javaProject)) {
            classPath.append(srcDirectory.getAbsolutePath()).append(pathSeparator);
        }
        
        for (File libDirectory : excludeKotlinBinFolder(ProjectUtils.getLibDirectories(javaProject))) {
            classPath.append(libDirectory.getAbsolutePath()).append(pathSeparator);
        }
        
        command.add("-classpath");
        command.add(classPath.toString());
        
        command.add("-d");
        command.add(getOutputDir(configuration));
        
        for (File srcDirectory : ProjectUtils.getSrcDirectories(javaProject)) {
            command.add(srcDirectory.getAbsolutePath());
        }
        
        return command.toArray(new String[command.size()]);
    }
    
    private List<File> excludeKotlinBinFolder(@NotNull List<File> libDirectories) {
        List<File> libraries = Lists.newArrayList();
        for (File libDirectory : libDirectories) {
            if (libDirectory.getName().equals(KotlinJavaManager.KOTLIN_BIN_FOLDER.toString())) {
                continue;
            }
            libraries.add(libDirectory);
        }
        
        return libraries;
    }
    
    private String getOutputDir(ILaunchConfiguration configuration) {
        try {
            String[] cp = getClasspath(configuration);
            if (cp.length > 0)
                return cp[0];
        } catch (CoreException e) {
            KotlinLogger.logError(e);
        }
        
        return ".";
    }
}