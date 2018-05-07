package me.ele.lancet.weaver.internal.global;

import me.ele.lancet.weaver.internal.util.TextUtils;
import org.eclipse.jdt.internal.compiler.util.SimpleSet;
import org.objectweb.asm.ClassWriter;

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

    /**
     * @param str 全限定方法名 {@link com/sample/playground/LancetProxy#onCreate}
     *            com_sample_playground_TestLancet_onCreate
     */
    public void addMethodIfNotIncluded(String str) {
        methodSet.addIfNotIncluded(str);
    }

    public boolean includedMethod(String str) {
        return methodSet.includes(str);
    }
}
