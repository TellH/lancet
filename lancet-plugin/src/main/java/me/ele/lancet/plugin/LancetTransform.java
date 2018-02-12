package me.ele.lancet.plugin;

import com.android.build.api.transform.QualifiedContent;
import com.android.build.api.transform.SecondaryFile;
import com.android.build.api.transform.Transform;
import com.android.build.api.transform.TransformException;
import com.android.build.api.transform.TransformInvocation;
import com.android.build.gradle.internal.pipeline.TransformManager;
import com.google.common.base.Joiner;
import com.google.common.base.Strings;
import com.google.common.io.Files;

import org.apache.commons.io.Charsets;
import org.gradle.api.Project;

import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import me.ele.lancet.plugin.internal.ExtraCache;
import me.ele.lancet.plugin.internal.GlobalContext;
import me.ele.lancet.plugin.internal.LocalCache;
import me.ele.lancet.plugin.internal.TransformContext;
import me.ele.lancet.plugin.internal.TransformProcessor;
import me.ele.lancet.plugin.internal.context.ContextReader;
import me.ele.lancet.plugin.internal.preprocess.PreClassAnalysis;
import me.ele.lancet.weaver.MetaParser;
import me.ele.lancet.weaver.Weaver;
import me.ele.lancet.weaver.internal.AsmWeaver;
import me.ele.lancet.weaver.internal.asm.classvisitor.CheckMethodInvokeClassVisitor;
import me.ele.lancet.weaver.internal.asm.classvisitor.CheckMethodInvokeClassVisitor.MethodCallLocation;
import me.ele.lancet.weaver.internal.entity.InsertInfo;
import me.ele.lancet.weaver.internal.entity.ProxyInfo;
import me.ele.lancet.weaver.internal.entity.TransformInfo;
import me.ele.lancet.weaver.internal.graph.Graph;
import me.ele.lancet.weaver.internal.graph.MethodEntity;
import me.ele.lancet.weaver.internal.graph.Node;
import me.ele.lancet.weaver.internal.log.Impl.FileLoggerImpl;
import me.ele.lancet.weaver.internal.log.Log;
import me.ele.lancet.weaver.internal.parser.AsmMetaParser;

import static me.ele.lancet.weaver.internal.asm.classvisitor.CheckMethodInvokeClassVisitor.SEPARATOR;

class LancetTransform extends Transform {

    private final LancetExtension lancetExtension;
    private final GlobalContext global;
    private LocalCache cache;
    private ArrayList<String> errorLog = new ArrayList<>();


    public LancetTransform(Project project, LancetExtension lancetExtension) {
        this.lancetExtension = lancetExtension;
        this.global = new GlobalContext(project);
        // load the LocalCache from disk
        this.cache = new LocalCache(global.getLancetDir());

        List<String> taskNames = project.getGradle().getStartParameter().getTaskNames();
        for (int index = 0; index < taskNames.size(); ++index) {
            String taskName = taskNames.get(index);
            if (taskName.contains("assemble") || taskName.contains("resguard")) {
                if (taskName.toLowerCase().endsWith("debug") &&
                        taskName.toLowerCase().contains("debug")) {
                    Util.isDebugging = true;
                }
            }
        }
    }

    @Override
    public String getName() {
        return "lancet";
    }


    @Override
    public Set<QualifiedContent.ContentType> getInputTypes() {
        return TransformManager.CONTENT_CLASS;
    }


    @Override
    public Set<QualifiedContent.Scope> getScopes() {
        return TransformManager.SCOPE_FULL_PROJECT;
    }

    @Override
    public boolean isIncremental() {
        return true;
    }


    /**
     * @return Hook classes we found in last compilation. If they has been changed,gradle will auto go full compile.
     */
    @Override
    public Collection<SecondaryFile> getSecondaryFiles() {
        return cache.hookClassesInDir()
                .stream()
                .map(File::new)
                .map(SecondaryFile::nonIncremental)
                .collect(Collectors.toList());
    }

    @Override
    public Collection<File> getSecondaryDirectoryOutputs() {
        return Collections.singletonList(global.getLancetDir());
    }

