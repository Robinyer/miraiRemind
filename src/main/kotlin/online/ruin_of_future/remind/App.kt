package online.ruin_of_future.remind

import kotlinx.coroutines.*
import kotlinx.serialization.Serializable
import net.mamoe.mirai.Bot
import net.mamoe.mirai.console.command.CommandManager
import net.mamoe.mirai.console.plugin.jvm.JvmPluginDescription
import net.mamoe.mirai.console.plugin.jvm.KotlinPlugin
import net.mamoe.mirai.contact.nameCardOrNick
import net.mamoe.mirai.event.globalEventChannel
import net.mamoe.mirai.event.subscribeGroupMessages
import net.mamoe.mirai.message.data.*
import java.lang.Thread.sleep
import java.text.SimpleDateFormat
import java.util.*


object ReporterPlugin : KotlinPlugin(
    JvmPluginDescription(
        id = "online.ruin_of_future.remind",
        version = "1.2.11",
    ) {
        name("Reporter")
        author("R")
    }
) {
    val timer = Timer()

    // 初始化大小为6的qq号的list
    val qqList = mutableListOf<Long>(1263281491, 757681223, 2426979747, 735171867, 2695591722, 1050545790)

    @Serializable
    class MyTimerTaskRemind(val botId: Long, val groupId: Long, val memberId: Long) : TimerTask() {
        override fun run() {
            async {
                //这里填上每次触发要执行的代码
                logger.info("remind")
                val bot = Bot.getInstance(botId)
                if (bot.id in RemindGroupWhiteList.groupIdsPerBot) {
                    try {
                        val group = bot.getGroup(groupId)
                        val mem = At(memberId)
                        val chain: MessageChain = buildMessageChain {
                            add(mem)
                            +PlainText("该接水啦")
                        }
                        group?.sendMessage(chain)

                    } catch (e: Exception) {
                        logger.error(e)
                    } finally {
                        val groupInfo = RemindGroupInfoList.groupInfoPerBot[bot.id]?.get(groupId)!!
                        groupInfo.myTimerTaskRemindList.removeFirst()
                    }
                }
            }
        }

        @Override
        override fun toString(): String {
            return "MyTimerTaskRemind(botId=$botId, groupId=$groupId, memberId=$memberId)"
        }
    }

    class MyAutoTimerTaskRemind(val botId: Long, val groupId: Long) : TimerTask() {
        override fun run() {
            async {
                //这里填上每次触发要执行的代码
                logger.info("auto reload remind")
                val bot = Bot.getInstance(botId)
                if (bot.id in RemindGroupWhiteList.groupIdsPerBot) {
                    try {
                        logger.info("auto reload remind")
                        val group = bot.getGroup(groupId)!!
                        logger.info("$group")
                        group.sendMessage("无人投掷，自动开投！")
                        val random = Random()
                        val randomInt = random.nextInt(6)
                        val memberId = qqList[randomInt]
                        logger.info("$group")
                        val chain = buildMessageChain {
                            At(memberId)
                            +PlainText(
                                "${
                                    if (group.contains(memberId)) {
                                        group[memberId]!!.nameCardOrNick
                                    } else {
                                        memberId
                                    }
                                }很幸运的被抽中了！速去接水！"
                            )
                        }
                        val groupInfo = RemindGroupInfoList.groupInfoPerBot[bot.id]?.get(groupId)!!
                        groupInfo.lastMemberId = memberId
                        group.sendMessage(chain)
                    } catch (e: Exception) {
                        logger.error(e)
                    }
                }
            }
        }
    }

    override fun onEnable() {
//        NewsGroupWhiteList.reload()
//        AnimeGroupWhiteList.reload()
        RemindGroupWhiteList.reload()
        RemindGroupInfoList.reload()
//        CommandManager.registerCommand(NewsGroupCommand)
//        CommandManager.registerCommand(AnimeGroupCommand)
        CommandManager.registerCommand(RemindGroupCommand)

        this.launch {
            sleep(5000)
            logger.info("launch")
            val cal = Calendar.getInstance()
            cal[Calendar.HOUR_OF_DAY] = 21
            cal[Calendar.MINUTE] = 0
            cal[Calendar.SECOND] = 0
            val dateRepresentation = cal.time
            if (cal.time.time < System.currentTimeMillis()) {
                logger.info("cal.time.time < System.currentTimeMillis()")
                dateRepresentation.time += 24 * 60 * 60 * 1000
            }
            val daySpan = (24 * 60 * 60 * 1000).toLong()
            for (bot in Bot.instances) {
                for (groupId in RemindGroupWhiteList.groupIdsPerBot[bot.id]!!) {
                    timer.scheduleAtFixedRate(
                        MyAutoTimerTaskRemind(bot.id, groupId),
                        Date(dateRepresentation.time),
                        daySpan
                    )
                }
            }
            logger.info("--------------------------------------------------------")

            // 将myTimerTaskRemindList的任务加入timer
            // 每天九点调度一次
            logger.info("Bot.instances: ${Bot.instances}")

            for (bot in Bot.instances) {
                for (groupId in RemindGroupWhiteList.groupIdsPerBot[bot.id]!!) {
                    val remindInfo = RemindGroupInfoList.groupInfoPerBot[bot.id]!!.get(groupId)
                    if (remindInfo!!.myTimerTaskRemindList.isEmpty()) {
                        continue
                    }
                    for (i in remindInfo.myTimerTaskRemindList.indices) {
                        logger.info("dateRepresentation: $dateRepresentation")
                        timer.schedule(
                            remindInfo.myTimerTaskRemindList[i],
                            Date(dateRepresentation.time + 24 * 60 * 60 * 1000 * i)
                        )
                    }
                }
            }

            logger.info("XXX")
        }


        // 创建时间
        this.globalEventChannel().subscribeGroupMessages {
            matching(Regex("-r|-roll|[0-6]")) {
                if (RemindGroupWhiteList.groupIdsPerBot[bot.id]?.contains(group.id) == false) {
                    return@matching
                }
                val remindInfo = RemindGroupInfoList.groupInfoPerBot[bot.id]!![group.id]!!
                if (sender.id != remindInfo.lastMemberId && remindInfo.lastMemberId != 0L) {
                    group.sendMessage("非最近被选中接水者，无法投掷")
                    return@matching
                }

                if (remindInfo.myTimerTaskRemindList.size >= 7) {
                    group.sendMessage("接水计划已满，明天再来吧")
                    return@matching
                }


                logger.info("$senderName 发起投掷...")
                group.sendMessage("${sender.nameCardOrNick} 发起投掷...")

                val random = Random(System.currentTimeMillis())
                val randomInt = random.nextInt(6)
                val memberId = qqList[randomInt]
                val myTimerTaskRemind = MyTimerTaskRemind(bot.id, group.id, memberId)

                val cal = Calendar.getInstance()
                cal[Calendar.HOUR_OF_DAY] = 21
                cal[Calendar.MINUTE] = 0
                cal[Calendar.SECOND] = 0
                // 如果当前时间大于21点，那么就是明天的21点
                if (cal.time.time < System.currentTimeMillis()) {
                    cal.add(Calendar.DAY_OF_YEAR, 1)
                }
                val dateRepresentation = cal.time.time.plus(
                    24 * 60 * 60 * 1000 * remindInfo.myTimerTaskRemindList.size
                )
                val remindTime = Date(dateRepresentation)
                timer.schedule(myTimerTaskRemind, remindTime)
                group.sendMessage(
                    "${sender.nameCardOrNick} 投掷成功...\n下一个接水的为${
                        if (group.contains(memberId)) {
                            group[memberId]!!.nameCardOrNick
                        } else {
                            memberId
                        }
                    }\n请在 ${
                        SimpleDateFormat(
                            "MM-dd HH:mm"
                        ).format(remindTime)
                    } 接水 "
                )
                remindInfo.myTimerTaskRemindList.add(myTimerTaskRemind)
                remindInfo.lastMemberId = memberId
            }
        }

        this.globalEventChannel().subscribeGroupMessages {
            matching(Regex("(-clear)|(-c)|清除")) {
                if (RemindGroupWhiteList.groupIdsPerBot[bot.id]?.contains(group.id) == false) {
                    return@matching
                }
                if (sender.id != 757681223L) {
                    group.sendMessage("请联系管理员添加删除权限")
                    return@matching
                }

                val remindInfo = RemindGroupInfoList.groupInfoPerBot[bot.id]?.get(group.id)!!
                remindInfo.myTimerTaskRemindList.forEach(TimerTask::cancel)
                remindInfo.myTimerTaskRemindList.clear()
                group.sendMessage("接水计划已毁灭")
                logger.info("${sender.nameCardOrNick} 发起了接水计划毁灭...")
            }
        }

        this.globalEventChannel().subscribeGroupMessages {
            matching(Regex("-clear all")) {
                if (RemindGroupWhiteList.groupIdsPerBot[bot.id]?.contains(group.id) == false) {
                    return@matching
                }
                if (sender.id != 757681223L) {
                    group.sendMessage("请联系管理员添加删除权限")
                    return@matching
                }

                val remindInfo = RemindGroupInfoList.groupInfoPerBot[bot.id]?.get(group.id)!!
                remindInfo.myTimerTaskRemindList.forEach(TimerTask::cancel)
                remindInfo.myTimerTaskRemindList.clear()
                remindInfo.lastMemberId = 0L

                group.sendMessage("接水计划已毁灭")
                logger.info("${sender.nameCardOrNick} 发起了接水计划毁灭...")
            }
        }

        this.globalEventChannel().subscribeGroupMessages {
            matching(Regex("(-h)|(-help)|帮助")) {
                if (RemindGroupWhiteList.groupIdsPerBot[bot.id]?.contains(group.id) == false) {
                    return@matching
                }
                group.sendMessage(
                    "当前为0.3测试版，可用指令为:\n" +
                            "1.投骰子 输入0-6的整数可加大该床号中奖概率 [0-6]|-r|-roll\n" +
                            "2.查看当前接水计划和时间 (-l)|(-list)\n" +
                            "3.清除当前接水计划 (-c)|(-clear)\n" +
                            "4.帮助 (-h)|(-help)"
                )
            }
        }

        this.globalEventChannel().subscribeGroupMessages {
            matching(Regex("(-l)|(-list)")) {
                if (RemindGroupWhiteList.groupIdsPerBot[bot.id]?.contains(group.id) == false) {
                    return@matching
                }
                logger.info(RemindGroupInfoList.groupInfoPerBot.toString())
                val remindInfo = RemindGroupInfoList.groupInfoPerBot[bot.id]!!.get(group.id)!!
                var chain = buildMessageChain {}
                chain += if (group.contains(remindInfo.lastMemberId)) {
                    PlainText("上一个被投掷到接水的是${group[remindInfo.lastMemberId]!!.nameCardOrNick}\n")
                } else {
                    PlainText("上一个被投掷到接水的是${remindInfo.lastMemberId}\n")
                }
                if (remindInfo.myTimerTaskRemindList.isEmpty()) {
                    chain += PlainText("当前无接水计划\n")
                    group.sendMessage(chain)
                    return@matching
                }
                // 输出接下来的接水时间和安排
                for (i in remindInfo.myTimerTaskRemindList.indices) {
                    logger.info(remindInfo.myTimerTaskRemindList[i].toString())
                    val dateFormated =
                        SimpleDateFormat("MM-dd HH:mm").format(remindInfo.myTimerTaskRemindList[i].scheduledExecutionTime())
                    chain += PlainText(
                        "${i + 1}: ${dateFormated}的接水计划，接水者为${
                            if (group.contains(remindInfo.myTimerTaskRemindList[i].memberId)) {
                                group[remindInfo.myTimerTaskRemindList[i].memberId]!!.nameCardOrNick
                            } else {
                                remindInfo.myTimerTaskRemindList[i].memberId
                            }
                        }\n"
                    )
                }
                group.sendMessage(chain)
            }
        }

    }
}

