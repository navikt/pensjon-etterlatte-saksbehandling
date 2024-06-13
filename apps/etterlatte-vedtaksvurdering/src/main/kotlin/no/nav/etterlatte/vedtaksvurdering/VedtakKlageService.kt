package no.nav.etterlatte.no.nav.etterlatte.vedtaksvurdering

import io.ktor.server.plugins.NotFoundException
import no.nav.etterlatte.libs.common.behandling.Klage
import no.nav.etterlatte.libs.common.person.Folkeregisteridentifikator
import no.nav.etterlatte.libs.common.rapidsandrivers.SKAL_SENDE_BREV
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.common.toObjectNode
import no.nav.etterlatte.libs.common.vedtak.Attestasjon
import no.nav.etterlatte.libs.common.vedtak.VedtakFattet
import no.nav.etterlatte.libs.common.vedtak.VedtakKafkaHendelseHendelseType
import no.nav.etterlatte.libs.common.vedtak.VedtakStatus
import no.nav.etterlatte.libs.common.vedtak.VedtakType
import no.nav.etterlatte.libs.ktor.token.BrukerTokenInfo
import no.nav.etterlatte.vedtaksvurdering.OpprettVedtak
import no.nav.etterlatte.vedtaksvurdering.RapidInfo
import no.nav.etterlatte.vedtaksvurdering.UgyldigAttestantException
import no.nav.etterlatte.vedtaksvurdering.Vedtak
import no.nav.etterlatte.vedtaksvurdering.VedtakInnhold
import no.nav.etterlatte.vedtaksvurdering.VedtakOgRapid
import no.nav.etterlatte.vedtaksvurdering.VedtakTilstandException
import no.nav.etterlatte.vedtaksvurdering.VedtaksvurderingRapidService
import no.nav.etterlatte.vedtaksvurdering.VedtaksvurderingRepository
import org.slf4j.LoggerFactory
import java.util.UUID

