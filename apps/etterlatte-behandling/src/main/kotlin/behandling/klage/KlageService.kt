package no.nav.etterlatte.behandling.klage

import io.ktor.server.plugins.NotFoundException
import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.behandling.hendelse.HendelseDao
import no.nav.etterlatte.behandling.klienter.BrevApiKlient
import no.nav.etterlatte.libs.common.behandling.Formkrav
import no.nav.etterlatte.libs.common.behandling.InnstillingTilKabal
import no.nav.etterlatte.libs.common.behandling.Kabalrespons
import no.nav.etterlatte.libs.common.behandling.Klage
import no.nav.etterlatte.libs.common.behandling.KlageBrevInnstilling
import no.nav.etterlatte.libs.common.behandling.KlageHendelseType
import no.nav.etterlatte.libs.common.behandling.KlageUtfall
import no.nav.etterlatte.libs.common.behandling.KlageUtfallUtenBrev
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import no.nav.etterlatte.libs.common.oppgave.OppgaveKilde
import no.nav.etterlatte.libs.common.oppgave.OppgaveType
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.oppgave.OppgaveService
import no.nav.etterlatte.sak.SakDao
import no.nav.etterlatte.token.Saksbehandler
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.UUID

interface KlageService {
    fun opprettKlage(sakId: Long): Klage

    fun hentKlage(id: UUID): Klage?

    fun hentKlagerISak(sakId: Long): List<Klage>

    fun lagreFormkravIKlage(
        klageId: UUID,
        formkrav: Formkrav,
        saksbehandler: Saksbehandler,
    ): Klage

    fun lagreUtfallAvKlage(
        klageId: UUID,
        utfall: KlageUtfallUtenBrev,
        saksbehandler: Saksbehandler,
    ): Klage

    fun oppdaterKabalStatus(
        klageId: UUID,
        kabalrespons: Kabalrespons,
    )
}

class KlageServiceImpl(
    private val klageDao: KlageDao,
    private val sakDao: SakDao,
    private val hendelseDao: HendelseDao,
    private val oppgaveService: OppgaveService,
    private val brevApiKlient: BrevApiKlient,
) : KlageService {
    private val logger: Logger = LoggerFactory.getLogger(this::class.java)

    override fun opprettKlage(sakId: Long): Klage {
        val sak = sakDao.hentSak(sakId) ?: throw NotFoundException("Fant ikke sak med id=$sakId")

        val klage = Klage.ny(sak)
        logger.info("Oppretter klage med id=${klage.id} på sak med id=$sakId")

        klageDao.lagreKlage(klage)

        oppgaveService.opprettNyOppgaveMedSakOgReferanse(
            referanse = klage.id.toString(),
            sakId = sakId,
            oppgaveKilde = OppgaveKilde.EKSTERN,
            oppgaveType = OppgaveType.KLAGE,
            merknad = null,
        )

        hendelseDao.klageHendelse(
            klageId = klage.id,
            sakId = klage.sak.id,
            hendelse = KlageHendelseType.OPPRETTET,
            inntruffet = Tidspunkt.now(),
            saksbehandler = null,
            kommentar = null,
            begrunnelse = null,
        )

        return klage
    }

    override fun hentKlage(id: UUID): Klage? {
        return klageDao.hentKlage(id)
    }

    override fun hentKlagerISak(sakId: Long): List<Klage> {
        return klageDao.hentKlagerISak(sakId)
    }

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

    override fun lagreUtfallAvKlage(
        klageId: UUID,
        utfall: KlageUtfallUtenBrev,
        saksbehandler: Saksbehandler,
    ): Klage {
        val klage = klageDao.hentKlage(klageId) ?: throw NotFoundException("Fant ikke klage med id=$klageId")

        val utfallMedBrev =
            when (utfall) {
                // Vurder å slette brev hvis det finnes her? Så vi ikke polluter med hengende brev
                is KlageUtfallUtenBrev.Omgjoering ->
                    KlageUtfall.Omgjoering(
                        omgjoering = utfall.omgjoering,
                        saksbehandler = Grunnlagsopplysning.Saksbehandler.create(saksbehandler.ident),
                    )

                is KlageUtfallUtenBrev.DelvisOmgjoering ->
                    KlageUtfall.DelvisOmgjoering(
                        omgjoering = utfall.omgjoering,
                        innstilling =
                            InnstillingTilKabal(
                                lovhjemmel = utfall.innstilling.lovhjemmel,
                                tekst = utfall.innstilling.tekst,
                                brev = brevForInnstilling(klage, saksbehandler),
                            ),
                        saksbehandler = Grunnlagsopplysning.Saksbehandler.create(saksbehandler.ident),
                    )

                is KlageUtfallUtenBrev.StadfesteVedtak ->
                    KlageUtfall.StadfesteVedtak(
                        innstilling =
                            InnstillingTilKabal(
                                lovhjemmel = utfall.innstilling.lovhjemmel,
                                tekst = utfall.innstilling.tekst,
                                brev = brevForInnstilling(klage, saksbehandler),
                            ),
                        saksbehandler = Grunnlagsopplysning.Saksbehandler.create(saksbehandler.ident),
                    )
            }

        val klageMedOppdatertUtfall = klage.oppdaterUtfall(utfallMedBrev)
        klageDao.lagreKlage(klageMedOppdatertUtfall)

        return klageMedOppdatertUtfall
    }

    private fun brevForInnstilling(
        klage: Klage,
        saksbehandler: Saksbehandler,
    ): KlageBrevInnstilling {
        return when (val utfall = klage.utfall) {
            is KlageUtfall.DelvisOmgjoering -> utfall.innstilling.brev
            is KlageUtfall.StadfesteVedtak -> utfall.innstilling.brev
            else -> {
                val brev = runBlocking { brevApiKlient.opprettKlageInnstillingsbrevISak(klage.sak.id, saksbehandler) }
                KlageBrevInnstilling(brev.id)
            }
        }
    }

    override fun oppdaterKabalStatus(
        klageId: UUID,
        kabalrespons: Kabalrespons,
    ) = klageDao.oppdaterKabalStatus(klageId, kabalrespons)
}
