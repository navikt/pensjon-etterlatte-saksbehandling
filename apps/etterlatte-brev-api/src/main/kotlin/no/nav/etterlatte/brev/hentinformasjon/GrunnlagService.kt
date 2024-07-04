package no.nav.etterlatte.brev.hentinformasjon

import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
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
        sakId: Long,
        bruker: BrukerTokenInfo,
        behandlingId: UUID?,
    ) = coroutineScope {
        when (vedtakType) {
            VedtakType.TILBAKEKREVING,
            VedtakType.AVVIST_KLAGE,
            -> async { hentGrunnlagForSak(sakId, bruker) }.await()

            null -> async { hentGrunnlagForSak(sakId, bruker) }.await()
            else -> async { klient.hentGrunnlag(behandlingId!!, bruker) }.await()
        }
    }

    suspend fun hentGrunnlagForSak(
        sakId: Long,
        bruker: BrukerTokenInfo,
    ) = klient.hentGrunnlagForSak(sakId, bruker)

    suspend fun hentGrunnlag(
        behandlingId: UUID,
        bruker: BrukerTokenInfo,
    ) = klient.hentGrunnlag(behandlingId, bruker)

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
            val vergeFnr = verger.first().vergeEllerFullmektig.motpartsPersonident!!
            val vergenavn =
                adresseService
                    .hentMottakerAdresse(sakType, vergeFnr.value)
                    .navn
            Vergemaal(
                vergenavn,
                vergeFnr,
            )
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
        } else if (sakType == SakType.BARNEPENSJON && !grunnlag.erOver18(brevutfallDto)) {
            grunnlag.hentForelderVerge()
        } else {
            null
        }
    }
}
