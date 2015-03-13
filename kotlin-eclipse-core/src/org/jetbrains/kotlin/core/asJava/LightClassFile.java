package org.jetbrains.kotlin.core.asJava;

import java.io.ByteArrayInputStream;
import java.io.File;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.core.log.KotlinLogger;

public class LightClassFile {
    private final IFile file;
    
    public LightClassFile(@NotNull IFile file) {
        this.file = file;
    }
    
    public boolean exists() {
        return file.exists();
    }
    
    public void createNewFile() {
        try {
            file.create(new ByteArrayInputStream(new byte[0]), true, null);
        } catch (CoreException e) {
            KotlinLogger.logAndThrow(e);
        }
    }
    
    @NotNull
    public File asFile() {
        return new File(file.getLocationURI().getSchemeSpecificPart());
    }
    
    @NotNull
    public IFile getResource() {
        return file;
    }
}
