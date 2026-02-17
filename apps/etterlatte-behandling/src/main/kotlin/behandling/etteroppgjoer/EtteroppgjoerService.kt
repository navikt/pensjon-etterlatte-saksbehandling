package no.nav.etterlatte.behandling.etteroppgjoer

import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.behandling.BehandlingService
import no.nav.etterlatte.behandling.domain.Behandling
import no.nav.etterlatte.behandling.etteroppgjoer.forbehandling.EtteroppgjoerForbehandling
import no.nav.etterlatte.behandling.etteroppgjoer.forbehandling.FantIkkEtteroppgjoer
import no.nav.etterlatte.behandling.etteroppgjoer.oppgave.EtteroppgjoerOppgaveService
import no.nav.etterlatte.behandling.etteroppgjoer.sigrun.SigrunKlient
import no.nav.etterlatte.behandling.klienter.BeregningKlient
import no.nav.etterlatte.behandling.klienter.VedtakKlient
import no.nav.etterlatte.inTransaction
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.behandling.UtlandstilknytningType
import no.nav.etterlatte.libs.common.behandling.etteroppgjoer.EtteroppgjoerFilter
import no.nav.etterlatte.libs.common.beregning.EtteroppgjoerResultatType
import no.nav.etterlatte.libs.common.feilhaandtering.IkkeTillattException
import no.nav.etterlatte.libs.common.feilhaandtering.InternfeilException
import no.nav.etterlatte.libs.common.feilhaandtering.UgyldigForespoerselException
import no.nav.etterlatte.libs.common.feilhaandtering.krev
import no.nav.etterlatte.libs.common.feilhaandtering.krevIkkeNull
import no.nav.etterlatte.libs.common.sak.Sak
import no.nav.etterlatte.libs.common.sak.SakId
import no.nav.etterlatte.libs.common.toJson
import no.nav.etterlatte.libs.common.vedtak.VedtakType
import no.nav.etterlatte.libs.ktor.token.BrukerTokenInfo
import no.nav.etterlatte.libs.ktor.token.HardkodaSystembruker
import no.nav.etterlatte.logger
import java.time.LocalDate
import java.time.Year
import java.time.YearMonth
import java.util.UUID

enum class EtteroppgjoerSvarfrist(
    val value: String,
) {
    ETT_MINUTT("1 minute"),
    FEM_MINUTT("5 minutes"),
    EN_MND("1 month"),
}

// TODO: finne en bedre plass for denne, evnt i config
const val ETTEROPPGJOER_AAR = 2024

