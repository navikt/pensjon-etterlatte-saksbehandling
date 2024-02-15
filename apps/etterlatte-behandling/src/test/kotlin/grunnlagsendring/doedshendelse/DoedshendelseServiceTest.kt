package no.nav.etterlatte.grunnlagsendring.doedshendelse

import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import no.nav.etterlatte.Context
import no.nav.etterlatte.DatabaseKontekst
import no.nav.etterlatte.Kontekst
import no.nav.etterlatte.common.klienter.PdlTjenesterKlient
import no.nav.etterlatte.funksjonsbrytere.FeatureToggleService
import no.nav.etterlatte.libs.common.pdl.OpplysningDTO
import no.nav.etterlatte.libs.common.pdlhendelse.Doedshendelse
import no.nav.etterlatte.libs.common.pdlhendelse.Endringstype
import no.nav.etterlatte.mockPerson
import no.nav.etterlatte.personOpplysning
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.sql.Connection
import java.time.LocalDate
import java.util.UUID

internal class DoedshendelseServiceTest {
    private val pdlTjenesterKlient = mockk<PdlTjenesterKlient>()
    private val dao = mockk<DoedshendelseDao>()
    private val toggle =
        mockk<FeatureToggleService> {
            every { isEnabled(any(), any()) } returns true
        }
    private val service =
        DoedshendelseService(
            pdlTjenesterKlient = pdlTjenesterKlient,
            doedshendelseDao = dao,
            featureToggleService = toggle,
        )

    private val avdoed =
        mockPerson().copy(
            doedsdato = OpplysningDTO(LocalDate.of(2022, 8, 17), null),
            avdoedesBarn =
                listOf(
                    personOpplysning(foedselsdato = LocalDate.of(2000, 1, 1)),
                    personOpplysning(foedselsdato = LocalDate.of(2002, 8, 30)),
                    personOpplysning().copy(foedselsaar = 2022),
                    personOpplysning(foedselsdato = LocalDate.of(2002, 9, 15)),
                    personOpplysning(foedselsdato = LocalDate.of(2005, 9, 15), doedsdato = LocalDate.of(2020, 8, 17)),
                    personOpplysning(foedselsdato = LocalDate.of(2020, 9, 15)),
                ),
        )

    @BeforeEach
    fun before() {
        Kontekst.set(
            Context(
                mockk(),
                object : DatabaseKontekst {
                    override fun activeTx(): Connection {
                        throw IllegalArgumentException()
                    }

                    override fun <T> inTransaction(block: () -> T): T {
                        return block()
                    }
                },
            ),
        )
    }

    @Test
    fun `Skal oppdatere doedshendelse med barna som kan ha rett paa barnepensjon ved doedsfall`() {
        every { pdlTjenesterKlient.hentPdlModell(avdoed.foedselsnummer.verdi.value, any(), any()) } returns avdoed
        every { dao.opprettDoedshendelse(any()) } just runs

        service.opprettDoedshendelseForBeroertePersoner(
            Doedshendelse(
                UUID.randomUUID().toString(),
                Endringstype.OPPRETTET,
                fnr = avdoed.foedselsnummer.verdi.value,
                doedsdato = avdoed.doedsdato!!.verdi,
            ),
        )

        coVerify(exactly = 3) {
            dao.opprettDoedshendelse(any())
        }
    }

    @Test
    fun `Skal ikke opprette doedshendelser dersom avdoed ikke er registert som avdoed i PDL`() {
        every { pdlTjenesterKlient.hentPdlModell(avdoed.foedselsnummer.verdi.value, any(), any()) } returns
            avdoed.copy(doedsdato = null)
        every { dao.opprettDoedshendelse(any()) } just runs

        service.opprettDoedshendelseForBeroertePersoner(
            Doedshendelse(
                UUID.randomUUID().toString(),
                Endringstype.OPPRETTET,
                fnr = avdoed.foedselsnummer.verdi.value,
                doedsdato = avdoed.doedsdato!!.verdi,
            ),
        )

        coVerify(exactly = 0) {
            dao.opprettDoedshendelse(any())
        }
    }
}
