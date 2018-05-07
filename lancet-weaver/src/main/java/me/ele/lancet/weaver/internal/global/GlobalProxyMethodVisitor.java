package me.ele.lancet.weaver.internal.global;

import me.ele.lancet.weaver.internal.asm.ClassCollector;
import me.ele.lancet.weaver.internal.asm.ClassTransform;
import me.ele.lancet.weaver.internal.asm.MethodChain;
import me.ele.lancet.weaver.internal.entity.ProxyInfo;
import me.ele.lancet.weaver.internal.log.Log;
import me.ele.lancet.weaver.internal.util.TypeUtil;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.util.List;
import java.util.Map;

/**
 * Created by leo.zhong on 2018/5/7.
 */
public class GlobalProxyMethodVisitor extends MethodVisitor {

    private final Map<String, MethodChain.Invoker> invokerMap;
    private final Map<String, List<ProxyInfo>> matchMap;
    private final String className;
    private final String name;
    private final ClassCollector classCollector;
    private final MethodChain chain;
    private final ExternalProxyModel externalProxyModel;

    public GlobalProxyMethodVisitor(MethodChain chain, MethodVisitor mv, Map<String, MethodChain.Invoker> invokerMap, Map<String, List<ProxyInfo>> matchMap, String className, String name, ClassCollector classCollector, ExternalProxyModel externalProxyModel) {
        super(Opcodes.ASM5, mv);
        this.chain = chain;
        this.invokerMap = invokerMap;
        this.matchMap = matchMap;
        this.className = className;
        this.name = name;
        this.classCollector = classCollector;
        this.externalProxyModel = externalProxyModel;
        if (externalProxyModel == null) {
            throw new NullPointerException("ExternalProxyModel can't be null!");
        }
    }

    @Override
    public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean itf) {
        String key = owner + " " + name + " " + desc;
        List<ProxyInfo> infos = matchMap.get(key);
        MethodChain.Invoker invoker = invokerMap.get(key);
        if (invoker != null) {
            invoker.invoke(mv);
        } else if (infos != null && infos.size() > 0) {
            infos.forEach(info -> info.isEffective = true);
            String staticDesc = TypeUtil.descToStatic(opcode == Opcodes.INVOKESTATIC ? Opcodes.ACC_STATIC : 0, desc, owner);
            // begin hook this code.
            chain.headFromProxy(opcode, owner, name, desc);

            String artificialClassname = externalProxyModel.getGlobalProxyClassName();
            ClassVisitor cv = classCollector.getGlobalProxyClassVisitor(artificialClassname, externalProxyModel);

            Log.tag("transform").i("start weave Call method " + " for " + owner + "." + name + desc + " in " + className + "." + this.name);

            infos.forEach(c -> {
                if (TypeUtil.isStatic(c.sourceMethod.access) != (opcode == Opcodes.INVOKESTATIC)) {
                    throw new IllegalStateException(c.sourceClass + "." + c.sourceMethod.name + " should have the same static flag with " + owner + "." + name);
                }
                Log.tag("transform").i(" from " + c.sourceClass + "." + c.sourceMethod.name);

                String methodName = c.sourceClass.replace("/", "_") + "_" + c.sourceMethod.name;
                if (!externalProxyModel.includedMethod(methodName)) {
                    externalProxyModel.addMethodIfNotIncluded(methodName);
                    chain.next(artificialClassname, Opcodes.ACC_STATIC, methodName, staticDesc, c.threadLocalNode(), cv);
                }
            });

            invokerMap.put(key, chain.getHead());
            chain.getHead().invoke(mv);
        } else {
            super.visitMethodInsn(opcode, owner, name, desc, itf);
        }
    }
}
