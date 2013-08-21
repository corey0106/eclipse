package org.jetbrains.kotlin.testframework.editor;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.ui.PlatformUI;
import org.jetbrains.kotlin.testframework.utils.WorkspaceUtil;
import org.junit.After;
import org.junit.Before;

public abstract class KotlinEditorTestCase {
	
	protected TextEditorTest testEditor;
	public static final String ERR_TAG_OPEN = "<err>";
	public static final String ERR_TAG_CLOSE = "</err>";
	public static final String BR = "<br>";

	@After
	public void afterTest() {
		deleteProjectAndCloseEditors();
	}
	
	@Before
	public void beforeTest() {
		refreshWorkspace();
	}
	
	protected TextEditorTest configureEditor(String fileName, String content) {
    	return configureEditor(fileName, content, TextEditorTest.TEST_PROJECT_NAME, TextEditorTest.TEST_PACKAGE_NAME);
    }
	
    protected TextEditorTest configureEditor(String fileName, String content, String projectName, String packageName) {
    	TextEditorTest testEditor = new TextEditorTest(projectName);
		String toEditor = resolveTestTags(content);
		testEditor.createEditor(fileName, toEditor, packageName);
		
		return testEditor;
    }
    
    public void deleteProjectAndCloseEditors() {
		try {
			IProject projects[] = ResourcesPlugin.getWorkspace().getRoot().getProjects();
			for (IProject project : projects) {
				project.delete(true, true, null);
			}
		} catch (CoreException e) {
			e.printStackTrace();
		}
		
		PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().closeAllEditors(false);
    }
    
    public void refreshWorkspace() {
		WorkspaceUtil.refreshWorkspace();
		try {
			Job.getJobManager().join(ResourcesPlugin.FAMILY_MANUAL_REFRESH, new NullProgressMonitor());
		} catch (OperationCanceledException | InterruptedException e) {
			e.printStackTrace();
		}
    }
    
    public String resolveTestTags(String text) {
		return text
				.replaceAll(ERR_TAG_OPEN, "")
				.replaceAll(ERR_TAG_CLOSE, "")
				.replaceAll(BR, System.lineSeparator());
    }
    
    public String removeTags(String text) {
    	return resolveTestTags(text).replaceAll("<caret>", "");
    }
    
	public void createSourceFile(String referenceFileName, String referenceFile) {
		referenceFile = removeTags(referenceFile);
		try {
			testEditor.getTestJavaProject().createSourceFile("testing", referenceFileName, referenceFile);
		} catch (CoreException e) {
			throw new RuntimeException(e);
		}
	}
	
	public void joinBuildThread() {
		while (true) {
			try {
				Job.getJobManager().join(ResourcesPlugin.FAMILY_AUTO_BUILD, null);
				break;
			} catch (OperationCanceledException | InterruptedException e) {
			}
		}
	}
}
