package org.jetbrains.kotlin.core.filesystem;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.IJavaProject;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.analyzer.AnalysisResult;
import org.jetbrains.kotlin.codegen.ClassBuilderMode;
import org.jetbrains.kotlin.codegen.state.JetTypeMapper;
import org.jetbrains.kotlin.core.asJava.LightClassFile;
import org.jetbrains.kotlin.core.builder.KotlinPsiManager;
import org.jetbrains.kotlin.core.log.KotlinLogger;
import org.jetbrains.kotlin.core.model.KotlinAnalysisProjectCache;
import org.jetbrains.kotlin.core.model.KotlinEnvironment;
import org.jetbrains.kotlin.core.model.KotlinJavaManager;
import org.jetbrains.kotlin.core.utils.ProjectUtils;
import org.jetbrains.kotlin.descriptors.ClassDescriptor;
import org.jetbrains.kotlin.load.kotlin.PackageClassUtils;
import org.jetbrains.kotlin.load.kotlin.PackagePartClassUtils;
import org.jetbrains.kotlin.name.FqName;
import org.jetbrains.kotlin.psi.JetClassOrObject;
import org.jetbrains.kotlin.psi.JetFile;
import org.jetbrains.kotlin.resolve.BindingContext;
import org.jetbrains.org.objectweb.asm.Type;

import com.google.common.base.Predicate;
import com.google.common.collect.Lists;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.project.Project;
import com.intellij.psi.util.PsiTreeUtil;

public class KotlinLightClassManager {
    private final IJavaProject javaProject;
    
    private final ConcurrentMap<File, List<IFile>> sourceFiles = new ConcurrentHashMap<>();
    
    @NotNull
    public static KotlinLightClassManager getInstance(@NotNull IJavaProject javaProject) {
        Project ideaProject = KotlinEnvironment.getEnvironment(javaProject).getProject();
        return ServiceManager.getService(ideaProject, KotlinLightClassManager.class);
    }
    
    public KotlinLightClassManager(@NotNull IJavaProject javaProject) {
        this.javaProject = javaProject;
    }
    
    public void computeLightClassesSources(
            @NotNull BindingContext context) {
        JetTypeMapper typeMapper = new JetTypeMapper(context, ClassBuilderMode.LIGHT_CLASSES);
        IProject project = javaProject.getProject();
        Map<File, List<IFile>> newSourceFilesMap = new HashMap<>();
        for (IFile sourceFile : KotlinPsiManager.INSTANCE.getFilesByProject(project)) {
            List<IPath> lightClassesPaths = getLightClassesPaths(sourceFile, context, typeMapper);
            
            for (IPath path : lightClassesPaths) {
                LightClassFile lightClassFile = new LightClassFile(project.getFile(path));
                
                List<IFile> newSourceFiles = newSourceFilesMap.get(lightClassFile.asFile());
                if (newSourceFiles == null) {
                    newSourceFiles = new ArrayList<>();
                    newSourceFilesMap.put(lightClassFile.asFile(), newSourceFiles);
                }
                newSourceFiles.add(sourceFile);
            }
        }
        
        sourceFiles.clear();
        sourceFiles.putAll(newSourceFilesMap);
    }
    
    public void updateLightClasses(@NotNull Set<IFile> affectedFiles) {
        IProject project = javaProject.getProject();
        for (Map.Entry<File, List<IFile>> entry : sourceFiles.entrySet()) {
            IFile lightClassIFile = ResourcesPlugin.getWorkspace().getRoot().getFile(new Path(entry.getKey().getPath()));
            if (lightClassIFile == null) continue;
            
            LightClassFile lightClassFile = new LightClassFile(lightClassIFile);
            
            createParentDirsFor(lightClassFile);
            lightClassFile.createIfNotExists();
            
            for (IFile sourceFile : entry.getValue()) {
                if (affectedFiles.contains(sourceFile)) {
                    lightClassFile.touchFile();
                    break;
                }
            }
        }
        
        cleanOutdatedLightClasses(project);
    }
    
