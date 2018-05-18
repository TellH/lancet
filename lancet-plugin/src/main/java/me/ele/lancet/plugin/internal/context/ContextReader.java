package me.ele.lancet.plugin.internal.context;

import com.android.build.api.transform.JarInput;
import com.android.build.api.transform.QualifiedContent;
import com.android.build.api.transform.Status;
import com.google.common.collect.ImmutableList;
import com.google.common.io.Files;
import me.ele.lancet.plugin.Util;
import me.ele.lancet.plugin.internal.TransformContext;
import me.ele.lancet.plugin.internal.preprocess.ParseFailureException;
import me.ele.lancet.weaver.ClassData;
import me.ele.lancet.weaver.internal.entity.TransformInfo;
import me.ele.lancet.weaver.internal.log.Log;
import org.objectweb.asm.ClassWriter;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Created by gengwanpeng on 17/5/2.
 * <p>
 * This class will unzip all jars,and accept all class with input ClassFetcher in thread pool.
 * Used in pre-analysis and formal analysis.
 */
public class ContextReader {

    private AtomicBoolean lock = new AtomicBoolean(false);
    private TransformContext context;
    private ClassifiedContentProvider provider;
    private TransformInfo transformInfo;

    private ExecutorService service = new ThreadPoolExecutor(Runtime.getRuntime().availableProcessors(), Runtime.getRuntime().availableProcessors(),
            0L, TimeUnit.MILLISECONDS,
            new LinkedBlockingQueue<>(), (r, executor) -> {
        Log.i("partial parse failed, executor has been shutdown");
    });

    public ContextReader(TransformContext context, TransformInfo transformInfo) {
        this.context = context;
        this.transformInfo = transformInfo;
    }

    /**
     * read the classes in thread pool and send class to fetcher.
     *
     * @param incremental is incremental compile
     * @param fetcher     the fetcher to visit classes
     * @throws IOException
     * @throws InterruptedException
     */
    public void accept(boolean incremental, ClassFetcher fetcher) throws IOException, InterruptedException {

        provider = ClassifiedContentProvider.newInstance(new JarContentProvider(), new DirectoryContentProvider(incremental));
        // get all jars
        Collection<JarInput> jars = !incremental ? context.getAllJars() :
                ImmutableList.<JarInput>builder()
                        .addAll(context.getAddedJars())
                        .addAll(context.getRemovedJars())
                        .addAll(changedToDeleteAndAdd())
                        .build();
        // accept the jar in thread pool
        List<Future<Void>> tasks = Stream.concat(jars.stream(), context.getAllDirs().stream())
                .map(q -> new QualifiedContentTask(q, fetcher))
                .map(t -> service.submit(t))
                .collect(Collectors.toList());

        // block until all task has finish.
        for (Future<Void> future : tasks) {
            try {
                future.get();
            } catch (ExecutionException e) {
                if (incremental && e.getCause() instanceof ParseFailureException) {
                    shutDownAndRestart();
                    continue;
                }
                Throwable cause = e.getCause();
                if (cause instanceof IOException) {
                    throw (IOException) cause;
                } else if (cause instanceof InterruptedException) {
                    throw (InterruptedException) cause;
                } else {
                    throw new RuntimeException(e.getCause());
                }
            }
        }

        if (transformInfo != null && transformInfo.externalProxyModel != null) {
            ClassWriter classWriter = transformInfo.externalProxyModel.getGlobalProxyExternalClassWriter();
            if (classWriter != null) {
                ClassData classData = new ClassData(classWriter.toByteArray(), transformInfo.externalProxyModel.getGlobalProxyClassName());
                File file = context.getGrobalRelativeFile();
                File target = Util.toSystemDependentFile(file, classData.getClassName() + ".class");
                Files.createParentDirs(target);
                Files.write(classData.getClassBytes(), target);
            }
        }
    }

    /**
     * Transform the change operation to delete & add.
     *
     * @return
     */
    private Collection<? extends JarInput> changedToDeleteAndAdd() {
        List<JarInput> jarInputs = new ArrayList<>();
        context.getChangedJars().stream()
                .peek(c -> jarInputs.add(new StatusOverrideJarInput(context, c, Status.REMOVED)))
                .peek(c -> jarInputs.add(new StatusOverrideJarInput(context, c, Status.ADDED)));
        return jarInputs;
    }


    private void shutDownAndRestart() {
        if (lock.compareAndSet(false, true)) {
            service.shutdown();
            service = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
        }
    }

    /**
     * Task to accept target QualifiedContent
     */
    private class QualifiedContentTask implements Callable<Void> {

        private QualifiedContent content;
        private ClassFetcher fetcher;

        QualifiedContentTask(QualifiedContent content, ClassFetcher fetcher) {
            this.content = content;
            this.fetcher = fetcher;
        }

        @Override
        public Void call() throws Exception {
            provider.forEach(content, fetcher);
            return null;
        }
    }
}
