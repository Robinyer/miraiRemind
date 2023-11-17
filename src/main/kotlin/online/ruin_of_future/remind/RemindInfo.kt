package online.ruin_of_future.remind

import kotlinx.serialization.Serializable
import java.util.*

@Serializable
class RemindInfo {
    var lastMemberId = 0L
    var myTimerTaskRemindList = mutableListOf<ReporterPlugin.MyTimerTaskRemind>()

    // 重新toString方法
    @Override
    override fun toString(): String {
        return "RemindInfo(lastMemberId=$lastMemberId, myTimerTaskRemindList=$myTimerTaskRemindList)"
    }
}