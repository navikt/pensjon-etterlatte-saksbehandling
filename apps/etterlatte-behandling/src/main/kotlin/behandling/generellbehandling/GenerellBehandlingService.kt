package no.nav.etterlatte.behandling.generellbehandling

import no.nav.etterlatte.behandling.BehandlingService
import no.nav.etterlatte.behandling.domain.Foerstegangsbehandling
import no.nav.etterlatte.behandling.hendelse.HendelseDao
import no.nav.etterlatte.grunnlag.GrunnlagService
import no.nav.etterlatte.inTransaction
import no.nav.etterlatte.libs.common.feilhaandtering.GenerellIkkeFunnetException
import no.nav.etterlatte.libs.common.feilhaandtering.IkkeFunnetException
import no.nav.etterlatte.libs.common.feilhaandtering.IkkeTillattException
import no.nav.etterlatte.libs.common.feilhaandtering.UgyldigForespoerselException
import no.nav.etterlatte.libs.common.generellbehandling.Attestant
import no.nav.etterlatte.libs.common.generellbehandling.Behandler
import no.nav.etterlatte.libs.common.generellbehandling.DokumentMedSendtDato
import no.nav.etterlatte.libs.common.generellbehandling.GenerellBehandling
import no.nav.etterlatte.libs.common.generellbehandling.GenerellBehandlingHendelseType
import no.nav.etterlatte.libs.common.generellbehandling.Innhold
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Opplysningstype
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.oppgave.OppgaveIntern
import no.nav.etterlatte.libs.common.oppgave.OppgaveKilde
import no.nav.etterlatte.libs.common.oppgave.OppgaveType
import no.nav.etterlatte.libs.common.oppgave.Status
import no.nav.etterlatte.libs.common.person.Person
import no.nav.etterlatte.libs.common.sak.SakId
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.common.toJson
import no.nav.etterlatte.libs.ktor.token.BrukerTokenInfo
import no.nav.etterlatte.libs.ktor.token.Saksbehandler
import no.nav.etterlatte.oppgave.OppgaveService
import no.nav.etterlatte.saksbehandler.SaksbehandlerInfoDao
import org.slf4j.LoggerFactory
import java.time.LocalDate
import java.time.LocalTime
import java.time.temporal.ChronoUnit
import java.util.UUID

class DokumentManglerDatoException(
    message: String,
) : UgyldigForespoerselException(
        code = "DOKUMENT_MANGLER_DATO",
        detail = message,
    )

class DokumentErIkkeMarkertSomSendt(
    message: String,
) : UgyldigForespoerselException(
        code = "DOKUMENT_MÅ_VÆRE_SENDT",
        detail = message,
    )

class UgyldigLandkodeIsokode3(
    message: String,
) : UgyldigForespoerselException(
        code = "UGYLDIG_LANDKODE",
        detail = message,
    )

class ManglerLandkodeException(
    message: String,
) : UgyldigForespoerselException(
        code = "MANGLER_LANDKODE",
        detail = message,
    )

class ManglerRinanummerException(
    message: String,
) : UgyldigForespoerselException(
        code = "MANGLER_RINANUMMER",
        detail = message,
    )

class KanIkkeEndreGenerellBehandling(
    message: String,
) : IkkeTillattException("KAN_IKKE_ENDRE_GENERELL_BEHANDLING", message)

class FeilStatusGenerellBehandling(
    message: String,
) : IkkeTillattException("FEIL_STATUS_BEHANDLING", message)

class ManglerSaksbehandler(
    message: String,
) : IkkeTillattException("MANGLER_SAKSBEHANDLER", message)

class UgyldigAttesteringsForespoersel(
    message: String,
    code: String,
) : UgyldigForespoerselException(
        code = code,
        detail = message,
    )

data class Kommentar(
    val begrunnelse: String,
)

