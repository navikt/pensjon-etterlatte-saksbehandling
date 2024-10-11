package no.nav.etterlatte.brev.hentinformasjon.grunnlag

import no.nav.etterlatte.brev.adresse.AdresseService
import no.nav.etterlatte.brev.behandling.erOver18
import no.nav.etterlatte.brev.behandling.hentForelderVerge
import no.nav.etterlatte.libs.common.behandling.BrevutfallDto
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.grunnlag.Grunnlag
import no.nav.etterlatte.libs.common.grunnlag.hentFoedselsnummer
import no.nav.etterlatte.libs.common.grunnlag.hentSoekerPdlV1
import no.nav.etterlatte.libs.common.person.UkjentVergemaal
import no.nav.etterlatte.libs.common.person.Verge
import no.nav.etterlatte.libs.common.person.Vergemaal
import no.nav.etterlatte.libs.common.person.hentVerger
import no.nav.etterlatte.libs.common.sak.Sak
import no.nav.etterlatte.libs.common.sak.SakId
import no.nav.etterlatte.libs.common.vedtak.VedtakType
import no.nav.etterlatte.libs.ktor.route.logger
import no.nav.etterlatte.libs.ktor.token.BrukerTokenInfo
import no.nav.etterlatte.sikkerLogg
import java.util.UUID

class GrunnlagService(
    private val klient: GrunnlagKlient,
    private val adresseService: AdresseService,
) {
    suspend fun hentGrunnlag(
        vedtakType: VedtakType?,
        sakId: SakId,
        bruker: BrukerTokenInfo,
        behandlingId: UUID?,
    ) = when (vedtakType) {
        VedtakType.TILBAKEKREVING,
        VedtakType.AVVIST_KLAGE,
        -> hentGrunnlagForSak(sakId, bruker)

        null -> hentGrunnlagForSak(sakId, bruker)
        else -> klient.hentGrunnlag(behandlingId!!, bruker)
    }

    suspend fun hentGrunnlagForSak(
        sakId: SakId,
        bruker: BrukerTokenInfo,
    ) = klient.hentGrunnlagForSak(sakId, bruker)

    suspend fun oppdaterGrunnlagForSak(
        sak: Sak,
        bruker: BrukerTokenInfo,
    ) = klient.oppdaterGrunnlagForSak(sak, bruker)

    suspend fun hentVergeForSak(
        sakType: SakType,
        brevutfallDto: BrevutfallDto?,
        grunnlag: Grunnlag,
    ): Verge? {
        val verger =
            hentVerger(
                grunnlag.soeker
                    .hentSoekerPdlV1()!!
                    .verdi.vergemaalEllerFremtidsfullmakt ?: emptyList(),
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
                val vergenavn =
                    adresseService
                        .hentMottakerAdresse(sakType, vergeFnr.value)
                        .navn
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
}
