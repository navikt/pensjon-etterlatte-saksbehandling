package no.nav.etterlatte.trygdetid

import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.every
import io.mockk.mockk
import no.nav.etterlatte.libs.common.behandling.DetaljertBehandling
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import no.nav.etterlatte.libs.common.sak.SakId
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.common.toJsonNode
import no.nav.etterlatte.libs.common.trygdetid.land.LandNormalisert
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.RegisterExtension
import java.time.LocalDate
import java.util.UUID.randomUUID
import javax.sql.DataSource

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class TrygdetidRepositoryTest(
    dataSource: DataSource,
) {
    companion object {
        @RegisterExtension
        val dbExtension = DatabaseExtension()
    }

    private val repository: TrygdetidRepository = TrygdetidRepository(dataSource)

    @AfterEach
    fun afterEach() {
        dbExtension.resetDb()
    }

    private val pdlKilde: Grunnlagsopplysning.Pdl = Grunnlagsopplysning.Pdl(Tidspunkt.now(), null, "opplysningsId1")

    private val regelKilde: Grunnlagsopplysning.RegelKilde = Grunnlagsopplysning.RegelKilde("regel", Tidspunkt.now(), "1")

    @Test
    fun `skal opprette trygdetid med opplysninger`() {
        val behandling = behandlingMock()

        val foedselsdato = LocalDate.of(2000, 1, 1)
        val doedsdato = LocalDate.of(2020, 1, 1)
        val seksten = LocalDate.of(2016, 1, 1)
        val seksti = LocalDate.of(2066, 1, 1)

        val opplysninger = opplysningsgrunnlag(foedselsdato, doedsdato, seksten, seksti)

        val opprettetTrygdetid = trygdetid(behandling.id, behandling.sak, opplysninger = opplysninger)

        val trygdetid = repository.opprettTrygdetid(opprettetTrygdetid)

        trygdetid shouldNotBe null
        trygdetid.behandlingId shouldBe behandling.id
        trygdetid.opplysninger.size shouldBe 4

        with(trygdetid.opplysninger[0]) {
            type shouldBe TrygdetidOpplysningType.FOEDSELSDATO
            opplysning shouldBe foedselsdato.toJsonNode()
            kilde shouldBe pdlKilde
        }
        with(trygdetid.opplysninger[1]) {
            type shouldBe TrygdetidOpplysningType.DOEDSDATO
            opplysning shouldBe doedsdato.toJsonNode()
            kilde shouldBe pdlKilde
        }
        with(trygdetid.opplysninger[2]) {
            type shouldBe TrygdetidOpplysningType.FYLT_16
            opplysning shouldBe seksten.toJsonNode()
            kilde shouldBe regelKilde
        }
        with(trygdetid.opplysninger[3]) {
            type shouldBe TrygdetidOpplysningType.FYLLER_66
            opplysning shouldBe seksti.toJsonNode()
            kilde shouldBe regelKilde
        }
    }

    private fun opplysningsgrunnlag(
        foedselsdato: LocalDate,
        doedsdato: LocalDate,
        seksten: LocalDate,
        seksti: LocalDate,
    ): List<Opplysningsgrunnlag> {
        val opplysninger =
            listOf(
                Opplysningsgrunnlag.ny(TrygdetidOpplysningType.FOEDSELSDATO, pdlKilde, foedselsdato),
                Opplysningsgrunnlag.ny(TrygdetidOpplysningType.DOEDSDATO, pdlKilde, doedsdato),
                Opplysningsgrunnlag.ny(TrygdetidOpplysningType.FYLT_16, regelKilde, seksten),
                Opplysningsgrunnlag.ny(TrygdetidOpplysningType.FYLLER_66, regelKilde, seksti),
            )
        return opplysninger
    }

    @Test
    fun `skal opprette og hente trygdetid`() {
        val behandling = behandlingMock()
        val opprettetTrygdetid = trygdetid(behandling.id, behandling.sak)

        repository.opprettTrygdetid(opprettetTrygdetid)
        val trygdetid = repository.hentTrygdetid(behandling.id)

        trygdetid shouldNotBe null
        trygdetid?.id shouldNotBe null
        trygdetid?.behandlingId shouldBe behandling.id
        trygdetid?.yrkesskade shouldBe false
    }

    @Test
    fun `skal opprette og hente trygdetid med yrkesskade`() {
        val behandling = behandlingMock()
        val opprettetTrygdetid = trygdetid(behandling.id, behandling.sak, yrkesskade = true)

        repository.opprettTrygdetid(opprettetTrygdetid)
        val trygdetid = repository.hentTrygdetid(behandling.id)

        trygdetid shouldNotBe null
        trygdetid?.id shouldNotBe null
        trygdetid?.behandlingId shouldBe behandling.id
        trygdetid?.yrkesskade shouldBe true
    }

    @Test
    fun `skal opprette og hente trygdetid med grunnlag og beregning`() {
        val behandling = behandlingMock()
        val beregnetTrygdetid = beregnetTrygdetid()
        val trygdetidGrunnlag = trygdetidGrunnlag()
        val opprettetTrygdetid =
            trygdetid(
                behandling.id,
                behandling.sak,
                trygdetidGrunnlag = listOf(trygdetidGrunnlag),
                beregnetTrygdetid = beregnetTrygdetid,
            )

        repository.opprettTrygdetid(opprettetTrygdetid)
        val trygdetid = repository.hentTrygdetid(behandling.id)

        trygdetid shouldNotBe null
        trygdetid?.id shouldNotBe null
        trygdetid?.behandlingId shouldBe behandling.id
        trygdetid?.trygdetidGrunnlag?.first() shouldBe trygdetidGrunnlag
        trygdetid?.beregnetTrygdetid shouldBe beregnetTrygdetid
        trygdetid?.yrkesskade shouldBe false
    }

    @Test
    fun `skal opprette og hente trygdetid med grunnlag og beregning for yrkesskade`() {
        val behandling = behandlingMock()
        val beregnetTrygdetid = beregnetTrygdetid(yrkesskade = true)
        val trygdetidGrunnlag = trygdetidGrunnlag()
        val opprettetTrygdetid =
            trygdetid(
                behandling.id,
                behandling.sak,
                trygdetidGrunnlag = listOf(trygdetidGrunnlag),
                beregnetTrygdetid = beregnetTrygdetid,
                yrkesskade = true,
            )

        repository.opprettTrygdetid(opprettetTrygdetid)
        val trygdetid = repository.hentTrygdetid(behandling.id)

        trygdetid shouldNotBe null
        trygdetid?.id shouldNotBe null
        trygdetid?.behandlingId shouldBe behandling.id
        trygdetid?.trygdetidGrunnlag?.first() shouldBe trygdetidGrunnlag
        trygdetid?.beregnetTrygdetid shouldBe beregnetTrygdetid
        trygdetid?.yrkesskade shouldBe true
    }

    @Test
    fun `skal opprette og hente trygdetid med grunnlag og poeng inn og ut aar og prorata`() {
        val behandling = behandlingMock()
        val beregnetTrygdetid = beregnetTrygdetid()
        val trygdetidGrunnlag = trygdetidGrunnlag(poengInnAar = true, poengUtAar = true, prorata = true)
        val opprettetTrygdetid =
            trygdetid(
                behandling.id,
                behandling.sak,
                trygdetidGrunnlag = listOf(trygdetidGrunnlag),
                beregnetTrygdetid = beregnetTrygdetid,
            )

        repository.opprettTrygdetid(opprettetTrygdetid)
        val trygdetid = repository.hentTrygdetid(behandling.id)

        trygdetid shouldNotBe null
        trygdetid?.id shouldNotBe null
        trygdetid?.behandlingId shouldBe behandling.id
        trygdetid?.trygdetidGrunnlag?.first() shouldBe trygdetidGrunnlag
        trygdetid?.beregnetTrygdetid shouldBe beregnetTrygdetid
    }

    @Test
    fun `skal opprette et trygdetidsgrunnlag`() {
        val behandling = behandlingMock()
        val trygdetidGrunnlag = trygdetidGrunnlag(beregnetTrygdetidGrunnlag = beregnetTrygdetidGrunnlag())
        val opprettetTrygdetid = trygdetid(behandling.id, behandling.sak)

        val lagretTrygdetid = repository.opprettTrygdetid(opprettetTrygdetid)
        val trygdetidMedTrygdetidGrunnlag =
            repository.oppdaterTrygdetid(lagretTrygdetid.leggTilEllerOppdaterTrygdetidGrunnlag(trygdetidGrunnlag))

        trygdetidMedTrygdetidGrunnlag shouldNotBe null
        with(trygdetidMedTrygdetidGrunnlag.trygdetidGrunnlag.first()) {
            this shouldBe trygdetidGrunnlag
        }
    }

    @Test
    fun `skal slette et trygdetidsgrunnlag`() {
        val behandling = behandlingMock()
        val trygdetidGrunnlag = trygdetidGrunnlag(beregnetTrygdetidGrunnlag = beregnetTrygdetidGrunnlag())
        val opprettetTrygdetid = trygdetid(behandling.id, behandling.sak)

        val lagretTrygdetid = repository.opprettTrygdetid(opprettetTrygdetid)
        val trygdetidMedTrygdetidGrunnlag =
            repository.oppdaterTrygdetid(lagretTrygdetid.leggTilEllerOppdaterTrygdetidGrunnlag(trygdetidGrunnlag))

        trygdetidMedTrygdetidGrunnlag shouldNotBe null
        with(trygdetidMedTrygdetidGrunnlag.trygdetidGrunnlag.first()) {
            this shouldBe trygdetidGrunnlag
        }

        val trygdetidUtenGrunnlag = trygdetidMedTrygdetidGrunnlag.copy(trygdetidGrunnlag = emptyList())
        val lagretTrygdetidUtenGrunnlag = repository.oppdaterTrygdetid(trygdetidUtenGrunnlag)

        lagretTrygdetidUtenGrunnlag shouldNotBe null
        lagretTrygdetidUtenGrunnlag.trygdetidGrunnlag.shouldBeEmpty()
    }

    @Test
    fun `skal oppdatere et trygdetidsgrunnlag`() {
        val behandling = behandlingMock()
        val trygdetidGrunnlag = trygdetidGrunnlag(beregnetTrygdetidGrunnlag = beregnetTrygdetidGrunnlag())
        val opprettetTrygdetid = trygdetid(behandling.id, behandling.sak, trygdetidGrunnlag = listOf(trygdetidGrunnlag))

        val trygdetid = repository.opprettTrygdetid(opprettetTrygdetid)
        val endretTrygdetidGrunnlag = trygdetidGrunnlag.copy(bosted = LandNormalisert.POLEN.isoCode)
        val trygdetidMedOppdatertGrunnlag =
            repository.oppdaterTrygdetid(trygdetid.leggTilEllerOppdaterTrygdetidGrunnlag(endretTrygdetidGrunnlag))

        trygdetidMedOppdatertGrunnlag shouldNotBe null
        with(trygdetidMedOppdatertGrunnlag.trygdetidGrunnlag.first()) {
            this shouldBe endretTrygdetidGrunnlag
        }
    }

    @Test
    fun `skal oppdatere et trygdetidsgrunnlag med begrunnelse`() {
        val behandling = behandlingMock()
        val trygdetidGrunnlag =
            trygdetidGrunnlag(
                beregnetTrygdetidGrunnlag = beregnetTrygdetidGrunnlag(),
                begrunnelse = "Test",
            )
        val opprettetTrygdetid = trygdetid(behandling.id, behandling.sak, trygdetidGrunnlag = listOf(trygdetidGrunnlag))

        val trygdetid = repository.opprettTrygdetid(opprettetTrygdetid)

        val lagretTrygdetid = repository.hentTrygdetid(behandling.id)

        lagretTrygdetid shouldNotBe null
        lagretTrygdetid?.trygdetidGrunnlag?.firstOrNull()?.begrunnelse shouldBe "Test"

        val endretTrygdetidGrunnlag =
            trygdetidGrunnlag.copy(
                bosted = LandNormalisert.POLEN.isoCode,
                begrunnelse = "Test2",
            )

        val trygdetidMedOppdatertGrunnlag =
            repository.oppdaterTrygdetid(trygdetid.leggTilEllerOppdaterTrygdetidGrunnlag(endretTrygdetidGrunnlag))

        trygdetidMedOppdatertGrunnlag shouldNotBe null
        with(trygdetidMedOppdatertGrunnlag.trygdetidGrunnlag.first()) {
            this shouldBe endretTrygdetidGrunnlag
        }
    }

    @Test
    fun `skal oppdatere et trygdetidsgrunnlag med poeng inn og ut aar og prorata`() {
        val behandling = behandlingMock()
        val trygdetidGrunnlag =
            trygdetidGrunnlag(
                beregnetTrygdetidGrunnlag = beregnetTrygdetidGrunnlag(),
                begrunnelse = "Test",
            )
        val opprettetTrygdetid = trygdetid(behandling.id, behandling.sak, trygdetidGrunnlag = listOf(trygdetidGrunnlag))

        val trygdetid = repository.opprettTrygdetid(opprettetTrygdetid)

        val lagretTrygdetid = repository.hentTrygdetid(behandling.id)

        lagretTrygdetid shouldNotBe null
        lagretTrygdetid?.trygdetidGrunnlag?.firstOrNull()?.begrunnelse shouldBe "Test"

        val endretTrygdetidGrunnlag =
            trygdetidGrunnlag.copy(
                bosted = LandNormalisert.POLEN.isoCode,
                begrunnelse = "Test2",
                poengUtAar = true,
                poengInnAar = true,
                prorata = true,
            )

        val trygdetidMedOppdatertGrunnlag =
            repository.oppdaterTrygdetid(trygdetid.leggTilEllerOppdaterTrygdetidGrunnlag(endretTrygdetidGrunnlag))

        trygdetidMedOppdatertGrunnlag shouldNotBe null
        with(trygdetidMedOppdatertGrunnlag.trygdetidGrunnlag.first()) {
            this shouldBe endretTrygdetidGrunnlag
        }
    }

    @Test
    fun `skal oppdatere beregnet trygdetid`() {
        val beregnetTrygdetid = beregnetTrygdetid(total = 12, tidspunkt = Tidspunkt.now())
        val behandling = behandlingMock()
        val opprettetTrygdetid = trygdetid(behandling.id, behandling.sak)

        val trygdetid = repository.opprettTrygdetid(opprettetTrygdetid)
        val trygdetidMedBeregnetTrygdetid =
            repository.oppdaterTrygdetid(trygdetid.oppdaterBeregnetTrygdetid(beregnetTrygdetid))

        trygdetidMedBeregnetTrygdetid shouldNotBe null
        trygdetidMedBeregnetTrygdetid.beregnetTrygdetid shouldBe beregnetTrygdetid
        trygdetidMedBeregnetTrygdetid.yrkesskade shouldBe false
    }

    @Test
    fun `skal oppdatere beregnet trygdetid med yrkesskade`() {
        val beregnetTrygdetid = beregnetTrygdetid(total = 12, tidspunkt = Tidspunkt.now(), yrkesskade = true)
        val behandling = behandlingMock()
        val opprettetTrygdetid = trygdetid(behandling.id, behandling.sak, yrkesskade = true)

        val trygdetid = repository.opprettTrygdetid(opprettetTrygdetid)
        val trygdetidMedBeregnetTrygdetid =
            repository.oppdaterTrygdetid(trygdetid.oppdaterBeregnetTrygdetid(beregnetTrygdetid))

        trygdetidMedBeregnetTrygdetid shouldNotBe null
        trygdetidMedBeregnetTrygdetid.beregnetTrygdetid shouldBe beregnetTrygdetid
        trygdetidMedBeregnetTrygdetid.yrkesskade shouldBe true
    }

    @Test
    fun `skal oppdatere trygdetid with overstyrt poengaar`() {
        val behandling = behandlingMock()
        val opprettetTrygdetid = trygdetid(behandling.id, behandling.sak)

        val trygdetid = repository.opprettTrygdetid(opprettetTrygdetid)

        val trygdetidMedOverstyrtPoengaar =
            repository.oppdaterTrygdetid(
                trygdetid.copy(overstyrtNorskPoengaar = 10),
            )

        trygdetidMedOverstyrtPoengaar shouldNotBe null
        trygdetidMedOverstyrtPoengaar.overstyrtNorskPoengaar shouldBe 10
    }

    @Test
    fun `skal nullstille beregnet trygdetid`() {
        val beregnetTrygdetid = beregnetTrygdetid(total = 12, tidspunkt = Tidspunkt.now())
        val behandling = behandlingMock()
        val opprettetTrygdetid = trygdetid(behandling.id, behandling.sak)

        val trygdetid = repository.opprettTrygdetid(opprettetTrygdetid)
        val trygdetidMedBeregnetTrygdetid =
            repository.oppdaterTrygdetid(trygdetid.oppdaterBeregnetTrygdetid(beregnetTrygdetid))

        trygdetidMedBeregnetTrygdetid.beregnetTrygdetid shouldBe beregnetTrygdetid

        val trygdetidUtenBeregning =
            repository.oppdaterTrygdetid(
                trygdetidMedBeregnetTrygdetid.nullstillBeregnetTrygdetid(),
            )
        trygdetidUtenBeregning.beregnetTrygdetid shouldBe null
    }

    @Test
    fun `trygdetiderForAvdoede finner behandling med trygdetid for samme avdøde`() {
        val behandling1 = behandlingMock()
        val behandling2 = behandlingMock()
        val behandling3 = behandlingMock()
        val fnr1 = "02438311109"
        val fnr2 = "18498248795"
        repository.opprettTrygdetid(trygdetid(behandling1.id, behandling1.sak, ident = fnr1))
        repository.opprettTrygdetid(trygdetid(behandling2.id, behandling2.sak, ident = fnr2))
        repository.opprettTrygdetid(trygdetid(behandling3.id, behandling3.sak, ident = fnr1))

        val behandlinger: List<TrygdetidPartial> =
            repository
                .hentTrygdetiderForAvdoede(
                    listOf(fnr1),
                ).sortedBy { it.ident }

        behandlinger shouldHaveSize 2
        behandlinger.find { it.behandlingId == behandling1.id }?.ident shouldBe fnr1
        behandlinger.find { it.behandlingId == behandling3.id }?.ident shouldBe fnr1
    }

    @Test
    fun `trygdetiderForAvdoede finner behandling med trygdetid for to avdøde`() {
        val behandling1 = behandlingMock()
        val behandling2 = behandlingMock()
        val behandling3 = behandlingMock()
        val fnr1 = "02438311109"
        val fnr2 = "18498248795"
        val fnr3 = "31488338237"
        repository.opprettTrygdetid(trygdetid(behandling1.id, behandling1.sak, ident = fnr1))
        repository.opprettTrygdetid(trygdetid(behandling1.id, behandling1.sak, ident = fnr2))
        repository.opprettTrygdetid(trygdetid(behandling2.id, behandling2.sak, ident = fnr1))
        repository.opprettTrygdetid(trygdetid(behandling3.id, behandling3.sak, ident = fnr3))

        val trygdetider: List<TrygdetidPartial> =
            repository.hentTrygdetiderForAvdoede(listOf(fnr1, fnr2))
        trygdetider shouldHaveSize 3
        trygdetider.groupBy { it.behandlingId }.let { byBehandling ->
            byBehandling[behandling1.id]!!.map { it.ident } shouldContainExactlyInAnyOrder listOf(fnr1, fnr2)
            byBehandling[behandling2.id]!!.map { it.ident } shouldContainExactlyInAnyOrder listOf(fnr1)
        }
    }

    @Test
    fun `trygdetiderForAvdoede returnerer tom liste hvis ingen matchende`() {
        val behandling1 = behandlingMock()

        val fnr1 = "02438311109"
        val fnr2 = "18498248795"
        repository.opprettTrygdetid(trygdetid(behandling1.id, behandling1.sak, ident = fnr1))

        val trygdetider: List<TrygdetidPartial> =
            repository.hentTrygdetiderForAvdoede(
                listOf(
                    fnr2,
                ),
            )
        trygdetider shouldBe emptyList()
    }

    private fun behandlingMock() =
        mockk<DetaljertBehandling>().apply {
            every { id } returns randomUUID()
            every { sak } returns SakId(123L)
        }
}
