package com.nikka.detekt

import io.gitlab.arturbosch.detekt.test.lint
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class NestedTypeBeforeMethodsTest {

    private val rule = NestedTypeBeforeMethods()

    @Test
    fun `reports nested class declared after method`() {
        val code = """
            class Foo {
                fun bar() {}
                private data class Result(val x: Int)
            }
        """.trimIndent()
        val findings = rule.lint(code)
        assertEquals(1, findings.size)
    }

    @Test
    fun `reports nested object declared after method`() {
        val code = """
            class Foo {
                fun bar() {}
                private object Helper
            }
        """.trimIndent()
        val findings = rule.lint(code)
        assertEquals(1, findings.size)
    }

    @Test
    fun `does not report nested class declared before methods`() {
        val code = """
            class Foo {
                private data class Result(val x: Int)
                fun bar() {}
            }
        """.trimIndent()
        val findings = rule.lint(code)
        assertTrue(findings.isEmpty())
    }

    @Test
    fun `does not report when only nested classes exist`() {
        val code = """
            class Foo {
                private data class A(val x: Int)
                private data class B(val y: Int)
            }
        """.trimIndent()
        val findings = rule.lint(code)
        assertTrue(findings.isEmpty())
    }

    @Test
    fun `does not report companion object after methods`() {
        // companion object は detekt 標準 ClassOrdering の管轄なので本ルールは対象外
        // KtObjectDeclaration なので KtClassOrObject に該当するが、companion は例外扱いすべき
        val code = """
            class Foo {
                fun bar() {}
                companion object {
                    const val X = 1
                }
            }
        """.trimIndent()
        val findings = rule.lint(code)
        assertTrue(findings.isEmpty(), "companion object は標準 ClassOrdering の管轄")
    }
}
