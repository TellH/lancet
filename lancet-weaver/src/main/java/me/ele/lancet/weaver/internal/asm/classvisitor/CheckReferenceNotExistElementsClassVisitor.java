package me.ele.lancet.weaver.internal.asm.classvisitor;

import com.android.utils.Pair;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.FieldVisitor;
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
 * 1. 检查每一次方法调用是否存在对应的方法和相应的类
 * 2. 检查是否引用了不存在的注解
 */

public class CheckReferenceNotExistElementsClassVisitor extends LinkedClassVisitor {
    // 不能存接口方法和抽象方法
    // key是ClassName#MethodName#descriptor
    private static volatile Map<String, MethodCallLocation> methodCache;
    private Graph graph;
    private static volatile Set<Pair<Pattern, Pattern>> excludeClass; // 白名单，这些类的方法不检查
    public static final String SEPARATOR = "#";

    private String className;
    private boolean isInterface;

    private static volatile Set<AnnotationLocation> notExistAnnotations;

    public CheckReferenceNotExistElementsClassVisitor(Graph graph) {
        this.graph = graph;
    }

    @Override
    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        this.className = name;
        this.isInterface = TypeUtil.isInterface(access);
        if (shouldCheck(className)) {
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
        return shouldCheck(className, ".*");
    }

    public static boolean shouldCheck(String className, String methodName) {
        boolean matched = false;
        for (Pair<Pattern, Pattern> pair : getExcludeClass()) {
            Pattern classPat = pair.getFirst();
            Pattern methodPat = pair.getSecond();
            if (classPat.matcher(className).matches() && methodPat.matcher(methodName).matches()) {
                matched = true;
                break;
            }

        }
        return !matched;
    }

    @Override
    public MethodVisitor visitMethod(int access, String methodName, String desc, String signature, String[] exceptions) {
        MethodVisitor mv = super.visitMethod(access, methodName, desc, signature, exceptions);
        if (shouldCheck(className, methodName)) {
            mv = new CheckNotFoundMethodVisitor(Opcodes.ASM5, mv, graph, methodName, className);
        }
        return mv;
    }

    @Override
    public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
        if (graph.get(TypeUtil.desc2Name(desc)) == null) {
            AnnotationLocation location = new AnnotationLocation(TypeUtil.desc2Name(desc));
            location.className = className;
            getNotExistAnnotations().add(location);
        }
        return super.visitAnnotation(desc, visible);
    }

    @Override
    public FieldVisitor visitField(int access, String name, String desc, String signature, Object value) {
        FieldVisitor fv = super.visitField(access, name, desc, signature, value);
        return new FieldVisitor(Opcodes.ASM5, fv) {
            @Override
            public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
                if (graph.get(TypeUtil.desc2Name(desc)) == null) {
                    AnnotationLocation location = new AnnotationLocation(TypeUtil.desc2Name(desc));
                    location.className = className;
                    location.fieldName = name;
                    getNotExistAnnotations().add(location);
                }
                return super.visitAnnotation(desc, visible);
            }
        };
    }

    public static Map<String, MethodCallLocation> getMethodCache() {
        if (methodCache == null) {
            synchronized (CheckReferenceNotExistElementsClassVisitor.class) {
                if (methodCache == null) methodCache = new ConcurrentHashMap<>();
            }
        }
        return methodCache;
    }

    public static Set<Pair<Pattern, Pattern>> getExcludeClass() {
        if (excludeClass == null) {
            synchronized (CheckReferenceNotExistElementsClassVisitor.class) {
                if (excludeClass == null) {
                    excludeClass = new HashSet<>();
                }
            }
        }
        return excludeClass;
    }

    public static Set<AnnotationLocation> getNotExistAnnotations() {
        if (notExistAnnotations == null) {
            synchronized (CheckReferenceNotExistElementsClassVisitor.class) {
                if (notExistAnnotations == null) {
                    notExistAnnotations = new HashSet<>();
                }
            }
        }
        return notExistAnnotations;
    }

    public synchronized static void initCheckingMethodCLassVisitor(List<String> whiteList) {
        Set<Pair<Pattern, Pattern>> excludeClass = getExcludeClass();
        if (!excludeClass.isEmpty()) {
            excludeClass.clear();
        }
        final Pattern matchAll = Pattern.compile(".*");
        excludeClass.add(Pair.of(Pattern.compile("android(/.+)"), matchAll));
        excludeClass.add(Pair.of(Pattern.compile("java(/.+)"), matchAll));
        excludeClass.add(Pair.of(Pattern.compile("org/apache(/.+)"), matchAll));
        excludeClass.add(Pair.of(Pattern.compile("org/xml(/.+)"), matchAll));
        excludeClass.add(Pair.of(Pattern.compile("org/w3c(/.+)"), matchAll));
        excludeClass.add(Pair.of(Pattern.compile("org/json(/.+)"), matchAll));
        excludeClass.add(Pair.of(Pattern.compile("org/xmlpull(/.+)"), matchAll));
        excludeClass.add(Pair.of(Pattern.compile("com/android(/.+)"), matchAll));
        excludeClass.add(Pair.of(Pattern.compile("javax(/.+)"), matchAll));
        excludeClass.add(Pair.of(Pattern.compile("dalvik(/.+)"), matchAll));
        excludeClass.add(Pair.of(Pattern.compile("(\\[L)+.+"), matchAll)); // 对象数组
        excludeClass.add(Pair.of(Pattern.compile("\\[+[BCDFISZJ]"), matchAll)); // 数组
        if (whiteList != null) {
            whiteList.forEach(s -> {
                String[] split = s.split("#");
                if (split.length == 1) {
                    excludeClass.add(Pair.of(Pattern.compile(resolveRegexChar(s)), matchAll));
                } else if (split.length == 2) {
                    excludeClass.add(Pair.of(Pattern.compile(resolveRegexChar(split[0])), Pattern.compile(resolveRegexChar(split[1]))));
                }
            });
        }

        excludeClass.forEach(clz -> Log.i("Exclude checking class: " + clz));

        Map<String, MethodCallLocation> methodCache = getMethodCache();
        if (!methodCache.isEmpty()) {
            methodCache.clear();
        }

        Set<AnnotationLocation> notExistAnnotations = getNotExistAnnotations();
        if (!notExistAnnotations.isEmpty()) {
            notExistAnnotations.clear();
        }
    }

    private static String resolveRegexChar(String s) {
        // 内部类的类名定义用的是$做分隔符
        s = s.replaceAll("\\$", "\\\\\\$");
        return s;
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

    public static class AnnotationLocation {
        public String annoName;
        public String className;
        public String fieldName;
        public String methodName;

        public AnnotationLocation(String annoName) {
            this.annoName = annoName;
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("Annotation: ").append(annoName).append(" at Class: ").append(className);
            if (fieldName != null && !fieldName.isEmpty()) {
                sb.append(" at Field: ").append(fieldName);
            }
            if (methodName != null && !methodName.isEmpty()) {
                sb.append(" at Method: ").append(methodName);
            }
            return sb.toString();
        }
    }
}
