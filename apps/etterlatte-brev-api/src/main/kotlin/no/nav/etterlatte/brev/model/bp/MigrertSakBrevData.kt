package no.nav.etterlatte.brev.model.bp

import no.nav.etterlatte.brev.behandling.GenerellBrevData
import no.nav.etterlatte.brev.behandling.Utbetalingsinfo
import no.nav.etterlatte.brev.model.BrevData
import no.nav.etterlatte.libs.common.Vedtaksloesning

data class MigrertSakBrevData(
    val utbetaltBeloepPesys: Int,
    val utbetaltBeloepGjenny: Int,
    val anvendtTrygdetid: Int,
) : BrevData() {
    companion object {
        fun fra(
            generellBrevData: GenerellBrevData,
            utbetalingsinfo: Utbetalingsinfo,
        ): MigrertSakBrevData {
            assert(generellBrevData.systemkilde == Vedtaksloesning.PESYS) {
                "Kan ikke lage et migreringsbrev hvis kilden ikke er pesys"
            }

            return MigrertSakBrevData(
                utbetaltBeloepPesys = 0, // TODO: hent dette fra migrering
                utbetaltBeloepGjenny = utbetalingsinfo.beloep.value,
                anvendtTrygdetid = utbetalingsinfo.beregningsperioder.first().trygdetid,
            )
        }
    }
}
