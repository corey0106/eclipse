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
package org.jetbrains.kotlin.ui.tests.editors.organizeImports

import org.jetbrains.kotlin.testframework.editor.KotlinEditorWithAfterFileTestCase
import org.junit.Before
import org.jetbrains.kotlin.ui.editors.KotlinFileEditor
import org.jetbrains.kotlin.ui.editors.organizeImports.KotlinOrganizeImportsAction
import org.jetbrains.kotlin.testframework.utils.EditorTestUtils

abstract class KotlinOrganizeImportsTestCase : KotlinEditorWithAfterFileTestCase() {
	open val includeStdLib: Boolean = true
	
    @Before
    fun before() {
		if (includeStdLib) {
		    configureProjectWithStdLib()
		} else {
			configureProject()
		}
    }
    
    override fun performTest(fileText: String, expectedFileText: String) {
        val editor = testEditor.getEditor() as KotlinFileEditor
        
        editor.getAction(KotlinOrganizeImportsAction.ACTION_ID).run()
        
        EditorTestUtils.assertByEditor(editor, expectedFileText)
    }
}