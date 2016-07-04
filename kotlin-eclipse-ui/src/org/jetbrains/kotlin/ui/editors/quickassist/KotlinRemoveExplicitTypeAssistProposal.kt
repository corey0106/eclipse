/*******************************************************************************
 * Copyright 2000-2015 JetBrains s.r.o.
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
package org.jetbrains.kotlin.ui.editors.quickassist

import com.intellij.psi.PsiElement
import org.eclipse.jface.text.IDocument
import org.jetbrains.kotlin.psi.KtCallableDeclaration
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.kotlin.psi.KtCodeFragment
import org.jetbrains.kotlin.psi.KtWithExpressionInitializer
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtParameter
import org.jetbrains.kotlin.core.resolve.KotlinAnalyzer
import org.eclipse.jdt.core.JavaCore
import org.jetbrains.kotlin.psi.KtTypeReference
import org.jetbrains.kotlin.eclipse.ui.utils.getBindingContext

public class KotlinRemoveExplicitTypeAssistProposal : KotlinQuickAssistProposal() {
    private var displayString: String? = null
    
    override fun isApplicable(psiElement: PsiElement): Boolean {
        val element = PsiTreeUtil.getNonStrictParentOfType(psiElement, KtCallableDeclaration::class.java)
        if (element == null) return false
        
        if (element.getContainingFile() is KtCodeFragment) return false
        if (element.getTypeReference() == null) return false

        val caretOffset = getCaretOffsetInPSI(editor, editor.document)
        
        val initializer = (element as? KtWithExpressionInitializer)?.getInitializer()
        if (initializer != null && initializer.getTextRange().containsOffset(caretOffset)) return false
        
        val bindingContext = getBindingContext(element)
        if (bindingContext == null) return false
        
        return when (element) {
            is KtProperty -> initializer != null
            is KtNamedFunction -> !element.hasBlockBody() && initializer != null
            is KtParameter -> element.isLoopParameter()
            else -> false
        }
    }
    
    override fun getDisplayString(): String {
        return "Remove explicit type specification"
    }
    
    override fun apply(document: IDocument, psiElement: PsiElement) {
        val element = PsiTreeUtil.getNonStrictParentOfType(psiElement, KtCallableDeclaration::class.java)!!
        val anchor = getAnchor(element)
        
        if (anchor == null) return
        
        removeTypeAnnotation(document, anchor, element.getTypeReference()!!)
    }
    
    private fun removeTypeAnnotation(document: IDocument, removeAfter: PsiElement, typeReference: KtTypeReference) {
        val endOffset = getEndOffset(removeAfter, editor)
        val endOfType = getEndOffset(typeReference, editor)
        document.replace(endOffset, endOfType - endOffset, "")
    }
}