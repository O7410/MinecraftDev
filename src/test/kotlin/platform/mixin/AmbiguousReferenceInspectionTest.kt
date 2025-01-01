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

package com.demonwav.mcdev.platform.mixin

import com.demonwav.mcdev.framework.EdtInterceptor
import com.demonwav.mcdev.platform.mixin.inspection.reference.AmbiguousReferenceInspection
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(EdtInterceptor::class)
@DisplayName("Ambiguous Reference Inspection Tests")
class AmbiguousReferenceInspectionTest : BaseMixinTest() {

    private fun doTest(@Language("JAVA") code: String) {
        buildProject {
            dir("test") {
                java("AmbiguousReferenceMixin.java", code)
            }
        }

        fixture.enableInspections(AmbiguousReferenceInspection::class)
        fixture.checkHighlighting(true, false, false)
    }

    @Test
    @DisplayName("Ambiguous Reference")
    fun ambiguousReference() {
        doTest(
            """
            package test;

            import com.demonwav.mcdev.mixintestdata.ambiguousReference.MixedIn;
            import org.spongepowered.asm.mixin.Mixin;
            import org.spongepowered.asm.mixin.injection.At;
            import org.spongepowered.asm.mixin.injection.Inject;

            @Mixin(MixedIn.class)
            class AmbiguousReferenceMixin {
            
                @Inject(method = <warning descr="Ambiguous reference to method 'method' in target class">"method"</warning>, at = @At("HEAD"))
                public void onMethod() {
                }
            }
            """,
        )
    }

    @Test
    @DisplayName("No Ambiguous Reference")
    fun noAmbiguousReference() {
        doTest(
            """
            package test;

            import com.demonwav.mcdev.mixintestdata.ambiguousReference.MixedIn;
            import org.spongepowered.asm.mixin.Mixin;
            import org.spongepowered.asm.mixin.injection.At;
            import org.spongepowered.asm.mixin.injection.Inject;

            @Mixin(MixedIn.class)
            class AmbiguousReferenceMixin {
            
                @Inject(method = "uniqueMethod", at = @At("HEAD"))
                public void onMethod() {
                }
            }
            """,
        )
    }

    @Test
    @DisplayName("Ambiguous Reference Multiple Targets")
    fun ambiguousReferenceMultipleTargets() {
        doTest(
            """
            package test;

            import com.demonwav.mcdev.mixintestdata.ambiguousReference.MixedIn;
            import org.spongepowered.asm.mixin.Mixin;
            import org.spongepowered.asm.mixin.injection.At;
            import org.spongepowered.asm.mixin.injection.Inject;

            @Mixin(MixedIn.class)
            class AmbiguousReferenceMixin {
            
                @Inject(method = {<warning descr="Ambiguous reference to method 'method' in target class">"method"</warning>, "uniqueMethod"}, at = @At("HEAD"))
                public void onMethod() {
                }
            }
            """,
        )
    }

    @Test
    @DisplayName("No Ambiguous Qualified Reference")
    fun noAmbiguousQualifiedReference() {
        doTest(
            """
            package test;

            import com.demonwav.mcdev.mixintestdata.ambiguousReference.MixedIn;
            import org.spongepowered.asm.mixin.Mixin;
            import org.spongepowered.asm.mixin.injection.At;
            import org.spongepowered.asm.mixin.injection.Inject;

            @Mixin(MixedIn.class)
            class AmbiguousReferenceMixin {
            
                @Inject(method = "method(Ljava/lang/String;)V", at = @At("HEAD"))
                public void onMethod() {
                }
            }
            """,
        )
    }

    @Test
    @DisplayName("No Ambiguous Reference Multiple Targets")
    fun noAmbiguousReferenceMultipleTargets() {
        doTest(
            """
            package test;

            import com.demonwav.mcdev.mixintestdata.ambiguousReference.MixedIn;
            import org.spongepowered.asm.mixin.Mixin;
            import org.spongepowered.asm.mixin.injection.At;
            import org.spongepowered.asm.mixin.injection.Inject;

            @Mixin(MixedIn.class)
            class AmbiguousReferenceMixin {
            
                @Inject(method = {"method(Ljava/lang/String;)V", "uniqueMethod"}, at = @At("HEAD"))
                public void onMethod() {
                }
            }
            """,
        )
    }
}
