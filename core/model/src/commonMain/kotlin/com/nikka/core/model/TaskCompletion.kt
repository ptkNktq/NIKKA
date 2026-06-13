package com.nikka.core.model

/**
 * グループ内タスクの完了判定。折りたたみとステータス表示の両方で使う。
 * [dailyOnly] の場合は日課のみで判定する (日課が 1 つもなければ全タスクで判定)。
 */
fun List<Task>.allTasksCompleted(dailyOnly: Boolean): Boolean {
    val targets = if (dailyOnly) {
        filter { it.type == TaskType.DAILY }.ifEmpty { this }
    } else {
        this
    }
    return targets.isNotEmpty() && targets.all { it.isCompleted }
}
