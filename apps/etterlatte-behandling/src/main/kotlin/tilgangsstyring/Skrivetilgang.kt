package no.nav.etterlatte.tilgangsstyring

import no.nav.etterlatte.SaksbehandlerMedEnheterOgRoller
import no.nav.etterlatte.SystemUser
import no.nav.etterlatte.User
import no.nav.etterlatte.libs.common.Enhetsnummer

/**
 * Om innlogget bruker kan endre noe på en sak i en gitt enhet. Sendes til frontend slik at
 * saksbehandler kan få vite hvorfor noe er skrivebeskyttet. Status på behandlingen inngår ikke.
 */
data class Skrivetilgang(
    val harSkrivetilgang: Boolean,
    val aarsak: ManglerSkrivetilgangAarsak?,
) {
    companion object {
        val HAR_SKRIVETILGANG = Skrivetilgang(harSkrivetilgang = true, aarsak = null)

        fun mangler(aarsak: ManglerSkrivetilgangAarsak) = Skrivetilgang(harSkrivetilgang = false, aarsak = aarsak)
    }
}

enum class ManglerSkrivetilgangAarsak {
    /** Bruker mangler saksbehandler- og attestantrollen, f.eks. har bare lesetilgang (GJENNY_LES). */
    MANGLER_ROLLE,

    /** Bruker har rollen, men sakens enhet er ikke blant brukerens saksbehandlende enheter. */
    ENHET_UTEN_SKRIVETILGANG,

    /** Verken saksbehandler eller systembruker. */
    UKJENT_BRUKER,
}

/**
 * Selve avgjørelsen om skrivetilgang for en saksbehandler. Brukes både av [sjekkSkrivetilgang] og
 * [vurderSkrivetilgang], slik at det frontend viser og det backend håndhever ikke kan gli fra hverandre.
 */
fun SaksbehandlerMedEnheterOgRoller.harSkrivetilgangTilEnhet(enhetNr: Enhetsnummer): Boolean =
    hentEnheterMedSkrivetilgang().contains(enhetNr)

fun vurderSkrivetilgang(
    user: User,
    enhetNr: Enhetsnummer,
): Skrivetilgang =
    when (user) {
        is SaksbehandlerMedEnheterOgRoller -> {
            when {
                user.harSkrivetilgangTilEnhet(enhetNr) -> Skrivetilgang.HAR_SKRIVETILGANG
                !user.harSkriverolle() -> Skrivetilgang.mangler(ManglerSkrivetilgangAarsak.MANGLER_ROLLE)
                else -> Skrivetilgang.mangler(ManglerSkrivetilgangAarsak.ENHET_UTEN_SKRIVETILGANG)
            }
        }

        is SystemUser -> Skrivetilgang.HAR_SKRIVETILGANG

        else -> {
            Skrivetilgang.mangler(ManglerSkrivetilgangAarsak.UKJENT_BRUKER)
        }
    }
