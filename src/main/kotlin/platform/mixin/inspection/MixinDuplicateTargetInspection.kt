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

import com.demonwav.mcdev.platform.mixin.util.MixinConstants.Annotations.MIXIN
import com.demonwav.mcdev.platform.mixin.util.findClassNodeByPsiClass
import com.demonwav.mcdev.platform.mixin.util.findClassNodeByQualifiedName
import com.demonwav.mcdev.platform.mixin.util.mixinTargets
import com.demonwav.mcdev.util.computeStringArray
import com.demonwav.mcdev.util.constantStringValue
import com.demonwav.mcdev.util.findModule
import com.demonwav.mcdev.util.resolveClass
import com.demonwav.mcdev.util.resolveClassArray
import com.intellij.codeInsight.intention.QuickFixFactory
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.JavaElementVisitor
import com.intellij.psi.PsiAnnotation
import com.intellij.psi.PsiArrayInitializerMemberValue
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.PsiExpression
import org.objectweb.asm.tree.ClassNode

class MixinDuplicateTargetInspection : MixinInspection() {

    override fun getStaticDescription() =
        "Targeting the same class multiple times in a mixin is redundant"

    override fun buildVisitor(holder: ProblemsHolder): PsiElementVisitor = Visitor(holder)

    private class Visitor(private val holder: ProblemsHolder) : JavaElementVisitor() {

        override fun visitClass(psiClass: PsiClass) {
            val mixinAnnotation = psiClass.modifierList?.findAnnotation(MIXIN) ?: return

            if (psiClass.mixinTargets.size != psiClass.mixinTargets.distinct().size) {
                goOverValues(psiClass, mixinAnnotation)
                goOverTargets(psiClass, mixinAnnotation)
            }
        }

        private fun goOverValues(psiClass: PsiClass, mixinAnnotation: PsiAnnotation) {

            // Read class targets (value)
            val value = mixinAnnotation.findDeclaredAttributeValue("value") ?: return
            if (value is PsiArrayInitializerMemberValue) {
                val classesElements = value.children.filterIsInstance<PsiExpression>()
                val classTargets = value.resolveClassArray()
                for (classTargetIndex in classTargets.indices) {
                    val classMixinTarget = classTargets[classTargetIndex]
                    val classNode = findClassNodeByPsiClass(classMixinTarget)
                    registerProblemIfProblematic(
                        psiClass,
                        classNode,
                        classTargetIndex,
                        classesElements[classTargetIndex]
                    )
                }
            } else {
                val targetClass = value.resolveClass() ?: return
                val classNode = findClassNodeByPsiClass(targetClass)
                val elementToDelete = mixinAnnotation.parameterList.attributes.find { it.name == "value" } ?: value
                registerProblemIfProblematic(psiClass, classNode, 0, elementToDelete)
            }
        }

        private fun goOverTargets(psiClass: PsiClass, mixinAnnotation: PsiAnnotation) {
            // Read string targets (targets)
            val targets = mixinAnnotation.findDeclaredAttributeValue("targets") ?: return
            val classTargetNames = targets.computeStringArray()
            val valueTargetCount = mixinAnnotation.findDeclaredAttributeValue("value")?.resolveClassArray()?.size ?: 0
            if (targets is PsiArrayInitializerMemberValue) {

                val classChildren = targets.children.filterIsInstance<PsiExpression>()

                for (i in classTargetNames.indices) {
                    val targetName = classTargetNames[i]

                    val classNode = findClassNodeByQualifiedName(
                        psiClass.project,
                        psiClass.findModule(),
                        targetName.replace('/', '.'),
                    )
                    registerProblemIfProblematic(psiClass, classNode, valueTargetCount + i, classChildren[i])
                }
            } else {
                if (targets.constantStringValue == null) return
                val classNode = findClassNodeByQualifiedName(
                    psiClass.project,
                    psiClass.findModule(),
                    targets.constantStringValue!!.replace('/', '.'),
                )
                val elementToDelete = mixinAnnotation.parameterList.attributes.find { it.name == "targets" } ?: targets
                registerProblemIfProblematic(psiClass, classNode, valueTargetCount, elementToDelete)
            }
        }

        private fun isClassNodeInTargets(
            psiClass: PsiClass,
            classNodeToCheck: ClassNode?,
            indexToIgnore: Int
        ): Boolean {
            return classNodeToCheck in psiClass.mixinTargets.subList(0, indexToIgnore) ||
                classNodeToCheck in psiClass.mixinTargets.subList(indexToIgnore + 1, psiClass.mixinTargets.size)
        }

        private fun registerProblemForDuplicate(duplicateExpression: PsiElement) {
            holder.registerProblem(
                duplicateExpression,
                "Duplicate target is redundant",
                QuickFixFactory.getInstance().createDeleteFix(duplicateExpression)
            )
        }

        private fun registerProblemIfProblematic(
            psiClass: PsiClass,
            classNode: ClassNode?,
            indexToIgnore: Int,
            possibleDuplicate: PsiElement
        ) {
            if (isClassNodeInTargets(psiClass, classNode, indexToIgnore)) {
                registerProblemForDuplicate(possibleDuplicate)
            }
        }
    }
}
