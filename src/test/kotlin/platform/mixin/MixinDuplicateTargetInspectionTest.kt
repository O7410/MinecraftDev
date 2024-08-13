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

package com.demonwav.mcdev.platform.mixin

import com.demonwav.mcdev.framework.EdtInterceptor
import com.demonwav.mcdev.platform.mixin.inspection.MixinDuplicateTargetInspection
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(EdtInterceptor::class)
@DisplayName("Mixin Duplicate Target Inspection Test")
class MixinDuplicateTargetInspectionTest : BaseMixinTest() {

    private fun doTest(@Language("JAVA") code: String) {
        buildProject {
            dir("test") {
                java("TestMixin.java", code)
            }
        }

        fixture.enableInspections(MixinDuplicateTargetInspection::class)
        fixture.checkHighlighting(false, false, true)
    }

    @Test
    @DisplayName("Simple class target")
    fun simpleClassTarget() {
        doTest(
            """
            package test;
            
            import com.demonwav.mcdev.mixintestdata.multipleTargetClasses.SimpleClass1;
            import org.spongepowered.asm.mixin.Mixin;
            
            @Mixin(SimpleClass1.class)
            public class TestMixin {
            }
            """,
        )
    }

    @Test
    @DisplayName("Simple class target in array")
    fun simpleClassTargetInArray() {
        doTest(
            """
            package test;

            import com.demonwav.mcdev.mixintestdata.multipleTargetClasses.SimpleClass1;
            import org.spongepowered.asm.mixin.Mixin;

            @Mixin({SimpleClass1.class})
            public class TestMixin {
            }
            """,
        )
    }

    @Test
    @DisplayName("Simple class target in array with attribute name")
    fun simpleClassTargetInArrayWithAttributeName() {
        doTest(
            """
            package test;

            import com.demonwav.mcdev.mixintestdata.multipleTargetClasses.SimpleClass1;
            import org.spongepowered.asm.mixin.Mixin;

            @Mixin(value = {SimpleClass1.class})
            public class TestMixin {
            }
            """,
        )
    }

    @Test
    @DisplayName("Simple class target with attribute name")
    fun simpleClassTargetWithAttributeName() {
        doTest(
            """
            package test;

            import com.demonwav.mcdev.mixintestdata.multipleTargetClasses.SimpleClass1;
            import org.spongepowered.asm.mixin.Mixin;

            @Mixin(value = SimpleClass1.class)
            public class TestMixin {
            }
            """,
        )
    }

    @Test
    @DisplayName("Different targets in array")
    fun differentTargetsInArray() {
        doTest(
            """
            package test;

            import com.demonwav.mcdev.mixintestdata.multipleTargetClasses.SimpleClass1;
            import com.demonwav.mcdev.mixintestdata.multipleTargetClasses.SimpleClass2;
            import com.demonwav.mcdev.mixintestdata.multipleTargetClasses.SimpleClass3;
            import org.spongepowered.asm.mixin.Mixin;

            @Mixin({SimpleClass1.class, SimpleClass2.class, SimpleClass3.class})
            public class TestMixin {
            }
            """,
        )
    }

    @Test
    @DisplayName("Different targets in array with attribute name")
    fun differentTargetsInArrayWithAttributeName() {
        doTest(
            """
            package test;

            import com.demonwav.mcdev.mixintestdata.multipleTargetClasses.SimpleClass1;
            import com.demonwav.mcdev.mixintestdata.multipleTargetClasses.SimpleClass2;
            import com.demonwav.mcdev.mixintestdata.multipleTargetClasses.SimpleClass3;
            import org.spongepowered.asm.mixin.Mixin;

            @Mixin(value = {SimpleClass1.class, SimpleClass2.class, SimpleClass3.class})
            public class TestMixin {
            }
            """,
        )
    }

    @Test
    @DisplayName("Different targets in string targets")
    fun differentTargetsInStringTargets() {
        doTest(
            """
            package test;

            import org.spongepowered.asm.mixin.Mixin;

            @Mixin(targets = {
                "com.demonwav.mcdev.mixintestdata.multipleTargetClasses.SimpleClass1", 
                "com.demonwav.mcdev.mixintestdata.multipleTargetClasses.SimpleClass2", 
                "com.demonwav.mcdev.mixintestdata.multipleTargetClasses.SimpleClass3"
            })
            public class TestMixin {
            }
            """,
        )
    }

    @Test
    @DisplayName("Unresolved duplicates")
    fun unreslovedDuplicates() {
        doTest(
            """
            package test;

            import org.spongepowered.asm.mixin.Mixin;

            @Mixin(
                value = {
                    UnresolvedClass1.class,
                    UnresolvedClass1.class,
                    UnresolvedClass2.class,
                    UnresolvedClass2.class
                },
                targets = {
                    "UnresolvedClass1",
                    "UnresolvedClass1",
                    "UnresolvedClass2",
                    "UnresolvedClass2"
                }
            )
            public class TestMixin {
            }
            """,
        )
    }

