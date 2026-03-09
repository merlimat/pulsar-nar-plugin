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

import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.util.jar.JarFile
import java.util.zip.ZipFile

class NarPluginFunctionalTest {

    @TempDir
    lateinit var projectDir: File

    private fun writeFile(path: String, content: String) {
        val file = File(projectDir, path)
        file.parentFile.mkdirs()
        file.writeText(content.trimIndent())
    }

    private fun setupSimpleProject() {
        writeFile("settings.gradle.kts", """rootProject.name = "test-connector"""")
        writeFile(
            "build.gradle.kts", """
            plugins {
                id("io.github.merlimat.nar")
            }
            group = "org.apache.pulsar"
            version = "1.0.0"
            repositories { mavenCentral() }
            dependencies {
                implementation("com.google.code.gson:gson:2.11.0")
            }
        """
        )
        writeFile(
            "src/main/java/com/example/MyConnector.java", """
            package com.example;
            public class MyConnector {}
        """
        )
        writeFile("src/main/resources/META-INF/services/pulsar-io.yaml", "name: test-connector\n")
    }

    private fun runner(vararg args: String) = GradleRunner.create()
        .withProjectDir(projectDir)
        .withArguments(*args, "--stacktrace")
        .withPluginClasspath()

    @Test
    fun `nar task produces a nar file with correct structure`() {
        setupSimpleProject()
        val result = runner("nar").build()

        assertEquals(TaskOutcome.SUCCESS, result.task(":nar")?.outcome)

        val narFile = File(projectDir, "build/libs/test-connector-1.0.0.nar")
        assertTrue(narFile.exists(), "NAR file should exist at build/libs/")

        ZipFile(narFile).use { zip ->
            // Compiled classes at root
            assertNotNull(
                zip.getEntry("com/example/MyConnector.class"),
                "Classes should be at archive root"
            )

            // Bundled dependencies
            val depEntries = zip.entries().asSequence()
                .filter { it.name.startsWith("META-INF/bundled-dependencies/") && !it.isDirectory }
                .map { it.name }
                .toList()
            assertTrue(depEntries.any { it.contains("gson-") }, "Gson should be in bundled-dependencies")

            // Service file
            assertNotNull(
                zip.getEntry("META-INF/services/pulsar-io.yaml"),
                "Service definition should be included"
            )
        }

        // Verify manifest
        JarFile(narFile).use { jar ->
            val attrs = jar.manifest.mainAttributes
            assertEquals("test-connector", attrs.getValue("Nar-Id"))
            assertEquals("org.apache.pulsar", attrs.getValue("Nar-Group"))
            assertEquals("1.0.0", attrs.getValue("Nar-Version"))
            assertEquals("false", attrs.getValue("Clone-During-Instance-Class-Loading"))
            assertNotNull(attrs.getValue("Build-Timestamp"))
            assertEquals("Pulsar NAR Gradle Plugin", attrs.getValue("Created-By"))
        }
    }

    @Test
    fun `compileOnly dependencies are not bundled`() {
        writeFile("settings.gradle.kts", """rootProject.name = "test-connector"""")
        writeFile(
            "build.gradle.kts", """
            plugins {
                id("io.github.merlimat.nar")
            }
            group = "org.apache.pulsar"
            version = "1.0.0"
            repositories { mavenCentral() }
            dependencies {
                compileOnly("org.slf4j:slf4j-api:2.0.17")
                implementation("com.google.code.gson:gson:2.11.0")
            }
        """
        )
        writeFile(
            "src/main/java/com/example/MyConnector.java", """
            package com.example;
            public class MyConnector {}
        """
        )

        runner("nar").build()

        val narFile = File(projectDir, "build/libs/test-connector-1.0.0.nar")
        ZipFile(narFile).use { zip ->
            val depEntries = zip.entries().asSequence()
                .filter { it.name.startsWith("META-INF/bundled-dependencies/") }
                .map { it.name }
                .toList()

            assertTrue(depEntries.any { it.contains("gson") }, "implementation dep should be bundled")
            assertFalse(depEntries.any { it.contains("slf4j-api") }, "compileOnly dep should NOT be bundled")
        }
    }

    @Test
    fun `custom extension values are reflected in manifest`() {
        writeFile("settings.gradle.kts", """rootProject.name = "test-connector"""")
        writeFile(
            "build.gradle.kts", """
            plugins {
                id("io.github.merlimat.nar")
            }
            group = "org.apache.pulsar"
            version = "1.0.0"
            repositories { mavenCentral() }
            nar {
                narId.set("custom-connector-id")
                cloneDuringInstanceClassLoading.set(true)
                buildTag.set("v1.0.0-rc1")
            }
        """
        )
        writeFile(
            "src/main/java/com/example/MyConnector.java", """
            package com.example;
            public class MyConnector {}
        """
        )

        runner("nar").build()

        val narFile = File(projectDir, "build/libs/custom-connector-id-1.0.0.nar")
        JarFile(narFile).use { jar ->
            val attrs = jar.manifest.mainAttributes
            assertEquals("custom-connector-id", attrs.getValue("Nar-Id"))
            assertEquals("true", attrs.getValue("Clone-During-Instance-Class-Loading"))
            assertEquals("v1.0.0-rc1", attrs.getValue("Build-Tag"))
        }
    }

    @Test
    fun `assemble triggers nar task`() {
        setupSimpleProject()
        val result = runner("assemble").build()
        assertEquals(TaskOutcome.SUCCESS, result.task(":nar")?.outcome)
    }

    @Test
    fun `jar task is disabled`() {
        setupSimpleProject()
        val result = runner("jar").build()
        assertEquals(TaskOutcome.SKIPPED, result.task(":jar")?.outcome)
        assertFalse(File(projectDir, "build/libs/test-connector-1.0.0.jar").exists())
    }
}
