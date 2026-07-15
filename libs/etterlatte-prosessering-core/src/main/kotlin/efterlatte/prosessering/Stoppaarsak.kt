package efterlatte.prosessering

/**
 * Underfelt på en task i status [Status.STOPPET]. Bevarer nyansen fra den gamle
 * modellens FEILET vs. MANUELL_OPPFØLGING uten å gjøre den til en egen status.
 */
enum class Stoppaarsak {
    /** Teknisk feil – retries er brukt opp. */
    FEIL,

    /** Selve task-steget rutet tasken til en saksbehandler. */
    MANUELL,
}
