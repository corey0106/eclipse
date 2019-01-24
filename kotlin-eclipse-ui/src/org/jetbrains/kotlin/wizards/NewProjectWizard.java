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
package org.jetbrains.kotlin.wizards;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.jface.dialogs.MessageDialog;
import org.jetbrains.kotlin.core.model.KotlinNature;
import org.jetbrains.kotlin.utils.ThrowableExtensionsKt;

public class NewProjectWizard extends AbstractWizard<NewProjectWizardPage> {

    private static final String TITLE = "Kotlin project";
    private static final String DESCRIPTION = "Create a new Kotlin project";

    @Override
    public boolean performFinish() {
        NewProjectWizardPage page = getWizardPage();
        
        ProjectCreationOp op = new ProjectCreationOp(page.getProjectName(), page.getProjectLocation(), getShell());
        try {
            getContainer().run(true, true, op);
        } catch (InvocationTargetException e) {
            MessageDialog.openError(getShell(), AbstractWizard.ERROR_MESSAGE, e.getMessage());
            return false;
        } catch (InterruptedException e) {
            return false;
        }

        ProjectCreationOp.OperationResult result = op.getResult();
        if (result.isSuccess()) {
            KotlinNature.Companion.addNature(result.getProject());
            selectAndRevealResource(result.getProject());
            return true;
        } else {
            MessageDialog.openError(getShell(),
                    ThrowableExtensionsKt.getErrorTitleForMessageDialog(result.getException()),
                    ThrowableExtensionsKt.getErrorDescriptionForMessageDialog(result.getException()));
            return false;
        }
    }

    @Override
    protected String getPageTitle() {
        return TITLE;
    }

    @Override
    protected NewProjectWizardPage createWizardPage() {
        return new NewProjectWizardPage(TITLE, DESCRIPTION);
    }

}
