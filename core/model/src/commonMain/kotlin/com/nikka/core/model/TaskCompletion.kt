package com.nikka.core.model

/**
 * グループ内タスクの完了判定。折りたたみとステータス表示の両方で使う。
 * 任意項目 (未達成でもよいタスク) は判定対象から常に除外する (対象が空になれば全タスクで判定)。
 * [dailyOnly] の場合はさらに日課のみで判定する (日課が 1 つもなければ非任意タスク全体で判定)。
 */
fun List<Task>.allTasksCompleted(dailyOnly: Boolean): Boolean {
    val required = filter { it.type != TaskType.OPTIONAL }
    val targets = if (dailyOnly) {
        required.filter { it.type == TaskType.DAILY }.ifEmpty { required }
    } else {
        required
    }.ifEmpty { this }
    return targets.isNotEmpty() && targets.all { it.isCompleted }
}
