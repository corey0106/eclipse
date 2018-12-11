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
package org.jetbrains.kotlin.testframework.editor;

import com.intellij.openapi.util.Condition;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.kotlin.testframework.utils.KotlinTestUtils;
import org.jetbrains.kotlin.testframework.utils.SourceFileData;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public abstract class KotlinEditorWithAfterFileTestCase extends KotlinEditorAutoTestCase {

    public enum AfterSuffixPosition {
        BEFORE_DOT, AFTER_NAME
    }

    protected AfterSuffixPosition getAfterPosition() {
        return AfterSuffixPosition.AFTER_NAME;
    }

    private static class WithAfterSourceFileData extends EditorSourceFileData {

        private static final Condition<WithAfterSourceFileData> TARGET_PREDICATE = new Condition<WithAfterSourceFileData>() {
            @Override
            public boolean value(WithAfterSourceFileData data) {
                return data.contentAfter != null;
            }
        };

        private static final String NO_TARGET_FILE_FOUND_ERROR_MESSAGE = "No target file found";
        private static final String NO_TARGET_FILE_FOUND_FOR_AFTER_FILE_ERROR_MESSAGE_FORMAT = "No target file found for \'%s\' file";

        private String contentAfter = null;

        public WithAfterSourceFileData(File file) {
            super(file);
        }

        public String getContentAfter() {
            return contentAfter;
        }

        public static Collection<WithAfterSourceFileData> getTestFiles(File testFolder) {
            Map<String, WithAfterSourceFileData> result = new HashMap<String, WithAfterSourceFileData>();

            File targetAfterFile = null;
            for (File file : testFolder.listFiles()) {
                String fileName = file.getName();

                if (!fileName.endsWith(AFTER_FILE_EXTENSION)) {
                    result.put(fileName, new WithAfterSourceFileData(file));
                } else {
                    targetAfterFile = file;
                }
            }

            if (targetAfterFile == null) {
                throw new RuntimeException(NO_TARGET_FILE_FOUND_ERROR_MESSAGE);
            }

            WithAfterSourceFileData target = result.get(targetAfterFile.getName().replace(AFTER_FILE_EXTENSION, ""));
            if (target == null) {
                throw new RuntimeException(String.format(NO_TARGET_FILE_FOUND_FOR_AFTER_FILE_ERROR_MESSAGE_FORMAT, targetAfterFile.getAbsolutePath()));
            }

            target.contentAfter = KotlinTestUtils.getText(targetAfterFile.getAbsolutePath());

            return result.values();
        }

        public static WithAfterSourceFileData getTargetFile(Iterable<WithAfterSourceFileData> files) {
            return ContainerUtil.find(files, TARGET_PREDICATE);
        }
    }

    private TextEditorTest testEditor;

    protected abstract void performTest(String fileText, String expectedFileText);

    protected TextEditorTest getTestEditor() {
        return testEditor;
    }

    protected boolean loadFilesBeforeOpeningEditor() {
        return false;
    }

    @Override
    protected void doSingleFileAutoTest(String testPath) {
        String fileText = loadEditor(testPath);

        String afterTestPath;
        AfterSuffixPosition afterPosition = getAfterPosition();
        if (afterPosition == AfterSuffixPosition.AFTER_NAME) {
            afterTestPath = testPath + AFTER_FILE_EXTENSION;
        } else {
            afterTestPath = testPath.substring(0, testPath.length() - getExtension().length()) + AFTER_FILE_EXTENSION + getExtension();
        }

        performTest(fileText, KotlinTestUtils.getText(afterTestPath));
    }

    @Override
    protected void doMultiFileAutoTest(File testFolder) {
        Collection<WithAfterSourceFileData> files = WithAfterSourceFileData.getTestFiles(testFolder);

        WithAfterSourceFileData target = WithAfterSourceFileData.getTargetFile(files);

        if (loadFilesBeforeOpeningEditor()) {
            loadFiles(files, target);
            testEditor = configureEditor(target.getFileName(), target.getContent(), target.getPackageName());
        } else {
            testEditor = configureEditor(target.getFileName(), target.getContent(), target.getPackageName());
            loadFiles(files, target);
        }

        performTest(target.getContent(), target.getContentAfter());
    }

    private void loadFiles(Collection<WithAfterSourceFileData> files, WithAfterSourceFileData target) {
        for (WithAfterSourceFileData file : files) {
            if (file != target) {
                createSourceFile(file.getPackageName(), file.getFileName(), file.getContent());
            }
        }
    }

    @Override
    protected void doAutoTestWithDependencyFile(String mainTestPath, File dependencyFile) {
        String fileText;

        if (loadFilesBeforeOpeningEditor()) {
            loadDependencyFile(dependencyFile);
            fileText = loadEditor(mainTestPath);
        } else {
            fileText = loadEditor(mainTestPath);
            loadDependencyFile(dependencyFile);
        }

        performTest(fileText, KotlinTestUtils.getText(mainTestPath + AFTER_FILE_EXTENSION));
    }

    private String loadEditor(String mainTestPath) {
        String fileText = KotlinTestUtils.getText(mainTestPath);
        testEditor = configureEditor(KotlinTestUtils.getNameByPath(mainTestPath), fileText,
                WithAfterSourceFileData.getPackageFromContent(fileText));
        return fileText;
    }

    private void loadDependencyFile(File dependencyFile) {
        try {
            SourceFileData dependencySourceFile = new SourceFileData(dependencyFile);
            String fileName = dependencySourceFile.getFileName();
            String dependencyFileName = fileName.substring(0, fileName.indexOf(FILE_DEPENDENCY_SUFFIX)) +
                    "_dependency" + getExtension();
            createSourceFile(dependencySourceFile.getPackageName(), dependencyFileName,
                    dependencySourceFile.getContent());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
