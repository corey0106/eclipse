package org.jetbrains.kotlin.core.utils;

import static org.eclipse.core.resources.ResourcesPlugin.getWorkspace;

import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.runtime.CoreException;
import org.jetbrains.kotlin.core.builder.KotlinManager;
import org.jetbrains.kotlin.core.log.KotlinLogger;

public class KotlinFilesCollector {
    
    public static void collectForParsing() {
        try {
            new KotlinFilesCollector().addFilesToParse();
        } catch (CoreException e) {
            KotlinLogger.logError(e);
        }
    }
    
    private void addFilesToParse() throws CoreException {
        for (IProject project : getWorkspace().getRoot().getProjects()) {
            for (IResource resource : project.members(false)) {
                scanForFiles(resource);
            }
        }
    }
    
    private void scanForFiles(IResource parentResource) throws CoreException {
        if (KotlinManager.isCompatibleResource(parentResource)) {
            KotlinManager.updateProjectPsiSources(parentResource, IResourceDelta.ADDED);
            return; 
        }
        if (parentResource.getType() != IResource.FOLDER) {
            return;
        }
        IResource[] resources = ((IFolder) parentResource).members();
        for (IResource resource : resources) {
            scanForFiles(resource);
        }
    }

}
