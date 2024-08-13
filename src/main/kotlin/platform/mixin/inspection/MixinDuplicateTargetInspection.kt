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
import com.intellij.codeInsight.intention.QuickFixFactory
import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.LocalQuickFixAndIntentionActionOnPsiElement
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.JavaElementVisitor
import com.intellij.psi.PsiAnnotation
import com.intellij.psi.PsiArrayInitializerMemberValue
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiClassObjectAccessExpression
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.PsiExpression
import com.intellij.psi.PsiFile
import org.jetbrains.plugins.groovy.intentions.style.inference.resolve
import org.objectweb.asm.tree.ClassNode

class MixinDuplicateTargetInspection : MixinInspection() {

    override fun getStaticDescription() = "Targeting the same class multiple times in a mixin is redundant"

    override fun buildVisitor(holder: ProblemsHolder): PsiElementVisitor = Visitor(holder)

    private class Visitor(private val holder: ProblemsHolder) : JavaElementVisitor() {

        override fun visitClass(psiClass: PsiClass) {
            val mixinAnnotation = psiClass.modifierList?.findAnnotation(MIXIN) ?: return

            val mixinTargets = psiClass.mixinTargets

            if (mixinTargets.size != mixinTargets.distinct().size) {
                val nonDuplicates = mutableListOf<ClassNode>()
                for (i in mixinTargets.indices) {
                    val mixinTarget = mixinTargets[i]
                    if (mixinTarget !in mixinTargets.subList(
                            i + 1, mixinTargets.size
                        ) && mixinTarget !in mixinTargets.subList(0, i)
                    ) {
                        nonDuplicates.add(mixinTarget)
                    }
                }

                goOverValues(nonDuplicates, mixinAnnotation)
                goOverTargets(psiClass, nonDuplicates, mixinAnnotation)
            }
        }

        private fun goOverValues(nonDuplicates: List<ClassNode>, mixinAnnotation: PsiAnnotation) {

            // Read class targets (value)
            val value = mixinAnnotation.findDeclaredAttributeValue("value") ?: return
            if (value is PsiArrayInitializerMemberValue) {
                val classElements = value.children.filterIsInstance<PsiExpression>()
                for (classTargetIndex in classElements.indices) {
                    val expression = classElements[classTargetIndex] as? PsiClassObjectAccessExpression ?: continue
                    val targetPsiClass = expression.operand.type.resolve() ?: continue
                    val targetClassNode = findClassNodeByPsiClass(targetPsiClass)

                    registerProblemIfProblematic(
                        nonDuplicates,
                        targetClassNode,
                        expression,
                        QuickFixFactory.getInstance().createDeleteFix(expression)
                    )
                }
            } else {
                val targetClass = value.resolveClass() ?: return
                val classNode = findClassNodeByPsiClass(targetClass)
                val possibleQuickFix = RemoveDuplicateTargetFix(mixinAnnotation, "value")
                registerProblemIfProblematic(nonDuplicates, classNode, value, possibleQuickFix)
            }
        }

        private fun goOverTargets(psiClass: PsiClass, nonDuplicates: List<ClassNode>, mixinAnnotation: PsiAnnotation) {
            // Read string targets (targets)
            val targets = mixinAnnotation.findDeclaredAttributeValue("targets") ?: return
            val classTargetNames = targets.computeStringArray()
            if (targets is PsiArrayInitializerMemberValue) {
                val classChildren = targets.children.filterIsInstance<PsiExpression>()

                // TODO: may be misaligned if there are numbers for example in the class array because they will have different lengths
                for (classTargetIndex in classTargetNames.indices) {
                    val targetName = classTargetNames[classTargetIndex]

                    val classNode = findClassNodeByQualifiedName(
                        psiClass.project,
                        psiClass.findModule(),
                        targetName.replace('/', '.'),
                    )
                    registerProblemIfProblematic(
                        nonDuplicates,
                        classNode,
                        classChildren[classTargetIndex],
                        QuickFixFactory.getInstance().createDeleteFix(classChildren[classTargetIndex])
                    )
                }
            } else {
                val stringValue = targets.constantStringValue ?: return
                val classNode = findClassNodeByQualifiedName(
                    psiClass.project,
                    psiClass.findModule(),
                    stringValue.replace('/', '.'),
                )
                val removeDuplicateTargetFix = RemoveDuplicateTargetFix(mixinAnnotation, "targets")
                registerProblemIfProblematic(nonDuplicates, classNode, targets, removeDuplicateTargetFix)
            }
        }

        private fun registerProblemForDuplicate(duplicateExpression: PsiElement, quickFix: LocalQuickFix) {
            holder.registerProblem(
                duplicateExpression,
                "Duplicate target is redundant",
                quickFix,
            )
        }

        private fun registerProblemIfProblematic(
            nonDuplicates: List<ClassNode>,
            classNode: ClassNode?,
            possibleDuplicate: PsiElement,
            possibleQuickFix: LocalQuickFix
        ) {
            if (classNode != null && classNode !in nonDuplicates) {
                registerProblemForDuplicate(possibleDuplicate, possibleQuickFix)
            }
        }
    }

    private class RemoveDuplicateTargetFix(annotation: PsiAnnotation, val annotationAttributeName: String) :
        LocalQuickFixAndIntentionActionOnPsiElement(annotation) {

        override fun getFamilyName() = "Remove duplicate target"

        override fun getText() = familyName

        override fun invoke(
            project: Project,
            file: PsiFile,
            editor: Editor?,
            startElement: PsiElement,
            endElement: PsiElement
        ) {
            val annotation = startElement as? PsiAnnotation ?: return
            annotation.setDeclaredAttributeValue(annotationAttributeName, null)
        }
    }
}
