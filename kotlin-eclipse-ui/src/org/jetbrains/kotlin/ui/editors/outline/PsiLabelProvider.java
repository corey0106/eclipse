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
package org.jetbrains.kotlin.ui.editors.outline;

import java.util.List;

import org.eclipse.jdt.ui.ISharedImages;
import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.graphics.Image;
import org.jetbrains.kotlin.core.model.KotlinAnalysisFileCache;
import org.jetbrains.kotlin.descriptors.CallableDescriptor;
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor;
import org.jetbrains.kotlin.psi.KtClass;
import org.jetbrains.kotlin.psi.KtClassInitializer;
import org.jetbrains.kotlin.psi.KtDeclaration;
import org.jetbrains.kotlin.psi.KtElement;
import org.jetbrains.kotlin.psi.KtFile;
import org.jetbrains.kotlin.psi.KtFunction;
import org.jetbrains.kotlin.psi.KtPackageDirective;
import org.jetbrains.kotlin.psi.KtParameter;
import org.jetbrains.kotlin.psi.KtProperty;
import org.jetbrains.kotlin.psi.KtTypeReference;
import org.jetbrains.kotlin.renderer.DescriptorRenderer;
import org.jetbrains.kotlin.resolve.BindingContext;
import org.jetbrains.kotlin.types.KotlinType;

public class PsiLabelProvider extends LabelProvider {
    
    public static final String CLASS_INITIALIZER = "<class initializer>";
    
    @Override
    public String getText(Object element) {
        if (element instanceof KtElement) {
            return getPresentableElement((KtElement) element);
        }
        
        return "";
    }
    
    @Override
    public Image getImage(Object element) {
        String imageName = null;
        if (element instanceof KtClass) {
            if (((KtClass) element).isInterface()) {
                imageName = ISharedImages.IMG_OBJS_INTERFACE;
            } else {
                imageName = ISharedImages.IMG_OBJS_CLASS;
            }
        } else if (element instanceof KtPackageDirective) {
            imageName = ISharedImages.IMG_OBJS_PACKAGE;
        } else if (element instanceof KtFunction) {
            imageName = ISharedImages.IMG_OBJS_PUBLIC;
        } else if (element instanceof KtProperty) {
            imageName = ISharedImages.IMG_FIELD_PUBLIC;
        }
        
        if (imageName != null) {
            return JavaUI.getSharedImages().getImage(imageName);
        }
        
        return null;
    }
    
    // Source code is taken from org.jetbrains.kotlin.idea.projectView.JetDeclarationTreeNode, updateImple()
    private String getPresentableElement(KtElement declaration) {
        String text = "";
        if (declaration != null) {
            text = declaration.getName();
            if (text == null) return "";
            if (declaration instanceof KtClassInitializer) {
                text = CLASS_INITIALIZER;
            } else if (declaration instanceof KtProperty) {
                KtProperty property = (KtProperty) declaration;
                KtTypeReference ref = property.getTypeReference();
                if (ref != null) {
                    text += " ";
                    text += ":";
                    text += " ";
                    text += ref.getText();
                } else {
                    text += computeReturnType(property);
                }
            } else if (declaration instanceof KtFunction) {
                KtFunction function = (KtFunction) declaration;
                KtTypeReference receiverTypeRef = function.getReceiverTypeReference();
                if (receiverTypeRef != null) {
                    text = receiverTypeRef.getText() + "." + text;
                }
                text += "(";
                List<KtParameter> parameters = function.getValueParameters();
                for (KtParameter parameter : parameters) {
                    if (parameter.getName() != null) {
                        text += parameter.getName();
                        text += " ";
                        text += ":";
                        text += " ";
                    }
                    KtTypeReference typeReference = parameter.getTypeReference();
                    if (typeReference != null) {
                        text += typeReference.getText();
                    }
                    text += ", ";
                }
                if (parameters.size() > 0) text = text.substring(0, text.length() - 2);
                text += ")";
                KtTypeReference typeReference = function.getTypeReference();
                if (typeReference != null) {
                    text += " ";
                    text += ":";
                    text += " ";
                    text += typeReference.getText();
                } else {
                    text += computeReturnType(function);
                }
            }
        }
        
        return text;
    }
    
    private String computeReturnType(KtDeclaration ktDeclaration) {
        KtFile ktFile = ktDeclaration.getContainingKtFile();
        BindingContext bindingContext = KotlinAnalysisFileCache.INSTANCE.getAnalysisResult(ktFile).getAnalysisResult().getBindingContext();
        DeclarationDescriptor declarationDescriptor = bindingContext.get(BindingContext.DECLARATION_TO_DESCRIPTOR, ktDeclaration);
        if (declarationDescriptor instanceof CallableDescriptor) {
            CallableDescriptor callableDescriptor = (CallableDescriptor) declarationDescriptor;
            KotlinType returnType = callableDescriptor.getReturnType();
            if (returnType != null) {
                return " : " + DescriptorRenderer.ONLY_NAMES_WITH_SHORT_TYPES.renderType(returnType);
            }
        }
        
        return "";
    }
}
