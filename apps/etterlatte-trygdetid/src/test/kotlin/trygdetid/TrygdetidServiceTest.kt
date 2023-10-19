package trygdetid

import com.fasterxml.jackson.databind.JsonNode
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import no.nav.etterlatte.libs.common.behandling.BehandlingType
import no.nav.etterlatte.libs.common.behandling.DetaljertBehandling
import no.nav.etterlatte.libs.common.behandling.Prosesstype
import no.nav.etterlatte.libs.common.behandling.Revurderingaarsak
import no.nav.etterlatte.libs.common.behandling.SisteIverksatteBehandling
import no.nav.etterlatte.libs.common.grunnlag.Grunnlag
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsdata
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import no.nav.etterlatte.libs.common.grunnlag.Opplysning
import no.nav.etterlatte.libs.common.grunnlag.hentDoedsdato
import no.nav.etterlatte.libs.common.grunnlag.hentFoedselsdato
import no.nav.etterlatte.libs.common.grunnlag.hentFoedselsnummer
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Opplysningstype
import no.nav.etterlatte.libs.common.person.Folkeregisteridentifikator
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.common.toJsonNode
import no.nav.etterlatte.libs.common.vilkaarsvurdering.VilkaarsvurderingDto
import no.nav.etterlatte.libs.testdata.grunnlag.GrunnlagTestData
import no.nav.etterlatte.trygdetid.LandNormalisert
import no.nav.etterlatte.trygdetid.ManglerForrigeTrygdetidMaaReguleresManuelt
import no.nav.etterlatte.trygdetid.Opplysningsgrunnlag
import no.nav.etterlatte.trygdetid.TrygdetidAlleredeOpprettetException
import no.nav.etterlatte.trygdetid.TrygdetidBeregningService
import no.nav.etterlatte.trygdetid.TrygdetidOpplysningType
import no.nav.etterlatte.trygdetid.TrygdetidPeriode
import no.nav.etterlatte.trygdetid.TrygdetidRepository
import no.nav.etterlatte.trygdetid.TrygdetidService
import no.nav.etterlatte.trygdetid.TrygdetidServiceImpl
import no.nav.etterlatte.trygdetid.TrygdetidType
import no.nav.etterlatte.trygdetid.klienter.BehandlingKlient
import no.nav.etterlatte.trygdetid.klienter.GrunnlagKlient
import no.nav.etterlatte.trygdetid.klienter.VilkaarsvuderingKlient
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.LocalDate
import java.time.Period
import java.util.UUID.randomUUID

internal class TrygdetidServiceTest {
    private val repository: TrygdetidRepository = mockk()
    private val behandlingKlient: BehandlingKlient = mockk()
    private val grunnlagKlient: GrunnlagKlient = mockk()
    private val vilkaarsvurderingKlient: VilkaarsvuderingKlient = mockk()
    private val beregningService: TrygdetidBeregningService = spyk(TrygdetidBeregningService)
    private val service: TrygdetidService =
        TrygdetidServiceImpl(
            repository,
            behandlingKlient,
            grunnlagKlient,
            vilkaarsvurderingKlient,
            beregningService,
        )

    @BeforeEach
    fun beforeEach() {
        clearAllMocks()
        coEvery { behandlingKlient.kanOppdatereTrygdetid(any(), any()) } returns true
    }

    @AfterEach
    fun afterEach() {
        confirmVerified()
    }

    @Test
    fun `skal hente trygdetid`() {
        val behandlingId = randomUUID()

        val vilkaarsvurderingDto = mockk<VilkaarsvurderingDto>()

        every { vilkaarsvurderingDto.isYrkesskade() } returns false
        coEvery { vilkaarsvurderingKlient.hentVilkaarsvurdering(any(), any()) } returns vilkaarsvurderingDto
        coEvery { repository.hentTrygdetiderForBehandling(any()) } returns listOf(trygdetid(behandlingId))

        val trygdetid = runBlocking { service.hentTrygdetid(behandlingId, saksbehandler) }

        trygdetid shouldNotBe null

        verify(exactly = 1) {
            repository.hentTrygdetiderForBehandling(behandlingId)
            vilkaarsvurderingDto.isYrkesskade()
        }

        coVerify(exactly = 1) {
            vilkaarsvurderingKlient.hentVilkaarsvurdering(any(), any())
        }
    }

    @Test
    fun `skal returnere null hvis trygdetid ikke finnes for behandling`() {
        val behandlingId = randomUUID()
        every { repository.hentTrygdetiderForBehandling(any()) } returns emptyList()

        val trygdetid = runBlocking { service.hentTrygdetid(behandlingId, saksbehandler) }

        trygdetid shouldBe null

        verify(exactly = 1) { repository.hentTrygdetiderForBehandling(behandlingId) }
    }