class EtteroppgjoerService(
    val dao: EtteroppgjoerDao,
    val vedtakKlient: VedtakKlient,
    val behandlingService: BehandlingService,
    val beregningKlient: BeregningKlient,
    val etteroppgjoerOppgaveService: EtteroppgjoerOppgaveService,
    val sigrunKlient: SigrunKlient,
) {
    fun hentEtteroppgjoerMedSvarfristUtloept(svarfrist: EtteroppgjoerSvarfrist): List<Etteroppgjoer>? =
        dao.hentEtteroppgjoerMedSvarfristUtloept(svarfrist)

    fun hentEtteroppgjoerForInntektsaar(
        sakId: SakId,
        inntektsaar: Int,
    ): Etteroppgjoer = dao.hentEtteroppgjoerForInntektsaar(sakId, inntektsaar) ?: throw FantIkkEtteroppgjoer(sakId, inntektsaar)

    fun hentEtteroppgjoerForSak(sakId: SakId): List<Etteroppgjoer> = dao.hentEtteroppgjoerForSak(sakId)

    fun hentEtteroppgjoerSakerIBulk(
        inntektsaar: Int,
        antall: Int,
        etteroppgjoerFilter: EtteroppgjoerFilter,
        status: EtteroppgjoerStatus,
        spesifikkeSaker: List<SakId> = emptyList(),
        ekskluderteSaker: List<SakId> = emptyList(),
        spesifikkeEnheter: List<String> = emptyList(),
    ): List<SakId> =
        dao.hentEtteroppgjoerSakerIBulk(
            inntektsaar = inntektsaar,
            antall = antall,
            etteroppgjoerFilter = etteroppgjoerFilter,
            status = status,
            spesifikkeSaker = spesifikkeSaker,
            ekskluderteSaker = ekskluderteSaker,
            spesifikkeEnheter = spesifikkeEnheter,
        )

    fun oppdaterEtteroppgjoerStatus(
        sakId: SakId,
        inntektsaar: Int,
        status: EtteroppgjoerStatus,
    ) {
        dao.oppdaterEtteroppgjoerStatus(sakId, inntektsaar, status)
    }

    fun hentEtteroppgjoerSomVenterPaaSkatteoppgjoer(antall: Int): List<Etteroppgjoer> =
        dao.hentEtteroppgjoerSomVenterPaaSkatteoppgjoer(antall)

    fun oppdaterEtteroppgjoerEtterFerdigstiltForbehandling(forbehandling: EtteroppgjoerForbehandling) {
        if (!forbehandling.erFerdigstilt()) {
            throw IkkeTillattException("FORBEHADNLING_IKKE_FERDIGSTILT", "Forbehandlingen er ikke ferdigstilt")
        }

        val nyEtteroppgjoerStatus = finnStatusForForbehandling(forbehandling)

        oppdaterEtteroppgjoerStatus(forbehandling.sak.id, forbehandling.aar, nyEtteroppgjoerStatus)
        oppdaterSisteFerdigstiltForbehandlingId(forbehandling.sak.id, forbehandling.aar, forbehandling.id)
    }

    fun oppdaterSisteFerdigstiltForbehandlingId(
        sakId: SakId,
        inntektsaar: Int,
        forbehandlingId: UUID,
    ) {
        dao.oppdaterFerdigstiltForbehandlingId(sakId, inntektsaar, forbehandlingId)
    }

    fun haandterSkatteoppgjoerMottatt(
        hendelse: SkatteoppgjoerHendelse,
        etteroppgjoer: Etteroppgjoer,
        sak: Sak,
    ) {
        // hente etteroppgjoer
        krev(etteroppgjoer.kanOppdateresMedSkatteoppgjoerMottatt()) {
            "Mottok skatteoppgjørhendelse for sakId=${sak.id}, men etteroppgjør har status ${etteroppgjoer.status}. " +
                "Se sikkerlogg for mer informasjon."
        }

        // TODO: fjerne hvis ikke et problem
        if (etteroppgjoer.mottattSkatteoppgjoer()) {
            logger.info(
                "Ny hendelse (type=${hendelse.hendelsetype}) mottatt etter at status allerede er " +
                    "MOTTATT_SKATTEOPPGJOER. Sekvensnummer=${hendelse.sekvensnummer}, sakId=${sak.id}.",
            )
        }

        oppdaterEtteroppgjoerStatus(
            sak.id,
            etteroppgjoer.inntektsaar,
            EtteroppgjoerStatus.MOTTATT_SKATTEOPPGJOER,
        )

        etteroppgjoerOppgaveService.opprettOppgaveForOpprettForbehandling(
            sakId = sak.id,
            inntektsAar = etteroppgjoer.inntektsaar,
        )
    }

    // TODO: må vi ha flere måter å opprette etteroppgjør på?
    fun opprettNyttEtteroppgjoer(
        sakId: SakId,
        inntektsaar: Int,
    ): Etteroppgjoer {
        logger.info(
            "Forsøker å opprette/oppdatere etteroppgjør for sakId=$sakId og inntektsaar=$inntektsaar",
        )
        val eksisterendeEtteroppgjoer = dao.hentEtteroppgjoerForInntektsaar(sakId, inntektsaar)
        if (eksisterendeEtteroppgjoer != null && !eksisterendeEtteroppgjoer.kanOppdateresMedSkatteoppgjoerMottatt()) {
            logger.info(
                "Vi har allerede et opprettet etteroppgjør for sakId=$sakId og inntektsaar=$inntektsaar, med status=" +
                    "${eksisterendeEtteroppgjoer.status}. Vi oppdaterer derfor ikke noen felter på dette etteroppgjøret.",
            )
        }

        val attesterteVedtak =
            runBlocking {
                vedtakKlient
                    .hentIverksatteVedtak(sakId, brukerTokenInfo = HardkodaSystembruker.etteroppgjoer)
                    .sortedByDescending { it.datoAttestert }
            }

        val harVedtakAvTypeOpphoer = attesterteVedtak.any { it.vedtakType == VedtakType.OPPHOER }

        val sisteIverksatteVedtak =
            attesterteVedtak
                .firstOrNull { it.vedtakType != VedtakType.OPPHOER }
                ?: throw InternfeilException("Fant ikke siste iverksatte vedtak i sak=$sakId")

        val sisteIverksatteBehandling =
            krevIkkeNull(behandlingService.hentBehandling(sisteIverksatteVedtak.behandlingId)) {
                "Siste iverksatte vedtak (id=${sisteIverksatteVedtak.id} peker på en behandling " +
                    "med id=${sisteIverksatteVedtak.behandlingId} som ikke finnes"
            }

        val oppdatertEtteroppgjoer =
            runBlocking {
                etteroppgjoer(
                    sakId,
                    inntektsaar,
                    sisteIverksatteBehandling,
                    eksisterendeEtteroppgjoer?.status ?: EtteroppgjoerStatus.MOTTATT_SKATTEOPPGJOER,
                    harVedtakAvTypeOpphoer || sisteIverksatteBehandling.opphoerFraOgMed != null,
                    eksisterendeEtteroppgjoer?.sisteFerdigstilteForbehandling,
                )
            }

        if (eksisterendeEtteroppgjoer != null && oppdatertEtteroppgjoer != eksisterendeEtteroppgjoer) {
            logger.info(
                "Endrer etteroppgjør for sakId=$sakId og inntektsaar=$inntektsaar. Endring: " +
                    mapOf("foer" to eksisterendeEtteroppgjoer, "etter" to oppdatertEtteroppgjoer).toJson(),
            )
        }
        dao.lagreEtteroppgjoer(oppdatertEtteroppgjoer)

        return oppdatertEtteroppgjoer
    }

    fun finnOgOpprettManglendeEtteroppgjoer(
        sakId: SakId,
        brukerTokenInfo: BrukerTokenInfo,
    ) {
        val innvilgedeAar = finnInnvilgedeAarForSak(sakId, brukerTokenInfo)

        val etteroppgjoer = dao.hentEtteroppgjoerForSak(sakId).map { it.inntektsaar }

        val aarUtenEtteroppgjoer = innvilgedeAar.toSet() - etteroppgjoer.toSet()
        if (aarUtenEtteroppgjoer.isEmpty()) {
            logger.info(
                "Sak med id $sakId har allerede etteroppgjør for alle inntektsår med innvilget periode, ingen etteroppgjør trenger å opprettes.",
            )
            return
        }

        aarUtenEtteroppgjoer.forEach {
            opprettNyttEtteroppgjoer(sakId, it)
        }
    }

    // TODO: må vi ha flere måter å opprette etteroppgjør på?
    suspend fun opprettEtteroppgjoerVedIverksattFoerstegangsbehandling(
        behandling: Behandling,
        inntektsaar: Int,
    ): Etteroppgjoer {
        val sakId = behandling.sak.id

        // TODO: blir ikke dette rett?
        val harOpphoer = behandling.opphoerFraOgMed != null

        logger.info(
            """
            Forsøker å opprette etteroppgjør for sakId=$sakId,
            behandling=$behandling og inntektsaar=$inntektsaar
            """.trimIndent(),
        )

        val status =
            try {
                sigrunKlient.hentPensjonsgivendeInntekt(behandling.sak.ident, inntektsaar)
                EtteroppgjoerStatus.MOTTATT_SKATTEOPPGJOER
            } catch (e: Exception) {
                logger.error(
                    "Vi har opprettet et etteroppgjør for $inntektsaar i sakId=${behandling.sak.id}, " +
                        "men vi klarte ikke hente skatteoppgjøret, vi antar at dette er fordi skatteoppgjøret ikke er " +
                        "tilgjengelig, hvis annen feil må saken ryddes opp manuelt",
                    e,
                )
                EtteroppgjoerStatus.VENTER_PAA_SKATTEOPPGJOER
            }

        return etteroppgjoer(sakId, inntektsaar, behandling, status, harOpphoer)
            .also { dao.lagreEtteroppgjoer(it) }
    }

    fun finnInnvilgedeAarForSak(
        sakId: SakId,
        brukerTokenInfo: BrukerTokenInfo,
    ): List<Int> {
        val innvilgedePerioder = runBlocking { vedtakKlient.hentInnvilgedePerioder(sakId, brukerTokenInfo) }

        if (innvilgedePerioder.isEmpty()) {
            throw UgyldigForespoerselException(
                "MANGLER_INNVILGET_PERIODE",
                "Saken har ingen innvilget periode.",
            )
        }

        val innvilgedeAar =
            innvilgedePerioder
                .flatMap { periodeDto ->
                    val periode = periodeDto.periode
                    val fomYear = periode.fom.year
                    val tomYear = (periode.tom ?: YearMonth.now().minusYears(1)).year

                    (fomYear..tomYear).toList()
                }.filter { aar ->
                    aar >= 2024
                }.distinct()

        return innvilgedeAar
    }

    private fun finnStatusForForbehandling(forbehandling: EtteroppgjoerForbehandling): EtteroppgjoerStatus {
        if (forbehandling.skyldesOpphoerDoedsfallIEtteroppgjoersaar()) {
            return EtteroppgjoerStatus.FERDIGSTILT
        }

        return when (forbehandling.etteroppgjoerResultatType) {
            EtteroppgjoerResultatType.ETTERBETALING -> EtteroppgjoerStatus.VENTER_PAA_SVAR

            EtteroppgjoerResultatType.TILBAKEKREVING -> EtteroppgjoerStatus.VENTER_PAA_SVAR

            EtteroppgjoerResultatType.INGEN_ENDRING_MED_UTBETALING -> EtteroppgjoerStatus.FERDIGSTILT

            EtteroppgjoerResultatType.INGEN_ENDRING_UTEN_UTBETALING -> EtteroppgjoerStatus.FERDIGSTILT

            null -> throw InternfeilException(
                "Mangler etteroppgjoerResultatType for forbehandling ${forbehandling.id} og kan derfor ikke oppdatere Etteroppgjør status",
            )
        }
    }

    private suspend fun etteroppgjoer(
        sakId: SakId,
        inntektsaar: Int,
        sisteIverksatteBehandling: Behandling,
        etteroppgjoerStatus: EtteroppgjoerStatus,
        harVedtakAvTypeOpphoer: Boolean,
        sisteFerdigstilteForbehandling: UUID? = null,
    ): Etteroppgjoer {
        val etteroppgjoer =
            Etteroppgjoer(
                sakId = sakId,
                inntektsaar = inntektsaar,
                status = etteroppgjoerStatus,
                harSanksjon = utledSanksjoner(sisteIverksatteBehandling.id, inntektsaar),
                harInstitusjonsopphold = utledInstitusjonsopphold(sisteIverksatteBehandling.id, inntektsaar),
                harOpphoer = harVedtakAvTypeOpphoer || sisteIverksatteBehandling.opphoerFraOgMed !== null,
                harBosattUtland = sisteIverksatteBehandling.utlandstilknytning?.type == UtlandstilknytningType.BOSATT_UTLAND,
                harUtlandstilsnitt = sisteIverksatteBehandling.utlandstilknytning?.type == UtlandstilknytningType.UTLANDSTILSNITT,
                harAdressebeskyttelseEllerSkjermet =
                    sisteIverksatteBehandling.sak.adressebeskyttelse?.harAdressebeskyttelse() == true ||
                        sisteIverksatteBehandling.sak.erSkjermet == true,
                harAktivitetskrav =
                    utledAktivitetskrav(
                        sisteIverksatteBehandling.id,
                        sisteIverksatteBehandling.sak.sakType,
                        inntektsaar,
                    ),
                harOverstyrtBeregning = utledOverstyrtBeregning(sisteIverksatteBehandling.id),
                sisteFerdigstilteForbehandling = sisteFerdigstilteForbehandling,
            )
        return etteroppgjoer
    }

    private suspend fun utledOverstyrtBeregning(behandlingId: UUID): Boolean {
        val overstyrtBeregningsgrunnlag =
            beregningKlient.hentOverstyrtBeregning(behandlingId, HardkodaSystembruker.etteroppgjoer)
        return overstyrtBeregningsgrunnlag != null
    }

    private fun utledAktivitetskrav(
        behandlingId: UUID,
        sakType: SakType,
        inntektsaar: Int,
    ): Boolean {
        val aktivitetsKravDato = LocalDate.of(inntektsaar, 7, 1)
        val sisteDoedsdato = behandlingService.hentFoersteDoedsdato(behandlingId, sakType) ?: return false
        return sisteDoedsdato.isBefore(aktivitetsKravDato)
    }

    private suspend fun utledSanksjoner(
        behandlingId: UUID,
        inntektsaar: Int,
    ): Boolean {
        val sanksjoner =
            beregningKlient.hentSanksjoner(
                behandlingId,
                HardkodaSystembruker.etteroppgjoer,
            )

        return sanksjoner?.any { sanksjon ->
            sanksjon.fom.year <= inntektsaar && (sanksjon.tom?.year ?: inntektsaar) >= inntektsaar
        } == true
    }

    private suspend fun utledInstitusjonsopphold(
        behandlingId: UUID,
        inntektsaar: Int,
    ): Boolean {
        val beregningsGrunnlag =
            beregningKlient.hentBeregningsgrunnlag(behandlingId, HardkodaSystembruker.etteroppgjoer)
        return beregningsGrunnlag.institusjonsopphold.any {
            it.fom.year <= inntektsaar && (it.tom?.year ?: inntektsaar) >= inntektsaar
        }
    }
}
