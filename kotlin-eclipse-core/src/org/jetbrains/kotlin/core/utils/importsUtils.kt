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
package org.jetbrains.kotlin.core.utils

import org.jetbrains.kotlin.config.ApiVersion
import org.jetbrains.kotlin.config.LanguageFeature.DefaultImportOfPackageKotlinComparisons
import org.jetbrains.kotlin.config.LanguageVersion
import org.jetbrains.kotlin.config.LanguageVersionSettingsImpl
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.idea.util.ImportDescriptorResult
import org.jetbrains.kotlin.idea.util.ImportInsertHelper
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.resolve.ImportPath
import java.util.Comparator
import org.jetbrains.kotlin.resolve.jvm.platform.JvmPlatformAnalyzerServices
import org.jetbrains.kotlin.idea.util.ActionRunningMode

class KotlinImportInserterHelper : ImportInsertHelper() {
    override val importSortComparator: Comparator<ImportPath> = object : Comparator<ImportPath> {
        override fun compare(o1: ImportPath?, o2: ImportPath?): Int {
            return 0
        }
    }

    override fun importDescriptor(file: KtFile, descriptor: DeclarationDescriptor, actionRunningMode: ActionRunningMode, forceAllUnderImport: Boolean): ImportDescriptorResult {
        throw UnsupportedOperationException()
    }

    override fun isImportedWithDefault(importPath: ImportPath, contextFile: KtFile): Boolean {
        val defaultImports = JvmPlatformAnalyzerServices.getDefaultImports(
            if (LanguageVersionSettingsImpl.DEFAULT.supportsFeature(DefaultImportOfPackageKotlinComparisons)) LanguageVersionSettingsImpl.DEFAULT
            else LanguageVersionSettingsImpl(LanguageVersion.KOTLIN_1_0, ApiVersion.KOTLIN_1_0),
            true
        )
        return importPath.isImported(defaultImports)
    }

    override fun mayImportOnShortenReferences(descriptor: DeclarationDescriptor): Boolean {
        return false
    }

    override fun isImportedWithLowPriorityDefaultImport(importPath: ImportPath, contextFile: KtFile): Boolean =
        isImportedWithDefault(importPath, contextFile)
}


// TODO: obtain these functions from fqNameUtil.kt (org.jetbrains.kotlin.idea.refactoring.fqName)
fun FqName.isImported(importPath: ImportPath, skipAliasedImports: Boolean = true): Boolean {
    return when {
        skipAliasedImports && importPath.hasAlias() -> false
        importPath.isAllUnder && !isRoot -> importPath.fqName == this.parent()
        else -> importPath.fqName == this
    }
}

fun ImportPath.isImported(alreadyImported: ImportPath): Boolean {
    return if (isAllUnder || hasAlias()) this == alreadyImported else fqName.isImported(alreadyImported)
}

fun ImportPath.isImported(imports: Iterable<ImportPath>): Boolean = imports.any { isImported(it) }