    @Test
    fun `skal opprette trygdetid`() {
        val behandlingId = randomUUID()
        val sakId = 123L
        val behandling =
            mockk<DetaljertBehandling>().apply {
                every { id } returns behandlingId
                every { sak } returns sakId
                every { behandlingType } returns BehandlingType.FØRSTEGANGSBEHANDLING
            }
        val grunnlag = GrunnlagTestData().hentOpplysningsgrunnlag()
        val forventetFoedselsdato = grunnlag.hentAvdoed().hentFoedselsdato()!!.verdi
        val forventetDoedsdato = grunnlag.hentAvdoed().hentDoedsdato()!!.verdi
        val forventetIdent = grunnlag.hentAvdoed().hentFoedselsnummer()!!.verdi
        val trygdetid = trygdetid(behandlingId, sakId, ident = forventetIdent.value)
        val vilkaarsvurderingDto = mockk<VilkaarsvurderingDto>()

        every { repository.hentTrygdetiderForBehandling(any()) } returns emptyList() andThen listOf(trygdetid)
        every { repository.hentTrygdetid(any()) } returns trygdetid
        every { repository.hentTrygdetidMedId(any(), any()) } returns trygdetid
        coEvery { behandlingKlient.hentBehandling(any(), any()) } returns behandling
        coEvery { grunnlagKlient.hentGrunnlag(any(), any(), any()) } returns grunnlag
        every { repository.opprettTrygdetid(any()) } returns trygdetid
        coEvery { behandlingKlient.settBehandlingStatusTrygdetidOppdatert(any(), any()) } returns true
        every { vilkaarsvurderingDto.isYrkesskade() } returns false
        coEvery { vilkaarsvurderingKlient.hentVilkaarsvurdering(any(), any()) } returns vilkaarsvurderingDto
        every { repository.oppdaterTrygdetid(any()) } returnsArgument 0

        runBlocking {
            val opprettetTrygdetid = service.opprettTrygdetid(behandlingId, saksbehandler)

            opprettetTrygdetid.trygdetidGrunnlag.size shouldBe 1
            opprettetTrygdetid.trygdetidGrunnlag[0].type shouldBe TrygdetidType.FREMTIDIG

            opprettetTrygdetid.beregnetTrygdetid?.resultat?.prorataBroek?.let {
                it.nevner shouldNotBe 0
            }
        }

        coVerify(exactly = 2) {
            behandlingKlient.kanOppdatereTrygdetid(behandlingId, saksbehandler)
            behandlingKlient.hentBehandling(behandlingId, saksbehandler)
            grunnlagKlient.hentGrunnlag(sakId, behandlingId, saksbehandler)
            behandlingKlient.settBehandlingStatusTrygdetidOppdatert(behandlingId, saksbehandler)
        }

        coVerify(exactly = 1) {
            repository.hentTrygdetiderForBehandling(behandlingId)
            repository.hentTrygdetidMedId(any(), any())
            vilkaarsvurderingKlient.hentVilkaarsvurdering(any(), any())
            repository.opprettTrygdetid(
                withArg { trygdetid ->
                    trygdetid.opplysninger.let { opplysninger ->
                        with(opplysninger[0]) {
                            type shouldBe TrygdetidOpplysningType.FOEDSELSDATO
                            opplysning shouldBe forventetFoedselsdato.toJsonNode()
                            kilde shouldNotBe null
                        }
                        with(opplysninger[1]) {
                            type shouldBe TrygdetidOpplysningType.FYLT_16
                            opplysning shouldBe forventetFoedselsdato.plusYears(16).toJsonNode()
                            kilde shouldNotBe null
                        }
                        with(opplysninger[2]) {
                            type shouldBe TrygdetidOpplysningType.FYLLER_66
                            opplysning shouldBe forventetFoedselsdato.plusYears(66).toJsonNode()
                            kilde shouldNotBe null
                        }
                        with(opplysninger[3]) {
                            type shouldBe TrygdetidOpplysningType.DOEDSDATO
                            opplysning shouldBe forventetDoedsdato!!.toJsonNode()
                            kilde shouldNotBe null
                        }
                    }
                },
            )
            repository.oppdaterTrygdetid(
                withArg { trygdetid ->
                    with(trygdetid.trygdetidGrunnlag[0]) {
                        type shouldBe TrygdetidType.FREMTIDIG
                    }
                },
            )
            beregningService.beregnTrygdetidGrunnlag(any())
            beregningService.beregnTrygdetid(any(), any(), any(), any())
        }
        verify {
            behandling.id
            behandling.sak
            behandling.behandlingType
            vilkaarsvurderingDto.isYrkesskade()
        }
    }

    @Test
    fun `skal kopiere trygdetid hvis revurdering`() {
        val behandlingId = randomUUID()
        val sakId = 123L
        val behandling =
            mockk<DetaljertBehandling>().apply {
                every { id } returns behandlingId
                every { sak } returns sakId
                every { behandlingType } returns BehandlingType.REVURDERING
            }
        val forrigebehandlingId = randomUUID()
        val trygdetid = trygdetid(behandlingId, sakId)

        val vilkaarsvurderingDto = mockk<VilkaarsvurderingDto>()

        every { vilkaarsvurderingDto.isYrkesskade() } returns false
        coEvery { vilkaarsvurderingKlient.hentVilkaarsvurdering(any(), any()) } returns vilkaarsvurderingDto
        every { repository.hentTrygdetiderForBehandling(behandlingId) } returns emptyList()
        coEvery { behandlingKlient.hentBehandling(any(), any()) } returns behandling
        coEvery { behandlingKlient.hentSisteIverksatteBehandling(any(), any()) } returns
            SisteIverksatteBehandling(
                forrigebehandlingId,
            )
        every { repository.hentTrygdetiderForBehandling(forrigebehandlingId) } returns listOf(trygdetid)
        every { repository.opprettTrygdetid(any()) } returns trygdetid
        coEvery { behandlingKlient.settBehandlingStatusTrygdetidOppdatert(any(), any()) } returns true

        runBlocking {
            service.opprettTrygdetid(behandlingId, saksbehandler)
        }

        coVerify(exactly = 1) {
            behandlingKlient.kanOppdatereTrygdetid(behandlingId, saksbehandler)
            repository.hentTrygdetiderForBehandling(behandlingId)
            behandlingKlient.hentBehandling(behandlingId, saksbehandler)
            behandlingKlient.hentSisteIverksatteBehandling(sakId, saksbehandler)
            repository.hentTrygdetiderForBehandling(forrigebehandlingId)
            behandlingKlient.settBehandlingStatusTrygdetidOppdatert(behandlingId, saksbehandler)

            repository.opprettTrygdetid(
                withArg {
                    it.opplysninger shouldBe trygdetid.opplysninger
                },
            )
            vilkaarsvurderingKlient.hentVilkaarsvurdering(any(), any())
        }
        verify {
            behandling.id
            behandling.sak
            behandling.behandlingType
            vilkaarsvurderingDto.isYrkesskade()
        }
    }

