/*
 * Copyright 2010-2015 JetBrains s.r.o.
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
 */

package org.jetbrains.kotlin.core.resolve;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import kotlin.CollectionsKt;

import org.eclipse.jdt.core.IJavaProject;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.builtins.KotlinBuiltIns;
import org.jetbrains.kotlin.context.ContextKt;
import org.jetbrains.kotlin.context.MutableModuleContext;
import org.jetbrains.kotlin.core.model.KotlinEnvironment;
import org.jetbrains.kotlin.core.utils.ProjectUtils;
import org.jetbrains.kotlin.descriptors.ClassDescriptor;
import org.jetbrains.kotlin.descriptors.ConstructorDescriptor;
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor;
import org.jetbrains.kotlin.descriptors.MemberDescriptor;
import org.jetbrains.kotlin.descriptors.ModuleDescriptor;
import org.jetbrains.kotlin.descriptors.ModuleDescriptorKt;
import org.jetbrains.kotlin.descriptors.PackageFragmentDescriptor;
import org.jetbrains.kotlin.descriptors.PackageViewDescriptor;
import org.jetbrains.kotlin.frontend.di.InjectionKt;
import org.jetbrains.kotlin.name.Name;
import org.jetbrains.kotlin.platform.JvmBuiltIns;
import org.jetbrains.kotlin.platform.PlatformToKotlinClassMap;
import org.jetbrains.kotlin.psi.KtFile;
import org.jetbrains.kotlin.renderer.DescriptorRenderer;
import org.jetbrains.kotlin.resolve.BindingTraceContext;
import org.jetbrains.kotlin.resolve.DescriptorUtils;
import org.jetbrains.kotlin.resolve.TargetPlatform;
import org.jetbrains.kotlin.resolve.descriptorUtil.DescriptorUtilsKt;
import org.jetbrains.kotlin.resolve.jvm.platform.JvmPlatform;
import org.jetbrains.kotlin.resolve.lazy.ResolveSession;
import org.jetbrains.kotlin.resolve.lazy.declarations.FileBasedDeclarationProviderFactory;
import org.jetbrains.kotlin.resolve.scopes.KtScope;
import org.jetbrains.kotlin.serialization.deserialization.FindClassInModuleKt;

import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.SystemInfo;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VfsBundle;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.util.Function;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.io.URLUtil;

public class BuiltInsReferenceResolver {
    private static final String RUNTIME_SRC_DIR = "jar:file:"+ ProjectUtils.buildLibPath("kotlin-runtime-sources")+ "!/kotlin";

    private volatile ModuleDescriptor moduleDescriptor;
    private volatile PackageFragmentDescriptor builtinsPackageFragment = null;
    private final Project myProject;

    public BuiltInsReferenceResolver(Project project) {
        myProject = project;
        initialize();
    }
    
    @NotNull
    public static BuiltInsReferenceResolver getInstance(@NotNull IJavaProject javaProject) {
        Project ideaProject = KotlinEnvironment.getEnvironment(javaProject).getProject();
        return ServiceManager.getService(ideaProject, BuiltInsReferenceResolver.class);
    }

    private void initialize() {
        if (!areSourcesExist()) {
            return;
        }
        
        Set<KtFile> jetBuiltInsFiles = getBuiltInSourceFiles();
        
        //if the sources are present, then the value cannot be null
        assert (jetBuiltInsFiles != null);
        
        MutableModuleContext newModuleContext = ContextKt.ContextForNewModule(myProject,
                Name.special("<built-ins resolver module>"), 
                ModuleDescriptorKt.ModuleParameters(
                        JvmPlatform.INSTANCE.getDefaultModuleParameters().getDefaultImports(),
                        PlatformToKotlinClassMap.EMPTY
                ), 
                JvmBuiltIns.getInstance());
        newModuleContext.setDependencies(newModuleContext.getModule());
        
        FileBasedDeclarationProviderFactory declarationFactory = new FileBasedDeclarationProviderFactory(
                newModuleContext.getStorageManager(), jetBuiltInsFiles);
        
        ResolveSession resolveSession = InjectionKt.createLazyResolveSession(newModuleContext, declarationFactory,
                new BindingTraceContext(), TargetPlatform.Default.INSTANCE);
        
        newModuleContext.initializeModuleContents(resolveSession.getPackageFragmentProvider());
        
        PackageViewDescriptor packageView = newModuleContext.getModule().getPackage(
                KotlinBuiltIns.BUILT_INS_PACKAGE_FQ_NAME);
        List<PackageFragmentDescriptor> fragments = packageView.getFragments();
        
        moduleDescriptor = newModuleContext.getModule();
        builtinsPackageFragment = CollectionsKt.single(fragments);
    }

