package no.nav.etterlatte.trygdetid.kafka

import no.nav.etterlatte.libs.common.Vedtaksloesning
import no.nav.etterlatte.libs.common.rapidsandrivers.eventName
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.common.toJson
import no.nav.etterlatte.libs.common.trygdetid.DetaljertBeregnetTrygdetidResultat
import no.nav.etterlatte.libs.common.trygdetid.TrygdetidDto
import no.nav.etterlatte.libs.common.trygdetid.TrygdetidGrunnlagDto
import no.nav.etterlatte.libs.common.trygdetid.TrygdetidGrunnlagKildeDto
import no.nav.etterlatte.rapidsandrivers.migrering.Beregning
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

internal class MigreringTrygdetidHendelserRiver(
    rapidsConnection: RapidsConnection,
    private val trygdetidService: TrygdetidService,
) : ListenerMedLoggingOgFeilhaandtering() {
    private val logger = LoggerFactory.getLogger(this::class.java)

    init {
        initialiserRiver(rapidsConnection, Migreringshendelser.TRYGDETID.lagEventnameForType()) {
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
        logger.info("Opprettet trygdetid for behandling $behandlingId")

        val oppdatertTrygdetid: TrygdetidDto =
            if (request.dodAvYrkesskade) {
                logger.info("Avdød hadde yrkesskade i Pesys, oppretter yrkesskadegrunnlag for behandling $behandlingId")
                trygdetidService.opprettGrunnlagVedYrkesskade(behandlingId)
            } else if (request.anvendtFlyktningerfordel()) {
                logger.info("Avdød hadde flyktningerfordel i Pesys, overstyrer med samme trygdetid $behandlingId")
                overstyrBeregnetTrygdetid(request, behandlingId)
            } else if (request.trygdetid.perioder.isNotEmpty()) {
                logger.info("Mottok trygdetidsperioder for behandling $behandlingId")
                leggTilPerioder(request, behandlingId)
            } else {
                logger.info("Vi mottok ingen trygdetidsperioder fra Pesys for behandling $behandlingId")
                overstyrBeregnetTrygdetid(request, behandlingId)
            }

        sendBeregnetTrygdetid(packet, oppdatertTrygdetid, context, behandlingId)
    }

    private fun leggTilPerioder(
        request: MigreringRequest,
        behandlingId: UUID,
    ) = try {
        val trygdetidMedFremtidig =
            request.trygdetid.perioder.map { periode ->
                val grunnlag = tilGrunnlag(periode)
                logger.info(
                    "Forsøker å legge til periode ${grunnlag.periodeFra}-${grunnlag.periodeTil} for behandling $behandlingId",
                )
                trygdetidService.beregnTrygdetidGrunnlag(behandlingId, grunnlag)
            }.last()

        val trygdetid =
            if (!trygdetidIGjennyStemmerMedTrygdetidIPesys(trygdetidMedFremtidig, request.beregning)) {
                trygdetidService.reberegnUtenFremtidigTrygdetid(behandlingId)
            } else {
                trygdetidMedFremtidig
            }

        check(trygdetidIGjennyStemmerMedTrygdetidIPesys(trygdetid, request.beregning)) {
            "Beregnet trygdetid i Gjenny basert på perioder fra Pesys stemmer ikke med anvendt trygdetid i Pesys"
        }

        trygdetid
    } catch (e: Exception) {
        logger.warn("Klarte ikke legge til perioder fra Pesys for behandling $behandlingId", e)
        overstyrBeregnetTrygdetid(request, behandlingId)
    }

    private fun trygdetidIGjennyStemmerMedTrygdetidIPesys(
        trygdetid: TrygdetidDto,
        beregning: Beregning,
    ): Boolean =
        if (beregning.prorataBroek == null) {
            trygdetid.beregnetTrygdetid?.resultat?.prorataBroek == null &&
                trygdetid.beregnetTrygdetid?.resultat?.samletTrygdetidNorge == beregning.anvendtTrygdetid
        } else {
            trygdetid.beregnetTrygdetid?.resultat?.prorataBroek == beregning.prorataBroek &&
                trygdetid.beregnetTrygdetid?.resultat?.samletTrygdetidTeoretisk == beregning.anvendtTrygdetid
        }

    private fun overstyrBeregnetTrygdetid(
        request: MigreringRequest,
        behandlingId: UUID,
    ): TrygdetidDto {
        val beregnetTrygdetid =
            if (request.beregning.prorataBroek == null) {
                DetaljertBeregnetTrygdetidResultat.fraSamletTrygdetidNorge(request.beregning.anvendtTrygdetid)
            } else {
                DetaljertBeregnetTrygdetidResultat.fraSamletTrygdetidProrata(
                    request.beregning.anvendtTrygdetid,
                    request.beregning.prorataBroek,
                )
            }

        return trygdetidService.overstyrBeregnetTrygdetid(
            behandlingId = behandlingId,
            beregnetTrygdetid = beregnetTrygdetid,
        ).also { logger.warn("Trygdetid for behandling $behandlingId ble overstyrt med anvendt trygdetid fra Pesys") }
    }

    private fun sendBeregnetTrygdetid(
        packet: JsonMessage,
        beregnetTrygdetid: TrygdetidDto,
        context: MessageContext,
        behandlingId: UUID,
    ) {
        packet[TRYGDETID_KEY] = beregnetTrygdetid.toJson()
        packet.eventName = Migreringshendelser.BEREGN.lagEventnameForType()
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
