package online.ruin_of_future.reporter

import kotlinx.coroutines.*
import net.mamoe.mirai.Bot
import net.mamoe.mirai.console.command.CommandManager
import net.mamoe.mirai.console.plugin.jvm.JvmPluginDescription
import net.mamoe.mirai.console.plugin.jvm.KotlinPlugin
import net.mamoe.mirai.contact.Contact
import net.mamoe.mirai.contact.Contact.Companion.sendImage
import net.mamoe.mirai.event.globalEventChannel
import net.mamoe.mirai.event.subscribeFriendMessages
import net.mamoe.mirai.event.subscribeGroupMessages
import net.mamoe.mirai.message.data.Image
import net.mamoe.mirai.message.data.PlainText
import net.mamoe.mirai.message.data.buildMessageChain
import net.mamoe.mirai.utils.ExternalResource
import net.mamoe.mirai.utils.ExternalResource.Companion.toExternalResource
import java.io.ByteArrayInputStream
import java.util.*
import java.util.concurrent.Executors


object ReporterPlugin : KotlinPlugin(
    JvmPluginDescription(
        id = "online.ruin_of_future.reporter",
        version = "1.2.11",
    ) {
        name("Reporter")
        author("LinHeLurking")
    }
) {
    val newsCrawler = NewsCrawler()
    val animeCrawler = AnimeCrawler()
    class MyTimerTask : TimerTask() {
        override fun run() {
            logger.info("Out async Daily pushing")
            async {
                //这里填上每次触发要执行的代码
                logger.info("Daily pushing")
                var successedSend=0
                Bot.instances.forEach {
                    if (it.id in NewsGroupWhiteList.groupIdsPerBot) {
                        for (groupId in NewsGroupWhiteList.groupIdsPerBot[it.id]!!) {
                            try {
                                val group = it.getGroup(groupId)
                                val myPair = newsCrawler.newsToday()
                                if (myPair.first.contains("Ops")){
                                    group?.sendMessage(myPair.first)
                                    logger.info(
                                        "No Daily news Message. push to group " +
                                                (group?.name ?: "<No group of ${groupId}> from ${it.id}")
                                    )
                                }else{
//                                    val inputStream = ByteArrayInputStream(myPair.second)
//                                    val imageExternalResource = inputStream.toExternalResource()
//                                    val imageId = group?.uploadImage(imageExternalResource)?.imageId
                                    val chain = buildMessageChain {
//                                        +Image(imageId ?: "")
                                        +PlainText(myPair.first)
                                    }
                                    group?.sendMessage(chain)
                                    logger.info(
                                        "Daily news push to group " +
                                                (group?.name ?: "<No group of ${groupId}> from ${it.id}")
                                    )
//                                    imageExternalResource.close()
                                }
                                successedSend += 1
                            } catch (e: Exception) {
                                logger.error(e)
                            }
                            delay(100)
                        }
                    }
                    if (it.id in AnimeGroupWhiteList.groupIdsPerBot) {
                        for (groupId in AnimeGroupWhiteList.groupIdsPerBot[it.id]!!) {
                            try {
                                val group = it.getGroup(groupId)
                                group?.sendMessage("早上好呀, 这是今天的 B 站番剧 \n( •̀ ω •́ )✧")
                                group?.sendImage(ByteArrayInputStream(animeCrawler.animeToday()))
                                logger.info(
                                    "Daily anime push to group " +
                                            (group?.name ?: "<No group of ${groupId}> from ${it.id}")
                                )
                            } catch (e: Exception) {
                                logger.error(e)
                            }
                            delay(100)
                        }
                    }
                    if (it.id in NewsGroupWhiteList.groupIdsPerBot && successedSend==0) {
                        for (groupId in NewsGroupWhiteList.groupIdsPerBot[it.id]!!) {
                            try {
                                val group = it.getGroup(groupId)
                                val myPair = newsCrawler.newsToday()
                                if (myPair.first.contains("Ops")){
                                    group?.sendMessage(myPair.first)
                                    logger.info(
                                        "No Daily news Message. push to group " +
                                                (group?.name ?: "<No group of ${groupId}> from ${it.id}")
                                    )
                                }else{
//                                    val inputStream = ByteArrayInputStream(myPair.second)
//                                    val imageExternalResource = inputStream.toExternalResource()
//                                    val imageId = group?.uploadImage(imageExternalResource)?.imageId
                                    val chain = buildMessageChain {
//                                        +Image(imageId ?: "")
                                        +PlainText(myPair.first)
                                    }
                                    group?.sendMessage(chain)
                                    logger.info(
                                        "Daily news push to group " +
                                                (group?.name ?: "<No group of ${groupId}> from ${it.id}")
                                    )
//                                    imageExternalResource.close()
                                }
                                successedSend += 1
                            } catch (e: Exception) {
                                logger.error(e)
                            }
                            delay(100)
                        }
                    }
                }
            }
        }
    }
    class MyTimerTask2 : TimerTask() {
        override fun run() {
            logger.info("WeakUp")
        }

    }
    override fun onEnable() {
        NewsGroupWhiteList.reload()
        AnimeGroupWhiteList.reload()

        CommandManager.registerCommand(NewsGroupCommand)
        CommandManager.registerCommand(AnimeGroupCommand)

        this.launch {
            delay(5000)
            val cal = Calendar.getInstance()
//            cal[Calendar.YEAR] = 2021
//            cal[Calendar.MONTH] = Calendar.NOVEMBER
//            cal[Calendar.DAY_OF_MONTH] = 27
//
//            cal[Calendar.HOUR_OF_DAY] = 9
//            cal[Calendar.MINUTE] = 0
//            cal[Calendar.SECOND] = 0
            cal.set(Calendar.HOUR_OF_DAY,9)
            cal.set(Calendar.MINUTE,0)

            cal.add(Calendar.DAY_OF_YEAR,1)
            val dateRepresentation = cal.time
            val timer = Timer()
            val daySpan = (24 * 60 * 60 * 1000).toLong()
//            val task2 =
            timer.scheduleAtFixedRate(MyTimerTask2(), dateRepresentation.time.minus(10 * 60 * 1000), daySpan)
//            val task =
            timer.scheduleAtFixedRate(MyTimerTask(), dateRepresentation, daySpan)
            logger.info("XXX")
        }

        val sendNewsToTarget: suspend (Contact) -> Unit = {
            try {
                it.sendMessage("这是今天的新闻速报 \nq(≧▽≦q)")
                val myPair = newsCrawler.newsToday()

//                it.sendMessage(myPair.first)

                if (myPair.first.contains("Ops")){
                    it.sendMessage(myPair.first)
                }else{
//                    val inputStream = ByteArrayInputStream(myPair.second)
//                    val imageExternalResource = inputStream.toExternalResource()
//                    val imageId = it.uploadImage(imageExternalResource).imageId
                    val chain = buildMessageChain {
//                        +Image(imageId ?: "")
                        +PlainText(myPair.first)
                    }
                    it.sendMessage(chain)
//                    imageExternalResource.close()
                }

            } catch (e: Exception) {
                it.sendMessage("出错啦, 等会再试试吧 ￣へ￣")
                logger.error(e)
            }
        }

        this.globalEventChannel().subscribeGroupMessages {
            matching(Regex("(每日|今日)?(新闻|速报|速递)")) {
                logger.info("$senderName 发起了新闻请求...")
                if (NewsGroupWhiteList.groupIdsPerBot[bot.id]?.contains(group.id) == true) {
                    sendNewsToTarget(group)
                } else {
                    sender.sendMessage("为了防止打扰到网友，这个群不在日报白名单呢 QwQ")
                }

            }
        }

        this.globalEventChannel().subscribeFriendMessages {
            matching(Regex("(每日|今日)?(新闻|速报|速递)")) {
                logger.info("$senderName 发起了新闻请求...")
                sendNewsToTarget(sender)
            }
        }

        val sendAnimeToTarget: suspend (Contact) -> Unit = {
            try {
                it.sendMessage("这是今天的 B 站番剧 \n( •̀ ω •́ )✧")
                it.sendImage(ByteArrayInputStream(animeCrawler.animeToday()))
            } catch (e: Exception) {
                when (e) {
                    is NoAnimeException -> {
                        it.sendMessage("好像今天没有放送呢 >_<")
                        logger.info(e)
                    }
                    else -> {
                        it.sendMessage("出错啦, 等会再试试吧 ￣へ￣")
                        logger.error(e)
                    }
                }
            }
        }


        this.globalEventChannel().subscribeGroupMessages {
            matching(Regex("(每日|今日)?(新番|番剧|动画)")) {
                logger.info("$senderName 发起了动画请求...")
                if (AnimeGroupWhiteList.groupIdsPerBot[bot.id]?.contains(group.id) == true) {
                    sendAnimeToTarget(group)
                } else {
                    sender.sendMessage("为了防止打扰到网友，这个群不在日报白名单呢 QwQ")
                }
            }
        }

        this.globalEventChannel().subscribeFriendMessages {
            matching(Regex("(每日|今日)?(新番|番剧|动画)")) {
                logger.info("$senderName 发起了动画请求...")
                sendAnimeToTarget(sender)
            }
        }
    }
}