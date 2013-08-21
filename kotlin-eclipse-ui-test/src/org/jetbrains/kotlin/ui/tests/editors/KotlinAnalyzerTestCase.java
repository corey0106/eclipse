package org.jetbrains.kotlin.ui.tests.editors;

import junit.framework.Assert;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.jetbrains.kotlin.testframework.editor.KotlinEditorTestCase;

public class KotlinAnalyzerTestCase extends KotlinEditorTestCase {

	protected void doTest(String input, String fileName) {
		testEditor = configureEditor(fileName, input);
		try {
			testEditor.save();
			joinBuildThread();
			
			String editorInput = insertTagsForErrors(testEditor.getEditorInput(), 
					testEditor.getEditingFile().findMarkers(IMarker.PROBLEM, true, IResource.DEPTH_INFINITE));
			
			String editorInputWithoutCR = editorInput.toString().replaceAll("\r", "");
			input = input
					.replaceAll("<br>", System.lineSeparator())
					.replaceAll("\r", "");
			
			Assert.assertEquals(input, editorInputWithoutCR);
		} catch (CoreException e) {
			throw new RuntimeException(e);
		}
	} 
	
	private String insertTagsForErrors(String text, IMarker[] markers) throws CoreException {
		StringBuilder editorInput = new StringBuilder(text);
		int tagShift = 0;
		for (IMarker marker : markers) {
			if (marker.getAttribute(IMarker.SEVERITY, 0) == IMarker.SEVERITY_ERROR) {
				editorInput.insert((int) marker.getAttribute(IMarker.CHAR_START) + tagShift, ERR_TAG_OPEN);
				tagShift += ERR_TAG_OPEN.length();
				
				editorInput.insert((int) marker.getAttribute(IMarker.CHAR_END) + tagShift, ERR_TAG_CLOSE);
				tagShift += ERR_TAG_CLOSE.length();
			}
		}
		
		return editorInput.toString();
	}
}
