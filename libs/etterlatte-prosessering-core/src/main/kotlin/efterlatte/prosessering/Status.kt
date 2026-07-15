package efterlatte.prosessering

/**
 * De fem persisterte tilstandene i task-livssyklusen (jf. funksjonsspec).
 *
 * Retry-teller, «henger?» og stopp-årsak er avledede signaler / underfelter –
 * ikke egne statuser.
 */
enum class Status {
    /** Klar til å kjøres – inkludert venting på fremtidig trigger_tid og mellom retries. */
    KLAR,

    /** Plukket og kjører akkurat nå. */
    KJØRER,

    /** Fullført (terminal). */
    FULLFØRT,

    /** Ga opp – trenger en menneskelig beslutning (teknisk feil eller manuell oppfølging). */
    STOPPET,

    /** Operatør avfeide tasken; den kjøres aldri. */
    AVBRUTT,
}
