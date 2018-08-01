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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import me.ele.lancet.plugin.internal.preprocess.MetaGraphGeneratorImpl;
import me.ele.lancet.weaver.internal.asm.classvisitor.CheckReferenceNotExistElementsClassVisitor;
import me.ele.lancet.weaver.internal.graph.ClassEntity;

/**
 * Created by tlh on 2018/2/7.
 */

public class ExtraCache {
    private File androidSDK;
    public List<ClassEntity> classMetas;
    public List<String> whiteList;

    private Gson gson = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    public ExtraCache(File dir) {
        this(dir, new String[]{"lancet_extra/white_list.json"});
    }

    public ExtraCache(File dir, String[] whiteListFilePaths) {
        androidSDK = new File(dir, "lancet_extra/" + "android_sdk.json");
        if (whiteListFilePaths == null) {
            whiteListFilePaths = new String[]{"lancet_extra/white_list.json"};
        }
        List<File> checkWhiteListFiles = new ArrayList<>();
        for (String path : whiteListFilePaths) {
            checkWhiteListFiles.add(new File(dir, path));
        }
        whiteList = loadWhiteList(checkWhiteListFiles);
        classMetas = load();
        CheckReferenceNotExistElementsClassVisitor.initCheckingMethodCLassVisitor(whiteList);
    }

    private List<String> loadWhiteList(List<File> checkWhiteListFiles) {
        List<String> result = new ArrayList<>();
        for (File file : checkWhiteListFiles) {
            if (file.exists() && file.isFile()) {
                try {
                    Reader reader = Files.newReader(file, Charsets.UTF_8);
                    result.addAll(gson.fromJson(reader, new TypeToken<List<String>>() {
                    }.getType()));
                } catch (IOException e) {
                    throw new RuntimeException("Fail to load android_sdk.json!");
                }
            }
        }
        return result;
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
