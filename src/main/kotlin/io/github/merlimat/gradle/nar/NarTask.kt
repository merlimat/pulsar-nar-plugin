/*
 * Copyright 2025 Matteo Merli <mmerli@apache.org>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.github.merlimat.gradle.nar

import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.bundling.Jar

@CacheableTask
abstract class NarTask : Jar() {

    /** Dependency JAR files to bundle into META-INF/bundled-dependencies/. */
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.NAME_ONLY)
    abstract val bundledDependencies: ConfigurableFileCollection

    @get:Input
    abstract val narId: Property<String>

    @get:Input
    abstract val narGroup: Property<String>

    @get:Input
    abstract val narVersion: Property<String>

    @get:Input
    abstract val cloneDuringInstanceClassLoading: Property<Boolean>

    @get:Input
    @get:Optional
    abstract val buildTag: Property<String>

    init {
        archiveExtension.set("nar")
        description = "Assembles a NAR archive containing classes and bundled dependencies"
        group = "build"
        isReproducibleFileOrder = true
        isPreserveFileTimestamps = false
    }
}
