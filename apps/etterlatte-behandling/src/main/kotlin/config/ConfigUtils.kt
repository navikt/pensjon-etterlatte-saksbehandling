package no.nav.etterlatte.config

import com.typesafe.config.Config

fun samle(config: Config, env: Map<String, String>): Map<String, String> {
    val conf = config
        .root()
        .toList()
        .map { unwrap(mutableSetOf(), Node(it.first, it.second.unwrapped())) }
        .filter { it.isNotEmpty() }
        .flatten()
        .associate { it.key to it.value!!.toString() }
    return conf + env
}

private fun unwrap(set: MutableSet<Node>, pair: Node): Set<Node> = when (pair.value) {
    is Map<*, *> -> set.addAllAndReturn(unwrapMap(pair.value, pair))
    is List<*> -> set.addAllAndReturn(unwrapList(pair.value, pair))
    is String -> mutableSetOf(Node(pair.key, pair.value.toString()))
    else -> set
}

private fun unwrapList(value: List<*>, pair: Node) = value.flatMap {
    unwrap(
        mutableSetOf(),
        Node(pair.key, it)
    )
}

private fun unwrapMap(value: Map<*, *>, pair: Node) = value.entries.flatMap {
    unwrap(
        mutableSetOf(),
        Node(pair.key + "." + it.key.toString(), it.value)
    )
}

private fun <E> MutableSet<E>.addAllAndReturn(newELements: Collection<E>): MutableSet<E> {
    addAll(newELements)
    return this
}

private data class Node(val key: String, val value: Any?)