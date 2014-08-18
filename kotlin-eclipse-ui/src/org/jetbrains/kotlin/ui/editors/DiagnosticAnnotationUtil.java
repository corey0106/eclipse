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
package org.jetbrains.kotlin.ui.editors;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.Path;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.texteditor.AbstractTextEditor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.diagnostics.Diagnostic;
import org.jetbrains.jet.lang.diagnostics.Errors;
import org.jetbrains.jet.lang.diagnostics.Severity;
import org.jetbrains.jet.lang.diagnostics.rendering.DefaultErrorMessages;
import org.jetbrains.jet.lang.resolve.Diagnostics;
import org.jetbrains.kotlin.utils.EditorUtil;
import org.jetbrains.kotlin.utils.LineEndUtil;

import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiErrorElement;
import com.intellij.psi.PsiFile;

public class DiagnosticAnnotationUtil {

    public static final DiagnosticAnnotationUtil INSTANCE = new DiagnosticAnnotationUtil();
    
    private DiagnosticAnnotationUtil() {
    }
    
    @NotNull
    public Map<IFile, List<DiagnosticAnnotation>> handleDiagnostics(@NotNull Diagnostics diagnostics) {
        Map<IFile, List<DiagnosticAnnotation>> annotations = new HashMap<IFile, List<DiagnosticAnnotation>>();
        for (Diagnostic diagnostic : diagnostics) {
            if (diagnostic.getTextRanges().isEmpty()) {
                continue;
            }
            
            VirtualFile virtualFile = diagnostic.getPsiFile().getVirtualFile();
            if (virtualFile == null) {
                continue;
            }
            
            IFile curFile = ResourcesPlugin.getWorkspace().getRoot().
                    getFileForLocation(new Path(virtualFile.getPath()));
            
            if (!annotations.containsKey(curFile)) {
                annotations.put(curFile, new ArrayList<DiagnosticAnnotation>());
            }
            
            DiagnosticAnnotation annotation = createKotlinAnnotation(diagnostic);
            annotations.get(curFile).add(annotation);
        }
        
        return annotations;
    }
    
    @NotNull
    public List<DiagnosticAnnotation> createParsingDiagnosticAnnotations(@NotNull PsiFile psiFile) {
        return recursiveCreateParsingDiagnosticAnnotations(psiFile);
    }
    
    @NotNull
    private List<DiagnosticAnnotation> recursiveCreateParsingDiagnosticAnnotations(@NotNull PsiElement psiElement) {
        List<DiagnosticAnnotation> result = new ArrayList<DiagnosticAnnotation>();
        
        if (psiElement instanceof PsiErrorElement) {
            result.add(createKotlinAnnotation((PsiErrorElement) psiElement));
        } else {
            for (PsiElement child : psiElement.getChildren()) {
                result.addAll(recursiveCreateParsingDiagnosticAnnotations(child));
            }
        }

        return result;
    }
    
    @NotNull
    private DiagnosticAnnotation createKotlinAnnotation(@NotNull PsiErrorElement psiErrorElement) {
        return new DiagnosticAnnotation(
                psiErrorElement.getNode().getTextRange(),
                AnnotationManager.ANNOTATION_ERROR_TYPE,
                psiErrorElement.getErrorDescription(),
                psiErrorElement.getText(),
                false);
    }
    
    @NotNull
    private DiagnosticAnnotation createKotlinAnnotation(@NotNull Diagnostic diagnostic) {
        List<TextRange> ranges = diagnostic.getTextRanges();
        String text = diagnostic.getPsiFile().getText();
        
        int offset = LineEndUtil.convertLfToOsOffset(text, ranges.get(0).getStartOffset());
        int length = ranges.get(0).getLength();
        
        String message = DefaultErrorMessages.RENDERER.render(diagnostic);
        String annotationType = getAnnotationType(diagnostic.getSeverity());
        String markedText = diagnostic.getPsiElement().getText();

        boolean isQuickFixable = Errors.UNRESOLVED_REFERENCE.equals(diagnostic.getFactory());
        DiagnosticAnnotation annotation = new DiagnosticAnnotation(offset, length, annotationType, 
                message, markedText, isQuickFixable);
        
        return annotation;
    }
    
    @Nullable
    private String getAnnotationType(@NotNull Severity severity) {
        String annotationType = null;
        switch (severity) {
            case ERROR:
                annotationType = AnnotationManager.ANNOTATION_ERROR_TYPE;
                break;
            case WARNING:
                annotationType = AnnotationManager.ANNOTATION_WARNING_TYPE;
                break;
            default:
                break;
        }
        
        return annotationType;
    }
    
    public void updateActiveEditorAnnotations(@NotNull final Map<IFile, List<DiagnosticAnnotation>> annotations) {
        Display.getDefault().asyncExec(new Runnable() {
            @Override
            public void run() {
                IWorkbenchWindow workbenchWindow = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
                
                if (workbenchWindow == null) {
                    return;
                }
                
                AbstractTextEditor editor = (AbstractTextEditor) workbenchWindow.getActivePage().getActiveEditor();
                if (editor != null) {
                    updateAnnotations(editor, annotations);
                }
            }
        });
    }
    
    private void updateAnnotations(@NotNull AbstractTextEditor editor, 
            @NotNull Map<IFile, List<DiagnosticAnnotation>> annotations) {
        IFile file = EditorUtil.getFile(editor);

        List<DiagnosticAnnotation> newAnnotations = annotations.get(file);
        if (newAnnotations == null) {
            newAnnotations = Collections.emptyList();
        }
        AnnotationManager.updateAnnotations(editor, newAnnotations);
    }
}