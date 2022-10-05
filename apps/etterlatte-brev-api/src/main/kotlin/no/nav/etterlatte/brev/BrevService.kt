package no.nav.etterlatte.brev

import no.nav.etterlatte.brev.model.AnnetBrevRequest
import no.nav.etterlatte.brev.model.Avsender
import no.nav.etterlatte.brev.model.AvslagBrevRequest
import no.nav.etterlatte.brev.model.InnvilgetBrevRequest
import no.nav.etterlatte.brev.model.mapper.finnBarn
import no.nav.etterlatte.db.BrevRepository
import no.nav.etterlatte.domene.vedtak.Vedtak
import no.nav.etterlatte.domene.vedtak.VedtakType
import no.nav.etterlatte.grunnbeloep.GrunnbeloepKlient
import no.nav.etterlatte.libs.common.brev.model.Brev
import no.nav.etterlatte.libs.common.brev.model.BrevEventTypes
import no.nav.etterlatte.libs.common.brev.model.BrevID
import no.nav.etterlatte.libs.common.brev.model.BrevInnhold
import no.nav.etterlatte.libs.common.brev.model.DistribusjonMelding
import no.nav.etterlatte.libs.common.brev.model.Mottaker
import no.nav.etterlatte.libs.common.brev.model.Status
import no.nav.etterlatte.libs.common.brev.model.UlagretBrev
import no.nav.etterlatte.libs.common.distribusjon.DistribusjonsType
import no.nav.etterlatte.libs.common.journalpost.Bruker
import no.nav.etterlatte.libs.common.person.Foedselsnummer
import no.nav.etterlatte.libs.common.rapidsandrivers.correlationIdKey
import no.nav.etterlatte.libs.common.rapidsandrivers.eventNameKey
import no.nav.etterlatte.libs.common.soeknad.dataklasser.common.Spraak
import no.nav.etterlatte.libs.common.toJson
import no.nav.etterlatte.norg2.Norg2Klient
import no.nav.etterlatte.pdf.PdfGeneratorKlient
import no.nav.etterlatte.vedtak.VedtakService
import no.nav.helse.rapids_rivers.JsonMessage
import org.slf4j.LoggerFactory
import java.util.*
import no.nav.etterlatte.brev.model.Mottaker as BrevMottaker

