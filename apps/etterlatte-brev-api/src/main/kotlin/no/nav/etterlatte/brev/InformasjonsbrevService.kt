package no.nav.etterlatte.brev

import no.nav.etterlatte.brev.InformasjonsbrevUtsendingStatus.AVVENTER_AVKLARING
import no.nav.etterlatte.brev.InformasjonsbrevUtsendingStatus.DISTRIBUSJON_PAABEGYNT
import no.nav.etterlatte.brev.InformasjonsbrevUtsendingStatus.FERDIG
import no.nav.etterlatte.brev.InformasjonsbrevUtsendingStatus.FERDIGSTILT
import no.nav.etterlatte.brev.InformasjonsbrevUtsendingStatus.JOURNALFOERING_PAABEGYNT
import no.nav.etterlatte.brev.InformasjonsbrevUtsendingStatus.JOURNALFOERT
import no.nav.etterlatte.brev.InformasjonsbrevUtsendingStatus.OPPRETTET
import no.nav.etterlatte.brev.InformasjonsbrevUtsendingStatus.PDF_LAGRET
import no.nav.etterlatte.brev.adresse.AdresseService
import no.nav.etterlatte.brev.behandling.PersonerISak
import no.nav.etterlatte.brev.brevbaker.BrevbakerRequest
import no.nav.etterlatte.brev.brevbaker.BrevbakerService
import no.nav.etterlatte.brev.brevbaker.SoekerOgEventuellVerge
import no.nav.etterlatte.brev.db.BrevRepository
import no.nav.etterlatte.brev.db.InformasjonsbrevRepository
import no.nav.etterlatte.brev.distribusjon.Brevdistribuerer
import no.nav.etterlatte.brev.distribusjon.DistribusjonsType
import no.nav.etterlatte.brev.model.BrevInnhold
import no.nav.etterlatte.brev.model.BrevProsessType
import no.nav.etterlatte.brev.model.OpprettNyttBrev
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.feilhaandtering.ForespoerselException
import no.nav.etterlatte.libs.common.feilhaandtering.IkkeFunnetException
import no.nav.etterlatte.libs.common.feilhaandtering.IkkeTillattException
import no.nav.etterlatte.libs.common.feilhaandtering.UgyldigForespoerselException
import no.nav.etterlatte.libs.common.sak.SakId
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.ktor.token.BrukerTokenInfo
import no.nav.etterlatte.libs.ktor.token.Claims
import no.nav.etterlatte.libs.ktor.token.Fagsaksystem
import no.nav.etterlatte.libs.ktor.token.Systembruker
import no.nav.etterlatte.libs.ktor.token.getClaimAsString
import org.slf4j.LoggerFactory
import java.util.UUID

data class BrevBatchKonfigurasjon(
    val aktivert: Boolean = false,
    val tillattBehandling: String = "",
) {
    fun kontrollerTilgang(
        bruker: BrukerTokenInfo,
        skriv: Boolean,
    ) {
        val klient = bruker.getClaims()?.getClaimAsString(Claims.azp_name)
        if (bruker !is Systembruker || tillattBehandling.isBlank() ||
            klient != tillattBehandling || bruker.ident() != klient
        ) {
            throw IkkeTillattException("BREV_BATCH_IKKE_TILLATT", "Klienten har ikke tilgang til informasjonsbrevutsendinger.")
        }
        if (skriv && !aktivert) throw ForespoerselException(503, "BREV_BATCH_DEAKTIVERT", "Nye utsendinger er deaktivert.")
    }
}

