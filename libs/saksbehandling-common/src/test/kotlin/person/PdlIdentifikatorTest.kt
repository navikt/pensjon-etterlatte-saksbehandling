package no.nav.etterlatte.person

import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.person.NavPersonIdent
import no.nav.etterlatte.libs.common.person.PdlIdentifikator
import no.nav.etterlatte.libs.common.toJson
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import tools.jackson.module.kotlin.readValue

class PdlIdentifikatorTest {
    @Test
    fun `kan serialisere og deserialisere PdlIdentifikator som er Npid`() {
        val npid = PdlIdentifikator.Npid(NavPersonIdent("12314512345"))
        val npidJson = npid.toJson()

        val npid2: PdlIdentifikator = objectMapper.readValue(npidJson)
        Assertions.assertEquals(npid, npid2)
    }
}
