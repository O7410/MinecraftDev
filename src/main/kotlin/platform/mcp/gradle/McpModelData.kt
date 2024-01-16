/*
 * Minecraft Development for IntelliJ
 *
 * https://mcdev.io/
 *
 * Copyright (C) 2024 minecraft-dev
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published
 * by the Free Software Foundation, version 3.0 only.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.demonwav.mcdev.platform.mcp.gradle

import com.demonwav.mcdev.platform.mcp.McpModuleSettings
import com.intellij.openapi.externalSystem.model.Key
import com.intellij.openapi.externalSystem.model.ProjectKeys
import com.intellij.openapi.externalSystem.model.project.AbstractExternalEntityData
import com.intellij.openapi.externalSystem.model.project.ModuleData
import java.io.File

data class McpModelData(
    val module: ModuleData,
    val settings: McpModuleSettings.State,
    val taskName: String?,
    val accessTransformers: List<File>?,
) : AbstractExternalEntityData(module.owner) {
    companion object {
        // Process McpModelData after builtin services (e.g. dependency or module data)
        val KEY = Key.create(McpModelData::class.java, ProjectKeys.TASK.processingWeight + 1)
    }
}
