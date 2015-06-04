package org.jetbrains.kotlin.core.filesystem;

import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.ResourcesPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.core.builder.KotlinPsiManager;
import org.jetbrains.kotlin.psi.JetFile;

import com.google.common.collect.Lists;

public class KotlinLightClassManager {
    public static final KotlinLightClassManager INSTANCE = new KotlinLightClassManager();
    
    private final ConcurrentMap<File, List<File>> sourceFiles = new ConcurrentHashMap<>();
    
    private KotlinLightClassManager() {
    }
    
    public void putClass(@NotNull File lightClass, @NotNull List<File> sourceIOFiles) {
        sourceFiles.put(lightClass, sourceIOFiles);
    }
    
    public boolean isLightClass(@NotNull File file) {
        return sourceFiles.get(file) != null;
    }
    
    public void clear() {
        sourceFiles.clear();
    }
    
    @NotNull
    public List<JetFile> getSourceFiles(@NotNull File lightClass) {
        List<File> sourceIOFiles = sourceFiles.get(lightClass);
        if (sourceIOFiles != null) {
            List<JetFile> jetSourceFiles = Lists.newArrayList();
            for (File sourceFile : sourceIOFiles) {
                JetFile jetFile = getJetFileBySourceFile(sourceFile);
                if (jetFile != null) {
                    jetSourceFiles.add(jetFile);
                }
            }
            
            return jetSourceFiles;
        }
        
        return Collections.<JetFile>emptyList();
    }
    
    @NotNull
    public List<File> getIOSourceFiles(@NotNull File lightClass) {
        List<File> sourceIOFiles = sourceFiles.get(lightClass);
        return sourceIOFiles != null ? sourceIOFiles : Collections.<File>emptyList();
    }
    
    @Nullable
    public static IFile getEclipseFile(@NotNull File sourceFile) {
        IFile[] eclipseFile = ResourcesPlugin.getWorkspace().getRoot().findFilesForLocationURI(sourceFile.toURI());
        return eclipseFile.length == 1 ? eclipseFile[0] : null;
    }
    
    @Nullable
    public static JetFile getJetFileBySourceFile(@NotNull File sourceFile) {
        IFile file = getEclipseFile(sourceFile);
        return file != null ? KotlinPsiManager.getKotlinParsedFile(file) : null;
    }
}
