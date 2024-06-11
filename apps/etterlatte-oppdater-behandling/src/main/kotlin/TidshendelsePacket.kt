package no.nav.etterlatte

import no.nav.etterlatte.rapidsandrivers.ALDERSOVERGANG_ID_KEY
import no.nav.etterlatte.rapidsandrivers.ALDERSOVERGANG_TYPE_KEY
import no.nav.etterlatte.rapidsandrivers.DRYRUN
import no.nav.etterlatte.rapidsandrivers.HENDELSE_DATA_KEY
import no.nav.etterlatte.rapidsandrivers.asUUID
import no.nav.etterlatte.rapidsandrivers.dato
import no.nav.etterlatte.rapidsandrivers.sakId
import no.nav.helse.rapids_rivers.JsonMessage
import java.time.YearMonth

class TidshendelsePacket(
    packet: JsonMessage,
) {
    val sakId = packet.sakId
    val behandlingsmaaned: YearMonth = packet.dato.let { YearMonth.of(it.year, it.month) }
    val harLoependeYtelse = packet[HENDELSE_DATA_KEY]["loependeYtelse"]?.asBoolean() == true
    val behandlingId = packet[HENDELSE_DATA_KEY]["loependeYtelse_behandlingId"]?.asUUID()
    val harMigrertYrkesskadeFordel = packet["yrkesskadefordel_pre_20240101"].asBoolean()
    val harRettUtenTidsbegrensning = packet["oms_rett_uten_tidsbegrensning"].asBoolean()
    val dryrun = packet[DRYRUN].asBoolean()
    val jobbtype = TidshendelseService.TidshendelserJobbType.valueOf(packet[ALDERSOVERGANG_TYPE_KEY].asText())
    val hendelseId: String = packet[ALDERSOVERGANG_ID_KEY].asText()
}
