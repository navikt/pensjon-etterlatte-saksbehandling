package no.nav.etterlatte.grunnlagsendring

import no.nav.etterlatte.libs.common.behandling.Saksrolle
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import no.nav.etterlatte.libs.common.grunnlag.Opplysning
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Opplysningstype
import no.nav.etterlatte.libs.common.person.FamilieRelasjon
import no.nav.etterlatte.libs.common.person.Folkeregisteridentifikator
import no.nav.etterlatte.libs.common.person.InnflyttingTilNorge
import no.nav.etterlatte.libs.common.person.UtflyttingFraNorge
import no.nav.etterlatte.libs.common.person.Utland
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.common.toJsonNode
import no.nav.etterlatte.libs.testdata.grunnlag.GrunnlagTestData
import no.nav.etterlatte.libs.testdata.grunnlag.kilde
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.LocalDate
import java.util.UUID

internal class GrunnlagHelperKtTest {
    @Test
    fun `skal hente doedsdatoer`() {
        val grunnlagDoedsdato = LocalDate.of(2022, 8, 17)
        val grunnlagDoedsdatoJsonNode = grunnlagDoedsdato.toJsonNode()
        val opplysningDoedsdato =
            Opplysningstype.DOEDSDATO to
                Opplysning.Konstant(
                    UUID.randomUUID(),
                    KILDE,
                    grunnlagDoedsdatoJsonNode,
                )
        val grunnlag =
            GrunnlagTestData(
                opplysningsmapSoekerOverrides = mapOf(opplysningDoedsdato),
                opplysningsmapGjenlevendeOverrides = mapOf(opplysningDoedsdato),
                opplysningsmapAvdoedOverrides = mapOf(opplysningDoedsdato),
                opplysningsmapSoeskenOverrides =
                    mapOf(
                        opplysningDoedsdato,
                        Opplysningstype.FOEDSELSNUMMER to
                            Opplysning.Konstant(
                                UUID.randomUUID(),
                                KILDE,
                                HELSOESKEN_FOEDSELSNUMMER.toJsonNode(),
                            ),
                    ),
            ).hentOpplysningsgrunnlag()
        val doedsdatoAvdoed = grunnlag.doedsdato(Saksrolle.AVDOED, AVDOED_FOEDSELSNUMMER.value)
        val doedsdatoGjenlevende = grunnlag.doedsdato(Saksrolle.GJENLEVENDE, GJENLEVENDE_FOEDSELSNUMMER.value)
        val doedsdatoSoeker = grunnlag.doedsdato(Saksrolle.SOEKER, SOEKER_FOEDSELSNUMMER.value)
        val doedsdatoSoesken = grunnlag.doedsdato(Saksrolle.SOESKEN, HELSOESKEN_FOEDSELSNUMMER.value)
        assertEquals(grunnlagDoedsdato, doedsdatoAvdoed?.verdi)
        assertEquals(grunnlagDoedsdato, doedsdatoGjenlevende?.verdi)
        assertEquals(grunnlagDoedsdato, doedsdatoSoeker?.verdi)
        assertEquals(grunnlagDoedsdato, doedsdatoSoesken?.verdi)
    }

    @Test
    fun `skal hente ansvarlige foreldre`() {
        val ansvarligeForeldre = listOf(AVDOED_FOEDSELSNUMMER, GJENLEVENDE_FOEDSELSNUMMER)
        val opplysningFamilierelasjon =
            Opplysningstype.FAMILIERELASJON to
                Opplysning.Konstant(
                    UUID.randomUUID(),
                    kilde,
                    FamilieRelasjon(
                        ansvarligeForeldre = ansvarligeForeldre,
                        foreldre = null,
                        barn = null,
                    ).toJsonNode(),
                )
        val grunnlag =
            GrunnlagTestData(
                opplysningsmapSoekerOverrides = mapOf(opplysningFamilierelasjon),
                opplysningsmapSoeskenOverrides = mapOf(OPPLYSNINGSTYPE_FOEDSELSNUMMER_HELSOESKEN, opplysningFamilierelasjon),
            ).hentOpplysningsgrunnlag()

        val ansvarligeForeldreSoeker = grunnlag.ansvarligeForeldre(Saksrolle.SOEKER, SOEKER_FOEDSELSNUMMER.value)
        val ansvarligeForeldreSoesken = grunnlag.ansvarligeForeldre(Saksrolle.SOESKEN, HELSOESKEN_FOEDSELSNUMMER.value)
        assertEquals(ansvarligeForeldre, ansvarligeForeldreSoeker)
        assertEquals(ansvarligeForeldre, ansvarligeForeldreSoesken)
        assertThrows<GrunnlagRolleException> {
            grunnlag.ansvarligeForeldre(
                Saksrolle.AVDOED,
                AVDOED_FOEDSELSNUMMER.value,
            )
        }
        assertThrows<GrunnlagRolleException> {
            grunnlag.ansvarligeForeldre(
                Saksrolle.GJENLEVENDE,
                GJENLEVENDE_FOEDSELSNUMMER.value,
            )
        }
        assertThrows<GrunnlagRolleException> { grunnlag.ansvarligeForeldre(Saksrolle.UKJENT, "123") }
    }

