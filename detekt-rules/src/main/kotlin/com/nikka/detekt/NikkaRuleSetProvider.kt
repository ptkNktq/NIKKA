package com.nikka.detekt

import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.api.RuleSet
import io.gitlab.arturbosch.detekt.api.RuleSetProvider

/**
 * NIKKA プロジェクト固有の detekt カスタムルールを登録する RuleSetProvider。
 */
class NikkaRuleSetProvider : RuleSetProvider {
    override val ruleSetId: String = "nikka"

    override fun instance(config: Config): RuleSet = RuleSet(
        ruleSetId,
        listOf(NestedTypeBeforeMethods(config)),
    )
}
