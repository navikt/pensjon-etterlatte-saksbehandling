package no.nav.etterlatte.behandling.klage

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import no.nav.etterlatte.behandling.hendelse.HendelseDao
import no.nav.etterlatte.libs.common.behandling.Klage
import no.nav.etterlatte.libs.common.behandling.KlageStatus
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.feilhaandtering.IkkeTillattException
import no.nav.etterlatte.libs.common.klage.AarsakTilAvbrytelse
import no.nav.etterlatte.libs.common.klage.KlageHendelseType
import no.nav.etterlatte.libs.common.oppgave.OppgaveIntern
import no.nav.etterlatte.libs.common.person.Folkeregisteridentifikator
import no.nav.etterlatte.libs.common.sak.Sak
import no.nav.etterlatte.libs.testdata.grunnlag.GrunnlagTestData
import no.nav.etterlatte.oppgave.OppgaveService
import no.nav.etterlatte.token.Saksbehandler
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import java.util.UUID

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class KlageServiceTest {
    private lateinit var service: KlageService

    private val saksbehandler = Saksbehandler("token", "ident", null)
    private val enhet = "1337"

    private val klageDaoMock = mockk<KlageDao>()
    private val oppgaveServiceMock = mockk<OppgaveService>()
    private val hendelseDaoMock = mockk<HendelseDao>()
    private val klageHendelserServiceMock = mockk<IKlageHendelserService>()

    @BeforeEach
    fun setUp() {
        service =
            KlageServiceImpl(
                klageDao = klageDaoMock,
                sakDao = mockk(),
                hendelseDao = hendelseDaoMock,
                oppgaveService = oppgaveServiceMock,
                brevApiKlient = mockk(),
                klageKlient = mockk(),
                klageHendelser = klageHendelserServiceMock,
            )
        every { hendelseDaoMock.klageHendelse(any(), any(), any(), any(), any(), any(), any()) } returns Unit
        every { klageHendelserServiceMock.sendKlageHendelseRapids(any(), any()) } returns Unit
        every { oppgaveServiceMock.avbrytOppgaveUnderBehandling(any(), any()) } returns mockk<OppgaveIntern>()
    }

    @Test
    fun `hent klage test`() {
        val klageid = UUID.randomUUID()
        every { klageDaoMock.hentKlage(klageid) } returns null
        val hentKlage = service.hentKlage(klageid)
        assertNull(hentKlage)
    }

    @Test
    fun `avbryt klage avbryter både klagen og oppgaven`() {
        val klageid = UUID.randomUUID()
        val eksisterendeKlage = klage(klageid)

        val oppdatertKlageSlot = slot<Klage>()
        every { klageDaoMock.lagreKlage(capture(oppdatertKlageSlot)) } returns Unit
        every { klageDaoMock.hentKlage(klageid) } returns eksisterendeKlage

        service.avbrytKlage(klageid, AarsakTilAvbrytelse.FEILREGISTRERT, "Fordi jeg vil", saksbehandler)

        oppdatertKlageSlot.captured.status shouldBe KlageStatus.AVBRUTT
        oppdatertKlageSlot.captured.aarsakTilAvbrytelse shouldBe AarsakTilAvbrytelse.FEILREGISTRERT

        verify { oppgaveServiceMock.avbrytOppgaveUnderBehandling(klageid.toString(), saksbehandler) }
        verify {
            hendelseDaoMock.klageHendelse(
                klageid,
                eksisterendeKlage.sak.id,
                KlageHendelseType.AVBRUTT,
                any(),
                any(),
                "Fordi jeg vil",
                AarsakTilAvbrytelse.FEILREGISTRERT.name,
            )
        }
    }

    @ParameterizedTest
    @EnumSource(
        KlageStatus::class,
        names = [
            "OPPRETTET",
            "FORMKRAV_OPPFYLT",
            "FORMKRAV_IKKE_OPPFYLT",
            "UTFALL_VURDERT",
        ],
        mode = EnumSource.Mode.EXCLUDE,
    )
    fun `avbryt klage feiler hvis ulovlig status`(statusUnderTest: KlageStatus) {
        val klageid = UUID.randomUUID()
        val eksisterendeKlage = klage(klageid, statusUnderTest)

        val oppdatertKlageSlot = slot<Klage>()
        every { klageDaoMock.lagreKlage(capture(oppdatertKlageSlot)) } returns Unit
        every { klageDaoMock.hentKlage(klageid) } returns eksisterendeKlage

        shouldThrow<IkkeTillattException> {
            service.avbrytKlage(klageid, AarsakTilAvbrytelse.FEILREGISTRERT, "Fordi jeg vil", saksbehandler)
        }
    }

    private fun klage(
        klageid: UUID,
        status: KlageStatus = KlageStatus.OPPRETTET,
    ) = Klage(
        klageid,
        omsSak(GrunnlagTestData().gjenlevende.foedselsnummer),
        mockk(),
        status,
        mockk(),
        mockk(),
        mockk(),
        mockk(),
        mockk(),
        mockk(),
        mockk(),
        mockk(),
    )

    fun omsSak(fnr: Folkeregisteridentifikator): Sak {
        val sak = Sak(fnr.value, SakType.OMSTILLINGSSTOENAD, 1L, enhet)
        return sak
    }
}
