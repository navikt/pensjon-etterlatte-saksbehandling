package no.nav.etterlatte.vedtaksvurdering

import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.libs.common.behandling.BehandlingType
import no.nav.etterlatte.libs.common.behandling.DetaljertBehandling
import no.nav.etterlatte.libs.common.behandling.RevurderingAarsak
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.feilhaandtering.IkkeTillattException
import no.nav.etterlatte.libs.common.oppgave.VedtakEndringDTO
import no.nav.etterlatte.libs.common.oppgave.VedtakOppgaveDTO
import no.nav.etterlatte.libs.common.person.Folkeregisteridentifikator
import no.nav.etterlatte.libs.common.rapidsandrivers.EVENT_NAME_KEY
import no.nav.etterlatte.libs.common.rapidsandrivers.REVURDERING_AARSAK
import no.nav.etterlatte.libs.common.rapidsandrivers.SKAL_SENDE_BREV
import no.nav.etterlatte.libs.common.rapidsandrivers.TEKNISK_TID_KEY
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.common.tidspunkt.toLocalDatetimeUTC
import no.nav.etterlatte.libs.common.tidspunkt.utcKlokke
import no.nav.etterlatte.libs.common.toObjectNode
import no.nav.etterlatte.libs.common.vedtak.Attestasjon
import no.nav.etterlatte.libs.common.vedtak.Periode
import no.nav.etterlatte.libs.common.vedtak.Utbetalingsperiode
import no.nav.etterlatte.libs.common.vedtak.UtbetalingsperiodeType
import no.nav.etterlatte.libs.common.vedtak.VedtakFattet
import no.nav.etterlatte.libs.common.vedtak.VedtakKafkaHendelseType
import no.nav.etterlatte.libs.common.vedtak.VedtakStatus
import no.nav.etterlatte.libs.common.vedtak.VedtakType
import no.nav.etterlatte.libs.common.vilkaarsvurdering.VilkaarsvurderingDto
import no.nav.etterlatte.libs.common.vilkaarsvurdering.VilkaarsvurderingUtfall
import no.nav.etterlatte.rapidsandrivers.migrering.KILDE_KEY
import no.nav.etterlatte.token.BrukerTokenInfo
import no.nav.etterlatte.vedtaksvurdering.klienter.BehandlingKlient
import no.nav.etterlatte.vedtaksvurdering.klienter.BeregningKlient
import no.nav.etterlatte.vedtaksvurdering.klienter.VilkaarsvurderingKlient
import no.nav.helse.rapids_rivers.JsonMessage
import org.slf4j.LoggerFactory
import java.util.UUID

class VedtaksvurderingService(
    private val repository: VedtaksvurderingRepository,
) {
    private val logger = LoggerFactory.getLogger(VedtaksvurderingService::class.java)

    fun hentVedtak(vedtakId: Long): Vedtak? {
        logger.info("Henter vedtak med id=$vedtakId")
        return repository.hentVedtak(vedtakId)
    }

    fun hentVedtak(behandlingId: UUID): Vedtak? {
        logger.info("Henter vedtak for behandling med behandlingId=$behandlingId")
        return repository.hentVedtak(behandlingId)
    }
}

class VedtakTilstandException(gjeldendeStatus: VedtakStatus, forventetStatus: List<VedtakStatus>) :
    Exception("Vedtak har status $gjeldendeStatus, men forventet status $forventetStatus")

class BehandlingstilstandException(vedtak: Vedtak) :
    IllegalStateException("Statussjekk for behandling ${vedtak.behandlingId} feilet")

class OpphoersrevurderingErIkkeOpphoersvedtakException(revurderingAarsak: RevurderingAarsak?, vedtakType: VedtakType) :
    IllegalStateException(
        "Vedtaket er av type $vedtakType, men dette er " +
            "ikke gyldig for revurderingen med årsak $revurderingAarsak",
    )

class UgyldigAttestantException(ident: String) :
    IkkeTillattException(
        code = "ATTESTANT_OG_SAKSBEHANDLER_ER_SAMME_PERSON",
        detail = "Saksbehandler og attestant må være to forskjellige personer (ident=$ident)",
    )
