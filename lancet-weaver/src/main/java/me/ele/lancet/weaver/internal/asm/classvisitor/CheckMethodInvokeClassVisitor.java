package me.ele.lancet.weaver.internal.asm.classvisitor;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

import me.ele.lancet.weaver.internal.asm.LinkedClassVisitor;
import me.ele.lancet.weaver.internal.asm.classvisitor.methodvisitor.CheckNotFoundMethodVisitor;
import me.ele.lancet.weaver.internal.graph.Graph;
import me.ele.lancet.weaver.internal.graph.Node;
import me.ele.lancet.weaver.internal.log.Log;
import me.ele.lancet.weaver.internal.util.TypeUtil;

/**
 * Created by tanlehua on 2018/2/6.
 * 检查每一次方法调用是否存在对应的方法和相应的类
 */

public class CheckMethodInvokeClassVisitor extends LinkedClassVisitor {
    // 不能存接口方法和抽象方法
    // key是ClassName#MethodName#descriptor
    private static volatile Map<String, MethodCallLocation> methodCache;
    private Graph graph;
    private static volatile Set<Pattern> excludeClass; // 白名单，这些类的方法不检查
    public static final String SEPARATOR = "#";

    private String className;
    private boolean isInterface;

    private boolean shouldCheck;

    public CheckMethodInvokeClassVisitor(Graph graph) {
        this.graph = graph;
    }

    @Override
    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        this.className = name;
        this.isInterface = TypeUtil.isInterface(access);
        this.shouldCheck = shouldCheck(className);
        if (this.shouldCheck) {
            Node classNode = graph.get(className);
            if (classNode != null) {
                classNode.entity.methods.forEach(m -> {
                    String key = String.join(SEPARATOR, className, m.name, m.desc);
                    // 排除接口方法和抽象方法
                    if (!isInterface && !TypeUtil.isAbstract(m.access)) {
                        getMethodCache().put(key, new MethodCallLocation(true));
                    }
                });
            }
        }
        super.visit(version, access, name, signature, superName, interfaces);
    }

    public static boolean shouldCheck(String className) {
        boolean matched = false;
        for (Pattern pattern : getExcludeClass()) {
            if (pattern.matcher(className).matches()) {
                matched = true;
                break;
            }
        }
        return !matched;
    }

    @Override
    public MethodVisitor visitMethod(int access, String methodName, String desc, String signature, String[] exceptions) {
        MethodVisitor mv = super.visitMethod(access, methodName, desc, signature, exceptions);
        if (shouldCheck) {
            mv = new CheckNotFoundMethodVisitor(Opcodes.ASM5, mv, graph, methodName, className);
        }
        return mv;
    }

    public static Map<String, MethodCallLocation> getMethodCache() {
        if (methodCache == null) {
            synchronized (CheckMethodInvokeClassVisitor.class) {
                if (methodCache == null) methodCache = new ConcurrentHashMap<>();
            }
        }
        return methodCache;
    }

    public static Set<Pattern> getExcludeClass() {
        if (excludeClass == null) {
            synchronized (CheckMethodInvokeClassVisitor.class) {
                if (excludeClass == null) {
                    excludeClass = new HashSet<>();
                }
            }
        }
        return excludeClass;
    }

    public synchronized static void initCheckingMethodCLassVisitor(List<String> whiteList) {
        Set<Pattern> excludeClass = getExcludeClass();
        if (!excludeClass.isEmpty()) {
            excludeClass.clear();
        }
        excludeClass.add(Pattern.compile("android(/.+)"));
        excludeClass.add(Pattern.compile("java(/.+)"));
        excludeClass.add(Pattern.compile("org/apache(/.+)"));
        excludeClass.add(Pattern.compile("org/xml(/.+)"));
        excludeClass.add(Pattern.compile("org/w3c(/.+)"));
        excludeClass.add(Pattern.compile("org/json(/.+)"));
        excludeClass.add(Pattern.compile("org/xmlpull(/.+)"));
        excludeClass.add(Pattern.compile("com/android(/.+)"));
        excludeClass.add(Pattern.compile("javax(/.+)"));
        excludeClass.add(Pattern.compile("dalvik(/.+)"));
        excludeClass.add(Pattern.compile("(\\[L)+.+")); // 对象数组
        excludeClass.add(Pattern.compile("\\[+[BCDFISZJ]")); // 数组
        whiteList.forEach(s -> excludeClass.add(Pattern.compile(s)));

        excludeClass.forEach(clz -> Log.i("Exclude checking class: " + clz));

        Map<String, MethodCallLocation> methodCache = getMethodCache();
        if (!methodCache.isEmpty()) {
            methodCache.clear();
        }
    }

    public static class MethodCallLocation {
        public String clzLoc;
        public String methodLoc;
        public boolean exist;

        public MethodCallLocation(boolean exist) {
            this.exist = exist;
        }

        @Override
        public String toString() {
            return "{" +
                    "clzLoc='" + clzLoc + '\'' +
                    ", methodLoc='" + methodLoc + '\'' +
                    ", exist=" + exist +
                    '}';
        }
    }
}
