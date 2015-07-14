package org.jetbrains.kotlin.ui.editors.selection;

import org.eclipse.jdt.internal.ui.javaeditor.selectionactions.SelectionHistory;
import org.eclipse.jdt.ui.actions.IJavaEditorActionDefinitionIds;
import org.eclipse.jface.text.ITextSelection;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.ui.editors.KotlinEditor;
import org.jetbrains.kotlin.ui.editors.selection.handlers.KotlinElementSelectioner;

import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiWhiteSpace;

public class KotlinSelectNextAction extends KotlinSemanticSelectionAction {
    
    private static final String ACTION_DESCRIPTION = "Select next element";
    
    public static final String SELECT_NEXT_TEXT = "SelectNext";
    
    public KotlinSelectNextAction(KotlinEditor editor, SelectionHistory history) {
        super(editor, history);
        setText(ACTION_DESCRIPTION);
        setActionDefinitionId(IJavaEditorActionDefinitionIds.SELECT_NEXT);
    }
    
    @Override
    protected @NotNull TextRange runInternalSelection(PsiElement enclosingElement, ITextSelection selection) {
        boolean isSelectionCandidate = false;
        PsiElement currentChild = enclosingElement.getFirstChild();
        KotlinElementSelectioner selectionerInstance = KotlinElementSelectioner.INSTANCE$;
        TextRange selectedRange = getCrConvertedTextRange(selection);
        String selectedText = selection.getText();
        
        // if selected text is all whitespaces then select enclosing
        if (!selectedText.isEmpty() && selectedText.trim().isEmpty()) {
            return selectionerInstance.selectEnclosing(enclosingElement, selectedRange);
        }
        
        while (currentChild != null) {
            ElementSelection selectionType = checkSelection(currentChild, selectedRange);
            // if all completely selected elements are not the children of the
            // enclosing element, then select enclosing element
            if (selectionType == ElementSelection.PartiallySelected && !(currentChild instanceof PsiWhiteSpace)) {
                return selectionerInstance.selectEnclosing(enclosingElement, selectedRange);
            }
            if (selectionType == ElementSelection.NotSelected) {
                // if we're already looking for selection candidate, select if
                // not whitespace
                if (!(currentChild instanceof PsiWhiteSpace) && !currentChild.getText().isEmpty()
                        && isSelectionCandidate) {
                    return selectionerInstance.selectNext(enclosingElement, currentChild, selectedRange);
                }
            } else {
                // next child is selection candidate
                isSelectionCandidate = true;
            }
            currentChild = currentChild.getNextSibling();
        }
        
        return selectionerInstance.selectEnclosing(enclosingElement, selectedRange);
    }
    
}
