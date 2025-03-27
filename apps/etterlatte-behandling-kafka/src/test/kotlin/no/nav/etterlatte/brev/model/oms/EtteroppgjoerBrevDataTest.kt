package no.nav.etterlatte.brev.model.oms

import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.etterlatte.brev.BrevFastInnholdData
import no.nav.etterlatte.libs.common.Enhetsnummer
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.sak.Sak
import no.nav.etterlatte.libs.common.sak.SakId
import no.nav.etterlatte.libs.common.toJson
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertInstanceOf

class EtteroppgjoerBrevDataTest {
    @Test
    fun `a`() {
        val brevData =
            EtteroppgjoerBrevData.VarselTilbakekreving(
                Sak(
                    ident = "",
                    sakType = SakType.OMSTILLINGSSTOENAD,
                    id = SakId(sakId = 0),
                    enhet = Enhetsnummer(enhetNr = "0001"),
                ),
            )

        val json = brevData.toJson()
        val gjenskapt = objectMapper.readValue<BrevFastInnholdData>(json)
        assertInstanceOf<EtteroppgjoerBrevData.VarselTilbakekreving>(gjenskapt)

        println(brevData)
    }
}