    @Test
    fun `skal opprette trygdetid hvis revurdering og forrige trygdetid mangler og det ikke er regulering`() {
        val behandlingId = randomUUID()
        val sakId = 123L
        val behandling =
            mockk<DetaljertBehandling>().apply {
                every { id } returns behandlingId
                every { sak } returns sakId
                every { behandlingType } returns BehandlingType.REVURDERING
                every { revurderingsaarsak } returns Revurderingaarsak.SOESKENJUSTERING
            }
        val forrigeBehandlingId = randomUUID()
        val grunnlag = GrunnlagTestData().hentOpplysningsgrunnlag()
        val forventetIdent = grunnlag.hentAvdoed().hentFoedselsnummer()!!.verdi
        val trygdetid = trygdetid(behandlingId, sakId, ident = forventetIdent.value)
        val vilkaarsvurderingDto = mockk<VilkaarsvurderingDto>()

        coEvery { grunnlagKlient.hentGrunnlag(any(), any(), any()) } returns grunnlag
        every { repository.hentTrygdetiderForBehandling(behandlingId) } returns emptyList()
        coEvery { behandlingKlient.hentBehandling(any(), any()) } returns behandling
        coEvery { behandlingKlient.hentSisteIverksatteBehandling(any(), any()) } returns
            SisteIverksatteBehandling(
                forrigeBehandlingId,
            )
        every { repository.hentTrygdetiderForBehandling(forrigeBehandlingId) } returns emptyList()
        every { repository.hentTrygdetidMedId(any(), any()) } returns trygdetid
        every { repository.opprettTrygdetid(any()) } returns trygdetid
        coEvery { behandlingKlient.settBehandlingStatusTrygdetidOppdatert(any(), any()) } returns true
        coEvery { vilkaarsvurderingKlient.hentVilkaarsvurdering(any(), any()) } returns vilkaarsvurderingDto
        every { vilkaarsvurderingDto.isYrkesskade() } returns false
        every { repository.oppdaterTrygdetid(any()) } returnsArgument 0

        runBlocking {
            service.opprettTrygdetid(behandlingId, saksbehandler)
        }

        coVerify(exactly = 2) {
            behandlingKlient.kanOppdatereTrygdetid(behandlingId, saksbehandler)
            behandlingKlient.hentBehandling(behandlingId, saksbehandler)
            grunnlagKlient.hentGrunnlag(sakId, behandlingId, saksbehandler)
            behandlingKlient.settBehandlingStatusTrygdetidOppdatert(behandlingId, saksbehandler)
        }

        coVerify(exactly = 1) {
            repository.hentTrygdetiderForBehandling(behandlingId)
            behandlingKlient.hentSisteIverksatteBehandling(sakId, saksbehandler)
            repository.hentTrygdetiderForBehandling(forrigeBehandlingId)
            vilkaarsvurderingKlient.hentVilkaarsvurdering(any(), any())
            repository.hentTrygdetidMedId(any(), any())
            repository.opprettTrygdetid(
                withArg {
                    it.trygdetidGrunnlag shouldBe emptyList()
                },
            )
            repository.oppdaterTrygdetid(
                withArg { trygdetid ->
                    with(trygdetid.trygdetidGrunnlag[0]) {
                        type shouldBe TrygdetidType.FREMTIDIG
                    }
                },
            )
            beregningService.beregnTrygdetidGrunnlag(any())
            beregningService.beregnTrygdetid(any(), any(), any(), any())
        }
        verify {
            behandling.revurderingsaarsak
            behandling.id
            behandling.sak
            behandling.behandlingType
            vilkaarsvurderingDto.isYrkesskade()
        }
    }

    @Test
    fun `skal ikke opprette trygdetid hvis revurdering og forrige trygdetid mangler og det er automatisk regulering`() {
        val behandlingId = randomUUID()
        val sakId = 123L
        val behandling =
            mockk<DetaljertBehandling>().apply {
                every { id } returns behandlingId
                every { sak } returns sakId
                every { behandlingType } returns BehandlingType.REVURDERING
                every { revurderingsaarsak } returns Revurderingaarsak.REGULERING
                every { prosesstype } returns Prosesstype.AUTOMATISK
            }
        val forrigeBehandlingId = randomUUID()
        every { repository.hentTrygdetiderForBehandling(behandlingId) } returns emptyList()
        coEvery { behandlingKlient.hentBehandling(any(), any()) } returns behandling
        coEvery { behandlingKlient.hentSisteIverksatteBehandling(any(), any()) } returns
            SisteIverksatteBehandling(
                forrigeBehandlingId,
            )
        every { repository.hentTrygdetiderForBehandling(forrigeBehandlingId) } returns emptyList()
        every { repository.hentTrygdetiderForBehandling(behandlingId) } returns emptyList()

        runBlocking {
            assertThrows<ManglerForrigeTrygdetidMaaReguleresManuelt> {
                service.opprettTrygdetid(behandlingId, saksbehandler)
            }
        }

        coVerify(exactly = 1) {
            behandlingKlient.kanOppdatereTrygdetid(behandlingId, saksbehandler)
            repository.hentTrygdetiderForBehandling(behandlingId)
            behandlingKlient.hentBehandling(behandlingId, saksbehandler)
            behandlingKlient.hentSisteIverksatteBehandling(sakId, saksbehandler)
            repository.hentTrygdetiderForBehandling(forrigeBehandlingId)
        }
        verify {
            behandling.revurderingsaarsak
            behandling.prosesstype
            behandling.id
            behandling.sak
            behandling.behandlingType
        }
    }

    @Test
    fun `skal opprette trygdetid hvis revurdering og forrige trygdetid mangler og det er manuell regulering`() {
        val behandlingId = randomUUID()
        val sakId = 123L
        val behandling =
            mockk<DetaljertBehandling>().apply {
                every { id } returns behandlingId
                every { sak } returns sakId
                every { behandlingType } returns BehandlingType.REVURDERING
                every { revurderingsaarsak } returns Revurderingaarsak.REGULERING
                every { prosesstype } returns Prosesstype.MANUELL
            }
        val forrigeBehandlingId = randomUUID()
        val grunnlag = GrunnlagTestData().hentOpplysningsgrunnlag()
        val forventetIdent = grunnlag.hentAvdoed().hentFoedselsnummer()!!.verdi
        val trygdetid = trygdetid(behandlingId, sakId, ident = forventetIdent.value)
        val vilkaarsvurderingDto = mockk<VilkaarsvurderingDto>()

        coEvery { grunnlagKlient.hentGrunnlag(any(), any(), any()) } returns grunnlag
        every { repository.hentTrygdetiderForBehandling(behandlingId) } returns emptyList()
        coEvery { behandlingKlient.hentBehandling(any(), any()) } returns behandling
        coEvery { behandlingKlient.hentSisteIverksatteBehandling(any(), any()) } returns
            SisteIverksatteBehandling(
                forrigeBehandlingId,
            )
        coEvery { behandlingKlient.settBehandlingStatusTrygdetidOppdatert(any(), any()) } returns true
        every { repository.hentTrygdetiderForBehandling(forrigeBehandlingId) } returns emptyList()
        every { repository.opprettTrygdetid(any()) } returns trygdetid
        every { repository.hentTrygdetidMedId(any(), any()) } returns trygdetid
        coEvery { vilkaarsvurderingKlient.hentVilkaarsvurdering(any(), any()) } returns vilkaarsvurderingDto
        every { vilkaarsvurderingDto.isYrkesskade() } returns false
        every { repository.oppdaterTrygdetid(any()) } returnsArgument 0

        runBlocking {
            service.opprettTrygdetid(behandlingId, saksbehandler)
        }

        coVerify(exactly = 2) {
            behandlingKlient.kanOppdatereTrygdetid(behandlingId, saksbehandler)
            behandlingKlient.hentBehandling(behandlingId, saksbehandler)
            grunnlagKlient.hentGrunnlag(sakId, behandlingId, saksbehandler)
            behandlingKlient.settBehandlingStatusTrygdetidOppdatert(behandlingId, saksbehandler)
        }

        coVerify(exactly = 1) {
            repository.hentTrygdetiderForBehandling(behandlingId)
            behandlingKlient.hentSisteIverksatteBehandling(sakId, saksbehandler)
            repository.hentTrygdetiderForBehandling(forrigeBehandlingId)
            vilkaarsvurderingKlient.hentVilkaarsvurdering(any(), any())
            repository.hentTrygdetidMedId(any(), any())
            repository.opprettTrygdetid(
                withArg {
                    it.trygdetidGrunnlag shouldBe emptyList()
                },
            )
            repository.oppdaterTrygdetid(
                withArg { trygdetid ->
                    with(trygdetid.trygdetidGrunnlag[0]) {
                        type shouldBe TrygdetidType.FREMTIDIG
                    }
                },
            )
            beregningService.beregnTrygdetidGrunnlag(any())
            beregningService.beregnTrygdetid(any(), any(), any(), any())
        }
        verify {
            behandling.revurderingsaarsak
            behandling.prosesstype
            behandling.id
            behandling.sak
            behandling.behandlingType
            vilkaarsvurderingDto.isYrkesskade()
        }
    }