class BrevService(
    private val db: BrevRepository,
    private val pdfGenerator: PdfGeneratorKlient,
    private val vedtakService: VedtakService,
    private val norg2Klient: Norg2Klient,
    private val grunnbeloepKlient: GrunnbeloepKlient,
    private val sendToRapid: (String) -> Unit
) {
    private val logger = LoggerFactory.getLogger(BrevService::class.java)

    fun hentAlleBrev(behandlingId: String): List<Brev> = db.hentBrevForBehandling(behandlingId)

    fun hentBrevInnhold(id: BrevID): BrevInnhold = db.hentBrevInnhold(id)

    fun slettBrev(id: BrevID): Boolean {
        val brev = db.hentBrev(id)

        if (brev.status != Status.OPPRETTET && brev.status != Status.OPPDATERT) {
            throw RuntimeException("Brev er ferdigstilt og kan ikke slettes!")
        } else if (brev.erVedtaksbrev) {
            throw RuntimeException("Vedtaksbrev kan ikke slettes!")
        }

        return db.slett(id)
    }

    suspend fun opprett(mottaker: Mottaker, mal: Mal, enhet: String): BrevInnhold {
        val brevMottaker = when {
            // todo: hent adresse fra pdl
            mottaker.foedselsnummer != null -> BrevMottaker("Fornavn", "Fødselsnummer", "Veien 22", "0000", "Oslo")
            // todo: hent adresse fra pdl og skriv om mottaker for brev til å støtte organisasjoner.
            mottaker.orgnummer != null -> BrevMottaker("Fornavn", "Organisasjon", "Veien 22", "0000", "Oslo")
            mottaker.adresse != null -> BrevMottaker.fraAdresse(adresse = mottaker.adresse!!)
            else -> throw Exception("Ingen brevmottaker spesifisert")
        }

        val avsender = hentAvsender(enhet)

        val request = AnnetBrevRequest(mal, Spraak.NB, avsender, brevMottaker)

        return BrevInnhold(mal.tittel, Spraak.NB.toString(), pdfGenerator.genererPdf(request))
    }

    fun lagreAnnetBrev(behandlingId: String, mottaker: Mottaker, brevInnhold: BrevInnhold): Brev {
        return db.opprettBrev(UlagretBrev(behandlingId, brevInnhold.mal, mottaker, false, brevInnhold.data))
    }

    suspend fun oppdaterVedtaksbrev(behandlingId: String): BrevID {
        val vedtak = vedtakService.hentVedtak(behandlingId)
        val nyttBrev = opprettNyttBrevFraVedtak(vedtak, behandlingId)

        val vedtaksbrev = db.hentBrevForBehandling(behandlingId)
            .find { it.erVedtaksbrev }

        return if (vedtaksbrev == null) {
            db.opprettBrev(nyttBrev).id
        } else {
            db.oppdaterBrev(vedtaksbrev.id, nyttBrev)
            vedtaksbrev.id
        }
    }

    fun ferdigstillAttestertVedtak(vedtak: Vedtak) = db.hentBrevForBehandling(vedtak.behandling.id.toString())
        .find { it.erVedtaksbrev }
        .let { brev ->
            require(brev != null) {
                "Klarte ikke finne vedtaksbrev for attestert vedtak med vedtakId = ${vedtak.vedtakId}"
            }
            ferdigstill(brev, vedtak)
        }

    fun ferdigstillBrev(id: BrevID): Brev {
        val brev: Brev = db.hentBrev(id)

        if (brev.erVedtaksbrev) {
            throw RuntimeException("Vedtaksbrev skal ikke ferdigstilles manuelt!")
        }

        // todo: vedtak må byttes ut med grunnlag for å fungere på behandlinger også.
        val vedtak = vedtakService.hentVedtak(brev.behandlingId)

        return ferdigstill(brev, vedtak)
    }

    private fun ferdigstill(brev: Brev, vedtak: Vedtak): Brev {
        sendToRapid(opprettDistribusjonsmelding(brev, vedtak))
        db.oppdaterStatus(brev.id, Status.FERDIGSTILT)

        return brev
    }

    private suspend fun opprettNyttBrevFraVedtak(vedtak: Vedtak, behandlingId: String? = null): UlagretBrev {
        val avsender = hentAvsender(vedtak.vedtakFattet!!.ansvarligEnhet)
        val grunnbeloep = grunnbeloepKlient.hentGrunnbeloep()

        val brevRequest = when (vedtak.type) {
            VedtakType.INNVILGELSE -> InnvilgetBrevRequest.fraVedtak(vedtak, avsender, grunnbeloep)
            VedtakType.AVSLAG -> AvslagBrevRequest.fraVedtak(vedtak, avsender)
            else -> throw Exception("Vedtakstype er ikke støttet: ${vedtak.type}")
        }

        val pdf = pdfGenerator.genererPdf(brevRequest)

        logger.info("Generert brev for vedtak (vedtakId=${vedtak.vedtakId}) med størrelse: ${pdf.size}")

        val tittel = "Vedtak om ${vedtak.type.name.lowercase()}"
        val mottaker = Mottaker(Foedselsnummer.of(vedtak.finnBarn().fnr))

        return UlagretBrev(
            behandlingId = behandlingId ?: vedtak.behandling.id.toString(),
            tittel,
            mottaker,
            true,
            pdf
        )
    }

    private fun opprettDistribusjonsmelding(brev: Brev, vedtak: Vedtak): String =
        DistribusjonMelding(
            behandlingId = brev.behandlingId,
            distribusjonType = if (brev.erVedtaksbrev) DistribusjonsType.VEDTAK else DistribusjonsType.VIKTIG,
            brevId = brev.id,
            mottaker = brev.mottaker,
            bruker = Bruker(vedtak.finnBarn().fnr),
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

    private suspend fun hentAvsender(navEnhetNr: String): Avsender {
        val enhet = norg2Klient.hentEnhet(navEnhetNr)

        // TODO: Hva gjør vi dersom postadresse mangler?
        val postadresse = enhet.kontaktinfo?.postadresse

        val kontor = enhet.navn ?: "NAV"
        val adresse = when (postadresse?.type) {
            "stedsadresse" -> postadresse.let { "${it.gatenavn} ${it.husnummer}${it.husbokstav ?: ""}" }
            "postboksadresse" -> "Postboks ${postadresse.postboksnummer} ${postadresse.postboksanlegg ?: ""}".trim()
            else -> throw Exception("Ukjent type postadresse ${postadresse?.type}")
        }
        val postnr = postadresse.let { "${it.postnummer} ${it.poststed}" }
        val telefon = enhet.kontaktinfo?.telefonnummer ?: ""

        return Avsender(
            kontor = kontor,
            adresse = adresse,
            postnummer = postnr,
            telefon = telefon
        )
    }
}