class VedtakKlageService(
    private val repository: VedtaksvurderingRepository,
    private val rapidService: VedtaksvurderingRapidService,
) {
    private val logger = LoggerFactory.getLogger(VedtakKlageService::class.java)

    fun opprettEllerOppdaterVedtakOmAvvisning(klage: Klage): Vedtak {
        logger.info("Oppretter eller oppdaterer vedtak for klage med id=${klage.id}")
        val eksisterendeVedtak = repository.hentVedtak(klage.id)
        val vedtak =
            if (eksisterendeVedtak == null) {
                repository.opprettVedtak(
                    OpprettVedtak(
                        soeker = Folkeregisteridentifikator.of(klage.sak.ident),
                        sakId = klage.sak.id,
                        sakType = klage.sak.sakType,
                        behandlingId = klage.id,
                        type = VedtakType.AVVIST_KLAGE,
                        innhold = VedtakInnhold.Klage(klage.toObjectNode()),
                    ),
                )
            } else {
                verifiserGyldigVedtakStatus(
                    eksisterendeVedtak.status,
                    listOf(VedtakStatus.OPPRETTET, VedtakStatus.RETURNERT),
                )
                repository.oppdaterVedtak(
                    eksisterendeVedtak.copy(
                        innhold =
                            VedtakInnhold.Klage(
                                klage = klage.toObjectNode(),
                            ),
                    ),
                )
            }
        return vedtak
    }

    fun fattVedtak(
        klage: Klage,
        brukerTokenInfo: BrukerTokenInfo,
    ): Vedtak {
        logger.info("Fatter vedtak for klage=${klage.id}")
        val eksisterendeVedtak =
            repository.hentVedtak(klage.id)
                ?: throw NotFoundException("Fant ikke vedtak for klage med id=${klage.id}")

        verifiserGyldigVedtakStatus(eksisterendeVedtak.status, listOf(VedtakStatus.OPPRETTET, VedtakStatus.RETURNERT))

        repository.oppdaterVedtak(eksisterendeVedtak.copy(innhold = VedtakInnhold.Klage(klage.toObjectNode())))

        val fattetVedtak =
            repository.fattVedtak(
                klage.id,
                VedtakFattet(brukerTokenInfo.ident(), klage.sak.enhet, Tidspunkt.now()),
            )

        rapidService.sendToRapid(vedtakOgRapidFattet(fattetVedtak))
        return fattetVedtak
    }

    fun attesterVedtak(
        klage: Klage,
        brukerTokenInfo: BrukerTokenInfo,
    ): Vedtak {
        logger.info("Attesterer vedtak for klage=${klage.id}")
        val eksisterendeVedtak =
            repository.hentVedtak(klage.id)
                ?: throw NotFoundException("Fant ikke vedtak for klage med id=${klage.id}")

        sjekkAttestantHarAnnenIdentEnnDenSomFattet(eksisterendeVedtak, brukerTokenInfo)
        verifiserGyldigVedtakStatus(eksisterendeVedtak.status, listOf(VedtakStatus.FATTET_VEDTAK))

        val attestertVedtak =
            repository.attesterVedtak(
                klage.id,
                Attestasjon(brukerTokenInfo.ident(), klage.sak.enhet, Tidspunkt.now()),
            )
        try {
            rapidService.sendToRapid(vedtakOgRapidAttestert(attestertVedtak))
        } catch (e: Exception) {
            logger.error(
                "Kan ikke sende attestert vedtak på kafka for behandling id: ${attestertVedtak.behandlingId}, " +
                    "vedtak: ${attestertVedtak.id}, " +
                    "saknr: ${attestertVedtak.sakId}. " +
                    "Det betyr at vi ikke får sendt ut vedtaksbrev. " +
                    "Denne hendelsen må sendes ut manuelt straks.",
                e,
            )
            throw e
        }
        return attestertVedtak
    }

    fun underkjennVedtak(klageId: UUID): Vedtak {
        logger.info("Underkjenner vedtak for klage=$klageId")
        val eksisterendeVedtak =
            repository.hentVedtak(klageId)
                ?: throw NotFoundException("Fant ikke vedtak for klage med id=$klageId")

        verifiserGyldigVedtakStatus(eksisterendeVedtak.status, listOf(VedtakStatus.FATTET_VEDTAK))
        val vedtak = repository.underkjennVedtak(klageId)

        rapidService.sendToRapid(vedtakOgRapidUnderkjent(vedtak))
        return vedtak
    }

    private fun vedtakOgRapidFattet(fattetVedtak: Vedtak) =
        VedtakOgRapid(
            fattetVedtak.toDto(),
            RapidInfo(
                vedtakhendelse = VedtakKafkaHendelseHendelseType.FATTET,
                vedtak = fattetVedtak.toDto(),
                tekniskTid = fattetVedtak.vedtakFattet!!.tidspunkt,
                behandlingId = fattetVedtak.behandlingId,
            ),
        )

    private fun vedtakOgRapidAttestert(attestertVedtak: Vedtak): VedtakOgRapid =
        VedtakOgRapid(
            attestertVedtak.toDto(),
            RapidInfo(
                vedtakhendelse = VedtakKafkaHendelseHendelseType.ATTESTERT,
                vedtak = attestertVedtak.toDto(),
                tekniskTid = attestertVedtak.attestasjon!!.tidspunkt,
                behandlingId = attestertVedtak.behandlingId,
                extraParams = mapOf(SKAL_SENDE_BREV to true),
            ),
        )

    private fun vedtakOgRapidUnderkjent(vedtak: Vedtak) =
        VedtakOgRapid(
            vedtak.toDto(),
            RapidInfo(
                vedtakhendelse = VedtakKafkaHendelseHendelseType.UNDERKJENT,
                vedtak = vedtak.toDto(),
                tekniskTid = Tidspunkt.now(),
                behandlingId = vedtak.behandlingId,
            ),
        )

    private fun verifiserGyldigVedtakStatus(
        gjeldendeStatus: VedtakStatus,
        forventetStatus: List<VedtakStatus>,
    ) {
        if (gjeldendeStatus !in forventetStatus) throw VedtakTilstandException(gjeldendeStatus, forventetStatus)
    }

    private fun sjekkAttestantHarAnnenIdentEnnDenSomFattet(
        vedtak: Vedtak,
        attestant: BrukerTokenInfo,
    ) {
        if (attestant.erSammePerson(vedtak.vedtakFattet!!.ansvarligSaksbehandler)) {
            throw UgyldigAttestantException(attestant.ident())
        }
    }
}
