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
package org.jetbrains.kotlin.ui.tests.editors.markers;

import java.io.File;
import java.util.Collection;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.jetbrains.kotlin.core.log.KotlinLogger;
import org.jetbrains.kotlin.testframework.editor.KotlinEditorAutoTestCase;
import org.jetbrains.kotlin.testframework.utils.EditorTestUtils;

public class KotlinParsingMarkersTestCase extends KotlinEditorAutoTestCase {
    
    private static final String PARSING_MARKERS_TEST_DATA_PATH = "markers/parsing/";
    
    private void performTest(String fileText, String expected) {
        joinBuildThread();
        
        try {
            IMarker[] markers = testEditor.getEditingFile().findMarkers(IMarker.PROBLEM, true, IResource.DEPTH_INFINITE);
            String actual = insertTagsForErrors(fileText, markers);
            
            EditorTestUtils.assertByStringWithOffset(actual, expected);
        } catch (CoreException e) {
            KotlinLogger.logAndThrow(e);
        }
    }
    
    @Override
    protected void doSingleFileAutoTest(String testPath) {
        String fileText = getText(testPath);
        testEditor = configureEditor(getNameByPath(testPath), fileText,
                WithAfterSourceFileData.getPackageFromContent(fileText));
        
        performTest(fileText, getText(testPath + AFTER_FILE_EXTENSION));
    }
    
    @Override
    protected void doMultiFileAutoTest(File testFolder) {
        Collection<WithAfterSourceFileData> files = WithAfterSourceFileData.getTestFiles(testFolder);
        
        WithAfterSourceFileData target = WithAfterSourceFileData.getTargetFile(files);
        testEditor = configureEditor(target.getFileName(), target.getContent(), target.getPackageName());
        
        for (WithAfterSourceFileData file : files) {
            if (file != target) {
                createSourceFile(file.getPackageName(), file.getFileName(), file.getContent());
            }
        }
        
        performTest(target.getContent(), target.getContentAfter());
    }
    
    @Override
    protected String getTestDataPath() {
        return super.getTestDataPath() + PARSING_MARKERS_TEST_DATA_PATH;
    }
    
    private static String insertTagsForErrors(String fileText, IMarker[] markers) throws CoreException {
        StringBuilder result = new StringBuilder(fileText);
        
        Integer offset = 0;
        for (IMarker marker : markers) {
            int openTagStartOffset = getOpenTagStartOffset(marker);
            int closeTagStartOffset = getCloseTagStartOffset(marker);
            
            switch (marker.getAttribute(IMarker.SEVERITY, 0)) {
            case IMarker.SEVERITY_ERROR:
                offset += insertTagByOffset(result, ERROR_TAG_OPEN, openTagStartOffset, offset);
                offset += insertTagByOffset(result, ERROR_TAG_CLOSE, closeTagStartOffset, offset);
                break;
            case IMarker.SEVERITY_WARNING:
                offset += insertTagByOffset(result, WARNING_TAG_OPEN, openTagStartOffset, offset);
                offset += insertTagByOffset(result, WARNING_TAG_CLOSE, closeTagStartOffset, offset);
                break;
            default:
                break;
            }
        }
        
        return result.toString();
    }
    
    private static int getOpenTagStartOffset(IMarker marker) throws CoreException {
        return getTagStartOffset(marker, IMarker.CHAR_START);
    }
    
    private static int getCloseTagStartOffset(IMarker marker) throws CoreException {
        return getTagStartOffset(marker, IMarker.CHAR_END);
    }
    
    private static int getTagStartOffset(IMarker marker, String type) throws CoreException {
        return (int) marker.getAttribute(type);
    }
    
    private static int insertTagByOffset(StringBuilder builder, String tag, int tagStartOffset, int offset) {
        builder.insert(tagStartOffset + offset, tag);
        return tag.length();
    }
}
