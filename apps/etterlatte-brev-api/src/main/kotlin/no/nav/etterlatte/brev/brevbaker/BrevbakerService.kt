package no.nav.etterlatte.brev.brevbaker

import no.nav.etterlatte.brev.EtterlatteBrevKode
import no.nav.etterlatte.brev.adresse.AdresseService
import no.nav.etterlatte.brev.behandling.ForenkletVedtak
import no.nav.etterlatte.brev.behandling.GenerellBrevData
import no.nav.etterlatte.brev.behandling.avsender
import no.nav.etterlatte.brev.model.BrevDataRedigerbar
import no.nav.etterlatte.brev.model.BrevID
import no.nav.etterlatte.brev.model.Pdf
import no.nav.etterlatte.brev.model.Slate
import no.nav.etterlatte.brev.model.Spraak
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.retryOgPakkUt
import no.nav.etterlatte.libs.ktor.token.BrukerTokenInfo
import org.slf4j.LoggerFactory
import java.util.Base64

class BrevbakerService(
    private val brevbakerKlient: BrevbakerKlient,
    private val adresseService: AdresseService,
) {
    private val logger = LoggerFactory.getLogger(this::class.java)

    suspend fun genererPdf(
        brevID: BrevID?,
        brevRequest: BrevbakerRequest,
    ): Pdf {
        val brevbakerResponse = retryOgPakkUt { brevbakerKlient.genererPdf(brevRequest) }

        return Base64
            .getDecoder()
            .decode(brevbakerResponse.base64pdf)
            .let { Pdf(it) }
            .also { logger.info("Generert brev (id=$brevID) med størrelse: ${it.bytes.size}") }
    }

    suspend fun hentRedigerbarTekstFraBrevbakeren(redigerbarTekstRequest: RedigerbarTekstRequest): Slate {
        val request =
            with(redigerbarTekstRequest) {
                BrevbakerRequest.fra(
                    brevkode,
                    brevdata(this),
                    adresseService.hentAvsender(avsender()),
                    soekerOgEventuellVerge,
                    sakId,
                    spraak,
                    sakType,
                )
            }
        val brevbakerResponse = retryOgPakkUt { brevbakerKlient.genererJSON(request) }
        return BlockTilSlateKonverterer.konverter(brevbakerResponse)
    }
}

data class RedigerbarTekstRequest(
    val generellBrevData: GenerellBrevData,
    val brukerTokenInfo: BrukerTokenInfo,
    val brevkode: EtterlatteBrevKode,
    val brevdata: suspend (RedigerbarTekstRequest) -> BrevDataRedigerbar,
    val soekerOgEventuellVerge: SoekerOgEventuellVerge,
    val sakId: Long,
    val spraak: Spraak,
    val sakType: SakType,
    val forenkletVedtak: ForenkletVedtak?,
    val enhet: String,
) {
    fun avsender() = avsender(brukerTokenInfo, forenkletVedtak, enhet)
}
