package no.nav.etterlatte.Grunnlag

import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.channels.SendChannel
import no.nav.etterlatte.Context
import no.nav.etterlatte.DatabaseKontekst
import no.nav.etterlatte.Kontekst
import no.nav.etterlatte.grunnlag.*
import no.nav.etterlatte.libs.common.behandling.opplysningstyper.Opplysningstyper
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import org.junit.jupiter.api.Assertions
import java.sql.Connection

import java.util.*

internal class RealGrunnlagServiceTest {

    //@BeforeEach
    fun before(){
        Kontekst.set(Context(mockk(), object:DatabaseKontekst{
            override fun activeTx(): Connection {
                throw IllegalArgumentException()
            }
            override fun <T> inTransaction(block: () -> T): T {
                return block()
            }
        }))
    }

    //@Test
    fun hentGrunnlag() {
        val GrunnlagerMock = mockk<GrunnlagDao>()
        val opplysningerMock = mockk<OpplysningDao>()

        val sut = RealGrunnlagService(GrunnlagerMock, opplysningerMock, GrunnlagFactory(GrunnlagerMock, opplysningerMock) , mockk())

        val id = UUID.randomUUID()

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
        //TODO finne ut av ID greier
        every { GrunnlagerMock.hent(3) } returns Grunnlag(id, 1, emptyList())
        every { opplysningerMock.finnOpplysningerIGrunnlag(3) } returns opplysninger
        Assertions.assertEquals(2, sut.hentGrunnlag(3).grunnlag.size)
    }

    //@Test
    fun startGrunnlag() {
        val GrunnlagerMock = mockk<GrunnlagDao>()
        val GrunnlagOpprettes = slot<Grunnlag>()
        val GrunnlagHentes = slot<Long>()
        val opplysningerHentes = slot<Long>()
        val opplysningerMock = mockk<OpplysningDao>()

        val hendelseskanal = mockk<SendChannel<Pair<Long, GrunnlagHendelserType>>>()
        val hendelse = slot<Pair<Long, GrunnlagHendelserType>>()
        val opprettetGrunnlag = Grunnlag(UUID.randomUUID(), 1, emptyList())

        val sut = RealGrunnlagService(GrunnlagerMock, opplysningerMock, GrunnlagFactory(GrunnlagerMock, opplysningerMock), hendelseskanal)

        every { GrunnlagerMock.opprett(capture(GrunnlagOpprettes)) } returns Unit
        every { GrunnlagerMock.hent(capture(GrunnlagHentes)) } returns opprettetGrunnlag
        every { opplysningerMock.finnOpplysningerIGrunnlag(capture(opplysningerHentes)) } returns emptyList()
        coEvery { hendelseskanal.send(capture(hendelse)) } returns Unit

        val resultat = sut.opprettGrunnlag(1, emptyList())

        Assertions.assertEquals(opprettetGrunnlag, resultat)
        Assertions.assertEquals(1, GrunnlagOpprettes.captured.id)
        Assertions.assertEquals(GrunnlagHentes.captured, GrunnlagOpprettes.captured.id)
        Assertions.assertEquals(opplysningerHentes.captured, GrunnlagOpprettes.captured.id)
        Assertions.assertEquals(resultat.id, hendelse.captured.first)
        Assertions.assertEquals(GrunnlagHendelserType.OPPRETTET, hendelse.captured.second)
    }



}

fun mockChannel() = mockk<SendChannel<Pair<Long, GrunnlagHendelserType>>>().apply { coEvery { send(any()) } returns Unit }
