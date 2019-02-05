/*******************************************************************************
 * Copyright 2000-2016 JetBrains s.r.o.
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
package org.jetbrains.kotlin.ui.editors.hover

import com.intellij.psi.util.PsiTreeUtil
import org.eclipse.jdt.internal.ui.text.JavaWordFinder
import org.eclipse.jface.text.IInformationControlCreator
import org.eclipse.jface.text.IRegion
import org.eclipse.jface.text.ITextHover
import org.eclipse.jface.text.ITextHoverExtension
import org.eclipse.jface.text.ITextHoverExtension2
import org.eclipse.jface.text.ITextViewer
import org.eclipse.jface.text.Region
import org.jetbrains.kotlin.core.model.loadExecutableEP
import org.jetbrains.kotlin.eclipse.ui.utils.findElementByDocumentOffset
import org.jetbrains.kotlin.eclipse.ui.utils.getOffsetByDocument
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.ui.editors.KotlinCommonEditor
import org.jetbrains.kotlin.ui.editors.KotlinEditor
import org.jetbrains.kotlin.utils.addToStdlib.firstNotNullResult

const val TEXT_HOVER_EP_ID = "org.jetbrains.kotlin.ui.editor.textHover"

class KotlinTextHover(private val editor: KotlinEditor) : ITextHover, ITextHoverExtension, ITextHoverExtension2 {

    private val extensionsHovers = loadExecutableEP<KotlinEditorTextHover<*>>(TEXT_HOVER_EP_ID)
        .mapNotNull { it.createProvider() }
        .sortedBy {
            it.hoverPriority
        }

    private var bestHover: KotlinEditorTextHover<*>? = null

    override fun getHoverRegion(textViewer: ITextViewer, offset: Int): IRegion? {
        return JavaWordFinder.findWord(textViewer.document, offset)
    }

    override fun getHoverInfo(textViewer: ITextViewer?, hoverRegion: IRegion): String? {
        return getHoverInfo2(textViewer, hoverRegion)?.toString()
    }

    override fun getHoverControlCreator(): IInformationControlCreator? {
        return bestHover?.getHoverControlCreator(editor)
    }

    override fun getHoverInfo2(textViewer: ITextViewer?, hoverRegion: IRegion): Any? =
        createHoverData(hoverRegion.offset)?.let { data ->
            extensionsHovers.firstNotNullResult { hover ->
                hover.takeIf { it.isAvailable(data) }?.getHoverInfo(data)?.also {
                    bestHover = hover
                }
            }
        } ?: also { bestHover = null }

    private fun createHoverData(offset: Int): HoverData? {
        val ktFile = editor.parsedFile ?: return null
        val psiElement = ktFile.findElementByDocumentOffset(offset, editor.document) ?: return null
        val ktElement = PsiTreeUtil.getParentOfType(psiElement, KtElement::class.java) ?: return null

        return HoverData(ktElement, editor)
    }
}

interface KotlinEditorTextHover<out Info> {
    val hoverPriority: Int

    fun getHoverInfo(hoverData: HoverData): Info?

    fun isAvailable(hoverData: HoverData): Boolean

    fun getHoverControlCreator(editor: KotlinEditor): IInformationControlCreator?
}

data class HoverData(val hoverElement: KtElement, val editor: KotlinEditor)

fun HoverData.getRegion(): Region? {
    val (element, editor) = this

    val psiTextRange = element.textRange
    val document = if (editor is KotlinCommonEditor) editor.getDocumentSafely() else editor.document
    document ?: return null

    val startOffset = element.getOffsetByDocument(document, psiTextRange.startOffset)

    return Region(startOffset, psiTextRange.length)
}