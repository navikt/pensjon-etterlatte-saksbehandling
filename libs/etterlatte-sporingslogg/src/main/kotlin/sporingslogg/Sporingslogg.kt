package sporingslogg

class Sporingslogg {

    val auditLogger = org.slf4j.LoggerFactory.getLogger("AUDIT_LOGGER")

    fun logg(sporingsrequest: Sporingsrequest) {
        auditLogger.info(sporingsrequest.tilCEFEntry().format())
    }
}