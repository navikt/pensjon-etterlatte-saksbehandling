package grunnlag

import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.module.kotlin.readValue
import io.mockk.every
import io.mockk.mockk
import no.nav.etterlatte.Context
import no.nav.etterlatte.Kontekst
import no.nav.etterlatte.Self
import no.nav.etterlatte.grunnlag.BehandlingEndretHendlese
import no.nav.etterlatte.grunnlag.GrunnlagFactory
import no.nav.etterlatte.grunnlag.OpplysningDao
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Opplysningstyper
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.toJson
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import testutils.TestDbKontekst
import java.io.FileNotFoundException
import java.util.*

internal class BehandlingEndretHendleseTest{
    companion object {
        val melding = readFile("/behandlinggrunnlagendret.json")
        val opplysningerMock = mockk<OpplysningDao>()
        fun readFile(file: String) = Companion::class.java.getResource(file)?.readText()
            ?: throw FileNotFoundException("Fant ikke filen $file")
    }


    private val inspector = TestRapid().apply { BehandlingEndretHendlese(this, GrunnlagFactory(opplysningerMock)) }

    @Test
    fun `skal legge på grunnlag når behandling er endret`() {
        Kontekst.set(Context(Self("testApp"), TestDbKontekst))

        val opplysninger = listOf(
            Grunnlagsopplysning(
                UUID.randomUUID(),
                Grunnlagsopplysning.Saksbehandler("S01"),
                Opplysningstyper.SOEKER_SOEKNAD_V1,
                objectMapper.createObjectNode(),
                objectMapper.createObjectNode()
            ),
            Grunnlagsopplysning(
                UUID.randomUUID(),
                Grunnlagsopplysning.Saksbehandler("S01"),
                Opplysningstyper.AVDOED_SOEKNAD_V1,
                objectMapper.createObjectNode(),
                objectMapper.createObjectNode()
            ),
        )

        every { opplysningerMock.finnOpplysningerIGrunnlag(any())} returns opplysninger
        every { opplysningerMock.leggOpplysningTilGrunnlag(any(),any())} returns Unit
        val inspector = inspector.apply { sendTestMessage(melding) }.inspektør


        assertEquals(1, inspector.size)
        assertEquals(2,  inspector.message(0).get("grunnlag").size())
        val grunnlag = objectMapper.readValue<List<Grunnlagsopplysning<ObjectNode>>>(inspector.message(0).get("grunnlag").toJson())
        assertEquals(2, grunnlag.size)
        assertEquals(opplysninger[0].id, grunnlag.find { it.opplysningType == Opplysningstyper.SOEKER_SOEKNAD_V1 }?.id)
        assertEquals(opplysninger[1].id, grunnlag.find { it.opplysningType == Opplysningstyper.AVDOED_SOEKNAD_V1 }?.id)

    }
}