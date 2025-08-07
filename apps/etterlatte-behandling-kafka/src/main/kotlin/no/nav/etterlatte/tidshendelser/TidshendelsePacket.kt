package no.nav.etterlatte.tidshendelser

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import no.nav.etterlatte.libs.tidshendelser.JobbType
import no.nav.etterlatte.rapidsandrivers.DRYRUN
import no.nav.etterlatte.rapidsandrivers.HENDELSE_DATA_KEY
import no.nav.etterlatte.rapidsandrivers.TIDSHENDELSE_ID_KEY
import no.nav.etterlatte.rapidsandrivers.TIDSHENDELSE_TYPE_KEY
import no.nav.etterlatte.rapidsandrivers.asUUID
import no.nav.etterlatte.rapidsandrivers.dato
import no.nav.etterlatte.rapidsandrivers.sakId
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
    val jobbtype = JobbType.valueOf(packet[TIDSHENDELSE_TYPE_KEY].asText())
    val hendelseId: String = packet[TIDSHENDELSE_ID_KEY].asText()
}
