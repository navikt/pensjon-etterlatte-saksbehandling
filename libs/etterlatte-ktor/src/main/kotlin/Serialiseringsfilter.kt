package no.nav.etterlatte.libs.ktor

import java.io.ObjectInputFilter
import java.io.ObjectInputFilter.FilterInfo
import java.io.ObjectInputFilter.Status

class Serialiseringsfilter : ObjectInputFilter {
    override fun checkInput(info: FilterInfo): Status {
        val serialClass = info.serialClass()
        return when {
            serialClass == null -> Status.UNDECIDED
            tillatt(serialClass) -> Status.ALLOWED
            else -> Status.REJECTED
        }
    }

    private fun tillatt(serialClass: Class<*>) =
        serialClass.module.name == "java.base" ||
            serialClass.module.name == "kotlin" ||
            serialClass.packageName.startsWith("no.nav.etterlatte")
}
