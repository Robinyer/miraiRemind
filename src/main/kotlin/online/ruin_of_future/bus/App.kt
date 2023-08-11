package online.ruin_of_future.bus

import kotlinx.coroutines.*
import net.mamoe.mirai.Bot
import net.mamoe.mirai.console.command.CommandManager
import net.mamoe.mirai.console.plugin.jvm.JvmPluginDescription
import net.mamoe.mirai.console.plugin.jvm.KotlinPlugin
import net.mamoe.mirai.event.globalEventChannel
import net.mamoe.mirai.event.subscribeGroupMessages
import net.mamoe.mirai.message.data.*
import java.text.SimpleDateFormat
import java.util.*


object ReporterPlugin : KotlinPlugin(
    JvmPluginDescription(
        id = "online.ruin_of_future.bus",
        version = "1.2.11",
    ) {
        name("Reporter")
        author("R")
    }
) {
    val busGroup = mutableMapOf<Long, Bus>()

    //    val bus = Bus()
    val timer = Timer()
    var xhh = 1

    class MyTimerTaskBusRemind(val groupId: Long) : TimerTask() {
        override fun run() {
            async {
                //这里填上每次触发要执行的代码
                logger.info("Bus remind")
                var successedSend = 0

                Bot.instances.forEach {
                    if (it.id in BusGroupWhiteList.groupIdsPerBot) {
                        val bus = busGroup[groupId]!!
                        try {
                            val group = it.getGroup(groupId)
//                                Message
                            val mem = bus.busMembers.map { At(it.id) }
                            val chain: MessageChain
                            if (mem.size <= 1) {
                                chain = buildMessageChain {
                                    addAll(mem)
                                    +PlainText("就一个人，发个鬼车，速速学习去")
                                }
                            } else {
                                chain = buildMessageChain {
                                    addAll(mem)
                                    +PlainText("不到5分钟发车，请提前在线")
                                }
                            }
                            group?.sendMessage(chain)
//                                group?.sendMessage(bus.busMembers.map { member -> {"[mirai:at:${member.id}]"} }.toString())
//                                group?.sendMessage("[mirai:at:757681223]")

                        } catch (e: Exception) {
                            logger.error(e)
                        } finally {
                            bus.myTimerTaskBusRemindList.removeFirst()
                            bus.busMembers.clear()
                        }
                    }
                }
            }
        }
    }

    override fun onEnable() {
//        NewsGroupWhiteList.reload()
//        AnimeGroupWhiteList.reload()
        BusGroupWhiteList.reload()

//        CommandManager.registerCommand(NewsGroupCommand)
//        CommandManager.registerCommand(AnimeGroupCommand)
        CommandManager.registerCommand(BusGroupCommand)

        this.launch {
            delay(5000)
            logger.info("forEach")
            Bot.instances.forEach {
                if (it.id in BusGroupWhiteList.groupIdsPerBot) {
                    logger.info(BusGroupWhiteList.groupIdsPerBot[it.id].toString())
                    for (groupId in BusGroupWhiteList.groupIdsPerBot[it.id]!!) {
                        busGroup[groupId] = Bus()
                    }
                }
            }
            logger.info(BusGroupWhiteList.toString())
            logger.info(busGroup.toString())
            val cal = Calendar.getInstance()
            cal.set(Calendar.HOUR_OF_DAY, 9)
            cal.set(Calendar.MINUTE, 0)
            cal.add(Calendar.DAY_OF_YEAR, 1)
            val dateRepresentation = cal.time
            val daySpan = (24 * 60 * 60 * 1000).toLong()
//            val task2 =
//            timer.scheduleAtFixedRate(MyTimerTask2(), dateRepresentation.time.minus(10 * 60 * 1000), daySpan)
//            val task =
//            timer.scheduleAtFixedRate(MyTimerTask(), dateRepresentation, daySpan)
            logger.info("XXX")
        }


        // 创建时间
        this.globalEventChannel().subscribeGroupMessages {
            matching(Regex("(([01][0-9]|2[0-3])[ .:：-][0-5][0-9])|(1[89]|2[0-3])")) {
                if (BusGroupWhiteList.groupIdsPerBot[bot.id]?.contains(group.id) == false) {
                    return@matching
                }
                val bus = busGroup[group.id]!!

                if (bus.myTimerTaskBusRemindList.isNotEmpty()) {
                    val dateFormated = SimpleDateFormat("HH:mm").format(bus.busTime)
                    group.sendMessage("已有{$dateFormated}的计划车队,可输入-c/-clear/清除")
                } else {
                    it.value.split(Regex("[ .:：-]")).let { time ->
                        var hour = 0
                        var minute = 0
                        if (time.size == 2) {
                            hour = time[0].toInt()
                            minute = time[1].toInt()
                        } else {
                            hour = it.value.toInt()
                        }
                        if (hour in 0..23 && minute in 0..59) {
                            val cal = Calendar.getInstance()
                            cal[Calendar.HOUR_OF_DAY] = hour
                            cal[Calendar.MINUTE] = minute
                            cal[Calendar.SECOND] = 0
                            val dateRepresentation = cal.time
                            if (dateRepresentation < Calendar.getInstance().time) {
                                group.sendMessage("鸽们你这时间不太对啊")
                                return@matching
                            }
                            val remind = Date(dateRepresentation.time.minus(5 * 60 * 1000))
//                        val daySpan = (24 * 60 * 60 * 1000).toLong()
                            bus.busTime = dateRepresentation
                            bus.busMembers.add(sender)
                            val myTimerTaskBusRemind = MyTimerTaskBusRemind(group.id)
                            timer.schedule(myTimerTaskBusRemind, remind)
                            bus.myTimerTaskBusRemindList.add(myTimerTaskBusRemind)
                            val dateFormated = SimpleDateFormat("HH:mm").format(dateRepresentation)
                            logger.info("Task created for ${group.id}")
                            group.sendMessage("$senderName 发起了在${dateFormated}的车请求...扣1上车")
                        }
                    }
                    logger.info("$senderName 发起了车请求...")
                }
            }
        }

        this.globalEventChannel().subscribeGroupMessages {
            matching(Regex("-r ((([01][0-9]|2[0-3])[ .:：-][0-5][0-9])|(1[89]|2[0-3]))")) {
                if (BusGroupWhiteList.groupIdsPerBot[bot.id]?.contains(group.id) == false) {
                    return@matching
                }

                val timeString = it.value.substring(3);
                timeString.split(Regex("[ .:：-]")).let { time ->
                    var hour = 0
                    var minute = 0
                    if (time.size == 2) {
                        hour = time[0].toInt()
                        minute = time[1].toInt()
                    } else {
                        hour = timeString.toInt()
                    }
                    if (hour in 0..23 && minute in 0..59) {
                        val cal = Calendar.getInstance()
                        cal[Calendar.HOUR_OF_DAY] = hour
                        cal[Calendar.MINUTE] = minute
                        cal[Calendar.SECOND] = 0
                        val dateRepresentation = cal.time
                        if (dateRepresentation < Calendar.getInstance().time) {
                            group.sendMessage("$senderName 鸽们你这时间不太对啊")
                            return@matching
                        }
                        val remind = Date(dateRepresentation.time.minus(5 * 60 * 1000))
                        val bus = busGroup[group.id]!!
                        bus.busTime = dateRepresentation
//                        bus.busMembers.clear()
                        bus.busMembers.add(sender)
                        val myTimerTaskBusRemind = MyTimerTaskBusRemind(group.id)
                        timer.schedule(myTimerTaskBusRemind, remind)

                        val dateFormated = SimpleDateFormat("HH:mm").format(bus.busTime)
                        if (bus.myTimerTaskBusRemindList.isNotEmpty()) {
                            bus.myTimerTaskBusRemindList.forEach(TimerTask::cancel)
                            bus.myTimerTaskBusRemindList.clear()

                            val mem = bus.busMembers.map { At(it.id) }
                            mem.dropLastWhile { it.equals(sender.id) }
                            val chain = buildMessageChain {
                                addAll(mem)
                                +PlainText("$senderName 将发车时间修改为了${dateFormated}")
                            }
                            group.sendMessage(chain)
                        } else {
                            group.sendMessage("$senderName 发起了在${dateFormated}的车请求...扣1上车")
                            logger.info("$senderName 发起了车请求...")
                        }

                        bus.myTimerTaskBusRemindList.add(myTimerTaskBusRemind)
                        logger.info("Task replace for ${group.id}")
                    }
                }

            }
        }

        this.globalEventChannel().subscribeGroupMessages {
            matching(Regex("(-clear)|(-c)|清除")) {
                if (BusGroupWhiteList.groupIdsPerBot[bot.id]?.contains(group.id) == false) {
                    logger.info("XXX")
                    return@matching
                }
                logger.info("YYY")
                val bus = busGroup[group.id]!!
                bus.myTimerTaskBusRemindList.forEach(TimerTask::cancel)
                bus.myTimerTaskBusRemindList.clear()
//                myTimerTaskBusRemind.cancel()
                bus.busMembers.clear()
                group.sendMessage("计划车队已毁灭")
                logger.info("${sender.nick} 发起了车毁灭...")
            }
        }

        this.globalEventChannel().subscribeGroupMessages {
            matching(Regex("(-add)|(-a)|1|投币")) {
                if (BusGroupWhiteList.groupIdsPerBot[bot.id]?.contains(group.id) == false) {
                    return@matching
                }
                val bus = busGroup[group.id]!!
                if (bus.myTimerTaskBusRemindList.isNotEmpty()) {
                    bus.busMembers.add(sender)
                    val dateFormated = SimpleDateFormat("HH:mm").format(bus.busTime)
                    group.sendMessage(
                        "${sender.nick} 已加入${dateFormated}的车队，目前车队成员为${
                            bus.busMembers.map { member -> member.nick }.toString()
                        }"
                    )
                    logger.info("${sender.nick} 加入了车...")
                }

            }
        }

        this.globalEventChannel().subscribeGroupMessages {
            matching(Regex("(-h)|(-help)|帮助")) {
                if (BusGroupWhiteList.groupIdsPerBot[bot.id]?.contains(group.id) == false) {
                    return@matching
                }
                group.sendMessage(
                    "当前为0.6测试版，可用指令为:\n" +
                            "1.创建车队 输入10:00至晚23:59的时间，或是输入18-23的整数 (([01][0-9]|2[0-3])[.:：-][0-5][0-9])|(1[89]|2[0-3])\n" +
                            "2.加入车队(-add)|(-a)|1|投币|上车\n" +
                            "3.改变发车时间-r ((([01][0-9]|2[0-3])[ .:：-][0-5][0-9])|(1[89]|2[0-3]))\n" +
                            "4.毁灭世界(-clear)|(-c)|清除\n" +
                            "5.召唤胡耀宇-xhh\n" +
                            "6.查看当前车队及时间 (-l)|(-list)"
                )
            }
        }
        this.globalEventChannel().subscribeGroupMessages {
            matching(Regex("(-l)|(-list)")) {
                if (BusGroupWhiteList.groupIdsPerBot[bot.id]?.contains(group.id) == false) {
                    return@matching
                }
                val bus = busGroup[group.id]!!
                if (bus.myTimerTaskBusRemindList.isNotEmpty()) {
//                    bus.busMembers.add(sender)
                    val dateFormated = SimpleDateFormat("HH:mm").format(bus.busTime)
                    group.sendMessage(
                        "${dateFormated}的车队，目前车队成员为${
                            bus.busMembers.map { member -> member.nick }
                        }"
                    )
                } else {
                    group.sendMessage("暂无计划车队，建议发起一个想要的时间点")
                }

            }
        }

        this.globalEventChannel().subscribeGroupMessages {
            matching(Regex("-xhh")) {
                if (BusGroupWhiteList.groupIdsPerBot[bot.id]?.contains(group.id) == false) {
                    return@matching
                }
                val chain = buildMessageChain {
                    add(At(303419782))
                    +PlainText("第${xhh++}次尝试召唤")
                }
                group.sendMessage(chain)
            }
        }

    }
}