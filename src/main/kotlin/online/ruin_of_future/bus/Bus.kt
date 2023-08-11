package online.ruin_of_future.bus

import net.mamoe.mirai.contact.Member
import java.util.*

class Bus {
    var busTime = Date()
    val busMembers = mutableSetOf<Member>()
    var myTimerTaskBusRemindList = mutableListOf<TimerTask>()
}