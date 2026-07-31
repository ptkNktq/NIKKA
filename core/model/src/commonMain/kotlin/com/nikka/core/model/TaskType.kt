package com.nikka.core.model

import kotlinx.serialization.Serializable

/** タスクの繰り返し種別 */
@Serializable
enum class TaskType {
    /** 日課: 毎日リセットされる */
    DAILY,

    /** 任意項目: 日課と同じタイミングでリセットされるが、未達成でも通知しない */
    OPTIONAL,

    /** 週課: 週単位でリセットされる */
    WEEKLY,
}