class GenerellBehandlingService(
    private val generellBehandlingDao: GenerellBehandlingDao,
    private val oppgaveService: OppgaveService,
    private val behandlingService: BehandlingService,
    private val grunnlagService: GrunnlagService,
    private val hendelseDao: HendelseDao,
    private val saksbehandlerInfoDao: SaksbehandlerInfoDao,
) {
    private val logger = LoggerFactory.getLogger(GenerellBehandlingService::class.java)

    fun opprettBehandling(
        generellBehandling: GenerellBehandling,
        saksbehandler: Saksbehandler?,
    ): GenerellBehandling {
        val opprettetbehandling = generellBehandlingDao.opprettGenerellbehandling(generellBehandling)
        val oppgaveForGenerellBehandling =
            oppgaveService.opprettOppgave(
                opprettetbehandling.id.toString(),
                opprettetbehandling.sakId,
                OppgaveKilde.GENERELL_BEHANDLING,
                OppgaveType.KRAVPAKKE_UTLAND,
                null,
            )
        tildelSaksbehandlerTilNyOppgaveHvisFinnes(oppgaveForGenerellBehandling, opprettetbehandling)
        opprettHendelse(GenerellBehandlingHendelseType.OPPRETTET, opprettetbehandling, saksbehandler)
        return opprettetbehandling
    }

    private fun tildelSaksbehandlerTilNyOppgaveHvisFinnes(
        oppgave: OppgaveIntern,
        generellBehandling: GenerellBehandling,
    ) {
        if (generellBehandling.tilknyttetBehandling !== null) {
            val kanskjeOppgaveMedSaksbehandler =
                oppgaveService
                    .hentOppgaverForReferanse(generellBehandling.tilknyttetBehandling!!.toString())
                    .filter { it.type == OppgaveType.FOERSTEGANGSBEHANDLING }
                    .maxByOrNull { it.opprettet }

            if (kanskjeOppgaveMedSaksbehandler != null) {
                val saksbehandlerIdent = oppgaveService.saksbehandlerSomFattetVedtak(kanskjeOppgaveMedSaksbehandler)
                if (saksbehandlerIdent != null) {
                    oppgaveService.tildelSaksbehandler(oppgave.id, saksbehandlerIdent)
                    logger.info(
                        "Opprettet generell behandling for utland for sak: ${generellBehandling.sakId} " +
                            "og behandling: ${generellBehandling.tilknyttetBehandling}. Gjelder oppgave: ${oppgave.id}",
                    )
                }
            }
        }
    }

    fun sendTilAttestering(
        generellBehandling: GenerellBehandling,
        saksbehandler: Saksbehandler,
    ) {
        sjekkStatusErTillattForAttestering(generellBehandling)

        when (generellBehandling.innhold) {
            is Innhold.KravpakkeUtland -> validerUtland(generellBehandling.innhold as Innhold.KravpakkeUtland)
            is Innhold.Annen -> throw NotImplementedError("Ikke implementert")
            null -> throw NotImplementedError("Ikke implementert")
        }

        val saksbehandlerNavn = saksbehandlerInfoDao.hentSaksbehandlerNavn(saksbehandler.ident)
        val merknad =
            "Attestering av ${generellBehandling.type.name}, behandlet av ${saksbehandlerNavn ?: saksbehandler.ident}."

        oppgaveService.tilAttestering(
            referanse = generellBehandling.id.toString(),
            type = OppgaveType.KRAVPAKKE_UTLAND,
            merknad = merknad,
        )

        oppdaterBehandling(
            generellBehandling.copy(
                status = GenerellBehandling.Status.FATTET,
                behandler = Behandler(saksbehandler.ident, Tidspunkt.now()),
            ),
        )
        opprettHendelse(GenerellBehandlingHendelseType.FATTET, generellBehandling, saksbehandler)
    }

    fun attester(
        generellbehandlingId: UUID,
        saksbehandler: Saksbehandler,
    ) {
        val hentetBehandling = hentBehandlingMedId(generellbehandlingId) ?: throw GenerellIkkeFunnetException()
        if (!hentetBehandling.erFattet()) {
            throw FeilStatusGenerellBehandling(
                "Behandlingen er ikke attester, hadde: ${hentetBehandling.status}. Id ${hentetBehandling.id}",
            )
        }

        if (hentetBehandling.behandler == null) {
            throw ManglerSaksbehandler("Behandlingen har ikke fått satt en saksbehandler i fattingen, id $generellbehandlingId")
        }

        verifiserRiktigSaksbehandler(saksbehandler, hentetBehandling)
        harRiktigTilstandIOppgave(generellbehandlingId)
        oppgaveService.ferdigstillOppgaveUnderBehandling(
            referanse = generellbehandlingId.toString(),
            type = OppgaveType.KRAVPAKKE_UTLAND,
            saksbehandler = saksbehandler,
        )
        oppdaterBehandling(
            hentetBehandling.copy(
                status = GenerellBehandling.Status.ATTESTERT,
                attestant = Attestant(saksbehandler.ident, Tidspunkt.now()),
            ),
        )

        opprettHendelse(GenerellBehandlingHendelseType.ATTESTERT, hentetBehandling, saksbehandler)
        oppgaveService.opprettOppgave(
            generellbehandlingId.toString(),
            hentetBehandling.sakId,
            OppgaveKilde.GENERELL_BEHANDLING,
            OppgaveType.OPPFOELGING,
            "Sluttbehandling - VO utland",
            Tidspunkt.ofNorskTidssone(LocalDate.now().plus(3, ChronoUnit.MONTHS), LocalTime.NOON),
        )
    }

    private fun verifiserRiktigSaksbehandler(
        saksbehandler: Saksbehandler,
        hentetBehandling: GenerellBehandling,
    ) {
        if (saksbehandler.ident == hentetBehandling.behandler?.saksbehandler) {
            throw UgyldigAttesteringsForespoersel(
                "Samme saksbehandler kan ikke attestere og behandle behandlingen",
                "ATTESTERING_SAMME_SAKSBEHANDLER",
            )
        }
    }

    private fun harRiktigTilstandIOppgave(generellbehandlingId: UUID) {
        val oppgaverForReferanse = oppgaveService.hentOppgaverForReferanse(generellbehandlingId.toString())
        if (oppgaverForReferanse.isEmpty()) {
            throw UgyldigAttesteringsForespoersel(
                "Fant ingen oppgaver for referanse $generellbehandlingId",
                "ATTESTERING_INGEN_OPPGAVER_FUNNET",
            )
        }

        oppgaverForReferanse.singleOrNull { it.status == Status.ATTESTERING }
            ?: throw UgyldigAttesteringsForespoersel(
                code = "ATTESTERING_INGEN_FERDIGSTILTE_OPPGAVER_AV_RIKTIG_TYPE",
                message = "Fant ingen oppgave med status ${Status.ATTESTERING} for referanse: $generellbehandlingId",
            )
    }

    fun underkjenn(
        generellbehandlingId: UUID,
        saksbehandler: Saksbehandler,
        kommentar: Kommentar,
        sakId: SakId,
    ) {
        val behandling = hentBehandlingMedId(generellbehandlingId) ?: throw GenerellIkkeFunnetException()
        if (!behandling.erFattet()) {
            throw FeilStatusGenerellBehandling("Behandlingen er ikke attester, hadde: ${behandling.status}. Id ${behandling.id}")
        }

        if (sakId != behandling.sakId) {
            throw SakParameterStemmerIkkeException(
                "Behandlingen med id $generellbehandlingId" +
                    " tilhører ikke sak $sakId, som ble sendt inn.",
            )
        }

        oppgaveService.tilUnderkjent(
            referanse = behandling.id.toString(),
            type = OppgaveType.KRAVPAKKE_UTLAND,
            merknad = kommentar.begrunnelse,
        )

        oppdaterBehandling(
            behandling.copy(
                status = GenerellBehandling.Status.RETURNERT,
                behandler = null,
                returnertKommenar = kommentar.begrunnelse,
            ),
        )
        opprettHendelse(
            GenerellBehandlingHendelseType.UNDERKJENT,
            behandling,
            saksbehandler,
            kommentar.begrunnelse,
        )
    }

    private fun opprettHendelse(
        hendelseType: GenerellBehandlingHendelseType,
        behandling: GenerellBehandling,
        saksbehandler: Saksbehandler?,
        begrunnelse: String? = null,
    ) {
        hendelseDao.generellBehandlingHendelse(
            behandlingId = behandling.id,
            sakId = behandling.sakId,
            hendelse = hendelseType,
            inntruffet = Tidspunkt.now(),
            saksbehandler = saksbehandler?.ident,
            begrunnelse = begrunnelse,
        )
    }

    private fun validerUtland(innhold: Innhold.KravpakkeUtland) {
        if (innhold.landIsoKode.isEmpty()) {
            throw ManglerLandkodeException("Mangler landkode")
        }
        if (innhold.landIsoKode.any { it.length != 3 }) {
            throw UgyldigLandkodeIsokode3("Landkoden er feil ${innhold.landIsoKode.toJson()}")
        }
        if (innhold.rinanummer.isEmpty()) {
            throw ManglerRinanummerException("Må ha rinanummer")
        }

        innhold.dokumenter.forEach { doc -> validerMaaHaDatoOgVaereMarkertSomSendt(doc) }
    }

    private fun validerMaaHaDatoOgVaereMarkertSomSendt(dokumentMedSendtDato: DokumentMedSendtDato) {
        if (!dokumentMedSendtDato.sendt) {
            throw DokumentErIkkeMarkertSomSendt("Dokument ${dokumentMedSendtDato.dokumenttype} er ikke markert som sendt ")
        }
        dokumentMedSendtDato.dato
            ?: throw DokumentManglerDatoException("Dokument ${dokumentMedSendtDato.dokumenttype} er markert som sendt men mangler dato")
    }

    fun avbrytBehandling(
        id: UUID,
        sakId: SakId,
        saksbehandler: BrukerTokenInfo,
    ) {
        val generellBehandling = generellBehandlingDao.hentGenerellBehandlingMedId(id)
        if (generellBehandling?.sakId != sakId) {
            throw SakParameterStemmerIkkeException(
                "Generell behandling med id=${generellBehandling?.id} tilhører ikke sak med id=$sakId, " +
                    "som ble sendt inn.",
            )
        }
        finnesOgErRedigerbar(generellBehandling)
        generellBehandlingDao.oppdaterGenerellBehandling(generellBehandling.copy(status = GenerellBehandling.Status.AVBRUTT))
        oppgaveService.avbrytOppgaveUnderBehandling(generellBehandling.id.toString(), saksbehandler)
    }

    fun lagreNyeOpplysninger(
        generellBehandling: GenerellBehandling,
        sakId: SakId,
    ): GenerellBehandling {
        val lagretBehandling = generellBehandlingDao.hentGenerellBehandlingMedId(generellBehandling.id)
        if (generellBehandling.sakId != sakId) {
            throw SakParameterStemmerIkkeException(
                "Generell behandling med id=${generellBehandling.id} tilhører ikke sak med id=$sakId, " +
                    "som ble sendt inn.",
            )
        }
        finnesOgErRedigerbar(lagretBehandling)
        return this.oppdaterBehandling(generellBehandling)
    }

    private fun finnesOgErRedigerbar(generellBehandling: GenerellBehandling?) {
        if (generellBehandling == null) {
            throw IkkeFunnetException("GENERELL_BEHANDLING_FINNES_IKKE", "Behandlingen finnes ikke.")
        }
        if (!generellBehandling.kanEndres()) {
            throw KanIkkeEndreGenerellBehandling(
                "Behandling kan ikke ha status ${generellBehandling.status} " +
                    "hvis du skal endre den.",
            )
        }
    }

    private fun oppdaterBehandling(generellBehandling: GenerellBehandling): GenerellBehandling =
        generellBehandlingDao.oppdaterGenerellBehandling(generellBehandling)

    fun hentBehandlingMedId(id: UUID): GenerellBehandling? = generellBehandlingDao.hentGenerellBehandlingMedId(id)

    fun hentBehandlingerForSak(sakId: SakId): List<GenerellBehandling> = generellBehandlingDao.hentGenerellBehandlingForSak(sakId)

    private fun hentGenerellbehandlingSinTilknyttetedeBehandling(tilknyttetBehandlingId: UUID): GenerellBehandling? =
        generellBehandlingDao.hentBehandlingForTilknyttetBehandling(tilknyttetBehandlingId)

    fun hentKravpakkeForSak(sakId: SakId): KravPakkeMedAvdoed {
        val (kravpakke, forstegangsbehandlingMedKravpakke) =
            inTransaction {
                val behandlingerForSak = behandlingService.hentBehandlingerForSak(sakId)
                val foerstegangsbehandlingMedKravpakke =
                    behandlingerForSak.find { behandling ->
                        (behandling is Foerstegangsbehandling) && behandling.boddEllerArbeidetUtlandet?.skalSendeKravpakke == true
                    }
                        ?: throw FantIkkeFoerstegangsbehandlingForKravpakkeOgSak("Fant ikke behandlingen for sak $sakId")
                val kravpakke =
                    hentGenerellbehandlingSinTilknyttetedeBehandling(foerstegangsbehandlingMedKravpakke.id)
                        ?: throw FantIkkeKravpakkeForFoerstegangsbehandling(
                            "Fant ikke" +
                                " kravpakke for ${foerstegangsbehandlingMedKravpakke.id}",
                        )
                Pair(kravpakke, foerstegangsbehandlingMedKravpakke)
            }
        if (kravpakke.status == GenerellBehandling.Status.ATTESTERT) {
            val avdoed =
                grunnlagService.hentGrunnlagAvType(
                    forstegangsbehandlingMedKravpakke.id,
                    Opplysningstype.AVDOED_PDL_V1,
                ) ?: throw FantIkkeAvdoedException(
                    "Fant ikke avdød for sak: $sakId behandlingid: ${forstegangsbehandlingMedKravpakke.id}",
                )

            return KravPakkeMedAvdoed(kravpakke, objectMapper.readValue(avdoed.opplysning.toJson(), Person::class.java))
        } else {
            throw KravpakkeIkkeAttestertException("Kravpakken for sak $sakId har ikke blitt attestert, status: ${kravpakke.status}")
        }
    }

    private fun sjekkStatusErTillattForAttestering(generellBehandling: GenerellBehandling) {
        val behandling = hentBehandlingMedId(generellBehandling.id) ?: throw GenerellIkkeFunnetException()
        if (!behandling.kanEndres()) {
            throw FeilStatusGenerellBehandling(
                """
                Behandlingen (id: ${behandling.id}) kan ikke ha status ${behandling.status} 
                hvis du skal sende den til attestering
                """.trimIndent(),
            )
        }
    }
}

class KravpakkeIkkeAttestertException(
    detail: String,
) : UgyldigForespoerselException(code = "KRAVPAKKE_IKKE_ATTESTERT", detail = detail)

class FantIkkeAvdoedException(
    msg: String,
) : UgyldigForespoerselException(
        code = "FANT_IKKE_AVDOED",
        detail = msg,
    )

class FantIkkeFoerstegangsbehandlingForKravpakkeOgSak(
    msg: String,
) : UgyldigForespoerselException(
        code = "FANT_IKKE_FOERSTEGANGSBEHANDLING_FOR_KRAVPAKKE",
        detail = msg,
    )

class FantIkkeKravpakkeForFoerstegangsbehandling(
    msg: String,
) : UgyldigForespoerselException(
        code = "FANT_IKKE_KRAVPAKKE_FOR_FOERSTEGANGSBEHANDLING",
        detail = msg,
    )

data class KravPakkeMedAvdoed(
    val kravpakke: GenerellBehandling,
    val avdoed: Person,
)
