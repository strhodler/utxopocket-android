package com.strhodler.utxopocket.presentation.wiki

object WikiNavigation {
    const val ListRoute = "wiki/list"
    const val SearchRoute = "wiki/search"
    const val DetailRoute = "wiki/detail/{topicId}"
    const val TopicIdArg = "topicId"

    fun detailRoute(topicId: String): String = "wiki/detail/$topicId"
}