    @Test
    fun `skal feile ved opprettelse av trygdetid naar det allerede finnes for behandling`() {
        val behandlingId = randomUUID()
        every { repository.hentTrygdetiderForBehandling(any()) } returns listOf(trygdetid(behandlingId))

        runBlocking {
            assertThrows<TrygdetidAlleredeOpprettetException> {
                service.opprettTrygdetid(behandlingId, saksbehandler)
            }
        }

        coVerify(exactly = 1) {
            repository.hentTrygdetiderForBehandling(behandlingId)
            behandlingKlient.kanOppdatereTrygdetid(behandlingId, saksbehandler)
        }
    }

    @Test
    fun `skal feile ved opprettelse av trygdetid dersom behandling er i feil tilstand`() {
        val behandlingId = randomUUID()
        coEvery { behandlingKlient.kanOppdatereTrygdetid(any(), any()) } returns false

        runBlocking {
            assertThrows<Exception> {
                service.opprettTrygdetid(behandlingId, saksbehandler)
            }
        }

        coVerify(exactly = 1) { behandlingKlient.kanOppdatereTrygdetid(behandlingId, saksbehandler) }
    }

    @Test
    fun `skal lagre nytt trygdetidsgrunnlag`() {
        val behandlingId = randomUUID()
        val avdoedIdent = "01478343724"
        val trygdetidGrunnlag = trygdetidGrunnlag()
        val eksisterendeTrygdetid = trygdetid(behandlingId, ident = avdoedIdent)
        val vilkaarsvurderingDto = mockk<VilkaarsvurderingDto>()

        val grunnlag = mockk<Grunnlag>()
        val avdoedGrunnlag = mockk<Grunnlagsdata<JsonNode>>()

        every { vilkaarsvurderingDto.isYrkesskade() } returns false
        coEvery { vilkaarsvurderingKlient.hentVilkaarsvurdering(any(), any()) } returns vilkaarsvurderingDto
        coEvery { behandlingKlient.settBehandlingStatusTrygdetidOppdatert(any(), any()) } returns true
        every { repository.hentTrygdetid(behandlingId) } returns eksisterendeTrygdetid
        every { repository.hentTrygdetidMedId(behandlingId, eksisterendeTrygdetid.id) } returns eksisterendeTrygdetid
        every { repository.oppdaterTrygdetid(any()) } answers { firstArg() }
        coEvery { behandlingKlient.hentBehandling(any(), any()) } answers { behandling(behandlingId = behandlingId) }
        coEvery { grunnlagKlient.hentGrunnlag(any(), any(), any()) } returns grunnlag
        every { grunnlag.hentAvdoede() } returns listOf(avdoedGrunnlag)
        every { avdoedGrunnlag[Opplysningstype.FOEDSELSDATO] } answers {
            Opplysning.Konstant(
                id = randomUUID(),
                kilde = Grunnlagsopplysning.Saksbehandler("", Tidspunkt.now()),
                verdi = LocalDate.now().toJsonNode(),
            )
        }
        every { avdoedGrunnlag[Opplysningstype.FOEDSELSNUMMER] } answers {
            Opplysning.Konstant(
                id = randomUUID(),
                kilde =
                    Grunnlagsopplysning.Pdl(
                        tidspunktForInnhenting = Tidspunkt.now(),
                        registersReferanse = null,
                        opplysningId = null,
                    ),
                verdi = Folkeregisteridentifikator.of(avdoedIdent).toJsonNode(),
            )
        }
        every { avdoedGrunnlag[Opplysningstype.DOEDSDATO] } answers {
            Opplysning.Konstant(
                id = randomUUID(),
                kilde = Grunnlagsopplysning.Saksbehandler("", Tidspunkt.now()),
                verdi = LocalDate.now().toJsonNode(),
            )
        }

        val trygdetid =
            runBlocking {
                service.lagreTrygdetidGrunnlag(
                    behandlingId,
                    saksbehandler,
                    trygdetidGrunnlag,
                )
            }

        with(trygdetid.trygdetidGrunnlag.first()) {
            beregnetTrygdetid?.verdi shouldBe Period.of(0, 1, 1)
            beregnetTrygdetid?.regelResultat shouldNotBe null
            beregnetTrygdetid?.tidspunkt shouldNotBe null
        }

        coVerify(exactly = 1) {
            behandlingKlient.kanOppdatereTrygdetid(behandlingId, saksbehandler)
            repository.hentTrygdetid(behandlingId)
            repository.hentTrygdetidMedId(behandlingId, trygdetid.id)
            repository.oppdaterTrygdetid(
                withArg {
                    it.trygdetidGrunnlag.first().let { tg -> tg.id shouldBe trygdetidGrunnlag.id }
                },
            )
            behandlingKlient.settBehandlingStatusTrygdetidOppdatert(behandlingId, saksbehandler)
            beregningService.beregnTrygdetidGrunnlag(any())
            beregningService.beregnTrygdetid(any(), any(), any(), any())
            vilkaarsvurderingKlient.hentVilkaarsvurdering(any(), any())
            vilkaarsvurderingDto.isYrkesskade()
            behandlingKlient.hentBehandling(any(), any())
            grunnlagKlient.hentGrunnlag(any(), behandlingId, any())
        }

        verify {
            avdoedGrunnlag[Opplysningstype.FOEDSELSDATO]
            avdoedGrunnlag[Opplysningstype.DOEDSDATO]
            avdoedGrunnlag[Opplysningstype.FOEDSELSNUMMER]
            grunnlag.hentAvdoede()
        }
    }

