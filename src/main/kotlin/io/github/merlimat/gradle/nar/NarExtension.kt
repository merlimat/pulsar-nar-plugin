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

import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import javax.inject.Inject

abstract class NarExtension @Inject constructor(objects: ObjectFactory) {

    /** NAR identifier. Defaults to project.name. Maps to Nar-Id manifest attribute. */
    val narId: Property<String> = objects.property(String::class.java)

    /** NAR group. Defaults to project.group. Maps to Nar-Group manifest attribute. */
    val narGroup: Property<String> = objects.property(String::class.java)

    /** NAR version. Defaults to project.version. Maps to Nar-Version manifest attribute. */
    val narVersion: Property<String> = objects.property(String::class.java)

    /** Whether to clone the classloader during instance class loading. Defaults to false. */
    val cloneDuringInstanceClassLoading: Property<Boolean> =
        objects.property(Boolean::class.java).convention(false)

    /** Build tag for the Build-Tag manifest attribute. Defaults to git describe output. */
    val buildTag: Property<String> = objects.property(String::class.java)
}
