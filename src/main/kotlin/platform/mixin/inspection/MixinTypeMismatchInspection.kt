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

package com.demonwav.mcdev.platform.mixin.inspection

import com.demonwav.mcdev.platform.mixin.util.MixinConstants.Annotations.ACCESSOR
import com.demonwav.mcdev.platform.mixin.util.MixinConstants.Annotations.INVOKER
import com.demonwav.mcdev.platform.mixin.util.findClassNodeByPsiClass
import com.demonwav.mcdev.platform.mixin.util.hasAccess
import com.demonwav.mcdev.platform.mixin.util.isMixin
import com.demonwav.mcdev.platform.mixin.util.mixinAnnotation
import com.demonwav.mcdev.util.computeStringArray
import com.demonwav.mcdev.util.findQualifiedClass
import com.demonwav.mcdev.util.resolveClassArray
import com.intellij.codeInsight.intention.QuickFixFactory
import com.intellij.codeInspection.LocalQuickFixAndIntentionActionOnPsiElement
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.JavaElementVisitor
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElementVisitor
import org.objectweb.asm.Opcodes

class MixinTypeMismatchInspection : MixinInspection() {

    override fun getStaticDescription() =
        "Interface and class thing"

    override fun buildVisitor(holder: ProblemsHolder): PsiElementVisitor = Visitor(holder)

    // warning: horrible code ahead
    private class Visitor(private val holder: ProblemsHolder) : JavaElementVisitor() {

        override fun visitClass(psiClass: PsiClass) {

            // I have no idea where to get the logger if there is one

            // println()
            // println("hello")
            if (!psiClass.isMixin) {
                return
            }
            // println("yes mixin")

            // mixin is class and target is interface: not good
            // mixin is interface and not accessor and target is class: not good

            // because for some reason the mixin targets are always empty, it's always saying it's not an accessor, so I use my own method
            val isAccessorMixin = isAccessor(psiClass)

            if (isAccessorMixin) {
                // println("ACCESSOR, returning")
                return
            }

            // println("not accessor")
            val isMixinInterface = psiClass.isInterface
            val mixinTargets = getMixinTargets(psiClass)

            // println("isMixinInterface $isMixinInterface")

            // println("class targets: $mixinTargets")
            // println("mixin targets: ${psiClass.mixinTargets}") // for some reason, the mixin targets are always empty, so I use my own method to get them
            val classOrInterfaceKeyword =
                psiClass.nameIdentifier?.prevSibling?.prevSibling?.originalElement ?: psiClass.nameIdentifier ?: return
            for (mixinTarget in mixinTargets) {
                val isTargetInterface = mixinTarget.isInterface
                // println("isTargetInterface $isTargetInterface")
                if (isMixinInterface != isTargetInterface) {
                    val mixinType = if (isMixinInterface) "interface" else "class"
                    val targetType = if (isTargetInterface) "interface" else "class"
                    holder.registerProblem(
                        classOrInterfaceKeyword, "Mixin is $mixinType but target is $targetType",
                        QuickFixFactory.getInstance()
                            .createConvertInterfaceToClassFix(psiClass) as LocalQuickFixAndIntentionActionOnPsiElement
                    ) // makes all the methods abstract (not good) while still having a method body (good)
                    // still need a convert class to interface quick fix
                    break
                }
            }
        }

        fun isAccessor(psiClass: PsiClass): Boolean {

            if (!psiClass.isInterface) {
                return false
            }
            if (
                psiClass.methods.any {
                    it.modifierList.findAnnotation(ACCESSOR) == null &&
                        it.modifierList.findAnnotation(INVOKER) == null
                }
            ) {
                return false
            }

            val targets = getMixinTargets(psiClass)
            return targets.isNotEmpty() && !targets.any {
                findClassNodeByPsiClass(it)?.hasAccess(Opcodes.ACC_INTERFACE)
                    ?: false
            }
        }

        fun getMixinTargets(psiClass: PsiClass): MutableList<PsiClass> {
            val mixin = psiClass.mixinAnnotation ?: return mutableListOf()

            val mixinTargets =
                mixin.findDeclaredAttributeValue(null)?.resolveClassArray()
                    ?.toCollection(mutableListOf()) ?: mutableListOf()

            // Read and add string targets (targets)
            mixin.findDeclaredAttributeValue("targets")?.computeStringArray()
                ?.mapNotNullTo(mixinTargets) { name ->
                    findQualifiedClass(
                        mixin.project,
                        name.replace('/', '.')
                    )
                }
            return mixinTargets
        }
    }
}