    public List<JetFile> getSourceFiles(@NotNull File file) {
        if (sourceFiles.isEmpty()) {
            AnalysisResult analysisResult = KotlinAnalysisProjectCache.INSTANCE$.getAnalysisResult(javaProject);
            computeLightClassesSources(analysisResult.getBindingContext());
        }
        
        return getSourceJetFiles(file);
    }
    
    @NotNull
    private List<JetFile> getSourceJetFiles(@NotNull File lightClass) {
        List<IFile> sourceIOFiles = sourceFiles.get(lightClass);
        if (sourceIOFiles != null) {
            List<JetFile> jetSourceFiles = Lists.newArrayList();
            for (IFile sourceFile : sourceIOFiles) {
                JetFile jetFile = KotlinPsiManager.getKotlinParsedFile(sourceFile);
                if (jetFile != null) {
                    jetSourceFiles.add(jetFile);
                }
            }
            
            return jetSourceFiles;
        }
        
        return Collections.<JetFile>emptyList();
    }

    @NotNull
    private List<IPath> getLightClassesPaths(
            @NotNull IFile sourceFile, 
            @NotNull BindingContext context,
            @NotNull JetTypeMapper typeMapper) {
        List<IPath> lightClasses = new ArrayList<IPath>();
        
        JetFile jetFile = KotlinPsiManager.INSTANCE.getParsedFile(sourceFile);
        for (JetClassOrObject classOrObject : PsiTreeUtil.findChildrenOfType(jetFile, JetClassOrObject.class)) {
            FqName fqName = classOrObject.getFqName();
            if (fqName == null) continue;
            
            ClassDescriptor descriptor = context.get(BindingContext.FQNAME_TO_CLASS_DESCRIPTOR, 
                    fqName.toUnsafe());
            if (descriptor != null) {
                Type asmType = typeMapper.mapClass(descriptor);
                lightClasses.add(computePathByInternalName(asmType.getInternalName()));
            }
        }
        
        if (PackagePartClassUtils.fileHasCallables(jetFile)) {
            String internalName = PackageClassUtils.getPackageClassInternalName(jetFile.getPackageFqName());
            lightClasses.add(computePathByInternalName(internalName));
        }
        
        return lightClasses;
    }
    
    private IPath computePathByInternalName(String internalName) {
        Path relativePath = new Path(internalName + ".class");
        return KotlinJavaManager.KOTLIN_BIN_FOLDER.append(relativePath);
    }
    
    private void cleanOutdatedLightClasses(IProject project) {
        ProjectUtils.cleanFolder(KotlinJavaManager.INSTANCE.getKotlinBinFolderFor(project), new Predicate<IResource>() {
            @Override
            public boolean apply(IResource resource) {
                if (resource instanceof IFile) {
                    IFile eclipseFile = (IFile) resource;
                    LightClassFile lightClass = new LightClassFile(eclipseFile);
                    List<IFile> sources = sourceFiles.get(lightClass.asFile());
                    return sources != null ? sources.isEmpty() : true;
                } else if (resource instanceof IFolder) {
                    try {
                        return ((IFolder) resource).members().length == 0;
                    } catch (CoreException e) {
                        KotlinLogger.logAndThrow(e);
                    } 
                }
                
                return false;
            }
        });
    }
    
    private void createParentDirsFor(@NotNull LightClassFile lightClassFile) {
        IFolder parent = (IFolder) lightClassFile.getResource().getParent();
        if (parent != null && !parent.exists()) {
            createParentDirs(parent);
        }
    }
    
    private void createParentDirs(IFolder folder) {
        IContainer parent = folder.getParent();
        if (!parent.exists()) {
            createParentDirs((IFolder) parent);
        }
        
        try {
            folder.create(true, true, null);
        } catch (CoreException e) {
            KotlinLogger.logAndThrow(e);
        }
    }
}