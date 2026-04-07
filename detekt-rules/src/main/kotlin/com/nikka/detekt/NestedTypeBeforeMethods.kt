package com.nikka.detekt

import io.gitlab.arturbosch.detekt.api.CodeSmell
import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.api.Debt
import io.gitlab.arturbosch.detekt.api.Entity
import io.gitlab.arturbosch.detekt.api.Issue
import io.gitlab.arturbosch.detekt.api.Rule
import io.gitlab.arturbosch.detekt.api.Severity
import org.jetbrains.kotlin.psi.KtClassBody
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtObjectDeclaration

/**
 * クラス内で nested type（class / object / interface）が methods より後に
 * 出現することを禁止するルール。
 *
 * Kotlin 公式コーディング規約のクラスレイアウト慣習では、型の宣言（プロパティ、
 * ネストされた型）はクラスの上部にまとめ、振る舞い（メソッド）は下部に置く。
 * detekt 標準の ClassOrdering ルールは nested type を対象にしないため、本ルールで補完する。
 */
class NestedTypeBeforeMethods(config: Config = Config.empty) : Rule(config) {

    override val issue = Issue(
        id = javaClass.simpleName,
        severity = Severity.Style,
        description = "Nested type declarations must come before method declarations.",
        debt = Debt.FIVE_MINS,
    )

    override fun visitClassBody(classBody: KtClassBody) {
        super.visitClassBody(classBody)

        var seenMethod = false
        for (decl in classBody.declarations) {
            when (decl) {
                is KtNamedFunction -> seenMethod = true
                is KtClassOrObject -> {
                    // companion object は detekt 標準 ClassOrdering の管轄なので除外
                    if (decl is KtObjectDeclaration && decl.isCompanion()) continue
                    if (seenMethod) {
                        report(
                            CodeSmell(
                                issue = issue,
                                entity = Entity.from(decl),
                                message = "Nested type '${decl.name ?: "<anonymous>"}' は " +
                                    "メソッド宣言より前に置いてください。",
                            ),
                        )
                    }
                }
            }
        }
    }
}