    @Test
    fun `skal lagre nytt trygdetidsgrunnlag med overstyrt poengaar`() {
        val behandlingId = randomUUID()
        val avdoedIdent = "01478343724"

        val trygdetidGrunnlag =
            trygdetidGrunnlag().copy(
                periode = TrygdetidPeriode(LocalDate.now().minusYears(2), LocalDate.now().minusYears(1)),
            )
        val eksisterendeTrygdetid = trygdetid(behandlingId, ident = avdoedIdent).copy(overstyrtNorskPoengaar = 10)
        val vilkaarsvurderingDto = mockk<VilkaarsvurderingDto>()

        val grunnlag = mockk<Grunnlag>()
        val avdoedGrunnlag = mockk<Grunnlagsdata<JsonNode>>()

        every { vilkaarsvurderingDto.isYrkesskade() } returns false
        coEvery { vilkaarsvurderingKlient.hentVilkaarsvurdering(any(), any()) } returns vilkaarsvurderingDto
        coEvery { behandlingKlient.settBehandlingStatusTrygdetidOppdatert(any(), any()) } returns true
        every { repository.hentTrygdetid(behandlingId) } returns eksisterendeTrygdetid
        every { repository.hentTrygdetidMedId(behandlingId, eksisterendeTrygdetid.id) } returns eksisterendeTrygdetid
        every { repository.oppdaterTrygdetid(any()) } answers { firstArg() }
        coEvery { behandlingKlient.hentBehandling(any(), any()) } answers { behandling(behandlingId = behandlingId) }
        coEvery { grunnlagKlient.hentGrunnlag(any(), any(), any()) } returns grunnlag
        every { grunnlag.hentAvdoede() } returns listOf(avdoedGrunnlag)
        every { avdoedGrunnlag[Opplysningstype.FOEDSELSDATO] } answers {
            Opplysning.Konstant(
                id = randomUUID(),
                kilde = Grunnlagsopplysning.Saksbehandler("", Tidspunkt.now()),
                verdi = LocalDate.now().toJsonNode(),
            )
        }
        every { avdoedGrunnlag[Opplysningstype.FOEDSELSNUMMER] } answers {
            Opplysning.Konstant(
                id = randomUUID(),
                kilde =
                    Grunnlagsopplysning.Pdl(
                        tidspunktForInnhenting = Tidspunkt.now(),
                        registersReferanse = null,
                        opplysningId = null,
                    ),
                verdi = Folkeregisteridentifikator.of(avdoedIdent).toJsonNode(),
            )
        }
        every { avdoedGrunnlag[Opplysningstype.DOEDSDATO] } answers {
            Opplysning.Konstant(
                id = randomUUID(),
                kilde = Grunnlagsopplysning.Saksbehandler("", Tidspunkt.now()),
                verdi = LocalDate.now().toJsonNode(),
            )
        }
        val trygdetid =
            runBlocking {
                service.lagreTrygdetidGrunnlag(
                    behandlingId,
                    saksbehandler,
                    trygdetidGrunnlag,
                )
            }

        with(trygdetid.trygdetidGrunnlag.first()) {
            beregnetTrygdetid?.verdi shouldBe Period.of(1, 0, 1)
            beregnetTrygdetid?.regelResultat shouldNotBe null
            beregnetTrygdetid?.tidspunkt shouldNotBe null
        }

        trygdetid.beregnetTrygdetid?.resultat?.samletTrygdetidNorge shouldBe 10

        coVerify(exactly = 1) {
            behandlingKlient.kanOppdatereTrygdetid(behandlingId, saksbehandler)
            repository.hentTrygdetid(behandlingId)
            repository.hentTrygdetidMedId(behandlingId, trygdetid.id)
            repository.oppdaterTrygdetid(
                withArg {
                    it.trygdetidGrunnlag.first().let { tg -> tg.id shouldBe trygdetidGrunnlag.id }
                },
            )
            behandlingKlient.settBehandlingStatusTrygdetidOppdatert(behandlingId, saksbehandler)
            beregningService.beregnTrygdetidGrunnlag(any())
            beregningService.beregnTrygdetid(any(), any(), any(), any())
            vilkaarsvurderingKlient.hentVilkaarsvurdering(any(), any())
            vilkaarsvurderingDto.isYrkesskade()
            behandlingKlient.hentBehandling(any(), any())
            grunnlagKlient.hentGrunnlag(any(), behandlingId, any())
        }
        verify {
            avdoedGrunnlag[Opplysningstype.FOEDSELSDATO]
            avdoedGrunnlag[Opplysningstype.DOEDSDATO]
            avdoedGrunnlag[Opplysningstype.FOEDSELSNUMMER]
            grunnlag.hentAvdoede()
        }
    }

