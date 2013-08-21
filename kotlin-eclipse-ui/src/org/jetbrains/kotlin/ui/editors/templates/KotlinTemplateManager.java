package org.jetbrains.kotlin.ui.editors.templates;

import java.io.IOException;

import org.eclipse.jface.text.templates.ContextTypeRegistry;
import org.eclipse.jface.text.templates.persistence.TemplateStore;
import org.eclipse.ui.editors.text.templates.ContributionContextTypeRegistry;
import org.eclipse.ui.editors.text.templates.ContributionTemplateStore;
import org.jetbrains.kotlin.core.log.KotlinLogger;
import org.jetbrains.kotlin.ui.Activator;

public class KotlinTemplateManager {
    
    public static final KotlinTemplateManager INSTANCE = new KotlinTemplateManager();
    
    private TemplateStore templateStore;
    private ContributionContextTypeRegistry contextTypeRegistry;
    
    private final String TEMPLATES_KEY = "org.jetbrains.kotlin.ui.templates.key";
    
    private KotlinTemplateManager() {
    }
    
    public ContextTypeRegistry getContextTypeRegistry() {
        if (contextTypeRegistry == null) {
            contextTypeRegistry = new ContributionContextTypeRegistry(KotlinTemplateContextType.CONTEXT_TYPE_REGISTRY);
        }
        
        contextTypeRegistry.addContextType(KotlinTemplateContextType.KOTLIN_ID_MEMBERS);
        
        return contextTypeRegistry;
    }
    
    public TemplateStore getTemplateStore() {
        if (templateStore == null) {
            templateStore = new ContributionTemplateStore(getContextTypeRegistry(), Activator.getDefault().getPreferenceStore(), TEMPLATES_KEY);
            try {
                templateStore.load();
            } catch (IOException e) {
                KotlinLogger.logAndThrow(e);
            }
        }
        
        return templateStore;
    }
}
