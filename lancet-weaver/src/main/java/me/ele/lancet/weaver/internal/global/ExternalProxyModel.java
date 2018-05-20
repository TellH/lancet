package me.ele.lancet.weaver.internal.global;

import me.ele.lancet.weaver.internal.util.TextUtils;
import org.eclipse.jdt.internal.compiler.util.SimpleSet;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.ParameterNode;

import java.util.List;

/**
 * Created by leo.zhong on 2018/5/7.
 */
public class ExternalProxyModel {

    private static final String DEFAULT_GLOBAL_PROXY_CLASS = "com/GlobalProxyLancet";

    private String globalProxyClassName;
    private ClassWriter globalProxyExternalClassWriter;
    private final SimpleSet methodSet;

    public ClassWriter getGlobalProxyExternalClassWriter() {
        return globalProxyExternalClassWriter;
    }

    public String getGlobalProxyClassName() {
        return globalProxyClassName;
    }

    public void setGlobalProxyExternalClassWriter(ClassWriter globalProxyExternalClassWriter) {
        this.globalProxyExternalClassWriter = globalProxyExternalClassWriter;
    }

    public ExternalProxyModel(String className) {
        if (TextUtils.isEmpty(className)) {
            this.globalProxyClassName = DEFAULT_GLOBAL_PROXY_CLASS;
        } else {
            this.globalProxyClassName = className;
        }
        this.methodSet = new SimpleSet();
    }

    public void addMethodIfNotIncluded(MethodNode methodNode) {
        methodSet.addIfNotIncluded(methodNode);
    }

    public boolean includedMethod(MethodNode methodNode) {
        int length = methodSet.values.length;
        int index = (methodNode.hashCode() & 0x7FFFFFFF) % length;
        Object current;
        while ((current = methodSet.values[index]) != null) {
            if (compareMethodNode((MethodNode) current, methodNode)) return true;
            if (++index == length) index = 0;
        }
        return false;
    }

    private static boolean compareMethodNode(MethodNode m1, MethodNode m2) {
        if (m1 == null || m2 == null) {
            return false;
        }
//        if (m1.access == m2.access
//                && m1.annotationDefault == m2.annotationDefault
//                && m1.attrs == m2.attrs
//                && m1.desc.equals(m2.desc)
//                && m1.exceptions == m2.exceptions
//                && m1.instructions == m2.instructions
//                && m1.invisibleAnnotations == m2.invisibleAnnotations
//                && m1.invisibleLocalVariableAnnotations == m2.invisibleLocalVariableAnnotations
//                && m1.invisibleTypeAnnotations == m2.invisibleTypeAnnotations
//                && m1.localVariables == m2.localVariables
//                && m1.maxLocals == m2.maxLocals
//                && m1.maxStack == m2.maxStack
//                && m1.name.equals(m2.name)
//                && m1.parameters == m2.parameters
//                && m1.signature.equals(m2.signature)) {
//            return true;
//        }
        if (m1.access == m2.access && m1.desc.equals(m2.desc)) {
            if (m1.name.equals(m2.name)) {
                return true;
            }
            // 使用不同方法名，声明了多个相同的代理target方法  如 ： (Lcom/sample/playground/Cup;I)V
            throw new IllegalStateException("duplicate proxy method : " + m1.desc);
        }
        return false;
    }
}