    @Test
    @DisplayName("Target twice in array")
    fun targetTwiceInArray() {
        doTest(
            """
            package test;

            import com.demonwav.mcdev.mixintestdata.multipleTargetClasses.SimpleClass1;
            import org.spongepowered.asm.mixin.Mixin;

            @Mixin({<weak_warning descr="Duplicate target is redundant">SimpleClass1.class</weak_warning>, <weak_warning descr="Duplicate target is redundant">SimpleClass1.class</weak_warning>})
            public class TestMixin {
            }
            """,
        )
    }

    @Test
    @DisplayName("Target twice in array with attribute name")
    fun targetTwiceInArrayWithAttributeName() {
        doTest(
            """
            package test;

            import com.demonwav.mcdev.mixintestdata.multipleTargetClasses.SimpleClass1;
            import org.spongepowered.asm.mixin.Mixin;

            @Mixin(value = {<weak_warning descr="Duplicate target is redundant">SimpleClass1.class</weak_warning>, <weak_warning descr="Duplicate target is redundant">SimpleClass1.class</weak_warning>})
            public class TestMixin {
            }
            """,
        )
    }

    @Test
    @DisplayName("Target twice with unresolved targets next to it")
    fun targetTwiceWithUnresolvedTargetsNextToIt() {
        doTest(
            """
            package test;

            import com.demonwav.mcdev.mixintestdata.multipleTargetClasses.SimpleClass1;
            import org.spongepowered.asm.mixin.Mixin;

            @Mixin(value = {
                UnresolvedClass1.class, 
                <weak_warning descr="Duplicate target is redundant">SimpleClass1.class</weak_warning>,
                UnresolvedClass2.class,
                <weak_warning descr="Duplicate target is redundant">SimpleClass1.class</weak_warning>,
                UnresolvedClass3.class
            })
            public class TestMixin {
            }
            """,
        )
    }

    @Test
    @DisplayName("Target three times with unresolved targets next to it")
    fun targetThreeTimesWithUnresolvedTargetsNextToIt() {
        doTest(
            """
            package test;

            import com.demonwav.mcdev.mixintestdata.multipleTargetClasses.SimpleClass1;
            import org.spongepowered.asm.mixin.Mixin;

            @Mixin(value = {
                UnresolvedClass1.class, 
                <weak_warning descr="Duplicate target is redundant">SimpleClass1.class</weak_warning>,
                UnresolvedClass2.class,
                <weak_warning descr="Duplicate target is redundant">SimpleClass1.class</weak_warning>,
                UnresolvedClass3.class,
                <weak_warning descr="Duplicate target is redundant">SimpleClass1.class</weak_warning>,
                UnresolvedClass4.class
            })
            public class TestMixin {
            }
            """,
        )/*
        <error descr="Cannot resolve symbol 'UnresolvedClass4'" textAttributesKey="WRONG_REFERENCES_ATTRIBUTES">UnresolvedClass4</error>.class
        is the expected
        */
    }

    @Test
    @DisplayName("Multiple targets multiple times")
    fun multipleTargetsMultipleTimes() {
        doTest(
            """
            package test;

            import com.demonwav.mcdev.mixintestdata.multipleTargetClasses.SimpleClass1;
            import com.demonwav.mcdev.mixintestdata.multipleTargetClasses.SimpleClass2;
            import org.spongepowered.asm.mixin.Mixin;

            @Mixin(value = {
                <weak_warning descr="Duplicate target is redundant">SimpleClass2.class</weak_warning>,
                UnresolvedClass1.class, 
                <weak_warning descr="Duplicate target is redundant">SimpleClass1.class</weak_warning>,
                UnresolvedClass2.class,
                <weak_warning descr="Duplicate target is redundant">SimpleClass1.class</weak_warning>,
                <weak_warning descr="Duplicate target is redundant">SimpleClass2.class</weak_warning>,
                UnresolvedClass3.class
            })
            public class TestMixin {
            }
            """,
        )
    }

    @Test
    @DisplayName("String target and class target")
    fun stringTargetAndClassTarget() {
        doTest(
            """
            package test;

            import com.demonwav.mcdev.mixintestdata.multipleTargetClasses.SimpleClass1;
            import org.spongepowered.asm.mixin.Mixin;

            @Mixin(value = <weak_warning descr="Duplicate target is redundant">SimpleClass1.class</weak_warning>, 
                    targets = <weak_warning descr="Duplicate target is redundant">"com.demonwav.mcdev.mixintestdata.multipleTargetClasses.SimpleClass1"</weak_warning>)
            public class TestMixin {
            }
            """,
        )
    }

    @Test
    @DisplayName("Multiple string targets")
    fun multipleStringTargets() {
        doTest(
            """
            package test;

            import org.spongepowered.asm.mixin.Mixin;

            @Mixin(targets = {
                    "com.demonwav.mcdev.mixintestdata.multipleTargetClasses.SimpleClass2",
                    <weak_warning descr="Duplicate target is redundant">"com.demonwav.mcdev.mixintestdata.multipleTargetClasses.SimpleClass1"</weak_warning>,
                    <weak_warning descr="Duplicate target is redundant">"com.demonwav.mcdev.mixintestdata.multipleTargetClasses.SimpleClass1"</weak_warning>,
                    "com.demonwav.mcdev.mixintestdata.multipleTargetClasses.SimpleClass3"
                }
            )
            public class TestMixin {
            }
            """,
        )
    }
}
