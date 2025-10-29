package no.nav.etterlatte.behandling.klage

import io.ktor.server.plugins.NotFoundException
import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.behandling.BehandlingService
import no.nav.etterlatte.behandling.hendelse.HendelseDao
import no.nav.etterlatte.behandling.hendelse.HendelseType
import no.nav.etterlatte.behandling.klienter.KlageKlient
import no.nav.etterlatte.behandling.klienter.OpprettJournalpostDto
import no.nav.etterlatte.behandling.klienter.VedtakKlient
import no.nav.etterlatte.brev.model.Brev
import no.nav.etterlatte.brev.model.MottakerType
import no.nav.etterlatte.funksjonsbrytere.FeatureToggleService
import no.nav.etterlatte.libs.common.behandling.BehandlingResultat
import no.nav.etterlatte.libs.common.behandling.EkstradataInnstilling
import no.nav.etterlatte.libs.common.behandling.Formkrav
import no.nav.etterlatte.libs.common.behandling.InitieltUtfallMedBegrunnelseDto
import no.nav.etterlatte.libs.common.behandling.InnkommendeKlage
import no.nav.etterlatte.libs.common.behandling.InnstillingTilKabal
import no.nav.etterlatte.libs.common.behandling.KabalStatus
import no.nav.etterlatte.libs.common.behandling.Kabalrespons
import no.nav.etterlatte.libs.common.behandling.Klage
import no.nav.etterlatte.libs.common.behandling.KlageMottaker
import no.nav.etterlatte.libs.common.behandling.KlageResultat
import no.nav.etterlatte.libs.common.behandling.KlageUtfall
import no.nav.etterlatte.libs.common.behandling.KlageUtfallMedData
import no.nav.etterlatte.libs.common.behandling.KlageUtfallUtenBrev
import no.nav.etterlatte.libs.common.behandling.KlageVedtak
import no.nav.etterlatte.libs.common.behandling.KlageVedtaksbrev
import no.nav.etterlatte.libs.common.behandling.Mottakerident
import no.nav.etterlatte.libs.common.behandling.SendtInnstillingsbrev
import no.nav.etterlatte.libs.common.feilhaandtering.IkkeFunnetException
import no.nav.etterlatte.libs.common.feilhaandtering.IkkeTillattException
import no.nav.etterlatte.libs.common.feilhaandtering.InternfeilException
import no.nav.etterlatte.libs.common.feilhaandtering.UgyldigForespoerselException
import no.nav.etterlatte.libs.common.feilhaandtering.krevIkkeNull
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import no.nav.etterlatte.libs.common.klage.AarsakTilAvbrytelse
import no.nav.etterlatte.libs.common.klage.KlageHendelseType
import no.nav.etterlatte.libs.common.klage.StatistikkKlage
import no.nav.etterlatte.libs.common.oppgave.OppgaveIntern
import no.nav.etterlatte.libs.common.oppgave.OppgaveKilde
import no.nav.etterlatte.libs.common.oppgave.OppgaveType
import no.nav.etterlatte.libs.common.sak.SakId
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.common.tidspunkt.toNorskTidspunkt
import no.nav.etterlatte.libs.common.vedtak.VedtakDto
import no.nav.etterlatte.libs.ktor.route.FeatureIkkeStoettetException
import no.nav.etterlatte.libs.ktor.token.Saksbehandler
import no.nav.etterlatte.oppgave.OppgaveService
import no.nav.etterlatte.sak.SakLesDao
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.UUID

interface KlageService {
    fun opprettKlage(
        sakId: SakId,
        innkommendeKlage: InnkommendeKlage,
        saksbehandler: Saksbehandler,
    ): Klage

    fun hentKlage(id: UUID): Klage?

    fun hentKlagerISak(sakId: SakId): List<Klage>

    fun lagreFormkravIKlage(
        klageId: UUID,
        formkrav: Formkrav,
        saksbehandler: Saksbehandler,
    ): Klage

    fun oppdaterMottattDato(
        klageId: UUID,
        nyMottattDato: LocalDate,
        saksbehandler: Saksbehandler,
    ): Klage

    fun lagreUtfallAvKlage(
        klageId: UUID,
        utfall: KlageUtfallUtenBrev,
        saksbehandler: Saksbehandler,
    ): Klage

