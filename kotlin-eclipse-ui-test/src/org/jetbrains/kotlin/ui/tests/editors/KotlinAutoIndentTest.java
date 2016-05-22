package org.jetbrains.kotlin.ui.tests.editors;

import org.junit.Ignore;
import org.junit.Test;

public class KotlinAutoIndentTest extends KotlinAutoIndentTestCase {
    @Override
    protected String getTestDataRelativePath() {
        return "../common_testData/ide/indentationOnNewline";
    }
    
    @Override
    protected AfterSuffixPosition getAfterPosition() {
        return AfterSuffixPosition.BEFORE_DOT;
    }
    
    @Test
    public void AfterCatch() {
        doAutoTest();
    }
    
    @Test
    public void AfterFinally() {
        doAutoTest();
    }
    
    @Test
    public void AfterImport() {
        doAutoTest();
    }
    
    @Test
    public void AfterTry() {
        doAutoTest();
    }
    
    @Test
    public void AssignmentAfterEq() {
        doAutoTest();
    }
    
    @Test
    public void BinaryWithTypeExpressions() {
        doAutoTest();
    }
    
    @Test
    public void ConsecutiveCallsAfterDot() {
        doAutoTest();
    }
    
    @Test
    public void ConsecutiveCallsInSaeCallsMiddle() {
        doAutoTest();
    }
    
    @Test
    public void ConsecutiveCallsInSafeCallsEnd() {
        doAutoTest();
    }
    
    @Test
    public void DoInFun() {
        doAutoTest();
    }
    
    @Test
    public void For() {
        doAutoTest();
    }
    
    @Test
    public void FunctionBlock() {
        doAutoTest();
    }
    
    @Test
    public void FunctionWithInference() {
        doAutoTest();
    }
    
    @Test
    public void If() {
        doAutoTest();
    }
    
    @Test
    public void InBinaryExpressionInMiddle() {
        doAutoTest();
    }
    
    @Test
    public void InBinaryExpressionsBeforeCloseParenthesis() {
        doAutoTest();
    }
    
    @Test
    public void InBinaryExpressionUnfinished() {
        doAutoTest();
    }
    
    @Test
    public void InDelegationListAfterColon() {
        doAutoTest();
    }
    
    @Test
    public void InDelegationListAfterComma() {
        doAutoTest();
    }
    
    @Test
    public void InDelegationListNotEmpty() {
        doAutoTest();
    }
    
    @Test
    public void InEnumAfterSemicolon() {
        doAutoTest();
    }
    
    @Test
    public void InEnumInitializerListAfterComma() {
        doAutoTest();
    }
    
    @Test
    public void InEnumInitializerListNotEmpty() {
        doAutoTest();
    }
    
    @Test
    public void InExpressionsParenthesesBeforeOperand() {
        doAutoTest();
    }
    
    @Test
    public void InLambdaBeforeParams() {
        doAutoTest();
    }
    
    @Test
    public void InLambdaInsideChainCallSameLine() {
        doAutoTest();
    }
    
    @Ignore("Format blocks relative to parent")
    @Test
    public void InLambdaInsideChainCallWithNewLine() {
        doAutoTest();
    }
    
    @Ignore("Format blocks relative to parent")
    @Test
    public void InLambdaInsideChainCallWithNewLineWithSpaces() {
        doAutoTest();
    }
    
    @Test
    public void InMultilineLambdaAfterArrow() {
        doAutoTest();
    }
    
    @Test
    public void IsExpressionAfterIs() {
        doAutoTest();
    }
    
    @Test
    public void MultideclarationAfterEq() {
        doAutoTest();
    }
    
    @Test
    public void MultideclarationBeforeEq() {
        doAutoTest();
    }
    
    @Test
    public void NotFirstParameter() {
        doAutoTest();
    }
    
    @Test
    public void PropertyWithInference() {
        doAutoTest();
    }
    
    @Test
    public void ReturnContinue() {
        doAutoTest();
    }
    
    @Test
    public void SettingAlignMultilineParametersInCalls() {
        doAutoTest();
    }
    
    @Test
    public void While() {
        doAutoTest();
    }
}
