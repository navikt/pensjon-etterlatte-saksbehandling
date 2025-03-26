package no.nav.etterlatte.brev

import no.nav.etterlatte.brev.behandling.erOver18
import no.nav.etterlatte.brev.behandling.hentForelderVerge
import no.nav.etterlatte.libs.common.behandling.BrevutfallDto
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.feilhaandtering.InternfeilException
import no.nav.etterlatte.libs.common.grunnlag.Grunnlag
import no.nav.etterlatte.libs.common.grunnlag.hentFoedselsnummer
import no.nav.etterlatte.libs.common.grunnlag.hentSoekerPdlV1
import no.nav.etterlatte.libs.common.person.UkjentVergemaal
import no.nav.etterlatte.libs.common.person.Verge
import no.nav.etterlatte.libs.common.person.Vergemaal
import no.nav.etterlatte.libs.common.person.hentVerger
import no.nav.etterlatte.libs.ktor.route.logger
import no.nav.etterlatte.sikkerLogg

fun hentVergeForSak(
    sakType: SakType,
    brevutfallDto: BrevutfallDto?,
    grunnlag: Grunnlag,
): Verge? {
    val soekerPdl =
        grunnlag.soeker.hentSoekerPdlV1()
            ?: throw InternfeilException(
                "Finner ikke søker i grunnlaget. Dette kan komme av flere ting, bl.a. endret ident på bruker. " +
                    "Hvis dette ikke er tilfellet må feilen meldes i Porten.",
            )

    val verger =
        hentVerger(
            soekerPdl.verdi.vergemaalEllerFremtidsfullmakt ?: emptyList(),
            grunnlag.soeker.hentFoedselsnummer()?.verdi,
        )
    return if (verger.size == 1) {
        val vergeFnr = verger.first().vergeEllerFullmektig.motpartsPersonident
        if (vergeFnr == null) {
            logger.error(
                "Vi genererer et brev til en person som har verge uten ident. Det er verdt å følge " +
                    "opp saken ekstra, for å sikre at det ikke blir noe feil her (koble på fag). saken har " +
                    "id=${grunnlag.metadata.sakId}. Denne loggmeldingen kan nok fjernes etter at løpet her" +
                    " er kvalitetssikret.",
            )
            UkjentVergemaal()
        } else {
            // TODO: Hente navn direkte fra Grunnlag eller PDL
            val vergenavn = "placeholder for vergenavn"

            Vergemaal(
                vergenavn,
                vergeFnr,
            )
        }
    } else if (verger.size > 1) {
        logger.info(
            "Fant flere verger for bruker med fnr ${grunnlag.soeker.hentFoedselsnummer()?.verdi} i " +
                "mapping av verge til brev.",
        )
        sikkerLogg.info(
            "Fant flere verger for bruker med fnr ${
                grunnlag.soeker.hentFoedselsnummer()?.verdi?.value
            } i mapping av verge til brev.",
        )
        UkjentVergemaal()
    } else if (sakType == SakType.BARNEPENSJON && !grunnlag.erOver18(brevutfallDto?.aldersgruppe)) {
        grunnlag.hentForelderVerge()
    } else {
        null
    }
}