    @Test
    fun `skal oppdatere trygdetidsgrunnlag`() {
        val behandlingId = randomUUID()
        val trygdetidGrunnlag = trygdetidGrunnlag()
        val avdoedIdent = "01478343724"

        val eksisterendeTrygdetid =
            trygdetid(behandlingId, trygdetidGrunnlag = listOf(trygdetidGrunnlag), ident = avdoedIdent)
        val endretTrygdetidGrunnlag = trygdetidGrunnlag.copy(bosted = LandNormalisert.NORGE.isoCode)
        val vilkaarsvurderingDto = mockk<VilkaarsvurderingDto>()

        coEvery { behandlingKlient.settBehandlingStatusTrygdetidOppdatert(any(), any()) } returns true
        val grunnlag = mockk<Grunnlag>()
        val avdoedGrunnlag = mockk<Grunnlagsdata<JsonNode>>()

        every { repository.hentTrygdetid(behandlingId) } returns eksisterendeTrygdetid
        every { repository.hentTrygdetidMedId(behandlingId, eksisterendeTrygdetid.id) } returns eksisterendeTrygdetid
        every { repository.oppdaterTrygdetid(any()) } answers { firstArg() }
        every { vilkaarsvurderingDto.isYrkesskade() } returns false
        coEvery { vilkaarsvurderingKlient.hentVilkaarsvurdering(any(), any()) } returns vilkaarsvurderingDto
        coEvery { behandlingKlient.hentBehandling(any(), any()) } answers { behandling(behandlingId = behandlingId) }
        coEvery { grunnlagKlient.hentGrunnlag(any(), any(), any()) } returns grunnlag
        every { grunnlag.hentAvdoede() } returns listOf(avdoedGrunnlag)
        every { avdoedGrunnlag[Opplysningstype.FOEDSELSDATO] } answers {
            Opplysning.Konstant(
                id = randomUUID(),
                kilde = Grunnlagsopplysning.Saksbehandler("", Tidspunkt.now()),
                verdi = LocalDate.now().toJsonNode(),
            )
        }
        every { avdoedGrunnlag[Opplysningstype.FOEDSELSNUMMER] } answers {
            Opplysning.Konstant(
                id = randomUUID(),
                kilde =
                    Grunnlagsopplysning.Pdl(
                        tidspunktForInnhenting = Tidspunkt.now(),
                        registersReferanse = null,
                        opplysningId = null,
                    ),
                verdi = Folkeregisteridentifikator.of(avdoedIdent).toJsonNode(),
            )
        }
        every { avdoedGrunnlag[Opplysningstype.DOEDSDATO] } answers {
            Opplysning.Konstant(
                id = randomUUID(),
                kilde = Grunnlagsopplysning.Saksbehandler("", Tidspunkt.now()),
                verdi = LocalDate.now().toJsonNode(),
            )
        }

        val trygdetid =
            runBlocking {
                service.lagreTrygdetidGrunnlag(
                    behandlingId,
                    saksbehandler,
                    endretTrygdetidGrunnlag,
                )
            }

        with(trygdetid.trygdetidGrunnlag.first()) {
            beregnetTrygdetid?.verdi shouldBe Period.of(0, 1, 1)
            beregnetTrygdetid?.regelResultat shouldNotBe null
            beregnetTrygdetid?.tidspunkt shouldNotBe null
        }

        coVerify(exactly = 1) {
            behandlingKlient.kanOppdatereTrygdetid(behandlingId, saksbehandler)
            repository.hentTrygdetid(behandlingId)
            repository.hentTrygdetidMedId(behandlingId, eksisterendeTrygdetid.id)
            repository.oppdaterTrygdetid(
                withArg {
                    it.trygdetidGrunnlag.first().let { tg ->
                        tg.id shouldBe trygdetidGrunnlag.id
                        tg.bosted shouldBe LandNormalisert.NORGE.isoCode
                    }
                },
            )
            beregningService.beregnTrygdetidGrunnlag(any())
            beregningService.beregnTrygdetid(any(), any(), any(), any())
            behandlingKlient.settBehandlingStatusTrygdetidOppdatert(behandlingId, saksbehandler)
            vilkaarsvurderingKlient.hentVilkaarsvurdering(any(), any())
            vilkaarsvurderingDto.isYrkesskade()
            behandlingKlient.hentBehandling(any(), any())
            grunnlagKlient.hentGrunnlag(any(), behandlingId, any())
        }

        verify {
            avdoedGrunnlag[Opplysningstype.FOEDSELSDATO]
            avdoedGrunnlag[Opplysningstype.DOEDSDATO]
            avdoedGrunnlag[Opplysningstype.FOEDSELSNUMMER]
            grunnlag.hentAvdoede()
        }
    }

    @Test
    fun `skal slette trygdetidsgrunnlag`() {
        val behandlingId = randomUUID()
        val avdoedIdent = "01478343724"
        val trygdetidGrunnlag = trygdetidGrunnlag()
        val eksisterendeTrygdetid =
            trygdetid(behandlingId, trygdetidGrunnlag = listOf(trygdetidGrunnlag), ident = avdoedIdent)

        coEvery { behandlingKlient.settBehandlingStatusTrygdetidOppdatert(any(), any()) } returns true
        val grunnlag = mockk<Grunnlag>()
        val avdoedGrunnlag = mockk<Grunnlagsdata<JsonNode>>()

        every { repository.hentTrygdetiderForBehandling(behandlingId) } returns listOf(eksisterendeTrygdetid)
        every { repository.hentTrygdetid(behandlingId) } returns eksisterendeTrygdetid
        every { repository.hentTrygdetidMedId(behandlingId, eksisterendeTrygdetid.id) } returns eksisterendeTrygdetid
        every { repository.oppdaterTrygdetid(any()) } answers { firstArg() }
        coEvery { behandlingKlient.hentBehandling(any(), any()) } answers { behandling(behandlingId = behandlingId) }
        coEvery { grunnlagKlient.hentGrunnlag(any(), any(), any()) } returns grunnlag
        every { grunnlag.hentAvdoede() } returns listOf(avdoedGrunnlag)
        every { avdoedGrunnlag[Opplysningstype.FOEDSELSDATO] } answers {
            Opplysning.Konstant(
                id = randomUUID(),
                kilde = Grunnlagsopplysning.Saksbehandler("", Tidspunkt.now()),
                verdi = LocalDate.now().toJsonNode(),
            )
        }
        every { avdoedGrunnlag[Opplysningstype.FOEDSELSNUMMER] } answers {
            Opplysning.Konstant(
                id = randomUUID(),
                kilde =
                    Grunnlagsopplysning.Pdl(
                        tidspunktForInnhenting = Tidspunkt.now(),
                        registersReferanse = null,
                        opplysningId = null,
                    ),
                verdi = Folkeregisteridentifikator.of(avdoedIdent).toJsonNode(),
            )
        }
        every { avdoedGrunnlag[Opplysningstype.DOEDSDATO] } answers {
            Opplysning.Konstant(
                id = randomUUID(),
                kilde = Grunnlagsopplysning.Saksbehandler("", Tidspunkt.now()),
                verdi = LocalDate.now().toJsonNode(),
            )
        }

        val trygdetid =
            runBlocking {
                service.slettTrygdetidGrunnlag(behandlingId, trygdetidGrunnlag.id, saksbehandler)
            }

        trygdetid.trygdetidGrunnlag shouldNotContain trygdetidGrunnlag
        trygdetid.beregnetTrygdetid shouldBe null

        coVerify(exactly = 1) {
            behandlingKlient.kanOppdatereTrygdetid(behandlingId, saksbehandler)
            repository.hentTrygdetid(behandlingId)
            repository.hentTrygdetidMedId(behandlingId, trygdetid.id)
            repository.oppdaterTrygdetid(
                withArg {
                    it.trygdetidGrunnlag shouldBe emptyList()
                },
            )
            beregningService.beregnTrygdetid(any(), any(), any(), any())
            behandlingKlient.settBehandlingStatusTrygdetidOppdatert(behandlingId, saksbehandler)
            behandlingKlient.hentBehandling(any(), any())
            grunnlagKlient.hentGrunnlag(any(), behandlingId, any())
        }
        verify {
            avdoedGrunnlag[Opplysningstype.FOEDSELSDATO]
            avdoedGrunnlag[Opplysningstype.DOEDSDATO]
            avdoedGrunnlag[Opplysningstype.FOEDSELSNUMMER]
            grunnlag.hentAvdoede()
        }
    }

