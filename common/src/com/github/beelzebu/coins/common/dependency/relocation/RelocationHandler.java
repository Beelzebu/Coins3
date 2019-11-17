/*
 * This file is part of LuckPerms, licensed under the MIT License.
 *
 *  Copyright (c) lucko (Luck) <luck@lucko.me>
 *  Copyright (c) contributors
 *
 *  Permission is hereby granted, free of charge, to any person obtaining a copy
 *  of this software and associated documentation files (the "Software"), to deal
 *  in the Software without restriction, including without limitation the rights
 *  to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *  copies of the Software, and to permit persons to whom the Software is
 *  furnished to do so, subject to the following conditions:
 *
 *  The above copyright notice and this permission notice shall be included in all
 *  copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 *  SOFTWARE.
 */
package com.github.beelzebu.coins.common.dependency.relocation;

import com.github.beelzebu.coins.common.dependency.Dependency;
import com.github.beelzebu.coins.common.dependency.DependencyManager;
import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.nio.file.Path;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class RelocationHandler {

    private final Set<Dependency> dependencies = EnumSet.of(Dependency.ASM, Dependency.ASM_COMMONS, Dependency.JAR_RELOCATOR);
    private final Constructor<?> relocatorConstructor;
    private final Method relocatorMethod;

    public RelocationHandler(DependencyManager dependencyManager) {
        try {
            dependencyManager.loadDependencies(dependencies);
            Class<?> relocator = dependencyManager.obtainClassLoaderWith(dependencies).loadClass("me.lucko.jarrelocator.JarRelocator");
            relocatorConstructor = relocator.getDeclaredConstructor(File.class, File.class, Map.class);
            relocatorConstructor.setAccessible(true);
            relocatorMethod = relocator.getDeclaredMethod("run");
            relocatorMethod.setAccessible(true);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void remap(Path input, Path output, List<Relocation> relocations) throws Exception {
        Map<String, String> mappings = new HashMap<>();
        for (Relocation relocation : relocations) {
            mappings.put(relocation.getPattern(), relocation.getRelocatedPattern());
        }
        relocatorMethod.invoke(relocatorConstructor.newInstance(input.toFile(), output.toFile(), mappings));
    }
}
