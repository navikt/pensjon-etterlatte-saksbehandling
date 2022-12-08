package no.nav.etterlatte.brev

import no.nav.etterlatte.libs.common.brev.model.Brev
import no.nav.etterlatte.libs.common.brev.model.BrevEventTypes
import no.nav.etterlatte.libs.common.brev.model.DistribusjonMelding
import no.nav.etterlatte.libs.common.distribusjon.DistribusjonsType
import no.nav.etterlatte.libs.common.journalpost.Bruker
import no.nav.etterlatte.libs.common.rapidsandrivers.correlationIdKey
import no.nav.etterlatte.libs.common.rapidsandrivers.eventNameKey
import no.nav.etterlatte.libs.common.toJson
import no.nav.etterlatte.libs.common.vedtak.Vedtak
import no.nav.helse.rapids_rivers.JsonMessage
import org.slf4j.LoggerFactory
import java.util.UUID

class DistribusjonService(
    private val sendToRapid: (String) -> Unit
) {
    private val logger = LoggerFactory.getLogger(DistribusjonService::class.java)

    fun distribuerBrev(brev: Brev, vedtak: Vedtak) = sendToRapid(opprettDistribusjonsmelding(brev, vedtak))

    private fun opprettDistribusjonsmelding(brev: Brev, vedtak: Vedtak): String =
        DistribusjonMelding(
            behandlingId = brev.behandlingId,
            distribusjonType = if (brev.erVedtaksbrev) DistribusjonsType.VEDTAK else DistribusjonsType.VIKTIG,
            brevId = brev.id,
            mottaker = brev.mottaker,
            bruker = Bruker(vedtak.sak.ident),
            tittel = brev.tittel,
            brevKode = "XX.YY-ZZ",
            journalfoerendeEnhet = vedtak.vedtakFattet!!.ansvarligEnhet
        ).let {
            val correlationId = UUID.randomUUID().toString()
            logger.info("Oppretter distribusjonsmelding for brev (id=${brev.id}) med correlation_id=$correlationId")

            JsonMessage.newMessage(
                mapOf(
                    eventNameKey to BrevEventTypes.FERDIGSTILT.toString(),
                    "brevId" to it.brevId,
                    correlationIdKey to correlationId,
                    "payload" to it.toJson()
                )
            ).toJson()
        }
}
