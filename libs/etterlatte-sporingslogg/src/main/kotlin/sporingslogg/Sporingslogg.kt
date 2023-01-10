package no.nav.etterlatte.libs.sporingslogg

class Sporingslogg {

    private val auditLogger = org.slf4j.LoggerFactory.getLogger("auditLogger")

    fun logg(sporingsrequest: Sporingsrequest) {
        auditLogger.info(sporingsrequest.tilCEFEntry().format())
    }
}