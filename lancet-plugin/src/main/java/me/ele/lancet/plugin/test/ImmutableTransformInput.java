package me.ele.lancet.plugin.test;

import com.android.build.api.transform.DirectoryInput;
import com.android.build.api.transform.JarInput;
import com.android.build.api.transform.TransformInput;
import com.google.common.base.MoreObjects;
import com.google.common.collect.ImmutableList;

import java.io.File;
import java.util.Collection;

class ImmutableTransformInput implements TransformInput {
    private File optionalRootLocation;
    private final Collection<JarInput> jarInputs;
    private final Collection<DirectoryInput> directoryInputs;

    ImmutableTransformInput(Collection<JarInput> jarInputs, Collection<DirectoryInput> directoryInputs, File optionalRootLocation) {
        this.jarInputs = ImmutableList.copyOf(jarInputs);
        this.directoryInputs = ImmutableList.copyOf(directoryInputs);
        this.optionalRootLocation = optionalRootLocation;
    }

    public Collection<JarInput> getJarInputs() {
        return this.jarInputs;
    }

    public Collection<DirectoryInput> getDirectoryInputs() {
        return this.directoryInputs;
    }

    public String toString() {
        return MoreObjects.toStringHelper(this).add("rootLocation", this.optionalRootLocation).add("jarInputs", this.jarInputs).add("folderInputs", this.directoryInputs).toString();
    }
}