package me.ele.lancet.plugin.test;

import com.android.build.api.transform.DirectoryInput;
import com.android.build.api.transform.JarInput;
import com.android.build.api.transform.Status;
import com.android.build.api.transform.TransformInput;
import com.android.build.api.transform.TransformInvocation;
import com.android.build.gradle.internal.pipeline.TransformInvocationBuilder;

import org.gradle.api.Project;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import me.ele.lancet.plugin.internal.GlobalContext;
import me.ele.lancet.plugin.internal.LocalCache;
import me.ele.lancet.plugin.internal.TransformContext;
import me.ele.lancet.plugin.internal.preprocess.PreClassAnalysis;

import static com.android.build.api.transform.QualifiedContent.Scope.TESTED_CODE;

public class Main {
    public static void main(String[] args) {
//        run();
    }

    public static void run(Project project) {
        File file = new File("/Users/tlh/Desktop/code/lancet/lancet_extra/android.jar");
        List<JarInput> jarInputs = new ArrayList<>();
        List<DirectoryInput> directoryInputs = new ArrayList<>();
        jarInputs.add(new ExtraJarInput(file));
        ImmutableTransformInput transformInput = new ImmutableTransformInput(jarInputs, directoryInputs, null);
        ArrayList<TransformInput> transformInputs = new ArrayList<>();
        transformInputs.add(transformInput);
        TransformInvocation transformInvocation = new TransformInvocationBuilder(null)
                .setIncrementalMode(false)
                .addInputs(transformInputs)
                .build();
        GlobalContext global = new GlobalContext(project);
        // load the LocalCache from disk
        LocalCache cache = new LocalCache(global.getLancetDir());
        TransformContext context = new TransformContext(transformInvocation, global);

        PreClassAnalysis preClassAnalysis = new PreClassAnalysis(cache, null);
        try {
            preClassAnalysis.execute(false, context);
            System.out.println(context.getGraph().toString());
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    public static class ExtraJarInput implements JarInput {

        private File file;

        public ExtraJarInput(File file) {
            this.file = file;
        }

        @Override
        public Status getStatus() {
            return Status.ADDED;
        }

        @Override
        public String getName() {
            return "android.jar";
        }

        @Override
        public File getFile() {
            return this.file;
        }

        @Override
        public Set<ContentType> getContentTypes() {
            HashSet<ContentType> contentTypes = new HashSet<>();
            contentTypes.add(DefaultContentType.CLASSES);
            return contentTypes;
        }

        @Override
        public Set<? super Scope> getScopes() {
            HashSet<? super Scope> set = new HashSet<>();
            set.add(TESTED_CODE);
            return set;
        }
    }
}
