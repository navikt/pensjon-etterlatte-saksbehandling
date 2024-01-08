package no.nav.etterlatte.brev.brevbaker

import no.nav.etterlatte.brev.MigreringBrevRequest
import no.nav.etterlatte.brev.adresse.AdresseService
import no.nav.etterlatte.brev.behandling.GenerellBrevData
import no.nav.etterlatte.brev.model.BrevDataMapper
import no.nav.etterlatte.brev.model.BrevID
import no.nav.etterlatte.brev.model.BrevProsessType
import no.nav.etterlatte.brev.model.Pdf
import no.nav.etterlatte.brev.model.Slate
import no.nav.etterlatte.token.BrukerTokenInfo
import org.slf4j.LoggerFactory
import java.util.Base64

class BrevbakerService(
    private val brevbakerKlient: BrevbakerKlient,
    private val adresseService: AdresseService,
    private val brevDataMapper: BrevDataMapper,
) {
    private val logger = LoggerFactory.getLogger(this::class.java)

    suspend fun genererPdf(
        brevID: BrevID,
        brevRequest: BrevbakerRequest,
    ): Pdf {
        val brevbakerResponse = brevbakerKlient.genererPdf(brevRequest)

        return Base64.getDecoder().decode(brevbakerResponse.base64pdf)
            .let { Pdf(it) }
            .also { logger.info("Generert brev (id=$brevID) med størrelse: ${it.bytes.size}") }
    }

    suspend fun hentRedigerbarTekstFraBrevbakeren(redigerbarTekstRequest: RedigerbarTekstRequest): Slate {
        val request =
            BrevbakerRequest.fra(
                brevDataMapper.brevKode(
                    redigerbarTekstRequest.generellBrevData,
                    BrevProsessType.REDIGERBAR,
                    erOmregningNyRegel = redigerbarTekstRequest.migrering?.erOmregningGjenny ?: false,
                ).redigering,
                brevDataMapper.brevData(redigerbarTekstRequest),
                adresseService.hentAvsender(redigerbarTekstRequest.generellBrevData.avsenderRequest()),
                redigerbarTekstRequest.generellBrevData.personerISak,
                redigerbarTekstRequest.generellBrevData.sak.id,
                redigerbarTekstRequest.generellBrevData.spraak,
                redigerbarTekstRequest.generellBrevData.sak.sakType,
            )
        val brevbakerResponse = brevbakerKlient.genererJSON(request)
        return BlockTilSlateKonverterer.konverter(brevbakerResponse)
    }
}

data class RedigerbarTekstRequest(
    val generellBrevData: GenerellBrevData,
    val brukerTokenInfo: BrukerTokenInfo,
    val prosessType: BrevProsessType,
    val migrering: MigreringBrevRequest? = null,
) {
    fun vedtakstype() = generellBrevData.forenkletVedtak.type.name.lowercase()
}
