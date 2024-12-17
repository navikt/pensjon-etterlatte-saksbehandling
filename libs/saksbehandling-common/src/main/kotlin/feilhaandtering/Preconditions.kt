package no.nav.etterlatte.libs.common.feilhaandtering

/**
 * Erstatter Kotlin sin precondition [require]
 *
 * @throws InternfeilException med [feilmelding] dersom [verdi] er false
 **/
inline fun krev(
    verdi: Boolean,
    feilmelding: () -> String,
) {
    if (!verdi) {
        throw InternfeilException(feilmelding())
    }
}

/**
 * Erstatter Kotlin sin precondition [requireNotNull]
 *
 * @throws InternfeilException med [feilmelding] dersom [verdi] er null
 *
 * @return [verdi] dersom den ikke er null
 **/
inline fun <T : Any> krevIkkeNull(
    verdi: T?,
    feilmelding: () -> String,
): T {
    if (verdi == null) {
        throw InternfeilException(feilmelding())
    }
    return verdi
}

/**
 * Erstatter Kotlin sin precondition [check]
 *
 * @throws UgyldigForespoerselException med [feilmelding] dersom [verdi] er false
 **/
inline fun sjekk(
    verdi: Boolean,
    feilmelding: () -> String,
) {
    if (!verdi) {
        throw UgyldigForespoerselException(code = "TILSTANDSSJEKK_FEILET", detail = feilmelding())
    }
}

/**
 * Erstatter Kotlin sin precondition [checkNotNull]
 *
 * @throws UgyldigForespoerselException med [feilmelding] dersom [verdi] er null
 *
 * @return [verdi] dersom den ikke er null
 **/
inline fun <T : Any> sjekkIkkeNull(
    verdi: T?,
    feilmelding: () -> String,
): T {
    if (verdi == null) {
        throw UgyldigForespoerselException(code = "VERDI_ER_NULL", detail = feilmelding())
    }
    return verdi
}