    @Test
    fun `skal feile ved lagring av trygdetidsgrunnlag hvis behandling er i feil tilstand`() {
        val behandlingId = randomUUID()
        val trygdetidGrunnlag = trygdetidGrunnlag()
        coEvery { repository.hentTrygdetid(any()) } returns trygdetid(behandlingId)
        coEvery { behandlingKlient.kanOppdatereTrygdetid(any(), any()) } returns false

        runBlocking {
            assertThrows<Exception> {
                service.lagreTrygdetidGrunnlag(
                    behandlingId,
                    saksbehandler,
                    trygdetidGrunnlag,
                )
            }
        }

        coVerify { behandlingKlient.kanOppdatereTrygdetid(behandlingId, saksbehandler) }
        coVerify { repository.hentTrygdetid(behandlingId) }
    }

    @Test
    fun `skal opprette ny trygdetid av kopi fra forrige behandling`() {
        val sakId = 123L
        val behandlingId = randomUUID()
        val forrigeBehandlingId = randomUUID()
        val forrigeTrygdetidGrunnlag = trygdetidGrunnlag()
        val forrigeTrygdetidOpplysninger = listOf<Opplysningsgrunnlag>()
        val forrigeTrygdetid =
            trygdetid(
                behandlingId,
                trygdetidGrunnlag = listOf(forrigeTrygdetidGrunnlag),
                opplysninger = forrigeTrygdetidOpplysninger,
            )

        val regulering =
            mockk<DetaljertBehandling>().apply {
                every { id } returns behandlingId
                every { sak } returns sakId
            }

        val vilkaarsvurderingDto = mockk<VilkaarsvurderingDto>()

        every { vilkaarsvurderingDto.isYrkesskade() } returns false
        coEvery { vilkaarsvurderingKlient.hentVilkaarsvurdering(any(), any()) } returns vilkaarsvurderingDto
        coEvery { behandlingKlient.hentBehandling(behandlingId, saksbehandler) } returns regulering
        every { repository.hentTrygdetiderForBehandling(forrigeBehandlingId) } returns listOf(forrigeTrygdetid)
        every { repository.opprettTrygdetid(any()) } answers { firstArg() }

        runBlocking {
            service.kopierSisteTrygdetidberegning(behandlingId, forrigeBehandlingId, saksbehandler)
        }

        coVerify(exactly = 1) {
            repository.hentTrygdetiderForBehandling(forrigeBehandlingId)
            behandlingKlient.hentBehandling(behandlingId, saksbehandler)
            repository.opprettTrygdetid(match { it.behandlingId == behandlingId })
            vilkaarsvurderingKlient.hentVilkaarsvurdering(any(), any())
        }
        verify {
            regulering.id
            regulering.sak
            vilkaarsvurderingDto.isYrkesskade()
        }
    }

    @Test
    fun `henting av trygdetid - yrkesskade true - vilkaar-yrkesskade true`() {
        val behandlingId = randomUUID()

        val vilkaarsvurderingDto = mockk<VilkaarsvurderingDto>()

        every { vilkaarsvurderingDto.isYrkesskade() } returns true
        coEvery { vilkaarsvurderingKlient.hentVilkaarsvurdering(any(), any()) } returns vilkaarsvurderingDto
        coEvery {
            repository.hentTrygdetiderForBehandling(any())
        } returns listOf(trygdetid(behandlingId).copy(beregnetTrygdetid = beregnetYrkesskadeTrygdetid()))

        val trygdetid = runBlocking { service.hentTrygdetid(behandlingId, saksbehandler) }

        trygdetid shouldNotBe null
        trygdetid?.beregnetTrygdetid?.resultat?.samletTrygdetidNorge shouldBe 40

        verify(exactly = 1) {
            repository.hentTrygdetiderForBehandling(behandlingId)
            vilkaarsvurderingDto.isYrkesskade()
        }

        coVerify(exactly = 1) {
            vilkaarsvurderingKlient.hentVilkaarsvurdering(any(), any())
        }
    }

    @Test
    fun `henting av trygdetid - yrkesskade false - vilkaar-yrkesskade true`() {
        val behandlingId = randomUUID()

        val vilkaarsvurderingDto = mockk<VilkaarsvurderingDto>()

        every { vilkaarsvurderingDto.isYrkesskade() } returns true
        coEvery { vilkaarsvurderingKlient.hentVilkaarsvurdering(any(), any()) } returns vilkaarsvurderingDto
        coEvery {
            repository.hentTrygdetiderForBehandling(any())
        } returns listOf(trygdetid(behandlingId).copy(beregnetTrygdetid = beregnetTrygdetid(20)))

        val trygdetid = runBlocking { service.hentTrygdetid(behandlingId, saksbehandler) }

        trygdetid shouldNotBe null
        trygdetid?.beregnetTrygdetid shouldBe null

        verify(exactly = 1) {
            repository.hentTrygdetiderForBehandling(behandlingId)
            vilkaarsvurderingDto.isYrkesskade()
        }

        coVerify(exactly = 1) {
            vilkaarsvurderingKlient.hentVilkaarsvurdering(any(), any())
        }
    }

    @Test
    fun `henting av trygdetid - yrkesskade true - vilkaar-yrkesskade false`() {
        val behandlingId = randomUUID()

        val vilkaarsvurderingDto = mockk<VilkaarsvurderingDto>()

        every { vilkaarsvurderingDto.isYrkesskade() } returns false
        coEvery { vilkaarsvurderingKlient.hentVilkaarsvurdering(any(), any()) } returns vilkaarsvurderingDto
        coEvery {
            repository.hentTrygdetiderForBehandling(any())
        } returns listOf(trygdetid(behandlingId).copy(beregnetTrygdetid = beregnetYrkesskadeTrygdetid()))

        val trygdetid = runBlocking { service.hentTrygdetid(behandlingId, saksbehandler) }

        trygdetid shouldNotBe null
        trygdetid?.beregnetTrygdetid shouldBe null

        verify(exactly = 1) {
            repository.hentTrygdetiderForBehandling(behandlingId)
            vilkaarsvurderingDto.isYrkesskade()
        }

        coVerify(exactly = 1) {
            vilkaarsvurderingKlient.hentVilkaarsvurdering(any(), any())
        }
    }

