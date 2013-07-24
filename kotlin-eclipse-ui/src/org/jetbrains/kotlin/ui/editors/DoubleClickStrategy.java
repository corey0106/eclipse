package org.jetbrains.kotlin.ui.editors;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextDoubleClickStrategy;
import org.eclipse.jface.text.ITextViewer;
import org.jetbrains.kotlin.core.log.KotlinLogger;

public class DoubleClickStrategy implements ITextDoubleClickStrategy {
    protected ITextViewer text;

    @Override
    public void doubleClicked(ITextViewer part) {
        int pos = part.getSelectedRange().x;

        if (pos < 0)
            return;

        text = part;

        if (!selectComment(pos)) {
            selectWord(pos);
        }
    }

    protected boolean selectComment(int caretPos) {
        IDocument doc = text.getDocument();
        int startPos, endPos;

        try {
            int pos = caretPos;
            char c = ' ';

            while (pos >= 0) {
                c = doc.getChar(pos);
                if (c == '\\') {
                    pos -= 2;
                    continue;
                }
                if (c == Character.LINE_SEPARATOR || c == '\"')
                    break;
                --pos;
            }

            if (c != '\"')
                return false;

            startPos = pos;

            pos = caretPos;
            int length = doc.getLength();
            c = ' ';

            while (pos < length) {
                c = doc.getChar(pos);
                if (c == Character.LINE_SEPARATOR || c == '\"')
                    break;
                ++pos;
            }
            if (c != '\"')
                return false;

            endPos = pos;

            int offset = startPos + 1;
            int len = endPos - offset;
            text.setSelectedRange(offset, len);
            return true;
        } catch (BadLocationException x) {
            KotlinLogger.logError(x);
        }

        return false;
    }

    protected boolean selectWord(int caretPos) {

        IDocument doc = text.getDocument();
        int startPos, endPos;

        try {

            int pos = caretPos;
            char c;

            while (pos >= 0) {
                c = doc.getChar(pos);
                if (!Character.isJavaIdentifierPart(c))
                    break;
                --pos;
            }

            startPos = pos;

            pos = caretPos;
            int length = doc.getLength();

            while (pos < length) {
                c = doc.getChar(pos);
                if (!Character.isJavaIdentifierPart(c))
                    break;
                ++pos;
            }

            endPos = pos;
            selectRange(startPos, endPos);
            return true;

        } catch (BadLocationException x) {
            KotlinLogger.logError(x);
        }

        return false;
    }

    private void selectRange(int startPos, int stopPos) {
        int offset = startPos + 1;
        int length = stopPos - offset;
        text.setSelectedRange(offset, length);
    }
}