package me.ele.lancet.weaver.spi;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.util.Map;

import me.ele.lancet.weaver.internal.asm.LinkedClassVisitor;

import static org.objectweb.asm.Opcodes.ACC_STATIC;
import static org.objectweb.asm.Opcodes.INVOKESTATIC;
import static org.objectweb.asm.Opcodes.IRETURN;
import static org.objectweb.asm.Opcodes.RETURN;

/**
 * Created by tlh on 2018/3/15.
 */

public class SpiClassVisitor extends LinkedClassVisitor {
    private final Map<String, String> spiServices;
    private final String injectClassName;

    public SpiClassVisitor(SpiModel spiModel) {
        spiServices = spiModel.getSpiServices();
        injectClassName = spiModel.getInjectClassName();
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
        MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
        if (isStaticBlock(access, name, desc)) {
            mv = new MethodVisitor(Opcodes.ASM5, mv) {
                @Override
                public void visitInsn(int opcode) {
                    if (opcode >= IRETURN && opcode <= RETURN) { // before return insn
                        for (Map.Entry<String, String> entry : spiServices.entrySet()) {
                            mv.visitLdcInsn(entry.getKey());
                            mv.visitLdcInsn(entry.getValue());
                            mv.visitMethodInsn(INVOKESTATIC, injectClassName, "addCache", "(Ljava/lang/String;Ljava/lang/String;)V", false);
                        }
                    }
                    super.visitInsn(opcode);
                }
            };
        }
        return mv;
    }

    private boolean isStaticBlock(int access, String name, String desc) {
        return access == ACC_STATIC && name.equals("<clinit>") && desc.equals("()V");
    }
}