    fun lagreInitieltUtfallMedBegrunnelseAvKlage(
        klageId: UUID,
        utfall: InitieltUtfallMedBegrunnelseDto,
        saksbehandler: Saksbehandler,
    ): Klage

    fun haandterKabalrespons(
        klageId: UUID,
        kabalrespons: Kabalrespons,
    )

    fun ferdigstillKlage(
        klageId: UUID,
        saksbehandler: Saksbehandler,
    ): Klage

    fun avbrytKlage(
        klageId: UUID,
        aarsak: AarsakTilAvbrytelse,
        kommentar: String,
        saksbehandler: Saksbehandler,
    )

    fun fattVedtak(
        klageId: UUID,
        saksbehandler: Saksbehandler,
    ): Klage

    fun attesterVedtak(
        klageId: UUID,
        kommentar: String,
        saksbehandler: Saksbehandler,
    ): Klage

    fun underkjennVedtak(
        klageId: UUID,
        kommentar: String,
        valgtBegrunnelse: String,
        saksbehandler: Saksbehandler,
    ): Klage

    fun oppdaterKlagerIkkeSvart(
        klageId: UUID,
        begrunnelse: String,
        saksbehandler: Saksbehandler,
    ): Klage

    fun opprettOppgaveOmOmgjoeringAvKlage(
        klageId: UUID,
        saksbehandler: Saksbehandler,
    )
}

class ManglerSaksbehandlerException(
    msg: String,
) : UgyldigForespoerselException(
        code = "MANGLER_SAKSBEHANDLER_PAA_OPPGAVE",
        detail = msg,
    )

