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
package org.jetbrains.kotlin.ui.tests.editors.formatter;

import org.junit.Ignore;
import org.junit.Test;

public class KotlinFormatActionTest extends KotlinFormatActionTestCase {
    @Override
    protected String getTestDataRelativePath() {
        return "format";
    }
    
    @Test
    public void classesAndPropertiesFormatTest() {
        doAutoTest();
    }
    
    @Test
    public void objectsAndLocalFunctionsFormatTest() {
        doAutoTest();
    }
    
    @Test
    public void packageFunctionsFormatTest() {
        doAutoTest();
    }
    
    @Test
    public void withBlockComments() {
    	doAutoTest();
    }
    
    @Test
    public void withJavaDoc() {
    	doAutoTest();
    }
    
    @Test
    public void withLineComments() {
    	doAutoTest();
    }
    
    @Test
    public void withoutComments() {
    	doAutoTest();
    }
        
    @Test
    public void initIndent() {
    	doAutoTest();
    }
    
    @Test
    public void newLineAfterImportsAndPackage() {
    	doAutoTest();
    }
    
    @Test
    public void withWhitespaceBeforeBrace() {
    	doAutoTest();
    }
    
    @Test
    public void withMutableVariable() {
    	doAutoTest();
    }
    
    @Test
    public void indentInWhenEntry() {
        doAutoTest();
    }
    
    @Test
    public void indentInDoWhile() {
        doAutoTest();
    }
    
    @Test
    public void indentInPropertyAccessor() {
        doAutoTest();
    }
    
    @Test
    public void indentInIfExpressionBlock() {
        doAutoTest();
    }
    
    @Test
    public void lambdaInBlock() {
        doAutoTest();
    }
    
    @Test
    public void formatSelection() {
        doAutoTest();
    }
    
    @Test
    public void respectCaretAfterFormatting() {
        doAutoTest();
    }
    
    @Ignore
    @Test
    public void blockCommentBeforeDeclaration() {
        doAutoTest();
    }
    
    @Test
    @Ignore
    public void formatScriptFile() {
        doAutoTest();
    }
    
    @Test
    public void commentOnTheLastLineOfLambda() {
        doAutoTest();
    }
}
