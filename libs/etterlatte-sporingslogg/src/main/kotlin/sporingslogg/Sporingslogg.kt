package no.nav.etterlatte.libs.sporingslogg

import org.slf4j.LoggerFactory

class Sporingslogg {
    private val auditLogger = LoggerFactory.getLogger("auditLogger")

    fun logg(sporingsrequest: Sporingsrequest) {
        auditLogger.info(sporingsrequest.tilCEFEntry().format())
    }
}
