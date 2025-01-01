/*
 * Minecraft Development for IntelliJ
 *
 * https://mcdev.io/
 *
 * Copyright (C) 2025 minecraft-dev
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

package com.demonwav.mcdev.platform.mcp.aw.fixes

import com.demonwav.mcdev.facet.MinecraftFacet
import com.demonwav.mcdev.platform.fabric.FabricModuleType
import com.demonwav.mcdev.platform.mcp.actions.CopyAwAction
import com.demonwav.mcdev.platform.sponge.SpongeModuleType
import com.demonwav.mcdev.util.findModule
import com.intellij.codeInsight.daemon.QuickFixActionRegistrar
import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.codeInsight.quickfix.UnresolvedReferenceQuickFixProvider
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiJavaCodeReferenceElement

class CopyAwAccessibleEntryFix(val target: PsiElement, val element: PsiElement) : IntentionAction {

    class Provider : UnresolvedReferenceQuickFixProvider<PsiJavaCodeReferenceElement>() {

        override fun registerFixes(ref: PsiJavaCodeReferenceElement, registrar: QuickFixActionRegistrar) {
            val module = ref.findModule() ?: return
            val isApplicable = MinecraftFacet.getInstance(module, FabricModuleType, SpongeModuleType) != null
            if (!isApplicable) {
                return
            }

            val resolve = ref.advancedResolve(true)
            val target = resolve.element
            if (target != null && !resolve.isAccessible) {
                registrar.register(CopyAwAccessibleEntryFix(target, ref))
            }
        }

        override fun getReferenceClass(): Class<PsiJavaCodeReferenceElement> = PsiJavaCodeReferenceElement::class.java
    }

    override fun startInWriteAction(): Boolean = false

    override fun getText(): String = "Copy AW entry"

    override fun getFamilyName(): String = "Copy AW entry for inaccessible element"

    override fun isAvailable(project: Project, editor: Editor?, file: PsiFile?): Boolean = true

    override fun invoke(project: Project, editor: Editor?, file: PsiFile?) {
        CopyAwAction.doCopy(target, element, editor, null)
    }
}
