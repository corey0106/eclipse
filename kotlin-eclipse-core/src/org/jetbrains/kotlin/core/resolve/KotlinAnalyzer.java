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
package org.jetbrains.kotlin.core.resolve;

import org.eclipse.jdt.core.IJavaProject;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.analyzer.AnalysisResult;
import org.jetbrains.jet.lang.descriptors.impl.ModuleDescriptorImpl;
import org.jetbrains.jet.lang.resolve.BindingTraceContext;
import org.jetbrains.jet.lang.types.lang.KotlinBuiltIns;
import org.jetbrains.kotlin.core.utils.KotlinEnvironment;
import org.jetbrains.kotlin.core.utils.ProjectUtils;

import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.intellij.psi.PsiFile;

public class KotlinAnalyzer {

    @NotNull
    public static AnalysisResult analyzeDeclarations(@NotNull IJavaProject javaProject) {
        return analyzeProject(javaProject, Predicates.<PsiFile>alwaysFalse());
    }
    
    @NotNull
    public static AnalysisResult analyzeWholeProject(@NotNull IJavaProject javaProject) {
        return analyzeProject(javaProject, Predicates.<PsiFile>alwaysTrue());
    }

    @NotNull
    public static AnalysisResult analyzeOneFileCompletely(@NotNull IJavaProject javaProject, @NotNull PsiFile psiFile) {
        return analyzeProject(javaProject, Predicates.equalTo(psiFile));
    }
    
    private static AnalysisResult analyzeProject(@NotNull IJavaProject javaProject, @NotNull Predicate<PsiFile> filesToAnalyzeCompletely) {
        KotlinEnvironment kotlinEnvironment = KotlinEnvironment.getEnvironment(javaProject);
        return analysisResultProject(javaProject, kotlinEnvironment, filesToAnalyzeCompletely);
    }
    
    @NotNull
    private static AnalysisResult analysisResultProject(@NotNull IJavaProject javaProject, @NotNull KotlinEnvironment kotlinEnvironment, 
            @NotNull Predicate<PsiFile> filesToAnalyzeCompletely) {
        ModuleDescriptorImpl module = EclipseAnalyzerFacadeForJVM.createJavaModule("<module>");
        module.addDependencyOnModule(module);
        module.addDependencyOnModule(KotlinBuiltIns.getInstance().getBuiltInsModule());
        module.seal();
        
        return EclipseAnalyzerFacadeForJVM.analyzeFilesWithJavaIntegration(
                javaProject, 
                kotlinEnvironment.getProject(), 
                ProjectUtils.getSourceFilesWithDependencies(javaProject), 
                new BindingTraceContext(), 
                filesToAnalyzeCompletely, 
                module);
    }
}