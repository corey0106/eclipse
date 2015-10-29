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
package org.jetbrains.kotlin.core.references

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.psi.KtSimpleNameExpression
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.psi.KtReferenceExpression
import org.jetbrains.kotlin.descriptors.PackageViewDescriptor
import org.jetbrains.kotlin.core.resolve.EclipseDescriptorUtils
import org.jetbrains.kotlin.descriptors.SourceElement
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.resolve.calls.callUtil.getCall
import org.jetbrains.kotlin.resolve.calls.callUtil.getResolvedCall
import org.jetbrains.kotlin.resolve.calls.model.VariableAsFunctionResolvedCall
import org.jetbrains.kotlin.psi.Call
import org.jetbrains.kotlin.psi.KtConstructorDelegationReferenceExpression
import org.eclipse.jdt.core.IJavaProject

public fun createReference(element: KtReferenceExpression): KotlinReference {
    return when(element) {
        is KtSimpleNameExpression -> KotlinSimpleNameReference(element)
        is KtCallExpression -> KotlinInvokeFunctionReference(element)
        is KtConstructorDelegationReferenceExpression -> KotlinConstructorDelegationReference(element)
        else -> throw UnsupportedOperationException("Reference for $element is not supported")
    }
}

public interface KotlinReference {
    val expression: KtReferenceExpression
    
    fun getTargetDescriptors(context: BindingContext): Collection<DeclarationDescriptor>
}

public class KotlinSimpleNameReference(override val expression: KtSimpleNameExpression) : KotlinReference {
    override fun getTargetDescriptors(context: BindingContext) = expression.getReferenceTargets(context)
}

public class KotlinInvokeFunctionReference(override val expression: KtCallExpression) : KotlinReference {
    override fun getTargetDescriptors(context: BindingContext): Collection<DeclarationDescriptor> {
        val call = expression.getCall(context)
        val resolvedCall = call.getResolvedCall(context)
        return when {
            resolvedCall is VariableAsFunctionResolvedCall -> listOf(resolvedCall.functionCall.getCandidateDescriptor())
            call != null && resolvedCall != null && call.getCallType() == Call.CallType.INVOKE -> listOf(resolvedCall.getCandidateDescriptor())
            else -> emptyList()
        }
    }
}

public class KotlinConstructorDelegationReference(override val expression: KtConstructorDelegationReferenceExpression) : KotlinReference {
    override fun getTargetDescriptors(context: BindingContext) = expression.getReferenceTargets(context)
}

fun KtReferenceExpression.getReferenceTargets(context: BindingContext): Collection<DeclarationDescriptor> {
    val targetDescriptor = context[BindingContext.REFERENCE_TARGET, this]
    return if (targetDescriptor != null) {
            listOf(targetDescriptor) 
        } else {
            context[BindingContext.AMBIGUOUS_REFERENCE_TARGET, this].orEmpty()
        }
}