package me.ele.lancet.weaver.internal.global;

import me.ele.lancet.weaver.internal.entity.ProxyInfo;
import me.ele.lancet.weaver.internal.exception.ErrorManager;
import me.ele.lancet.weaver.internal.log.Log;
import me.ele.lancet.weaver.internal.util.TextUtils;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.MethodNode;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 * Created by leo.zhong on 2018/5/7.
 */
public class ExternalProxyModel {

    private static final String DEFAULT_GLOBAL_PROXY_CLASS = "com/GlobalProxyLancet";

    private String globalProxyClassName;
    private ClassWriter globalProxyExternalClassWriter;
    private final Set methodSet;

    public ClassWriter getGlobalProxyExternalClassWriter() {
        if (globalProxyExternalClassWriter == null) {
            globalProxyExternalClassWriter = new ClassWriter(ClassWriter.COMPUTE_MAXS);
            globalProxyExternalClassWriter.visit(Opcodes.V1_7, Opcodes.ACC_PUBLIC | Opcodes.ACC_SUPER, globalProxyClassName, null, "java/lang/Object", null);
            MethodVisitor mv = globalProxyExternalClassWriter.visitMethod(Opcodes.ACC_PRIVATE, "<init>", "()V", null, null);
            mv.visitCode();
            mv.visitVarInsn(Opcodes.ALOAD, 0);
            mv.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);
            mv.visitInsn(Opcodes.RETURN);
            mv.visitMaxs(1, 1);
            mv.visitEnd();
        }
        return globalProxyExternalClassWriter;
    }

    public String getGlobalProxyClassName() {

        return globalProxyClassName;
    }

    public ExternalProxyModel(String className) {
        if (TextUtils.isEmpty(className)) {
            this.globalProxyClassName = DEFAULT_GLOBAL_PROXY_CLASS;
        } else {
            this.globalProxyClassName = className;
        }
        this.methodSet = new HashSet();
    }

    public void addMethodIfNotIncluded(ProxyInfo proxyInfo) {
        methodSet.add(proxyInfo);
    }

    public boolean includedMethod(ProxyInfo proxyInfo) {
        Iterator<ProxyInfo> it = methodSet.iterator();
        while (it.hasNext()) {
            if (compareMethodNode(it.next(), proxyInfo)) {
                return true;
            }
        }
        return false;
    }

    private boolean compareMethodNode(ProxyInfo p1, ProxyInfo p2) {
        if (p1 == null || p2 == null) {
            return false;
        }

        // 手动拼凑，解决android 全限定方法名问题
        String str1 = p1.toTargetMethodString();
        String str2 = p2.toTargetMethodString();
        if (TextUtils.isEmpty(str1) || TextUtils.isEmpty(str2)) {
            return false;
        }

        MethodNode m1 = p1.sourceMethod;
        MethodNode m2 = p2.sourceMethod;
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

        if (m1.access == m2.access && str1.equals(str2)) {
            if (m1.name.equals(m2.name)) {
                return true;
            }
            // 使用不同方法名，声明了多个相同的代理target方法  如 ： (Lcom/sample/playground/Cup;I)V
            ErrorManager.getInstance().throwException(new IllegalStateException(
                    "duplicate proxy method : " + p1.sourceClass + "#" + p1.sourceMethod.name + " -> "
                            + p2.sourceClass + "#" + p2.sourceMethod.name));
        }
        return false;
    }

    private String methodToString(MethodNode m1) {
        return "access:" + m1.access
                + "," + "name:" + m1.name
                + "," + "desc:" + m1.desc
                + "," + "signature:" + m1.signature;
    }
}
