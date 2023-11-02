package no.nav.etterlatte.trygdetid.kafka

import no.nav.etterlatte.libs.common.Vedtaksloesning
import no.nav.etterlatte.libs.common.rapidsandrivers.eventName
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.common.toJson
import no.nav.etterlatte.libs.common.trygdetid.DetaljertBeregnetTrygdetidResultat
import no.nav.etterlatte.libs.common.trygdetid.TrygdetidDto
import no.nav.etterlatte.libs.common.trygdetid.TrygdetidGrunnlagDto
import no.nav.etterlatte.libs.common.trygdetid.TrygdetidGrunnlagKildeDto
import no.nav.etterlatte.rapidsandrivers.migrering.MigreringRequest
import no.nav.etterlatte.rapidsandrivers.migrering.Migreringshendelser
import no.nav.etterlatte.rapidsandrivers.migrering.TRYGDETID_KEY
import no.nav.etterlatte.rapidsandrivers.migrering.Trygdetidsgrunnlag
import no.nav.etterlatte.rapidsandrivers.migrering.VILKAARSVURDERT_KEY
import no.nav.etterlatte.rapidsandrivers.migrering.hendelseData
import no.nav.etterlatte.trygdetid.TrygdetidType
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.RapidsConnection
import org.slf4j.LoggerFactory
import rapidsandrivers.BEHANDLING_ID_KEY
import rapidsandrivers.HENDELSE_DATA_KEY
import rapidsandrivers.behandlingId
import rapidsandrivers.migrering.ListenerMedLoggingOgFeilhaandtering
import java.util.UUID

internal class MigreringHendelserRiver(rapidsConnection: RapidsConnection, private val trygdetidService: TrygdetidService) :
    ListenerMedLoggingOgFeilhaandtering(Migreringshendelser.TRYGDETID) {
    private val logger = LoggerFactory.getLogger(this::class.java)

    init {
        initialiserRiver(rapidsConnection, Migreringshendelser.TRYGDETID) {
            validate { it.requireKey(BEHANDLING_ID_KEY) }
            validate { it.requireKey(VILKAARSVURDERT_KEY) }
            validate { it.requireKey(HENDELSE_DATA_KEY) }
        }
    }

    override fun haandterPakke(
        packet: JsonMessage,
        context: MessageContext,
    ) {
        val behandlingId = packet.behandlingId
        val request = packet.hendelseData

        logger.info("Mottatt trygdetid-migreringshendelse for behandling $behandlingId")
        trygdetidService.beregnTrygdetid(behandlingId)
        logger.info("Opprettet trygdetid for behandling $behandlingId, forsøker å legge til perioder")

        val oppdatertTrygdetid: TrygdetidDto =
            if (request.trygdetid.perioder.isNotEmpty()) {
                try {
                    request.trygdetid.perioder.map { periode ->
                        val grunnlag = tilGrunnlag(periode)
                        logger.info(
                            "Forsøker å legge til periode ${grunnlag.periodeFra}-${grunnlag.periodeTil} for behandling $behandlingId",
                        )
                        trygdetidService.beregnTrygdetidGrunnlag(behandlingId, grunnlag)
                    }.last()
                } catch (e: Exception) {
                    logger.warn("Klarte ikke legge til perioder fra Pesys for behandling $behandlingId", e)
                    overstyrBeregnetTrygdetidNorge(request, behandlingId)
                }
            } else {
                logger.info("Vi mottok ingen trygdetidsperioder fra Pesys for behandling $behandlingId")
                overstyrBeregnetTrygdetidNorge(request, behandlingId)
            }

        sendBeregnetTrygdetid(packet, oppdatertTrygdetid, context, behandlingId)
    }

    private fun overstyrBeregnetTrygdetidNorge(
        request: MigreringRequest,
        behandlingId: UUID,
    ): TrygdetidDto =
        // TODO - EY-2602 - flere felter trengs - feks utland
        trygdetidService.overstyrBeregnetTrygdetid(
            behandlingId = behandlingId,
            beregnetTrygdetid =
                DetaljertBeregnetTrygdetidResultat.fraSamletTrygdetidNorge(
                    request.beregning.anvendtTrygdetid,
                ).copy(overstyrt = true),
        ).also { logger.warn("Trygdetid for behandling $behandlingId ble overstyrt med anvendt norsk tt fra Pesys") }

    private fun sendBeregnetTrygdetid(
        packet: JsonMessage,
        beregnetTrygdetid: TrygdetidDto,
        context: MessageContext,
        behandlingId: UUID,
    ) {
        packet[TRYGDETID_KEY] = beregnetTrygdetid.toJson()
        packet.eventName = Migreringshendelser.BEREGN
        context.publish(packet.toJson())
        logger.info(
            "Publiserte oppdatert migreringshendelse fra trygdetid for behandling $behandlingId",
        )
    }

    private fun tilGrunnlag(trygdetidsgrunnlag: Trygdetidsgrunnlag) =
        TrygdetidGrunnlagDto(
            id = null,
            type = TrygdetidType.FAKTISK.name,
            bosted = trygdetidsgrunnlag.landTreBokstaver,
            periodeFra = trygdetidsgrunnlag.datoFom.toLocalDate(),
            periodeTil = trygdetidsgrunnlag.datoTom.toLocalDate(),
            kilde =
                TrygdetidGrunnlagKildeDto(
                    tidspunkt = Tidspunkt.now().toString(),
                    ident = Vedtaksloesning.PESYS.name,
                ),
            beregnet = null,
            begrunnelse = "Migrert fra pesys",
            poengInnAar = trygdetidsgrunnlag.poengIInnAar,
            poengUtAar = trygdetidsgrunnlag.poengIUtAar,
            prorata = !trygdetidsgrunnlag.ikkeIProrata,
        )
}
