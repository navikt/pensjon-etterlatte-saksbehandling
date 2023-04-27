package no.nav.etterlatte.config

import com.typesafe.config.Config
import no.nav.etterlatte.libs.common.Miljoevariabler

fun samle(config: Config, env: Map<String, String>): Miljoevariabler {
    val conf = config
        .root()
        .toList()
        .map { unwrap(mutableSetOf(), Node(it.first, it.second.unwrapped())) }
        .filter { it.isNotEmpty() }
        .flatten()
        .associate { it.key to it.value.toString() }
    return Miljoevariabler(conf + env)
}

private fun unwrap(set: MutableSet<Node>, node: Node): Set<Node> = when (node.value) {
    is Map<*, *> -> set.addAllAndReturn(unwrapMap(node.value, node.key))
    is Collection<*> -> set.addAllAndReturn(unwrapCollection(node.value, node.key))
    is String -> mutableSetOf(Node(node.key, node.value.toString()))
    else -> set
}

private fun unwrapCollection(value: Collection<Any?>, key: String) = value.flatMap {
    unwrap(
        mutableSetOf(),
        Node(key, it)
    )
}

private fun unwrapMap(value: Map<*, *>, key: String) = value.entries.flatMap {
    unwrap(
        mutableSetOf(),
        Node(key + "." + it.key.toString(), it.value)
    )
}

private fun <E> MutableSet<E>.addAllAndReturn(newELements: Collection<E>): MutableSet<E> {
    addAll(newELements)
    return this
}

private data class Node(val key: String, val value: Any?)