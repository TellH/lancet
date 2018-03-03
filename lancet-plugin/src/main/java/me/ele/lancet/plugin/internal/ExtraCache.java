package me.ele.lancet.plugin.internal;

import com.android.build.api.transform.Status;
import com.google.common.io.Files;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import org.apache.commons.io.Charsets;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import me.ele.lancet.plugin.internal.preprocess.MetaGraphGeneratorImpl;
import me.ele.lancet.weaver.internal.asm.classvisitor.CheckMethodInvokeClassVisitor;
import me.ele.lancet.weaver.internal.graph.ClassEntity;
import me.ele.lancet.weaver.internal.log.Log;

/**
 * Created by tlh on 2018/2/7.
 */

public class ExtraCache {
    private File androidSDK;
    private File checkWhiteListFile;
    public List<ClassEntity> classMetas;
    public List<String> whiteList;

    private Gson gson = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    public ExtraCache(File dir) {
        androidSDK = new File(dir, "android_sdk.json");
        checkWhiteListFile = new File(dir, "white_list.json");
        classMetas = load();
        whiteList = loadWhiteList();
        CheckMethodInvokeClassVisitor.initCheckingClassWhiteList(whiteList);
    }

    private List<String> loadWhiteList() {
        if (checkWhiteListFile.exists() && checkWhiteListFile.isFile()) {
            try {
                Reader reader = Files.newReader(checkWhiteListFile, Charsets.UTF_8);
                return gson.fromJson(reader, new TypeToken<List<String>>() {
                }.getType());
            } catch (IOException e) {
                throw new RuntimeException("Fail to load android_sdk.json!");
            }
        }
        return Collections.emptyList();
    }

    private List<ClassEntity> load() {
        if (androidSDK.exists() && androidSDK.isFile()) {
            try {
                Reader reader = Files.newReader(androidSDK, Charsets.UTF_8);
                return gson.fromJson(reader, new TypeToken<List<ClassEntity>>() {
                }.getType());
            } catch (IOException e) {
                throw new RuntimeException("Fail to load android_sdk.json!");
            }
        }
        return Collections.emptyList();
    }

    public void accept(MetaGraphGeneratorImpl graph) {
        classMetas.forEach(m -> graph.add(m, Status.NOTCHANGED));
    }
}
