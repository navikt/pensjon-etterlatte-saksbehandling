package trygdetid.regulering

import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.slot
import kotlinx.coroutines.runBlocking
import kotliquery.TransactionalSession
import no.nav.etterlatte.libs.common.behandling.DetaljertBehandling
import no.nav.etterlatte.trygdetid.BeregnetTrygdetid
import no.nav.etterlatte.trygdetid.Opplysningsgrunnlag
import no.nav.etterlatte.trygdetid.Trygdetid
import no.nav.etterlatte.trygdetid.TrygdetidBeregningService
import no.nav.etterlatte.trygdetid.TrygdetidGrunnlag
import no.nav.etterlatte.trygdetid.TrygdetidRepository
import no.nav.etterlatte.trygdetid.klienter.BehandlingKlient
import no.nav.etterlatte.trygdetid.regulering.RegulerTrygdetidService
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import trygdetid.saksbehandler
import java.util.UUID.randomUUID

internal class RegulerTrygdetidServiceTest {

    private val repository: TrygdetidRepository = mockk()
    private val behandlingKlient: BehandlingKlient = mockk()
    private val service = RegulerTrygdetidService(repository, behandlingKlient, TrygdetidBeregningService)

    @BeforeEach
    fun beforeEach() {
        clearAllMocks()
        mockkObject(TrygdetidBeregningService)
        coEvery { behandlingKlient.kanBeregnes(any(), any()) } returns true
    }

    @AfterEach
    fun afterEach() {
        confirmVerified()
    }

    @Test
    fun `skal opprette ny trygdetid til regulering basert paa grunnlag til forrige behandling`() {
        val behandlingId = randomUUID()
        val forrigeBehandlingId = randomUUID()
        val forrigeTrygdetid = mockk<Trygdetid>()
        val forrigeTrygdetidGrunnlag = listOf(mockk<TrygdetidGrunnlag>(), mockk())
        val forrigeTrygdetidOpplysninger = mockk<List<Opplysningsgrunnlag>>()
        val regulering = mockk<DetaljertBehandling>()
        val beregnetTrygdetid = mockk<BeregnetTrygdetid>()

        val transactionSlot = slot<(TransactionalSession) -> Trygdetid>()
        val tx: TransactionalSession = mockk()

        every { repository.hentTrygdetid(forrigeBehandlingId) } returns forrigeTrygdetid
        every { forrigeTrygdetid.trygdetidGrunnlag } returns forrigeTrygdetidGrunnlag
        every { forrigeTrygdetid.opplysninger } returns forrigeTrygdetidOpplysninger
        coEvery { behandlingKlient.hentBehandling(behandlingId, saksbehandler) } returns regulering
        every { repository.opprettTrygdetid(regulering, forrigeTrygdetidOpplysninger, tx) } returns mockk()
        every { repository.opprettTrygdetidGrunnlag(any(), any(), any()) } returns mockk()
        every { TrygdetidBeregningService.beregnTrygdetid(forrigeTrygdetidGrunnlag) } returns beregnetTrygdetid
        every { repository.oppdaterBeregnetTrygdetid(any(), beregnetTrygdetid, tx) } returns mockk()
        coEvery { behandlingKlient.settBehandlingStatusVilkaarsvurdert(behandlingId, saksbehandler) } returns mockk()
        every { repository.transaction(capture(transactionSlot)) } answers { transactionSlot.captured.invoke(tx) }

        runBlocking {
            service.regulerTrygdetid(behandlingId, forrigeBehandlingId, saksbehandler)
        }

        coVerify(exactly = 1) {
            repository.hentTrygdetid(forrigeBehandlingId)
            forrigeTrygdetid.trygdetidGrunnlag
            forrigeTrygdetid.opplysninger
            behandlingKlient.hentBehandling(behandlingId, saksbehandler)
            repository.opprettTrygdetid(regulering, forrigeTrygdetidOpplysninger, tx)
            repository.opprettTrygdetidGrunnlag(behandlingId, forrigeTrygdetidGrunnlag[0], tx)
            repository.opprettTrygdetidGrunnlag(behandlingId, forrigeTrygdetidGrunnlag[1], tx)
            repository.oppdaterBeregnetTrygdetid(behandlingId, beregnetTrygdetid, tx)
            behandlingKlient.settBehandlingStatusVilkaarsvurdert(behandlingId, saksbehandler)
            TrygdetidBeregningService.beregnTrygdetid(forrigeTrygdetidGrunnlag)
            repository.transaction(any<(TransactionalSession) -> Trygdetid>())
        }
    }
}