    @Nullable
    private Set<KtFile> getBuiltInSourceFiles() {
        URL url;
        try {
            url = new URL(RUNTIME_SRC_DIR);
        } catch (MalformedURLException e) {
            return null;
        }
        VirtualFile vf = getSourceVirtualFile();
        assert vf != null : "Virtual file not found by URL: " + url;
        
        PsiDirectory psiDirectory = PsiManager.getInstance(myProject).findDirectory(vf);
        assert psiDirectory != null : "No PsiDirectory for " + vf;
        return new HashSet<KtFile>(ContainerUtil.mapNotNull(psiDirectory.getFiles(), new Function<PsiFile, KtFile>() {
            @Override
            public KtFile fun(PsiFile file) {
                return file instanceof KtFile ? (KtFile) file : null;
            }
        }));
    }
    
    @Nullable
    private VirtualFile getSourceVirtualFile() {
        URL runtimeUrl;
        try {
            runtimeUrl = new URL(RUNTIME_SRC_DIR);
            String fromUrl = convertPathFromURL(runtimeUrl);
            return VirtualFileManager.getInstance().findFileByUrl(fromUrl);
        } catch (MalformedURLException e) {
            return null;
        }
    }
    
    private boolean areSourcesExist() {
        return getSourceVirtualFile() != null;
    }

    //the method is a copy of com.intellij.openapi.vfs.VfsUtilCore.convertFromUrl(URL)
    private String convertPathFromURL(URL url) {
        String protocol = url.getProtocol();
        String path = url.getPath();
        if (protocol.equals("jar")) {
          if (StringUtil.startsWithConcatenation(path, "file", ":")) {
            try {
              URL subURL = new URL(path);
              path = subURL.getPath();
            }
            catch (MalformedURLException e) {
              throw new RuntimeException(VfsBundle.message("url.parse.unhandled.exception"), e);
            }
          }
          else {
            throw new RuntimeException(new IOException(VfsBundle.message("url.parse.error", url.toExternalForm())));
          }
        }
        if (SystemInfo.isWindows || SystemInfo.isOS2) {
          while (!path.isEmpty() && path.charAt(0) == '/') {
            path = path.substring(1, path.length());
          }
        }

        path = URLUtil.unescapePercentSequences(path);
        String fromUrl = protocol + "://" + path;
        return fromUrl;
    }

    @Nullable
    private DeclarationDescriptor findCurrentDescriptorForMember(@NotNull MemberDescriptor originalDescriptor) {
        DeclarationDescriptor containingDeclaration = findCurrentDescriptor(originalDescriptor.getContainingDeclaration());
        KtScope memberScope = getMemberScope(containingDeclaration);
        if (memberScope == null) return null;

        String renderedOriginal = DescriptorRenderer.Companion.getFQ_NAMES_IN_TYPES().render(originalDescriptor);
        Collection<? extends DeclarationDescriptor> descriptors;
        if (originalDescriptor instanceof ConstructorDescriptor && containingDeclaration instanceof ClassDescriptor) {
            descriptors = ((ClassDescriptor) containingDeclaration).getConstructors();
        }
        else {
            descriptors = memberScope.getAllDescriptors();
        }
        for (DeclarationDescriptor member : descriptors) {
            if (renderedOriginal.equals(DescriptorRenderer.Companion.getFQ_NAMES_IN_TYPES().render(member))) {
                return member;
            }
        }
        return null;
    }

    @Nullable
    public DeclarationDescriptor findCurrentDescriptor(@NotNull DeclarationDescriptor originalDescriptor) {
        //if there's no sources
        if (moduleDescriptor == null) {
            return null;
        }
        
        if (!isFromBuiltinModule(originalDescriptor)) {
            return null;
        }
        
        if (originalDescriptor instanceof ClassDescriptor) {
            return FindClassInModuleKt.findClassAcrossModuleDependencies(
                    moduleDescriptor, 
                    DescriptorUtilsKt.getClassId((ClassDescriptor) originalDescriptor));
        }
        
        if (originalDescriptor instanceof PackageFragmentDescriptor) {
            return KotlinBuiltIns.BUILT_INS_PACKAGE_FQ_NAME.equals(((PackageFragmentDescriptor) originalDescriptor).getFqName())
                   ? builtinsPackageFragment
                   : null;
        }
        
        if (originalDescriptor instanceof MemberDescriptor) {
            return findCurrentDescriptorForMember((MemberDescriptor) originalDescriptor);
        }
        
        return null;
    }

    public static boolean isFromBuiltinModule(@NotNull DeclarationDescriptor originalDescriptor) {
        // TODO This is optimization only
        // It should be rewritten by checking declarationDescriptor.getSource(), when the latter returns something non-trivial for builtins.
        return JvmBuiltIns.getInstance().getBuiltInsModule().equals(DescriptorUtils.getContainingModule(originalDescriptor));
    }

    @Nullable
    private static KtScope getMemberScope(@Nullable DeclarationDescriptor parent) {
        if (parent instanceof ClassDescriptor) {
            return ((ClassDescriptor) parent).getDefaultType().getMemberScope();
        }
        else if (parent instanceof PackageFragmentDescriptor) {
            return ((PackageFragmentDescriptor) parent).getMemberScope();
        }
        else {
            return null;
        }
    }
}
