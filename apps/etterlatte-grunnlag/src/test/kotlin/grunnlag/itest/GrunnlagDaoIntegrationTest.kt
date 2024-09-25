package no.nav.etterlatte.grunnlag.itest

import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.module.kotlin.treeToValue
import io.mockk.clearAllMocks
import io.mockk.spyk
import io.mockk.verify
import lagGrunnlagsopplysning
import no.nav.etterlatte.behandling.randomSakId
import no.nav.etterlatte.behandling.sakId1
import no.nav.etterlatte.behandling.sakId2
import no.nav.etterlatte.behandling.sakId3
import no.nav.etterlatte.behandling.tilSakId
import no.nav.etterlatte.grunnlag.BehandlingGrunnlagVersjon
import no.nav.etterlatte.grunnlag.GrunnlagDbExtension
import no.nav.etterlatte.grunnlag.OpplysningDao
import no.nav.etterlatte.libs.common.behandling.Persongalleri
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Opplysningstype
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Opplysningstype.AVDOED_PDL_V1
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Opplysningstype.BOSTEDSADRESSE
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Opplysningstype.GJENLEVENDE_FORELDER_PDL_V1
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Opplysningstype.INNSENDER_PDL_V1
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Opplysningstype.PERSONGALLERI_V1
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Opplysningstype.SOEKER_PDL_V1
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Opplysningstype.SOEKER_SOEKNAD_V1
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Opplysningstype.SOEKNAD_MOTTATT_DATO
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.SoeknadMottattDato
import no.nav.etterlatte.libs.common.objectMapper
import no.nav.etterlatte.libs.common.periode.Periode
import no.nav.etterlatte.libs.common.person.Folkeregisteridentifikator
import no.nav.etterlatte.libs.common.person.Person
import no.nav.etterlatte.libs.common.toJson
import no.nav.etterlatte.libs.common.toJsonNode
import no.nav.etterlatte.libs.database.toList
import no.nav.etterlatte.libs.testdata.grunnlag.ADRESSE_DEFAULT
import no.nav.etterlatte.libs.testdata.grunnlag.AVDOED2_FOEDSELSNUMMER
import no.nav.etterlatte.libs.testdata.grunnlag.AVDOED_FOEDSELSNUMMER
import no.nav.etterlatte.libs.testdata.grunnlag.GJENLEVENDE_FOEDSELSNUMMER
import no.nav.etterlatte.libs.testdata.grunnlag.HALVSOESKEN_ANNEN_FORELDER
import no.nav.etterlatte.libs.testdata.grunnlag.HALVSOESKEN_FOEDSELSNUMMER
import no.nav.etterlatte.libs.testdata.grunnlag.HELSOESKEN_FOEDSELSNUMMER
import no.nav.etterlatte.libs.testdata.grunnlag.INNSENDER_FOEDSELSNUMMER
import no.nav.etterlatte.libs.testdata.grunnlag.SOEKER2_FOEDSELSNUMMER
import no.nav.etterlatte.libs.testdata.grunnlag.SOEKER_FOEDSELSNUMMER
import no.nav.etterlatte.libs.testdata.grunnlag.kilde
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import java.time.LocalDate
import java.time.Month
import java.time.YearMonth
import java.util.UUID
import javax.sql.DataSource
import kotlin.random.Random

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExtendWith(GrunnlagDbExtension::class)
internal class GrunnlagDaoIntegrationTest(
    private val dataSource: DataSource,
) {
    private lateinit var opplysningRepo: OpplysningDao

    @BeforeAll
    fun beforeAll() {
        opplysningRepo = spyk(OpplysningDao(dataSource))
    }

    @AfterEach
    fun afterEach() {
        clearAllMocks()
    }

    @Test
    fun `Legge til opplysning og hente den etterpaa`() {
        val datoMottat = LocalDate.of(2022, Month.MAY, 3).atStartOfDay()
        val uuid = UUID.randomUUID()

        lagGrunnlagsopplysning(
            opplysningstype = SOEKNAD_MOTTATT_DATO,
            verdi = objectMapper.valueToTree(SoeknadMottattDato(datoMottat)) as ObjectNode,
        ).also { opplysningRepo.leggOpplysningTilGrunnlag(sakId1, it) }
        lagGrunnlagsopplysning(SOEKNAD_MOTTATT_DATO).also { opplysningRepo.leggOpplysningTilGrunnlag(sakId2, it) }
        lagGrunnlagsopplysning(
            opplysningstype = SOEKNAD_MOTTATT_DATO,
            uuid = uuid,
        ).also { opplysningRepo.leggOpplysningTilGrunnlag(sakId2, it) }

        assertEquals(1, opplysningRepo.finnHendelserIGrunnlag(sakId2).size)
        assertEquals(1, opplysningRepo.finnHendelserIGrunnlag(sakId2).size)
        assertEquals(
            uuid,
            opplysningRepo
                .finnHendelserIGrunnlag(sakId2)
                .first()
                .opplysning.id,
        )
        assertEquals(
            datoMottat,
            opplysningRepo
                .finnHendelserIGrunnlag(sakId1)
                .first()
                .opplysning
                .let { objectMapper.treeToValue<SoeknadMottattDato>(it.opplysning) }
                .mottattDato,
        )
    }

    @Test
    fun `kan legge til en personopplysning`() {
        val uuid = UUID.randomUUID()
        val fnr = SOEKER_FOEDSELSNUMMER

        lagGrunnlagsopplysning(
            uuid = uuid,
            opplysningstype = Opplysningstype.NAVN,
            verdi = objectMapper.valueToTree("Per"),
            fnr = fnr,
        ).also { opplysningRepo.leggOpplysningTilGrunnlag(sakId1, it, fnr) }

        assertEquals(1, opplysningRepo.finnHendelserIGrunnlag(sakId1).size)
        assertEquals(
            uuid,
            opplysningRepo
                .finnHendelserIGrunnlag(sakId1)
                .first()
                .opplysning.id,
        )
        assertEquals(
            fnr,
            opplysningRepo
                .finnHendelserIGrunnlag(sakId1)
                .first()
                .opplysning.fnr,
        )
    }

    @Test
    fun `skal liste alle nyeste opplysningene knyttet til en sak`() {
        lagGrunnlagsopplysning(SOEKNAD_MOTTATT_DATO).also { opplysningRepo.leggOpplysningTilGrunnlag(sakId1, it) }
        lagGrunnlagsopplysning(SOEKNAD_MOTTATT_DATO).also { opplysningRepo.leggOpplysningTilGrunnlag(sakId2, it) }
        lagGrunnlagsopplysning(SOEKER_PDL_V1).also { opplysningRepo.leggOpplysningTilGrunnlag(sakId2, it) }

        assertEquals(1, opplysningRepo.finnHendelserIGrunnlag(sakId1).size)
        assertEquals(2, opplysningRepo.finnHendelserIGrunnlag(sakId2).size)
    }

    @Test
    fun `Hente nyeste grunnlag av type`() {
        val sakId = randomSakId()
        val behandlingId = UUID.randomUUID()
        val type = SOEKER_PDL_V1

        lagGrunnlagsopplysning(type, verdi = opprettMockPerson(SOEKER_FOEDSELSNUMMER).toJsonNode())
            .also { opplysningRepo.leggOpplysningTilGrunnlag(sakId, it) }
        lagGrunnlagsopplysning(type, verdi = opprettMockPerson(SOEKER_FOEDSELSNUMMER).toJsonNode())
            .also { opplysningRepo.leggOpplysningTilGrunnlag(sakId, it) }

        val forventetOpplysning =
            lagGrunnlagsopplysning(type, verdi = opprettMockPerson(SOEKER_FOEDSELSNUMMER).toJsonNode())
        val forventetHendelsenummer = opplysningRepo.leggOpplysningTilGrunnlag(sakId, forventetOpplysning)

        assertNull(
            opplysningRepo.finnNyesteGrunnlagForBehandling(behandlingId, type),
            "Skal ikke være låst til behandlingId enda",
        )
        assertEquals(3, forventetHendelsenummer)

        opplysningRepo.oppdaterVersjonForBehandling(behandlingId, sakId, forventetHendelsenummer)

        // Legge til to nye grunnlag på saken, men ikke koble til behandlingId
        lagGrunnlagsopplysning(type, verdi = opprettMockPerson(SOEKER_FOEDSELSNUMMER).toJsonNode())
            .also { opplysningRepo.leggOpplysningTilGrunnlag(sakId, it) }
        val sisteOpplysning =
            lagGrunnlagsopplysning(type, verdi = opprettMockPerson(SOEKER_FOEDSELSNUMMER).toJsonNode())
                .also { opplysningRepo.leggOpplysningTilGrunnlag(sakId, it) }

        val sisteGrunnlagForSak = opplysningRepo.finnNyesteGrunnlagForSak(sakId, type)!!
        assertEquals(5, sisteGrunnlagForSak.hendelseNummer)
        assertEquals(sisteOpplysning.toJson(), sisteGrunnlagForSak.opplysning.toJson())

        val sisteGrunnlagForBehandling = opplysningRepo.finnNyesteGrunnlagForBehandling(behandlingId, type)!!
        assertEquals(forventetHendelsenummer, sisteGrunnlagForBehandling.hendelseNummer)
        assertEquals(forventetOpplysning.toJson(), sisteGrunnlagForBehandling.opplysning.toJson())

        assertEquals(sisteGrunnlagForSak.sakId, sisteGrunnlagForBehandling.sakId)
        assertNotEquals(sisteGrunnlagForSak.hendelseNummer, sisteGrunnlagForBehandling.hendelseNummer)
        assertNotEquals(sisteGrunnlagForSak.opplysning.toJson(), sisteGrunnlagForBehandling.opplysning.toJson())
    }

    @Test
    fun `Skal hente opplysning fra nyeste hendelse basert paa SAK_ID og opplysningType`() {
        val uuid = UUID.randomUUID()
        val sakId = randomSakId()

        lagGrunnlagsopplysning(AVDOED_PDL_V1).also { opplysningRepo.leggOpplysningTilGrunnlag(sakId, it) }
        lagGrunnlagsopplysning(AVDOED_PDL_V1, uuid = uuid).also { opplysningRepo.leggOpplysningTilGrunnlag(sakId, it) }

        assertEquals(uuid, opplysningRepo.finnNyesteGrunnlagForSak(sakId, AVDOED_PDL_V1)?.opplysning?.id)
        // Skal håndtere at opplysning ikke finnes
        assertEquals(null, opplysningRepo.finnNyesteGrunnlagForSak(randomSakId(), AVDOED_PDL_V1))
    }

    @Test
    fun `Skal hente opplysning fra nyeste hendelse basert paa BEHANDLING_ID og opplysningType`() {
        val sakId = randomSakId()
        val behandlingId = UUID.randomUUID()

        lagGrunnlagsopplysning(AVDOED_PDL_V1).also { opplysningRepo.leggOpplysningTilGrunnlag(sakId, it) }
        lagGrunnlagsopplysning(
            AVDOED_PDL_V1,
            uuid = behandlingId,
        ).also { opplysningRepo.leggOpplysningTilGrunnlag(sakId, it) }

        opplysningRepo.oppdaterVersjonForBehandling(behandlingId, sakId, 2)

        assertEquals(
            behandlingId,
            opplysningRepo.finnNyesteGrunnlagForBehandling(behandlingId, AVDOED_PDL_V1)?.opplysning?.id,
        )
        // Skal håndtere at opplysning ikke finnes
        assertEquals(
            null,
            opplysningRepo.finnNyesteGrunnlagForBehandling(UUID.randomUUID(), AVDOED_PDL_V1),
        )
    }

    @Test
    fun `Ny opplysning skal overskrive gammel, new way`() {
        val uuid = UUID.randomUUID()

        lagGrunnlagsopplysning(SOEKNAD_MOTTATT_DATO).also { opplysningRepo.leggOpplysningTilGrunnlag(sakId2, it) }
        lagGrunnlagsopplysning(SOEKNAD_MOTTATT_DATO, uuid = uuid).also {
            opplysningRepo.leggOpplysningTilGrunnlag(
                sakId2,
                it,
            )
        }

        opplysningRepo.finnHendelserIGrunnlag(sakId2).also {
            assertEquals(1, it.size)
            assertEquals(uuid, it.first().opplysning.id)
        }
    }

    @Test
    fun `kan lage og hente periodiserte opplysninger`() {
        val opplysning =
            Grunnlagsopplysning(
                id = UUID.randomUUID(),
                kilde = kilde,
                opplysningType = BOSTEDSADRESSE,
                meta = objectMapper.createObjectNode(),
                opplysning = ADRESSE_DEFAULT.first().toJsonNode(),
                attestering = null,
                fnr = SOEKER_FOEDSELSNUMMER,
                periode =
                    Periode(
                        fom = YearMonth.of(2022, 1),
                        tom = YearMonth.of(2022, 12),
                    ),
            )

        opplysningRepo.leggOpplysningTilGrunnlag(sakId1, opplysning, SOEKER_FOEDSELSNUMMER)
        val expected = OpplysningDao.GrunnlagHendelse(opplysning, sakId1, 1).toJson()
        val actual = opplysningRepo.hentAlleGrunnlagForSak(sakId1).first().toJson()

        assertEquals(expected, actual)
    }

    @Test
    fun `Finn alle persongalleri person er tilknyttet`() {
        val gjenlevendeFnr = GJENLEVENDE_FOEDSELSNUMMER

        val barnepensjonSoeker1 = SOEKER_FOEDSELSNUMMER
        val persongalleri1 =
            Persongalleri(
                soeker = barnepensjonSoeker1.value,
                innsender = GJENLEVENDE_FOEDSELSNUMMER.value,
                soesken =
                    listOf(
                        SOEKER2_FOEDSELSNUMMER.value,
                        HELSOESKEN_FOEDSELSNUMMER.value,
                    ),
                avdoed = listOf(AVDOED_FOEDSELSNUMMER.value),
                gjenlevende = listOf(GJENLEVENDE_FOEDSELSNUMMER.value),
            )

        val opplysning1 =
            lagGrunnlagsopplysning(PERSONGALLERI_V1, verdi = persongalleri1.toJsonNode(), fnr = barnepensjonSoeker1)
        opplysningRepo.leggOpplysningTilGrunnlag(sakId1, opplysning1, barnepensjonSoeker1)

        val barnepensjonSoeker2 = SOEKER2_FOEDSELSNUMMER
        val persongalleri2 =
            Persongalleri(
                soeker = barnepensjonSoeker2.value,
                innsender = gjenlevendeFnr.value,
                soesken =
                    listOf(
                        SOEKER_FOEDSELSNUMMER.value,
                        HELSOESKEN_FOEDSELSNUMMER.value,
                    ),
                avdoed = listOf(AVDOED_FOEDSELSNUMMER.value),
                gjenlevende = listOf(gjenlevendeFnr.value),
            )

        val opplysning2 =
            lagGrunnlagsopplysning(PERSONGALLERI_V1, verdi = persongalleri2.toJsonNode(), fnr = barnepensjonSoeker2)
        opplysningRepo.leggOpplysningTilGrunnlag(sakId2, opplysning2, barnepensjonSoeker2)

        val persongalleri3 =
            Persongalleri(
                soeker = gjenlevendeFnr.value,
                innsender = gjenlevendeFnr.value,
                avdoed = listOf(AVDOED_FOEDSELSNUMMER.value),
            )

        val opplysning3 =
            lagGrunnlagsopplysning(PERSONGALLERI_V1, verdi = persongalleri3.toJsonNode(), fnr = gjenlevendeFnr)
        opplysningRepo.leggOpplysningTilGrunnlag(sakId3, opplysning3, gjenlevendeFnr)

        // gjenlevende skal finnes i 3 behandlingshendelser
        assertEquals(3, opplysningRepo.finnAllePersongalleriHvorPersonFinnes(gjenlevendeFnr).size)
        // BP soeker 1 skal finnes i 2 behandlingshendelser
        assertEquals(2, opplysningRepo.finnAllePersongalleriHvorPersonFinnes(barnepensjonSoeker1).size)
        // BP soeker 2 skal finnes i 2 behandlingshendelser
        assertEquals(2, opplysningRepo.finnAllePersongalleriHvorPersonFinnes(barnepensjonSoeker2).size)
        // Søsken GROENN_STAUDE har ikke søkt, men skal finnes i 2 behandlingshendelser
        assertEquals(2, opplysningRepo.finnAllePersongalleriHvorPersonFinnes(HELSOESKEN_FOEDSELSNUMMER).size)

        assertTrue(opplysningRepo.finnAllePersongalleriHvorPersonFinnes(HALVSOESKEN_ANNEN_FORELDER).isEmpty())
        assertTrue(opplysningRepo.finnAllePersongalleriHvorPersonFinnes(HALVSOESKEN_FOEDSELSNUMMER).isEmpty())

        verify(exactly = 1) { opplysningRepo.leggOpplysningTilGrunnlag(sakId1, opplysning1, barnepensjonSoeker1) }
        verify(exactly = 1) { opplysningRepo.leggOpplysningTilGrunnlag(sakId2, opplysning2, barnepensjonSoeker2) }
        verify(exactly = 1) { opplysningRepo.leggOpplysningTilGrunnlag(sakId3, opplysning3, gjenlevendeFnr) }
    }

    @Test
    fun `Uthenting av alle saker tilknyttet person fungerer`() {
        val grunnlagsopplysning1 = lagGrunnlagsopplysning(AVDOED_PDL_V1, fnr = AVDOED_FOEDSELSNUMMER)
        opplysningRepo.leggOpplysningTilGrunnlag(sakId1, grunnlagsopplysning1, AVDOED_FOEDSELSNUMMER)

        val grunnlagsopplysning2 =
            lagGrunnlagsopplysning(
                PERSONGALLERI_V1,
                verdi =
                    Persongalleri(
                        soeker = SOEKER_FOEDSELSNUMMER.value,
                        innsender = AVDOED2_FOEDSELSNUMMER.value,
                        gjenlevende = listOf(AVDOED2_FOEDSELSNUMMER.value),
                        avdoed = listOf(AVDOED_FOEDSELSNUMMER.value),
                    ).toJsonNode(),
            )
        opplysningRepo.leggOpplysningTilGrunnlag(sakId2, grunnlagsopplysning2)

        val grunnlagsopplysning3 =
            lagGrunnlagsopplysning(
                PERSONGALLERI_V1,
                verdi =
                    Persongalleri(
                        soeker = SOEKER_FOEDSELSNUMMER.value,
                        gjenlevende = emptyList(),
                        innsender = INNSENDER_FOEDSELSNUMMER.value,
                        avdoed = listOf(AVDOED_FOEDSELSNUMMER.value, AVDOED2_FOEDSELSNUMMER.value),
                    ).toJsonNode(),
            )
        opplysningRepo.leggOpplysningTilGrunnlag(sakId3, grunnlagsopplysning3)

        // mange dummy-opplysninger tilknyttet andre personer, skal ignoreres...
        listOf(SOEKER_FOEDSELSNUMMER, HALVSOESKEN_ANNEN_FORELDER, GJENLEVENDE_FOEDSELSNUMMER, HALVSOESKEN_FOEDSELSNUMMER).forEachIndexed {
                i,
                fnr,
            ->
            val sakId = tilSakId(i.toLong())
            opplysningRepo.leggOpplysningTilGrunnlag(sakId, lagGrunnlagsopplysning(SOEKER_SOEKNAD_V1, fnr = fnr))
            opplysningRepo.leggOpplysningTilGrunnlag(sakId, lagGrunnlagsopplysning(INNSENDER_PDL_V1, fnr = fnr))
            opplysningRepo.leggOpplysningTilGrunnlag(sakId, lagGrunnlagsopplysning(AVDOED_PDL_V1, fnr = fnr))
            opplysningRepo.leggOpplysningTilGrunnlag(sakId, lagGrunnlagsopplysning(PERSONGALLERI_V1, fnr = fnr))
        }

        val result = opplysningRepo.finnAlleSakerForPerson(AVDOED_FOEDSELSNUMMER)
        assertEquals(3, result.size)

        verify(exactly = 19) { opplysningRepo.leggOpplysningTilGrunnlag(any(), any(), any()) }
    }

    @Test
    fun `Lagring av versjon for behandling`() {
        val sakId = randomSakId()
        val behandlingId = UUID.randomUUID()

        assertNull(opplysningRepo.hentBehandlingVersjon(behandlingId))

        val hendelsenummer1 = 1L
        opplysningRepo.oppdaterVersjonForBehandling(behandlingId, sakId, hendelsenummer1)

        val versjon1 = opplysningRepo.hentBehandlingVersjon(behandlingId)!!
        assertEquals(behandlingId, versjon1.behandlingId)
        assertEquals(sakId, versjon1.sakId)
        assertEquals(hendelsenummer1, versjon1.hendelsenummer)
        assertFalse(versjon1.laast)

        val hendelsenummer2 = 42L
        opplysningRepo.oppdaterVersjonForBehandling(behandlingId, sakId, hendelsenummer2)

        val versjon2 = opplysningRepo.hentBehandlingVersjon(behandlingId)!!
        assertEquals(hendelsenummer2, versjon2.hendelsenummer)

        opplysningRepo.laasGrunnlagVersjonForBehandling(behandlingId)

        val laastVersjon = opplysningRepo.hentBehandlingVersjon(behandlingId)!!
        assertTrue(laastVersjon.laast)

        // TODO: Teste endring av låst sak når det er avklart hvordan det skal håndteres
    }

    @Test
    fun `Hente grunnlag på behandling`() {
        val sakId = randomSakId()
        val behandlingId = UUID.randomUUID()

        opplysningRepo.leggOpplysningTilGrunnlag(sakId, lagGrunnlagsopplysning(SOEKER_PDL_V1))
        opplysningRepo.leggOpplysningTilGrunnlag(sakId, lagGrunnlagsopplysning(AVDOED_PDL_V1))
        val sisteHendelsenummerForBehandling =
            opplysningRepo.leggOpplysningTilGrunnlag(sakId, lagGrunnlagsopplysning(GJENLEVENDE_FORELDER_PDL_V1))

        opplysningRepo.oppdaterVersjonForBehandling(behandlingId, sakId, sisteHendelsenummerForBehandling)

        repeat(10) {
            val annenBehandlingId = UUID.randomUUID()
            val tempHendelsenummer =
                Opplysningstype.entries.toTypedArray().maxOf { opplysningstype ->
                    opplysningRepo.leggOpplysningTilGrunnlag(sakId, lagGrunnlagsopplysning(opplysningstype))
                }
            opplysningRepo.oppdaterVersjonForBehandling(annenBehandlingId, sakId, tempHendelsenummer)
        }

        val opplysninger = opplysningRepo.hentAlleGrunnlagForBehandling(behandlingId)
        assertEquals(3, opplysninger.size)

        val sisteVersjon = opplysningRepo.hentBehandlingVersjon(behandlingId)
        assertEquals(sisteHendelsenummerForBehandling, sisteVersjon?.hendelsenummer)

        val opplysningerForSak = opplysningRepo.hentAlleGrunnlagForSak(sakId)
        assertEquals(413, opplysningerForSak.size)
    }

    @Test
    fun `Lagring av versjon på flere behandlinger på samme sak`() {
        val sakId = randomSakId()

        val antall = 10

        repeat(antall) {
            opplysningRepo.oppdaterVersjonForBehandling(UUID.randomUUID(), sakId, Random.nextLong())
        }

        val versjoner =
            dataSource.connection.use {
                it
                    .prepareStatement("SELECT * FROM behandling_versjon WHERE sak_id = $sakId")
                    .executeQuery()
                    .toList {
                        BehandlingGrunnlagVersjon(
                            getObject("behandling_id") as UUID,
                            getLong("sak_id"),
                            getLong("hendelsenummer"),
                            getBoolean("laast"),
                        )
                    }
            }

        assertEquals(antall, versjoner.size)
        versjoner.forEach {
            assertEquals(sakId, it.sakId)
        }
    }

    private fun opprettMockPerson(fnr: Folkeregisteridentifikator): Person =
        Person(
            UUID.randomUUID().toString(),
            null,
            UUID.randomUUID().toString(),
            fnr,
            null,
            1234,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
        )
}
