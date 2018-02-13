package me.ele.lancet.weaver.internal.asm.classvisitor.methodvisitor;

import org.objectweb.asm.MethodVisitor;

import me.ele.lancet.base.Scope;
import me.ele.lancet.weaver.internal.graph.ClassEntity;
import me.ele.lancet.weaver.internal.graph.Graph;
import me.ele.lancet.weaver.internal.graph.MethodEntity;
import me.ele.lancet.weaver.internal.graph.Node;
import me.ele.lancet.weaver.internal.util.TypeUtil;

import static me.ele.lancet.weaver.internal.asm.classvisitor.CheckMethodInvokeClassVisitor.MethodCallLocation;
import static me.ele.lancet.weaver.internal.asm.classvisitor.CheckMethodInvokeClassVisitor.SEPARATOR;
import static me.ele.lancet.weaver.internal.asm.classvisitor.CheckMethodInvokeClassVisitor.getMethodCache;
import static me.ele.lancet.weaver.internal.asm.classvisitor.CheckMethodInvokeClassVisitor.shouldCheck;

/**
 * Created by tlh on 2018/2/13.
 */

public class CheckNotFoundMethodVisitor extends MethodVisitor {
    private Graph graph;
    private String methodName;
    private String className;

    public CheckNotFoundMethodVisitor(int api, MethodVisitor mv, Graph graph, String methodName, String className) {
        super(api, mv);
        this.graph = graph;
        this.methodName = methodName;
        this.className = className;
    }

    @Override
    public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean itf) {
        if (shouldCheck(owner)) {
            if (itf) { // 接口调用，把所有都实现类的调用方法都放入Cache里
                graph.implementsOf(owner, Scope.ALL).forEach(node -> {
                    ClassEntity clz = node.entity;
                    if (!TypeUtil.isInterface(clz.access) && !TypeUtil.isAbstract(clz.access)) {
                        addMethodToCache(clz.name, name, desc);
                    }
                });
            } else if (isAbstract(owner, name, desc)) {
                graph.childrenOf(owner, Scope.ALL).forEach(node -> {
                    ClassEntity clz = node.entity;
                    if (!TypeUtil.isInterface(clz.access) && !TypeUtil.isAbstract(clz.access)) {
                        addMethodToCache(clz.name, name, desc);
                    }
                });
            } else {
                addMethodToCache(owner, name, desc);
            }
        }
        super.visitMethodInsn(opcode, owner, name, desc, itf);
    }

    private boolean isAbstract(String clazz, String method, String desc) {
        Node node = graph.get(clazz);
        if (node == null) return false;
        ClassEntity entity = node.entity;
        if (!TypeUtil.isAbstract(entity.access)) {
            return false;
        }
        for (MethodEntity m : entity.methods) {
            if (method.equals(m.name) && desc.equals(m.desc))
                return TypeUtil.isAbstract(m.access);
        }
        // 往上从父类中找方法
        Node parent = node.parent;
        while (parent != null) {
            for (MethodEntity m : parent.entity.methods) {
                if (methodName.equals(m.name) && desc.equals(m.desc)) {
                    // 找到这个方法
                    return TypeUtil.isAbstract(m.access);
                }
            }
            parent = parent.parent;
        }
        return TypeUtil.isAbstract(entity.access); // 有可能是抽象类里的接口方法
    }

    private void addMethodToCache(String ownerClz, String name, String desc) {
        String methodKey = String.join(SEPARATOR, ownerClz, name, desc);
        if (!getMethodCache().containsKey(methodKey)) {
            MethodCallLocation callLocation = new MethodCallLocation(false);
            callLocation.clzLoc = className;
            callLocation.methodLoc = methodName;
            getMethodCache().put(methodKey, callLocation);
        }
    }

}