class KlageServiceImpl(
    private val klageDao: KlageDao,
    private val sakDao: SakLesDao,
    private val hendelseDao: HendelseDao,
    private val behandlingService: BehandlingService,
    private val oppgaveService: OppgaveService,
    private val klageKlient: KlageKlient,
    private val klageHendelser: IKlageHendelserService,
    private val vedtakKlient: VedtakKlient,
    private val featureToggleService: FeatureToggleService,
    private val klageBrevService: KlageBrevService,
) : KlageService {
    private val logger: Logger = LoggerFactory.getLogger(this::class.java)

    override fun opprettKlage(
        sakId: SakId,
        innkommendeKlage: InnkommendeKlage,
        saksbehandler: Saksbehandler,
    ): Klage {
        val sak = sakDao.hentSak(sakId) ?: throw NotFoundException("Fant ikke sak med id=$sakId")

        val klage = Klage.ny(sak, innkommendeKlage)
        logger.info("Oppretter klage med id=${klage.id} på sak med id=$sakId")

        klageDao.lagreKlage(klage)

        oppgaveService.opprettOppgave(
            referanse = klage.id.toString(),
            sakId = sakId,
            kilde = OppgaveKilde.EKSTERN,
            type = OppgaveType.KLAGE,
            frist =
                innkommendeKlage.mottattDato
                    .plusMonths(1L)
                    .atStartOfDay()
                    .toNorskTidspunkt(),
            merknad = null,
        )

        opprettKlageHendelse(klage, KlageHendelseType.OPPRETTET, saksbehandler)
        val utlandstilknytningType = behandlingService.hentUtlandstilknytningForSak(sakId)?.type
        klageHendelser.sendKlageHendelseRapids(
            StatistikkKlage(klage.id, klage, Tidspunkt.now(), utlandstilknytningType),
            KlageHendelseType.OPPRETTET,
        )

        return klage
    }

    override fun hentKlage(id: UUID): Klage? = klageDao.hentKlage(id)

    override fun hentKlagerISak(sakId: SakId): List<Klage> = klageDao.hentKlagerISak(sakId)

    override fun lagreFormkravIKlage(
        klageId: UUID,
        formkrav: Formkrav,
        saksbehandler: Saksbehandler,
    ): Klage {
        val klage = klageDao.hentKlage(klageId) ?: throw NotFoundException("Klage med id=$klageId finnes ikke")
        logger.info("Lagrer vurdering av formkrav for klage med id=$klageId")

        if (!Formkrav.erFormkravKonsistente(formkrav)) {
            logger.warn(
                "Mottok 'rare' formkrav fra frontend, der det er mismatch mellom svar på formkrav. Gjelder " +
                    "klagen med id=$klageId. Stopper ikke opp lagringen av formkravene, siden alt er vurdert av " +
                    "saksbehandler.",
            )
        }

        val oppdatertKlage = klage.oppdaterFormkrav(formkrav, saksbehandler.ident)
        klageDao.lagreKlage(oppdatertKlage)

        return oppdatertKlage
    }

    override fun oppdaterMottattDato(
        klageId: UUID,
        nyMottattDato: LocalDate,
        saksbehandler: Saksbehandler,
    ): Klage {
        logger.info("Oppdaterer klage mottatt dato for klagen med id=$klageId")
        val klage = klageDao.hentKlage(klageId) ?: throw NotFoundException("Fant ikke klage med id=$klageId")
        if (nyMottattDato.isAfter(LocalDate.now())) {
            throw UgyldigForespoerselException("DATO_FRAM_I_TID", "Klagedatoen kan ikke være fram i tid.")
        }

        val oppdatertKlage = klage.oppdaterMottattDato(nyMottattDato)
        klageDao.lagreKlage(oppdatertKlage)

        val kommentar =
            "Gammel mottatt dato i klagen var ${klage.innkommendeDokument?.mottattDato}, " +
                "endret til ${oppdatertKlage.innkommendeDokument?.mottattDato}."
        hendelseDao.klageHendelse(
            klageId = klageId,
            sakId = klage.sak.id,
            hendelse = KlageHendelseType.OPPDATERT_MOTTATT_DATO,
            inntruffet = Tidspunkt.now(),
            saksbehandler = saksbehandler.ident,
            kommentar = kommentar,
            begrunnelse = null,
        )

        logger.info("Oppdaterte klage mottatt dato for klagen med id=$klageId. $kommentar")
        return oppdatertKlage
    }

    override fun lagreInitieltUtfallMedBegrunnelseAvKlage(
        klageId: UUID,
        utfall: InitieltUtfallMedBegrunnelseDto,
        saksbehandler: Saksbehandler,
    ): Klage {
        if (utfall.utfall == KlageUtfall.DELVIS_OMGJOERING &&
            !featureToggleService.isEnabled(
                KlageFeatureToggle.StoetterUtfallDelvisOmgjoering,
                false,
            )
        ) {
            throw FeatureIkkeStoettetException()
        }
        val klage = klageDao.hentKlage(klageId) ?: throw KlageIkkeFunnetException(klageId)
        val oppdatertKlage = klage.oppdaterInitieltUtfallMedBegrunnelse(utfall, saksbehandler.ident)
        klageDao.lagreKlage(oppdatertKlage)
        return oppdatertKlage
    }

    override fun lagreUtfallAvKlage(
        klageId: UUID,
        utfall: KlageUtfallUtenBrev,
        saksbehandler: Saksbehandler,
    ): Klage {
        val klage = klageDao.hentKlage(klageId) ?: throw KlageIkkeFunnetException(klageId)
        if (!utfall.erStoettet()) throw FeatureIkkeStoettetException()

        val utfallMedBrev =
            when (utfall) {
                is KlageUtfallUtenBrev.Omgjoering ->
                    KlageUtfallMedData.Omgjoering(
                        omgjoering = utfall.omgjoering,
                        saksbehandler = Grunnlagsopplysning.Saksbehandler.create(saksbehandler.ident),
                    )

                is KlageUtfallUtenBrev.DelvisOmgjoering ->
                    KlageUtfallMedData.DelvisOmgjoering(
                        omgjoering = utfall.omgjoering,
                        innstilling =
                            InnstillingTilKabal(
                                lovhjemmel = enumValueOf(utfall.innstilling.lovhjemmel),
                                internKommentar = utfall.innstilling.internKommentar,
                                innstillingTekst = utfall.innstilling.innstillingTekst,
                                brev = klageBrevService.oversendelsesbrev(klage, saksbehandler),
                            ),
                        saksbehandler = Grunnlagsopplysning.Saksbehandler.create(saksbehandler.ident),
                    )

                is KlageUtfallUtenBrev.StadfesteVedtak ->
                    KlageUtfallMedData.StadfesteVedtak(
                        innstilling =
                            InnstillingTilKabal(
                                lovhjemmel = enumValueOf(utfall.innstilling.lovhjemmel),
                                internKommentar = utfall.innstilling.internKommentar,
                                innstillingTekst = utfall.innstilling.innstillingTekst,
                                brev = klageBrevService.oversendelsesbrev(klage, saksbehandler),
                            ),
                        saksbehandler = Grunnlagsopplysning.Saksbehandler.create(saksbehandler.ident),
                    )

                is KlageUtfallUtenBrev.Avvist -> {
                    val (vedtak, brev) = opprettVedtakOgBrev(klage, saksbehandler)
                    KlageUtfallMedData.Avvist(
                        saksbehandler = Grunnlagsopplysning.Saksbehandler.create(saksbehandler.ident),
                        vedtak = vedtak,
                        brev = brev,
                    )
                }

                is KlageUtfallUtenBrev.AvvistMedOmgjoering ->
                    KlageUtfallMedData.AvvistMedOmgjoering(
                        omgjoering = utfall.omgjoering,
                        saksbehandler = Grunnlagsopplysning.Saksbehandler.create(saksbehandler.ident),
                    )
            }

        val klageMedOppdatertUtfall = klage.oppdaterUtfall(utfallMedBrev)
        klageDao.lagreKlage(klageMedOppdatertUtfall)

        return klageMedOppdatertUtfall
    }

    override fun opprettOppgaveOmOmgjoeringAvKlage(
        klageId: UUID,
        saksbehandler: Saksbehandler,
    ) {
        val klage = klageDao.hentKlage(klageId) ?: throw NotFoundException()

        klage.utfall?.omgjoering()?.let {
            lagOppgaveForOmgjoering(klage)
        } ?: throw IkkeTillattException(
            "KAN_IKKE_OPPRETE_OMGJOERINGSOPPGAVE",
            "Kan ikke opprette oppgave om omgjøring av vedtak for klageId=$klageId",
        )
    }

    override fun haandterKabalrespons(
        klageId: UUID,
        kabalrespons: Kabalrespons,
    ) {
        val klage = klageDao.hentKlage(klageId) ?: throw NotFoundException()
        logger.info("Mottok status=${kabalrespons.kabalStatus} fra kabal, med resultat=${kabalrespons.resultat}, for sakId=${klage.sak.id}")

        if (kabalrespons.kabalStatus == KabalStatus.FERDIGSTILT) {
            val vedtaketKlagenGjelder =
                klage.formkrav?.formkrav?.vedtaketKlagenGjelder ?: throw OmgjoeringMaaGjeldeEtVedtakException(klage)

            oppgaveService.opprettOppgave(
                referanse = klage.id.toString(),
                sakId = klage.sak.id,
                kilde = OppgaveKilde.BEHANDLING,
                type = OppgaveType.KLAGE_SVAR_KABAL,
                merknad = "Klage på vedtak om ${vedtaketKlagenGjelder.vedtakType?.toString()?.lowercase()} (${
                    vedtaketKlagenGjelder.datoAttestert?.format(
                        DateTimeFormatter.ISO_LOCAL_DATE,
                    )
                }) er ferdig behandlet. Resultat: ${kabalrespons.resultat}",
            )
        }

        opprettKlageHendelse(
            klage = klage,
            hendelse = KlageHendelseType.KABAL_HENDELSE,
            saksbehandler = null,
            kommentar = "Mottok status=${kabalrespons.kabalStatus} fra kabal, med resultat=${kabalrespons.resultat}",
        )

        klageDao.oppdaterKabalStatus(klageId, kabalrespons)
    }

    override fun ferdigstillKlage(
        klageId: UUID,
        saksbehandler: Saksbehandler,
    ): Klage {
        val klage = klageDao.hentKlage(klageId) ?: throw KlageIkkeFunnetException(klageId)
        sjekkAtStatusTillaterFerdigstilling(klage)
        sjekkAtSaksbehandlerHarOppgaven(klageId, saksbehandler, "ferdigstille behandlingen")

        val oppgaveOmgjoering =
            klage.utfall?.omgjoering()?.let {
                lagOppgaveForOmgjoering(klage)
            }
        val sendtInnstillingsbrev =
            klage.utfall?.innstilling()?.let {
                ferdigstillBrevOgSendTilKabal(it, klage, saksbehandler)
            }

        val klageMedResultat =
            klage.ferdigstill(
                KlageResultat(
                    opprettetOppgaveOmgjoeringId = oppgaveOmgjoering?.id,
                    sendtInnstillingsbrev = sendtInnstillingsbrev,
                ),
            )
        klageDao.lagreKlage(klageMedResultat)

        oppgaveService.ferdigstillOppgaveUnderBehandling(
            referanse = klageMedResultat.id.toString(),
            type = OppgaveType.KLAGE,
            saksbehandler = saksbehandler,
        )
        opprettKlageHendelse(klageMedResultat, KlageHendelseType.FERDIGSTILT, saksbehandler)
        val utlandstilknytningType = behandlingService.hentUtlandstilknytningForSak(klage.sak.id)?.type
        klageHendelser.sendKlageHendelseRapids(
            StatistikkKlage(
                klageMedResultat.id,
                klageMedResultat,
                Tidspunkt.now(),
                utlandstilknytningType,
                saksbehandler.ident,
            ),
            KlageHendelseType.FERDIGSTILT,
        )
        klageBrevService.slettUferdigeBrev(klageMedResultat.id, saksbehandler)

        return klageMedResultat
    }

    override fun avbrytKlage(
        klageId: UUID,
        aarsak: AarsakTilAvbrytelse,
        kommentar: String,
        saksbehandler: Saksbehandler,
    ) {
        val klage =
            hentKlage(klageId)
                ?: throw NotFoundException("Klage med id=$klageId finnes ikke")
        if (!klage.kanAvbryte()) {
            throw IkkeTillattException("KLAGE_KAN_IKKE_AVBRYTES", "Klage med id=$klageId kan ikke avbrytes")
        }

        val avbruttKlage = klage.avbryt(aarsak)

        klageDao.lagreKlage(avbruttKlage).also {
            oppgaveService.avbrytOppgaveUnderBehandling(klageId.toString(), saksbehandler)

            opprettKlageHendelse(
                klage = avbruttKlage,
                hendelse = KlageHendelseType.AVBRUTT,
                saksbehandler = saksbehandler,
                kommentar = kommentar,
                begrunnelse = aarsak.name,
            )
        }

        val utlandstilknytningType = behandlingService.hentUtlandstilknytningForSak(avbruttKlage.sak.id)?.type

        klageHendelser.sendKlageHendelseRapids(
            StatistikkKlage(
                avbruttKlage.id,
                avbruttKlage,
                Tidspunkt.now(),
                utlandstilknytningType,
                saksbehandler.ident,
            ),
            KlageHendelseType.AVBRUTT,
        )
    }

    override fun fattVedtak(
        klageId: UUID,
        saksbehandler: Saksbehandler,
    ): Klage {
        val klage = klageDao.hentKlage(klageId) ?: throw NotFoundException("Klage med id=$klageId finnes ikke")
        sjekkAtSaksbehandlerHarOppgaven(klageId, saksbehandler, "fatte vedtak")

        val oppdatertKlage = klage.fattVedtak()
        val vedtak =
            runBlocking {
                vedtakKlient.fattVedtakKlage(klage, saksbehandler)
            }
        sjekkVedtakIdLagretIUtfall(klage, vedtak.id)
        klageDao.lagreKlage(oppdatertKlage)

        opprettVedtakHendelse(klage, vedtak, HendelseType.FATTET, saksbehandler)
        val utlandstilknytningType = behandlingService.hentUtlandstilknytningForSak(klage.sak.id)
        klageHendelser.sendKlageHendelseRapids(
            statistikkKlage =
                StatistikkKlage(
                    id = oppdatertKlage.id,
                    klage = oppdatertKlage,
                    tidspunkt = Tidspunkt.now(),
                    utlandstilknytningType = utlandstilknytningType?.type,
                ),
            klageHendelseType = KlageHendelseType.FATTET_VEDTAK,
        )

        oppgaveService.tilAttestering(
            referanse = klageId.toString(),
            type = OppgaveType.KLAGE,
            "Vedtak om avvist klage til attestering",
        )

        return oppdatertKlage
    }

    override fun attesterVedtak(
        klageId: UUID,
        kommentar: String,
        saksbehandler: Saksbehandler,
    ): Klage {
        logger.info("Attesterer vedtak for klage=$klageId")
        val klage = klageDao.hentKlage(klageId) ?: throw NotFoundException("Klage med id=$klageId finnes ikke")

        sjekkAtSaksbehandlerHarOppgaven(klageId, saksbehandler, "attestere vedtak")

        krevIkkeNull(klage.utfall as? KlageUtfallMedData.Avvist) {
            "Vi har en klage som kunne attesteres, men har feil utfall lagret. Id: $klageId"
        }

        val oppdatertKlage = klage.attesterVedtak()

        klageBrevService.ferdigstillVedtaksbrev(oppdatertKlage, saksbehandler)
        val vedtak =
            runBlocking {
                vedtakKlient.attesterVedtakKlage(oppdatertKlage, saksbehandler)
            }
        sjekkVedtakIdLagretIUtfall(oppdatertKlage, vedtak.id)
        klageDao.lagreKlage(oppdatertKlage)

        opprettVedtakHendelse(oppdatertKlage, vedtak, HendelseType.ATTESTERT, saksbehandler, kommentar)
        val utlandstilknytningType = behandlingService.hentUtlandstilknytningForSak(klage.sak.id)
        klageHendelser.sendKlageHendelseRapids(
            statistikkKlage =
                StatistikkKlage(
                    id = oppdatertKlage.id,
                    klage = oppdatertKlage,
                    tidspunkt = Tidspunkt.now(),
                    utlandstilknytningType = utlandstilknytningType?.type,
                ),
            klageHendelseType = KlageHendelseType.ATTESTERT,
        )
        oppgaveService.ferdigstillOppgaveUnderBehandling(
            referanse = klageId.toString(),
            type = OppgaveType.KLAGE,
            saksbehandler = saksbehandler,
        )

        klageBrevService.slettUferdigeBrev(oppdatertKlage.id, saksbehandler)
        return oppdatertKlage
    }

    override fun underkjennVedtak(
        klageId: UUID,
        kommentar: String,
        valgtBegrunnelse: String,
        saksbehandler: Saksbehandler,
    ): Klage {
        logger.info("Underkjenner vedtak for klage=$klageId")
        val klage =
            klageDao.hentKlage(klageId) ?: throw IkkeFunnetException(
                "KLAGE_IKKE_FUNNET",
                "Klage med id=$klageId finnes ikke",
            )

        val oppdatertKlage = klage.underkjennVedtak()

        val vedtak =
            runBlocking {
                vedtakKlient.underkjennVedtakKlage(
                    klageId = klageId,
                    brukerTokenInfo = saksbehandler,
                )
            }

        klageDao.lagreKlage(oppdatertKlage)

        opprettVedtakHendelse(oppdatertKlage, vedtak, HendelseType.UNDERKJENT, saksbehandler, kommentar)
        val utlandstilknytningType = behandlingService.hentUtlandstilknytningForSak(klage.sak.id)
        klageHendelser.sendKlageHendelseRapids(
            statistikkKlage =
                StatistikkKlage(
                    id = oppdatertKlage.id,
                    klage = oppdatertKlage,
                    tidspunkt = Tidspunkt.now(),
                    utlandstilknytningType = utlandstilknytningType?.type,
                ),
            klageHendelseType = KlageHendelseType.UNDERKJENT,
        )
        oppgaveService.tilUnderkjent(
            referanse = klageId.toString(),
            type = OppgaveType.KLAGE,
            "Vedtak er underkjent",
        )

        return oppdatertKlage
    }

    override fun oppdaterKlagerIkkeSvart(
        klageId: UUID,
        begrunnelse: String,
        saksbehandler: Saksbehandler,
    ): Klage {
        val klage =
            klageDao.hentKlage(klageId) ?: throw IkkeFunnetException(
                "KLAGE_IKKE_FUNNET",
                "Klage med id=$klageId finnes ikke",
            )
        val oppdatertKlage = klage.oppdaterKlagerIkkeSvartBegrunnelse(begrunnelse, saksbehandler.ident)
        klageDao.lagreKlage(oppdatertKlage)
        return oppdatertKlage
    }

    private fun opprettVedtakOgBrev(
        klage: Klage,
        saksbehandler: Saksbehandler,
    ): Pair<KlageVedtak, KlageVedtaksbrev> {
        return when (val utfall = klage.utfall) {
            is KlageUtfallMedData.Avvist -> Pair(utfall.vedtak, utfall.brev)
            else -> {
                return runBlocking {
                    val vedtakId = lagreVedtakForAvvisning(klage, saksbehandler)
                    val vedtaksbrevId = klageBrevService.vedtaksbrev(klage, saksbehandler)
                    Pair(vedtakId, vedtaksbrevId)
                }
            }
        }
    }

    private suspend fun lagreVedtakForAvvisning(
        klage: Klage,
        saksbehandler: Saksbehandler,
    ): KlageVedtak {
        val vedtak = vedtakKlient.lagreVedtakKlage(klage, saksbehandler)
        return KlageVedtak(vedtak.id)
    }

    private fun ferdigstillBrevOgSendTilKabal(
        it: InnstillingTilKabal,
        klage: Klage,
        saksbehandler: Saksbehandler,
    ): SendtInnstillingsbrev {
        val ferdigstillResultat = klageBrevService.ferdigstillBrevOgNotatTilKa(it, klage, saksbehandler)
        sendTilKabal(klage, ferdigstillResultat)
        return SendtInnstillingsbrev(
            journalfoerTidspunkt = ferdigstillResultat.journalfoertOversendelsesbrevTidspunkt,
            sendtKabalTidspunkt = Tidspunkt.now(),
            brevId = ferdigstillResultat.oversendelsesbrev.id,
            journalpostId = ferdigstillResultat.journalpostIdOversendelsesbrev,
        )
    }

    private fun sendTilKabal(
        klage: Klage,
        ferdigstillResultat: FerdigstillResultat,
    ) = runBlocking {
        klageKlient.sendKlageTilKabal(
            klage = klage,
            ekstradataInnstilling =
                EkstradataInnstilling(
                    mottakerInnstilling =
                        mapMottakerInnstilling(
                            klage.innkommendeDokument?.innsender,
                            ferdigstillResultat,
                        ),
                    // TODO: Håndter verge
                    vergeEllerFullmektig = null,
                    journalpostInnstillingsbrev = ferdigstillResultat.notatTilKa.journalpostId,
                    journalpostKlage = klage.innkommendeDokument?.journalpostId,
                    // TODO: koble på når vi har journalpost soeknad inn
                    journalpostSoeknad = null,
                    // TODO: hent ut vedtaksbrev for vedtaket
                    journalpostVedtak = null,
                ),
        )
    }

    private fun mapMottakerInnstilling(
        innsender: String?,
        resultat: FerdigstillResultat,
    ): KlageMottaker {
        val mottakere = resultat.oversendelsesbrev.mottakere

        val brevMottaker =
            if (mottakere.size > 1) {
                mottakere.find { it.foedselsnummer?.value == innsender }
                    ?: mottakere.single { it.type == MottakerType.HOVED }
            } else {
                mottakere.singleOrNull()
                    ?: throw InternfeilException("Mangler mottaker på klageinnstilling")
            }

        return KlageMottaker(
            navn = brevMottaker.navn,
            foedselsnummer = brevMottaker.foedselsnummer?.value?.let { Mottakerident(it) },
            orgnummer = brevMottaker.orgnummer,
        )
    }

    private fun opprettKlageHendelse(
        klage: Klage,
        hendelse: KlageHendelseType,
        saksbehandler: Saksbehandler?,
        kommentar: String? = null,
        begrunnelse: String? = null,
    ) {
        hendelseDao.klageHendelse(
            klageId = klage.id,
            sakId = klage.sak.id,
            hendelse = hendelse,
            inntruffet = Tidspunkt.now(),
            saksbehandler = saksbehandler?.ident,
            kommentar = kommentar,
            begrunnelse = begrunnelse,
        )
    }

    private fun opprettVedtakHendelse(
        klage: Klage,
        vedtak: VedtakDto,
        hendelse: HendelseType,
        saksbehandler: Saksbehandler,
        kommentar: String? = null,
    ) {
        hendelseDao.vedtakHendelse(
            behandlingId = klage.id,
            sakId = klage.sak.id,
            vedtakId = vedtak.id,
            hendelse = hendelse,
            inntruffet = Tidspunkt.now(),
            saksbehandler = saksbehandler.ident,
            kommentar = kommentar,
            begrunnelse = null,
        )
    }

    private fun sjekkAtSaksbehandlerHarOppgaven(
        klageId: UUID,
        saksbehandler: Saksbehandler,
        handling: String,
    ) {
        val saksbehandlerForKlageOppgave =
            oppgaveService.hentOppgaveForAttesterbarBehandling(klageId.toString())?.saksbehandler
                ?: throw ManglerSaksbehandlerException("Fant ingen saksbehandler på oppgave relatert til klageid $klageId")

        if (saksbehandlerForKlageOppgave.ident != saksbehandler.ident) {
            throw UgyldigForespoerselException(
                code = "SAKSBEHANDLER_HAR_IKKE_OPPGAVEN",
                detail =
                    "Saksbehandler er ikke ansvarlig på oppgaven til klagen med id=$klageId, " +
                        "og kan derfor ikke $handling",
            )
        }
    }

    private fun sjekkVedtakIdLagretIUtfall(
        klage: Klage,
        vedtakId: Long,
    ) {
        val utfall = klage.utfall as KlageUtfallMedData.Avvist
        if (vedtakId != utfall.vedtak.vedtakId) {
            throw InternfeilException(
                "VedtakId=$vedtakId er forskjellig fra det som ligger i utfall=$vedtakId",
            )
        }
    }

    private fun sjekkAtStatusTillaterFerdigstilling(klage: Klage) {
        if (!klage.kanFerdigstille()) {
            throw UgyldigForespoerselException(
                code = "KLAGE_KAN_IKKE_FERDIGSTILLES",
                detail = "Klagen med id=${klage.id} kan ikke ferdigstilles siden den har feil status / tilstand",
            )
        }
        val hjemmel = klage.utfall?.innstilling()?.lovhjemmel
        if (hjemmel != null && !hjemmel.kanBrukesForSaktype(klage.sak.sakType)) {
            throw UgyldigForespoerselException(
                code = "UGYLDIG_HJEMMEL_FOR_KLAGE",
                detail = "Klagen har en hjemmel (${hjemmel.lesbarTekst()}) som ikke kan brukes for sakstype ${klage.sak.sakType}.",
            )
        }
    }

    private fun lagOppgaveForOmgjoering(klage: Klage): OppgaveIntern {
        val vedtaketKlagenGjelder =
            klage.formkrav?.formkrav?.vedtaketKlagenGjelder ?: throw OmgjoeringMaaGjeldeEtVedtakException(klage)

        return oppgaveService.opprettOppgave(
            referanse = klage.id.toString(),
            sakId = klage.sak.id,
            kilde = OppgaveKilde.BEHANDLING,
            type = OppgaveType.OMGJOERING,
            merknad = "Vedtak om ${vedtaketKlagenGjelder.vedtakType?.toString()?.lowercase()} (${
                vedtaketKlagenGjelder.datoAttestert?.format(
                    DateTimeFormatter.ISO_LOCAL_DATE,
                )
            }) skal omgjøres",
        )
    }

    private fun KlageUtfallUtenBrev.erStoettet(): Boolean =
        when (this) {
            is KlageUtfallUtenBrev.DelvisOmgjoering ->
                featureToggleService.isEnabled(
                    KlageFeatureToggle.StoetterUtfallDelvisOmgjoering,
                    false,
                )

            else -> true
        }
}

class OmgjoeringMaaGjeldeEtVedtakException(
    klage: Klage,
) : UgyldigForespoerselException(
        code = "OMGJOERING_MAA_GJELDE_ET_VEDTAK",
        detail = "Klagen med id=${klage.id} skal resultere i en omgjøring, men mangler et vedtak som klagen gjelder",
    )

class KlageIkkeFunnetException(
    klageId: UUID,
) : IkkeFunnetException(code = "KLAGE_IKKE_FUNNET", detail = "Kunne ikke finne klage med id=$klageId")

data class FerdigstillResultat(
    val oversendelsesbrev: Brev,
    val notatTilKa: OpprettJournalpostDto,
    val journalfoertOversendelsesbrevTidspunkt: Tidspunkt,
    val journalpostIdOversendelsesbrev: String,
)
