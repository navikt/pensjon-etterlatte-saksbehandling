package trygdetid

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.token.Saksbehandler
import no.nav.etterlatte.trygdetid.TrygdetidRepository
import no.nav.etterlatte.trygdetid.TrygdetidService
import no.nav.etterlatte.trygdetid.klienter.BehandlingKlient
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.util.UUID.randomUUID

internal class TrygdetidServiceTest {

    private val repository: TrygdetidRepository = mockk()
    private val behandlingKlient: BehandlingKlient = mockk()
    private val service = TrygdetidService(repository, behandlingKlient)

    @BeforeEach
    fun beforeEach() {
        coEvery { behandlingKlient.fastsettTrygdetid(any(), any(), false) } returns true
    }

    @AfterEach
    fun afterEach() {
        confirmVerified()
    }

    @Test
    fun `skal hente trygdetid`() {
        val behandlingId = randomUUID()
        every { repository.hentTrygdetid(any()) } returns trygdetid(behandlingId)

        val trygdetid = service.hentTrygdetid(behandlingId)

        trygdetid shouldNotBe null
    }

    @Test
    fun `skal returnere null hvis trygdetid ikke finnes for behandling`() {
        val behandlingId = randomUUID()
        every { repository.hentTrygdetid(any()) } returns null

        val trygdetid = service.hentTrygdetid(behandlingId)

        trygdetid shouldBe null
    }

    @Test
    fun `skal opprette trygdetid`() {
        val behandlingId = randomUUID()
        every { repository.hentTrygdetid(any()) } returns null
        every { repository.opprettTrygdetid(any()) } returns trygdetid(behandlingId)

        runBlocking {
            service.opprettTrygdetid(behandlingId, Saksbehandler("token", "ident"))
        }

        verify { repository.hentTrygdetid(any()) }
        verify { repository.opprettTrygdetid(any()) }
        coVerify { behandlingKlient.fastsettTrygdetid(any(), any(), any()) }
    }

    @Test
    fun `skal feile ved opprettelse av trygdetid naar det allerede finnes for behandling`() {
        val behandlingId = randomUUID()
        every { repository.hentTrygdetid(any()) } returns trygdetid(behandlingId)

        runBlocking {
            assertThrows<IllegalArgumentException> {
                service.opprettTrygdetid(behandlingId, Saksbehandler("token", "ident"))
            }
        }

        verify { repository.hentTrygdetid(any()) }
        coVerify { behandlingKlient.fastsettTrygdetid(any(), any(), any()) }
    }

    @Test
    fun `skal feile ved opprettelse av trygdetid dersom behandling er i feil tilstand`() {
        val behandlingId = randomUUID()
        coEvery { behandlingKlient.fastsettTrygdetid(any(), any(), false) } returns false

        runBlocking {
            assertThrows<Exception> {
                service.opprettTrygdetid(behandlingId, Saksbehandler("token", "ident"))
            }
        }

        coVerify { behandlingKlient.fastsettTrygdetid(any(), any(), any()) }
    }

    @Test
    fun `skal lagre trygdetidsgrunnlag`() {
        val behandlingId = randomUUID()
        val trygdetid = trygdetid(behandlingId)
        val trygdetidGrunnlag = trygdetidGrunnlag(trygdetid.id)
        every { repository.opprettTrygdetidGrunnlag(any(), any()) } returns trygdetid(behandlingId)

        runBlocking {
            service.lagreTrygdetidGrunnlag(
                behandlingId,
                Saksbehandler("token", "ident"),
                trygdetidGrunnlag
            )
        }

        coVerify { behandlingKlient.fastsettTrygdetid(any(), any(), any()) }
        coVerify { repository.opprettTrygdetidGrunnlag(any(), any()) }
    }

    @Test
    fun `skal feile ved lagring av trygdetidsgrunnlag hvis behandling er i feil tilstand`() {
        val behandlingId = randomUUID()
        val trygdetid = trygdetid(behandlingId)
        val trygdetidGrunnlag = trygdetidGrunnlag(trygdetid.id)
        coEvery { behandlingKlient.fastsettTrygdetid(any(), any(), false) } returns false

        runBlocking {
            assertThrows<Exception> {
                service.lagreTrygdetidGrunnlag(
                    behandlingId,
                    Saksbehandler("token", "ident"),
                    trygdetidGrunnlag
                )
            }
        }

        coVerify { behandlingKlient.fastsettTrygdetid(any(), any(), any()) }
    }

    @Test
    fun `skal lagre beregnet trygdetid`() {
        val behandlingId = randomUUID()
        val beregnetTrygdetid = beregnetTrygdetid(10, 10, 20)
        every { repository.oppdaterBeregnetTrygdetid(any(), any()) } returns trygdetid(behandlingId, beregnetTrygdetid)
        coEvery { behandlingKlient.fastsettTrygdetid(any(), any(), true) } returns true

        runBlocking {
            service.lagreBeregnetTrygdetid(
                behandlingId,
                Saksbehandler("token", "ident"),
                beregnetTrygdetid
            )
        }
        coVerifyOrder {
            behandlingKlient.fastsettTrygdetid(any(), any(), false)
            behandlingKlient.fastsettTrygdetid(any(), any(), true)
            repository.oppdaterBeregnetTrygdetid(any(), any())
        }
    }

    @Test
    fun `skal feile ved lagring av beregnet trygdetid hvis behandling er i feil tilstand`() {
        val behandlingId = randomUUID()
        val beregnetTrygdetid = beregnetTrygdetid(10, 10, 20)
        coEvery { behandlingKlient.fastsettTrygdetid(any(), any(), false) } returns false

        runBlocking {
            assertThrows<Exception> {
                service.lagreBeregnetTrygdetid(
                    behandlingId,
                    Saksbehandler("token", "ident"),
                    beregnetTrygdetid
                )
            }
        }

        coVerify {
            behandlingKlient.fastsettTrygdetid(any(), any(), false)
        }
    }
}