    @Override
    public void transform(TransformInvocation transformInvocation) throws TransformException, InterruptedException, IOException {
        Util.setEnableCheckMethodNotFound(lancetExtension.isCheckMethodNotFoundEnable());
        initLog();

        Log.i("start time: " + System.currentTimeMillis());

        // collect the information this compile need
        TransformContext context = new TransformContext(transformInvocation, global);

        Log.i("after android plugin, incremental: " + context.isIncremental());
        Log.i("now: " + System.currentTimeMillis());

        boolean incremental = lancetExtension.getIncremental();
        PreClassAnalysis preClassAnalysis = new PreClassAnalysis(cache, new ExtraCache(global.getLancetExtraDir()));
        incremental = preClassAnalysis.execute(incremental, context);

        Log.i("after pre analysis, incremental: " + incremental);
        Log.i("now: " + System.currentTimeMillis());

        MetaParser parser = createParser(context);
        if (incremental && !context.getGraph().checkFlow()) {
            incremental = false;
            context.clear();
        }
        Log.i("after check flow, incremental: " + incremental);
        Log.i("now: " + System.currentTimeMillis());

        context.getGraph().flow().clear();
        TransformInfo transformInfo = parser.parse(context.getHookClasses(), context.getGraph());
        transformInfo.enableCheckMethodNotFound = Util.enableCheckMethodNotFound();

        Weaver weaver = AsmWeaver.newInstance(transformInfo, context.getGraph());
        Map<String, List<InsertInfo>> executeInfoBak = new HashMap<>();
        if (lancetExtension.isCheckUselessProxyMethodEnable()) {
            // backup @Insert executeInfo
            for (String k : transformInfo.executeInfo.keySet()) {
                List<InsertInfo> infosBak = new ArrayList<>();
                List<InsertInfo> insertInfos = transformInfo.executeInfo.get(k);
                infosBak.addAll(insertInfos);
                executeInfoBak.put(k, infosBak);
            }
        }
        new ContextReader(context).accept(incremental, new TransformProcessor(context, weaver));
        if (lancetExtension.isCheckUselessProxyMethodEnable()) {
            List<ProxyInfo> proxyInfoList = transformInfo.proxyInfo;
            proxyInfoList.forEach(info -> {
                if (!info.isTargetMethodExist) {
                    errorLog.add(String.format("@Proxy: %s target method is not exist!", info.toString()));
                }
                if (!info.isEffective) {
                    errorLog.add(String.format("@Proxy: %s is useless!", info.toString()));
                }
            });
            for (String k : executeInfoBak.keySet()) {
                List<InsertInfo> insertInfos = executeInfoBak.get(k);
                insertInfos.forEach(info -> {
                    if (!info.shouldIgoreCheck && !info.isTargetMethodExist) {
                        errorLog.add(String.format("@Insert: %s target method is not exist!", info.toString()));
                    }
                });
            }
        }
        if (Util.enableCheckMethodNotFound() && !preClassAnalysis.getExtraCache().classMetas.isEmpty()) {
            Map<String, MethodCallLocation> methodCache = CheckMethodInvokeClassVisitor.getMethodCache();
            Map<String, MethodCallLocation> pendingMethods = methodCache.entrySet().stream()
                    .filter(entry -> !entry.getValue().exist)
                    .filter(entry -> {
                        String[] split = entry.getKey().split(SEPARATOR);
                        String className = split[0];
                        return !checkIfSuperMethodExisted(context.getGraph(), className, split[1], split[2], entry.getValue())
                                && CheckMethodInvokeClassVisitor.shouldCheck(className);
                    })
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
            if (!pendingMethods.isEmpty()) {
                pendingMethods.forEach((k, v) -> {
                    String[] split = k.split(SEPARATOR);
                    String className = split[0];
                    errorLog.add(String.format("Class: %s, Method: %s, Desc: %s not found. It was called at Class: %s, Method: %s \n",
                            className, split[1], split[2], v.clzLoc, v.methodLoc));
                });
            }
        }

        handleErrorLog();

        Log.i("build successfully done");
        Log.i("now: " + System.currentTimeMillis());

        cache.saveToLocal();
        Log.i("cache saved");
        Log.i("now: " + System.currentTimeMillis());
    }

    private void handleErrorLog() {
        File log = new File(global.getLancetExtraDir(), "error_log.txt");
        try {
            Writer writer = Files.newWriter(log, Charsets.UTF_8);
            writer.write(errorLog.toString());
            writer.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        if (!errorLog.isEmpty() && lancetExtension.isStrictMode()) {
            throw new RuntimeException(errorLog.toString());
        }
    }

    private boolean checkIfSuperMethodExisted(Graph graph, String className, String methodName, String desc, MethodCallLocation value) {
        Node node = graph.get(className);
        if (node == null) {
            errorLog.add(String.format("Class: %s not found, with Method: %s. It was called at Class: %s, Method: %s \n",
                    className, methodName, value.clzLoc, value.methodLoc));
            return false;
        }
        Node parent = node.parent;
        while (parent != null) {
            for (MethodEntity m : parent.entity.methods) {
                if (methodName.equals(m.name) && desc.equals(m.desc))
                    return true;
            }
            parent = parent.parent;
        }
        return false;
    }

    private AsmMetaParser createParser(TransformContext context) {
        URL[] urls = Stream.concat(context.getAllJars().stream(), context.getAllDirs().stream()).map(QualifiedContent::getFile)
                .map(File::toURI)
                .map(u -> {
                    try {
                        return u.toURL();
                    } catch (MalformedURLException e) {
                        throw new AssertionError(e);
                    }
                })
                .toArray(URL[]::new);
        Log.d("urls:\n" + Joiner.on("\n ").join(urls));
        ClassLoader cl = URLClassLoader.newInstance(urls, null);
        return new AsmMetaParser(cl);
    }

    private void initLog() throws IOException {
        Log.setLevel(lancetExtension.getLogLevel());
        if (!Strings.isNullOrEmpty(lancetExtension.getFileName())) {
            String name = lancetExtension.getFileName();
            if (name.contains(File.separator)) {
                throw new IllegalArgumentException("Log file name can't contains file separator");
            }
            File logFile = new File(global.getLancetDir(), "log_" + lancetExtension.getFileName());
            Files.createParentDirs(logFile);
            Log.setImpl(FileLoggerImpl.of(logFile.getAbsolutePath()));
        }
    }
}

