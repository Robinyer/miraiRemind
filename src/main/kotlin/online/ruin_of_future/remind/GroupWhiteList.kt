package online.ruin_of_future.remind

import net.mamoe.mirai.Bot
import net.mamoe.mirai.console.command.CommandSender
import net.mamoe.mirai.console.command.CommandSender.Companion.asCommandSender
import net.mamoe.mirai.console.command.CompositeCommand
import net.mamoe.mirai.console.command.ConsoleCommandSender
import net.mamoe.mirai.console.command.UserCommandSender
import net.mamoe.mirai.console.data.AutoSavePluginConfig
import net.mamoe.mirai.console.data.value
import net.mamoe.mirai.console.util.ConsoleExperimentalApi
import net.mamoe.mirai.contact.Group


private suspend fun addRemindWhiteList(
    bot: Bot,
    sender: CommandSender,
    target: Group,
    whiteList: MutableMap<Long, MutableList<Long>>,
    groupInfoPerBot: MutableMap<Long, MutableMap<Long, RemindInfo>>
) {
    try {
        if (whiteList.containsKey(bot.id)) {
            whiteList[bot.id]!!.add(target.id)
        } else {
            whiteList[bot.id] = mutableListOf(target.id)
            groupInfoPerBot[bot.id] = mutableMapOf()
        }
        if (groupInfoPerBot.containsKey(bot.id)) {
            groupInfoPerBot[bot.id]!![target.id] = RemindInfo()
        } else {
            groupInfoPerBot[bot.id] = mutableMapOf(target.id to RemindInfo())
        }
        sender.sendMessage("添加 ${target.name} 成功")
    } catch (e: Exception) {
        sender.sendMessage("添加 ${target.name} 失败 QAQ")
    }
}

private suspend fun removeWhiteList(
    bot: Bot,
    sender: CommandSender,
    target: Group,
    whiteList: MutableMap<Long, MutableList<Long>>,
    groupInfoPerBot: MutableMap<Long, MutableMap<Long, RemindInfo>>
) {
    try {
        if (whiteList.containsKey(bot.id)) {
            whiteList[bot.id]!!.remove(target.id)
        }
        if (groupInfoPerBot.containsKey(bot.id)) {
            groupInfoPerBot[bot.id]!!.remove(target.id)
        }
        sender.sendMessage("移除 ${target.name} 成功")
    } catch (e: Exception) {
        sender.sendMessage("移除 ${target.name} 失败 QAQ")
    }
}

private suspend fun listWhiteList(
    bot: Bot,
    sender: CommandSender,
    whiteList: MutableMap<Long, MutableList<Long>>
) {
    try {
        val resStrBuilder = StringBuilder()
        resStrBuilder.append("来自 ${bot.nick}(${bot.id}) 的配置，该机器人的白名单如下\n")
        if (whiteList[bot.id]?.isEmpty() != false) {
            resStrBuilder.append("白名单为空呢 >_<")
        } else {
            for (groupId in whiteList[bot.id]!!) {
                resStrBuilder.append("$groupId, ${bot.getGroup(groupId)?.name}")
                resStrBuilder.append('\n')
            }
        }
        sender.sendMessage(resStrBuilder.toString())
    } catch (e: Exception) {
        sender.sendMessage("出错啦 QAQ")
    }
}

@OptIn(ConsoleExperimentalApi::class)
object RemindGroupCommand : CompositeCommand(
    ReporterPlugin,
    "remind_group", "Remind群组", // "manage" 是主指令名
    description = "Remind的群组白名单管理"
) {


    @SubCommand("list", "显示", "展示", "show")
    suspend fun CommandSender.list() {
        if (this is UserCommandSender) {
            listWhiteList(bot, user.asCommandSender(true), RemindGroupWhiteList.groupIdsPerBot)
        } else if (this is ConsoleCommandSender) {
            for (bot in Bot.instances) {
                listWhiteList(bot, this, RemindGroupWhiteList.groupIdsPerBot)
            }
        }
    }

    @SubCommand("add", "添加")
    suspend fun CommandSender.add(target: Group) {
        if (this is UserCommandSender) {
            addRemindWhiteList(
                bot,
                user.asCommandSender(true),
                target,
                RemindGroupWhiteList.groupIdsPerBot,
                RemindGroupInfoList.groupInfoPerBot
            )
        } else if (this is ConsoleCommandSender) {
            for (bot in Bot.instances) {
                addRemindWhiteList(
                    bot,
                    this,
                    target,
                    RemindGroupWhiteList.groupIdsPerBot,
                    RemindGroupInfoList.groupInfoPerBot
                )
            }
        }
    }

    @SubCommand("delete", "remove", "删除", "移除")
    suspend fun CommandSender.remove(target: Group) {
        if (this is UserCommandSender) {
            removeWhiteList(bot, user.asCommandSender(true), target, RemindGroupWhiteList.groupIdsPerBot, RemindGroupInfoList.groupInfoPerBot)
        } else {
            for (bot in Bot.instances) {
                removeWhiteList(bot, this, target, RemindGroupWhiteList.groupIdsPerBot, RemindGroupInfoList.groupInfoPerBot)
            }
        }
    }
}

//object NewsGroupWhiteList : AutoSavePluginConfig("newsGroupWhiteList2") {
//    val groupIdsPerBot: MutableMap<Long, MutableList<Long>> by value()
//}

//object AnimeGroupWhiteList : AutoSavePluginConfig("bangumiGroupWhiteList2") {
//    val groupIdsPerBot: MutableMap<Long, MutableList<Long>> by value()
//}

object RemindGroupWhiteList : AutoSavePluginConfig("RemindGroupWhiteList") {
    val groupIdsPerBot: MutableMap<Long, MutableList<Long>> by value()
}
object RemindGroupInfoList : AutoSavePluginConfig("RemindGroupInfoList") {
    val groupInfoPerBot: MutableMap<Long, MutableMap<Long, RemindInfo>> by value()
}