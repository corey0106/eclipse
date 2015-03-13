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

import org.eclipse.core.resources.IFile;
import org.eclipse.jdt.ui.actions.IJavaEditorActionDefinitionIds;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.text.IDocument;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.core.builder.KotlinPsiManager;
import org.jetbrains.kotlin.eclipse.ui.utils.EditorUtil;
import org.jetbrains.kotlin.ui.formatter.AlignmentStrategy;

import com.intellij.psi.PsiFile;

public class KotlinFormatAction extends Action {
    
    public static final String FORMAT_ACTION_TEXT = "Format";
    
    @NotNull
    private final KotlinEditor editor;
    
    public KotlinFormatAction(@NotNull KotlinEditor editor) {
        this.editor = editor;
        
        setText(FORMAT_ACTION_TEXT);
        setActionDefinitionId(IJavaEditorActionDefinitionIds.FORMAT);
    }
    
    @Override
    public void run() {
        String sourceCode = EditorUtil.getSourceCode(editor);
        IFile file = EditorUtil.getFile(editor);
        
        PsiFile parsedCode = KotlinPsiManager.getKotlinFileIfExist(file, sourceCode);
        if (parsedCode == null) {
            return;
        }
        
        IDocument document = editor.getViewer().getDocument(); 
        document.set(AlignmentStrategy.alignCode(parsedCode.getNode()));
    }
}