class InformasjonsbrevService(
    private val repository: InformasjonsbrevRepository,
    private val brevRepository: BrevRepository,
    private val adresseService: AdresseService,
    private val brevbaker: BrevbakerService,
    private val journalfoerer: JournalfoerBrevService,
    private val distribuerer: Brevdistribuerer,
    private val konfigurasjon: BrevBatchKonfigurasjon,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    suspend fun opprett(
        sakId: SakId,
        id: UUID,
        request: StrukturertInformasjonsbrevRequest,
        bruker: BrukerTokenInfo,
    ): InformasjonsbrevUtsending {
        konfigurasjon.kontrollerTilgang(bruker, true)
        if (request.sak.id != sakId) throw UgyldigForespoerselException("SAKID_AVVIK", "Sak i request samsvarer ikke med stien.")
        repository.hent(id)?.let {
            return repository.kontroller(it, sakId, bruker.ident(), InformasjonsbrevRepository.fingerprint(request)).resultat.synligStatus()
        }
        validerInformasjonsbrev(request.tittel, request.innhold)
        val mottakere =
            adresseService.hentMottakere(
                request.sak.sakType,
                PersonerISak(
                    request.innsender,
                    request.soeker,
                    request.avdoede,
                    request.verge,
                    gjenlevende = if (request.sak.sakType == SakType.BARNEPENSJON) request.gjenlevende else emptyList(),
                ),
                sakId,
                bruker,
                krevAvklartMottaker = true,
            )
        if (mottakere.size !in 1..3 || mottakere.any { it.erGyldig().isNotEmpty() } ||
            mottakere.any { it.foedselsnummer?.value.isNullOrBlank() && it.orgnummer.isNullOrBlank() } ||
            mottakere.map { it.foedselsnummer?.value ?: it.orgnummer }.distinct().size != mottakere.size
        ) {
            throw UgyldigForespoerselException("UAVKLART_MOTTAKER", "Informasjonsbrevet mangler gyldige, entydige mottakere.")
        }
        val brev =
            OpprettNyttBrev(
                sakId = sakId,
                behandlingId = null,
                prosessType = BrevProsessType.REDIGERBAR,
                soekerFnr = request.soeker.fnr.value,
                mottakere = mottakere,
                opprettet = Tidspunkt.now(),
                innhold = BrevInnhold(request.tittel, request.spraak, request.innhold),
                innholdVedlegg = emptyList(),
                brevtype = Brevtype.INFORMASJON,
                brevkoder = Brevkoder.TOMT_INFORMASJONSBREV,
            )
        return repository.opprett(id, request, brev, bruker).resultat.synligStatus()
    }

    fun hent(
        sakId: SakId,
        id: UUID,
        bruker: BrukerTokenInfo,
    ): InformasjonsbrevUtsending? {
        konfigurasjon.kontrollerTilgang(bruker, false)
        return repository.hent(id)?.let { repository.kontroller(it, sakId, bruker.ident()).resultat.synligStatus() }
    }

    suspend fun send(
        sakId: SakId,
        id: UUID,
        bruker: BrukerTokenInfo,
    ): InformasjonsbrevUtsending {
        konfigurasjon.kontrollerTilgang(bruker, true)
        val lagret = repository.hent(id) ?: throw IkkeFunnetException("UTSENDING_IKKE_FUNNET", "Fant ikke utsendingen.")
        repository.kontroller(lagret, sakId, bruker.ident())
        val eier = UUID.randomUUID()
        if (!repository.overta(id, eier)) return requireNotNull(hent(sakId, id, bruker))
        var steg = requireNotNull(repository.hent(id)).resultat.status
        try {
            if (steg == OPPRETTET) {
                val request = lagret.request
                val avsender =
                    adresseService.hentAvsender(
                        AvsenderRequest(saksbehandlerIdent = Fagsaksystem.EY.navn, sakenhet = request.sak.enhet),
                        bruker,
                    )
                val pdf =
                    brevbaker.genererPdf(
                        lagret.resultat.brevId,
                        BrevbakerRequest.fra(
                            Brevkoder.TOMT_INFORMASJONSBREV.ferdigstilling,
                            ManueltBrevMedTittelData(request.innhold.elements, ManueltBrevMedTittelDataData(request.tittel)),
                            avsender,
                            SoekerOgEventuellVerge(request.soeker, request.verge),
                            sakId,
                            request.spraak,
                            request.sak.sakType,
                        ),
                    )
                repository.lagrePdf(id, eier, pdf)
                steg = PDF_LAGRET
            }
            if (steg == PDF_LAGRET) {
                repository.ferdigstill(id, eier, bruker)
                steg = FERDIGSTILT
            }
            if (steg == FERDIGSTILT) {
                if (!repository.steg(id, eier, JOURNALFOERING_PAABEGYNT)) return requireNotNull(hent(sakId, id, bruker))
                steg = JOURNALFOERING_PAABEGYNT
                journalfoerer.journalfoer(lagret.resultat.brevId, bruker, eier)
                check(brevRepository.hentBrev(lagret.resultat.brevId).mottakere.all { !it.journalpostId.isNullOrBlank() })
                if (!repository.steg(id, eier, JOURNALFOERT)) return requireNotNull(hent(sakId, id, bruker))
                steg = JOURNALFOERT
            }
            if (steg == JOURNALFOERT) {
                if (!repository.steg(id, eier, DISTRIBUSJON_PAABEGYNT)) return requireNotNull(hent(sakId, id, bruker))
                steg = DISTRIBUSJON_PAABEGYNT
                distribuerer.distribuer(lagret.resultat.brevId, DistribusjonsType.ANNET, bruker, eier)
                check(
                    brevRepository.hentBrev(lagret.resultat.brevId).mottakere.all {
                        !it.journalpostId.isNullOrBlank() && !it.bestillingId.isNullOrBlank()
                    },
                )
                repository.steg(id, eier, FERDIG, frigi = true)
            }
        } catch (_: Exception) {
            val uavklart = steg in setOf(JOURNALFOERING_PAABEGYNT, DISTRIBUSJON_PAABEGYNT)
            val kode = if (uavklart) "UAVKLART_$steg" else "MIDLERTIDIG_$steg"
            repository.steg(id, eier, if (uavklart) AVVENTER_AVKLARING else steg, kode, frigi = !uavklart)
            logger.warn("Informasjonsbrevutsending={} steg={} feilkode={}", id, steg, kode)
        }
        return requireNotNull(hent(sakId, id, bruker))
    }

    private fun InformasjonsbrevUtsending.synligStatus(): InformasjonsbrevUtsending =
        if (status in setOf(JOURNALFOERING_PAABEGYNT, DISTRIBUSJON_PAABEGYNT)) {
            copy(status = AVVENTER_AVKLARING, feilkode = "UAVKLART_$status")
        } else {
            this
        }
}
