/*******************************************************************************
 * Copyright 2000-2014 JetBrains s.r.o.
 *  
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
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

import org.eclipse.jdt.core.dom.IBinding;
import org.jetbrains.kotlin.descriptors.ClassDescriptor;
import org.jetbrains.kotlin.descriptors.ConstructorDescriptor;
import org.jetbrains.kotlin.descriptors.SimpleFunctionDescriptor;
import org.jetbrains.kotlin.descriptors.VariableDescriptor;
import org.jetbrains.kotlin.util.slicedMap.Slices;
import org.jetbrains.kotlin.util.slicedMap.WritableSlice;

public class EclipseBindingContext {
    public static WritableSlice<IBinding, ConstructorDescriptor> ECLIPSE_CONSTRUCTOR = 
            Slices.<IBinding, ConstructorDescriptor>sliceBuilder().build();
    
    public static WritableSlice<IBinding, VariableDescriptor> ECLIPSE_VARIABLE =
            Slices.<IBinding, VariableDescriptor>sliceBuilder().build();

    public static WritableSlice<IBinding, ClassDescriptor> ECLIPSE_CLASS = 
            Slices.<IBinding, ClassDescriptor>sliceBuilder().build();

    public static WritableSlice<IBinding, SimpleFunctionDescriptor> ECLIPSE_FUNCTION = 
            Slices.<IBinding, SimpleFunctionDescriptor>sliceBuilder().build();
}