    @Test
    fun `henting av trygdetid - yrkesskade false - vilkaar-yrkesskade false`() {
        val behandlingId = randomUUID()

        val vilkaarsvurderingDto = mockk<VilkaarsvurderingDto>()

        every { vilkaarsvurderingDto.isYrkesskade() } returns false
        coEvery { vilkaarsvurderingKlient.hentVilkaarsvurdering(any(), any()) } returns vilkaarsvurderingDto
        coEvery {
            repository.hentTrygdetiderForBehandling(any())
        } returns listOf(trygdetid(behandlingId).copy(beregnetTrygdetid = beregnetTrygdetid(20)))

        val trygdetid = runBlocking { service.hentTrygdetid(behandlingId, saksbehandler) }

        trygdetid shouldNotBe null
        trygdetid?.beregnetTrygdetid?.resultat?.samletTrygdetidNorge shouldBe 20

        verify(exactly = 1) {
            repository.hentTrygdetiderForBehandling(behandlingId)
            vilkaarsvurderingDto.isYrkesskade()
        }

        coVerify(exactly = 1) {
            vilkaarsvurderingKlient.hentVilkaarsvurdering(any(), any())
        }
    }

    @Test
    fun `skal lagre nytt yrkesskade trygdetidsgrunnlag`() {
        val behandlingId = randomUUID()
        val eksisterendeTrygdetid = trygdetid(behandlingId)
        val vilkaarsvurderingDto = mockk<VilkaarsvurderingDto>()

        every { vilkaarsvurderingDto.isYrkesskade() } returns true
        coEvery { vilkaarsvurderingKlient.hentVilkaarsvurdering(any(), any()) } returns vilkaarsvurderingDto
        coEvery { behandlingKlient.settBehandlingStatusTrygdetidOppdatert(any(), any()) } returns true
        every {
            repository.hentTrygdetiderForBehandling(behandlingId)
        } returns listOf(eksisterendeTrygdetid)
        every { repository.oppdaterTrygdetid(any()) } answers { firstArg() }

        val trygdetid =
            runBlocking {
                service.lagreYrkesskadeTrygdetidGrunnlag(
                    behandlingId,
                    saksbehandler,
                )
            }

        trygdetid.beregnetTrygdetid?.resultat?.samletTrygdetidNorge shouldBe 40

        coVerify(exactly = 1) {
            behandlingKlient.kanOppdatereTrygdetid(behandlingId, saksbehandler)
            repository.hentTrygdetiderForBehandling(behandlingId)
            repository.oppdaterTrygdetid(any())
            behandlingKlient.settBehandlingStatusTrygdetidOppdatert(behandlingId, saksbehandler)
            beregningService.beregnTrygdetidForYrkesskade(any())
            vilkaarsvurderingKlient.hentVilkaarsvurdering(any(), any())
            vilkaarsvurderingDto.isYrkesskade()
        }
    }

    @Test
    fun `skal oppdater yrkesskade trygdetidsgrunnlag selv om det ikke var yrkesskade foer`() {
        val behandlingId = randomUUID()
        val eksisterendeTrygdetid = trygdetid(behandlingId)
        val vilkaarsvurderingDto = mockk<VilkaarsvurderingDto>()

        every { vilkaarsvurderingDto.isYrkesskade() } returns true
        coEvery { vilkaarsvurderingKlient.hentVilkaarsvurdering(any(), any()) } returns vilkaarsvurderingDto
        coEvery { behandlingKlient.settBehandlingStatusTrygdetidOppdatert(any(), any()) } returns true
        every {
            repository.hentTrygdetiderForBehandling(behandlingId)
        } returns listOf(eksisterendeTrygdetid.copy(beregnetTrygdetid = beregnetTrygdetid(20)))
        every { repository.oppdaterTrygdetid(any()) } answers { firstArg() }

        val trygdetid =
            runBlocking {
                service.lagreYrkesskadeTrygdetidGrunnlag(
                    behandlingId,
                    saksbehandler,
                )
            }

        trygdetid.beregnetTrygdetid?.resultat?.samletTrygdetidNorge shouldBe 40

        coVerify(exactly = 1) {
            behandlingKlient.kanOppdatereTrygdetid(behandlingId, saksbehandler)
            repository.hentTrygdetiderForBehandling(behandlingId)
            repository.oppdaterTrygdetid(any())
            behandlingKlient.settBehandlingStatusTrygdetidOppdatert(behandlingId, saksbehandler)
            beregningService.beregnTrygdetidForYrkesskade(any())
            vilkaarsvurderingKlient.hentVilkaarsvurdering(any(), any())
            vilkaarsvurderingDto.isYrkesskade()
        }
    }

    @Test
    fun `skal oppdater yrkesskade trygdetidsgrunnlag naar det var yrkesskade foer`() {
        val behandlingId = randomUUID()
        val eksisterendeTrygdetid = trygdetid(behandlingId)
        val vilkaarsvurderingDto = mockk<VilkaarsvurderingDto>()

        every { vilkaarsvurderingDto.isYrkesskade() } returns true
        coEvery { vilkaarsvurderingKlient.hentVilkaarsvurdering(any(), any()) } returns vilkaarsvurderingDto
        coEvery { behandlingKlient.settBehandlingStatusTrygdetidOppdatert(any(), any()) } returns true
        every {
            repository.hentTrygdetiderForBehandling(behandlingId)
        } returns listOf(eksisterendeTrygdetid.copy(beregnetTrygdetid = beregnetYrkesskadeTrygdetid()))
        every { repository.oppdaterTrygdetid(any()) } answers { firstArg() }

        val trygdetid =
            runBlocking {
                service.lagreYrkesskadeTrygdetidGrunnlag(
                    behandlingId,
                    saksbehandler,
                )
            }

        trygdetid.beregnetTrygdetid?.resultat?.samletTrygdetidNorge shouldBe 40

        coVerify(exactly = 1) {
            behandlingKlient.kanOppdatereTrygdetid(behandlingId, saksbehandler)
            repository.hentTrygdetiderForBehandling(behandlingId)
            repository.oppdaterTrygdetid(any())
            behandlingKlient.settBehandlingStatusTrygdetidOppdatert(behandlingId, saksbehandler)
            beregningService.beregnTrygdetidForYrkesskade(any())
            vilkaarsvurderingKlient.hentVilkaarsvurdering(any(), any())
            vilkaarsvurderingDto.isYrkesskade()
        }
    }

    @Test
    fun `skal oppdater overstyrt poengaar`() {
        val behandlingId = randomUUID()

        val eksisterendeTrygdetid = trygdetid(behandlingId)

        coEvery { repository.hentTrygdetidMedId(any(), any()) } returns eksisterendeTrygdetid
        coEvery { repository.oppdaterTrygdetid(any(), any()) } returnsArgument 0

        val trygdetid =
            runBlocking {
                service.overstyrNorskPoengaar(eksisterendeTrygdetid.id, behandlingId, 10)
            }

        trygdetid shouldNotBe null
        trygdetid.overstyrtNorskPoengaar shouldBe 10

        verify(exactly = 1) {
            repository.hentTrygdetidMedId(any(), any())
            repository.oppdaterTrygdetid(trygdetid, false)
        }
    }
}