    @Test
    fun `skal hente barn`() {
        val barn = listOf(SOEKER_FOEDSELSNUMMER, HELSOESKEN_FOEDSELSNUMMER)
        val grunnlag =
            GrunnlagTestData(
                opplysningsmapAvdoedOverrides =
                    mapOf(
                        Opplysningstype.FAMILIERELASJON to
                            Opplysning.Konstant(
                                UUID.randomUUID(),
                                kilde,
                                FamilieRelasjon(
                                    ansvarligeForeldre = null,
                                    foreldre = null,
                                    barn = barn,
                                ).toJsonNode(),
                            ),
                    ),
                opplysningsmapGjenlevendeOverrides =
                    mapOf(
                        Opplysningstype.FAMILIERELASJON to
                            Opplysning.Konstant(
                                UUID.randomUUID(),
                                kilde,
                                FamilieRelasjon(
                                    ansvarligeForeldre = null,
                                    foreldre = null,
                                    barn = barn,
                                ).toJsonNode(),
                            ),
                    ),
            ).hentOpplysningsgrunnlag()
        val avdoedesBarn = grunnlag.barn(Saksrolle.AVDOED)
        val gjenlevendesBarn = grunnlag.barn(Saksrolle.GJENLEVENDE)
        assertEquals(barn, avdoedesBarn)
        assertEquals(barn, gjenlevendesBarn)
        assertThrows<GrunnlagRolleException> { grunnlag.barn(Saksrolle.SOESKEN) }
        assertThrows<GrunnlagRolleException> { grunnlag.barn(Saksrolle.UKJENT) }
    }

    @Test
    fun `skal hente utland`() {
        val utland =
            Utland(
                innflyttingTilNorge = listOf(InnflyttingTilNorge("Danmark", LocalDate.of(2007, 4, 1))),
                utflyttingFraNorge =
                    listOf(
                        UtflyttingFraNorge("Sverige", LocalDate.of(2005, 7, 8)),
                        UtflyttingFraNorge("Sveits", LocalDate.of(2022, 12, 12)),
                    ),
            )
        val opplysningUtland =
            Opplysningstype.UTLAND to
                Opplysning.Konstant(
                    UUID.randomUUID(),
                    kilde,
                    utland.toJsonNode(),
                )

        val grunnlag =
            GrunnlagTestData(
                opplysningsmapAvdoedOverrides = mapOf(opplysningUtland),
                opplysningsmapGjenlevendeOverrides = mapOf(opplysningUtland),
                opplysningsmapSoekerOverrides = mapOf(opplysningUtland),
                opplysningsmapSoeskenOverrides = mapOf(OPPLYSNINGSTYPE_FOEDSELSNUMMER_HELSOESKEN, opplysningUtland),
            ).hentOpplysningsgrunnlag()

        val utlandAvdoed = grunnlag.utland(Saksrolle.AVDOED, AVDOED_FOEDSELSNUMMER.value)
        val utlandGjenlevende = grunnlag.utland(Saksrolle.GJENLEVENDE, GJENLEVENDE_FOEDSELSNUMMER.value)
        val utlandSoeker = grunnlag.utland(Saksrolle.SOEKER, SOEKER_FOEDSELSNUMMER.value)
        val utlandSoesken = grunnlag.utland(Saksrolle.SOESKEN, HELSOESKEN_FOEDSELSNUMMER.value)
        assertEquals(utland, utlandAvdoed)
        assertEquals(utland, utlandGjenlevende)
        assertEquals(utland, utlandSoeker)
        assertEquals(utland, utlandSoesken)
    }

    companion object {
        val KILDE =
            Grunnlagsopplysning.Pdl(
                tidspunktForInnhenting = Tidspunkt.now(),
                registersReferanse = null,
                opplysningId = "opplysningsId1",
            )
        val SOEKER_FOEDSELSNUMMER = Folkeregisteridentifikator.of("30106519672")
        val HELSOESKEN_FOEDSELSNUMMER = Folkeregisteridentifikator.of("01018100157")
        val OPPLYSNINGSTYPE_FOEDSELSNUMMER_HELSOESKEN =
            Opplysningstype.FOEDSELSNUMMER to
                Opplysning.Konstant(
                    UUID.randomUUID(),
                    KILDE,
                    HELSOESKEN_FOEDSELSNUMMER.toJsonNode(),
                )
        val AVDOED_FOEDSELSNUMMER = Folkeregisteridentifikator.of("07081177656")
        val GJENLEVENDE_FOEDSELSNUMMER = Folkeregisteridentifikator.of("06048010820")
    }
}
