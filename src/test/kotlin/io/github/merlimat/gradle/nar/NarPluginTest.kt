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

import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class NarPluginTest {

    @Test
    fun `plugin applies java plugin`() {
        val project = ProjectBuilder.builder().build()
        project.pluginManager.apply("io.github.merlimat.nar")
        assertTrue(project.plugins.hasPlugin("java"))
    }

    @Test
    fun `nar task is registered`() {
        val project = ProjectBuilder.builder().build()
        project.pluginManager.apply("io.github.merlimat.nar")
        assertNotNull(project.tasks.findByName("nar"))
    }

    @Test
    fun `jar task is disabled`() {
        val project = ProjectBuilder.builder().build()
        project.pluginManager.apply("io.github.merlimat.nar")
        assertFalse(project.tasks.getByName("jar").enabled)
    }

    @Test
    fun `extension defaults narId to project name`() {
        val project = ProjectBuilder.builder().withName("my-connector").build()
        project.group = "com.example"
        project.version = "1.0.0"
        project.pluginManager.apply("io.github.merlimat.nar")

        val ext = project.extensions.getByType(NarExtension::class.java)
        assertEquals("my-connector", ext.narId.get())
        assertEquals("com.example", ext.narGroup.get())
        assertEquals("1.0.0", ext.narVersion.get())
        assertFalse(ext.cloneDuringInstanceClassLoading.get())
    }

    @Test
    fun `extension values can be overridden`() {
        val project = ProjectBuilder.builder().build()
        project.pluginManager.apply("io.github.merlimat.nar")

        val ext = project.extensions.getByType(NarExtension::class.java)
        ext.narId.set("custom-id")
        ext.cloneDuringInstanceClassLoading.set(true)

        assertEquals("custom-id", ext.narId.get())
        assertTrue(ext.cloneDuringInstanceClassLoading.get())
    }

    @Test
    fun `nar task has correct archive extension`() {
        val project = ProjectBuilder.builder().build()
        project.pluginManager.apply("io.github.merlimat.nar")

        val narTask = project.tasks.getByName("nar") as NarTask
        assertEquals("nar", narTask.archiveExtension.get())
    }
}
