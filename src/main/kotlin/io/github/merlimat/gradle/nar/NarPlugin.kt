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

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.tasks.bundling.Jar
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

class NarPlugin : Plugin<Project> {

    companion object {
        const val NAR_EXTENSION_NAME = "nar"
        const val NAR_TASK_NAME = "nar"
    }

    override fun apply(project: Project) {
        project.pluginManager.apply(JavaPlugin::class.java)

        val extension = project.extensions.create(NAR_EXTENSION_NAME, NarExtension::class.java)

        // Set conventions from project properties
        extension.narId.convention(project.provider { project.name })
        extension.narGroup.convention(project.provider { project.group.toString() })
        extension.narVersion.convention(project.provider { project.version.toString() })
        extension.buildTag.convention(project.provider { resolveGitTag(project) })

        val narTask = project.tasks.register(NAR_TASK_NAME, NarTask::class.java) { task ->
            // Wire extension to task
            task.narId.set(extension.narId)
            task.narGroup.set(extension.narGroup)
            task.narVersion.set(extension.narVersion)
            task.cloneDuringInstanceClassLoading.set(extension.cloneDuringInstanceClassLoading)
            task.buildTag.set(extension.buildTag)

            // Include compiled classes + resources
            val mainSourceSet = project.extensions
                .getByType(JavaPluginExtension::class.java)
                .sourceSets.getByName("main")
            task.from(mainSourceSet.output)

            // Bundle runtime dependencies into META-INF/bundled-dependencies/
            task.from(project.configurations.getByName("runtimeClasspath")) {
                it.into("META-INF/bundled-dependencies")
            }
            task.bundledDependencies.from(project.configurations.getByName("runtimeClasspath"))

            // Set deterministic manifest attributes at configuration time
            task.manifest { manifest ->
                manifest.attributes(
                    mapOf(
                        "Nar-Id" to extension.narId,
                        "Nar-Group" to extension.narGroup,
                        "Nar-Version" to extension.narVersion,
                        "Clone-During-Instance-Class-Loading" to extension.cloneDuringInstanceClassLoading,
                        "Created-By" to "Pulsar NAR Gradle Plugin",
                    )
                )
            }

            // Set timestamp and buildTag at execution time to avoid breaking up-to-date checks
            task.doFirst {
                val timestamp = DateTimeFormatter
                    .ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'")
                    .withZone(ZoneOffset.UTC)
                    .format(Instant.now())
                task.manifest { manifest ->
                    val attrs = mutableMapOf<String, String>("Build-Timestamp" to timestamp)
                    if (extension.buildTag.isPresent) {
                        attrs["Build-Tag"] = extension.buildTag.get()
                    }
                    manifest.attributes(attrs)
                }
            }

            // Archive naming
            task.archiveBaseName.set(extension.narId)
            task.archiveVersion.set(extension.narVersion)

            task.dependsOn(project.tasks.named("classes"))
        }

        // Hook into assemble
        project.tasks.named("assemble") { it.dependsOn(narTask) }

        // Disable default jar task
        project.tasks.named("jar", Jar::class.java) { it.enabled = false }

        // Register nar as default artifact
        project.artifacts { it.add("default", narTask) }
    }

    private fun resolveGitTag(project: Project): String? {
        return try {
            val process = ProcessBuilder("git", "describe", "--always", "--dirty")
                .directory(project.projectDir)
                .redirectErrorStream(true)
                .start()
            val output = process.inputStream.bufferedReader().readText().trim()
            if (process.waitFor() == 0 && output.isNotBlank()) output else null
        } catch (_: Exception) {
            null
        }
    }
}
