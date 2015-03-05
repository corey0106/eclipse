package org.jetbrains.kotlin.testframework.utils;

import java.io.File;
import java.io.IOException;

import org.eclipse.core.resources.ICommand;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jdt.internal.ui.javaeditor.JavaEditor;
import org.jetbrains.kotlin.core.model.KotlinNature;

import com.intellij.openapi.util.io.FileUtil;

public class KotlinTestUtils {
	public enum Separator {
		TAB, SPACE;
	}
	
	public static final String ERROR_TAG_OPEN = "<error>";
	public static final String ERROR_TAG_CLOSE = "</error>";
	public static final String BR = "<br>";

	public static String getText(String testPath) {
		try {
			File file = new File(testPath);
			return String.valueOf(FileUtil.loadFile(file));
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
	
	public static String getNameByPath(String testPath) {
		return new Path(testPath).lastSegment();
	}
	
	public static void joinBuildThread() {
		while (true) {
			try {
				Job.getJobManager().join(ResourcesPlugin.FAMILY_AUTO_BUILD, null);
				break;
			} catch (OperationCanceledException | InterruptedException e) {
			}
		}
	}
	
	public static void refreshWorkspace() {
		WorkspaceUtil.refreshWorkspace();
		try {
			Job.getJobManager().join(ResourcesPlugin.FAMILY_MANUAL_REFRESH, new NullProgressMonitor());
		} catch (OperationCanceledException | InterruptedException e) {
			e.printStackTrace();
		}
    }
	
	public static int getCaret(JavaEditor javaEditor) {
		return javaEditor.getViewer().getTextWidget().getCaretOffset();
	}
	
	public static void addKotlinBuilder(IProject project) {
		try {
			KotlinNature.addBuilder(project);
		} catch (CoreException e) {
			throw new RuntimeException(e);
		}
	}
	
	public static void removeKotlinBuilder(IProject project) {
		try {
			if (KotlinNature.hasKotlinBuilder(project)) {
				IProjectDescription description = project.getDescription();
				ICommand[] buildCommands = description.getBuildSpec();
				ICommand[] newBuildCommands = new ICommand[buildCommands.length - 1];
				int i = 0;
		        for (ICommand buildCommand : buildCommands) {
		            if (KotlinNature.KOTLIN_BUILDER.equals(buildCommand.getBuilderName())) {
		            	continue;
		            }
		            newBuildCommands[i] = buildCommand;
		            i++;
		        }
		        
		        description.setBuildSpec(newBuildCommands);
	            project.setDescription(description, null);
			}
		} catch (CoreException e) {
			throw new RuntimeException(e);
		}
	}
	
	public static String resolveTestTags(String text) {
		return text
				.replaceAll(ERROR_TAG_OPEN, "")
				.replaceAll(ERROR_TAG_CLOSE, "")
				.replaceAll(BR, System.lineSeparator());
    }
}
