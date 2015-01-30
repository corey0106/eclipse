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
package org.jetbrains.kotlin.eclipse.ui.utils;

import org.eclipse.core.resources.IFile;
import org.eclipse.jdt.internal.ui.javaeditor.JavaEditor;
import org.eclipse.ui.texteditor.AbstractTextEditor;
import org.jetbrains.annotations.NotNull;

public class EditorUtil {
    
    @NotNull
    public static IFile getFile(@NotNull AbstractTextEditor editor) {
        return (IFile) editor.getEditorInput().getAdapter(IFile.class);
    }
    
    @NotNull
    public static String getSourceCode(@NotNull JavaEditor editor) {
        return editor.getViewer().getDocument().get();
    }
    
    public static int getOffsetInEditor(@NotNull JavaEditor editor, int offset) {
        return LineEndUtil.convertLfToOsOffset(LineEndUtil.removeAllCarriageReturns(getSourceCode(editor)), offset);
    }
}