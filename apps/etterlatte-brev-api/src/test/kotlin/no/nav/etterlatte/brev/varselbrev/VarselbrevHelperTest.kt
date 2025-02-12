package no.nav.etterlatte.brev.varselbrev

import com.fasterxml.jackson.databind.JsonNode
import io.mockk.every
import io.mockk.mockk
import no.nav.etterlatte.libs.common.behandling.DetaljertBehandling
import no.nav.etterlatte.libs.common.grunnlag.Grunnlag
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsdata
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import no.nav.etterlatte.libs.common.grunnlag.Opplysning
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Opplysningstype
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.common.toJsonNode
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.time.LocalDate
import java.time.YearMonth
import java.util.UUID.randomUUID

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class VarselbrevHelperTest {
    @Nested
    inner class GjelderAktivitetspliktVarselOver12mnd {
        @Test
        fun `skal returnere True hvis varsel over 12 mnd`() {
            val detaljertBehandling = mockk<DetaljertBehandling>()
            val grunnlag = mockk<Grunnlag>()

            val avdoedGrunnlag = mockk<Grunnlagsdata<JsonNode>>()
            every { grunnlag.hentAvdoede() } returns listOf(avdoedGrunnlag)

            // opphoertPleierforhold
            every { detaljertBehandling.tidligereFamiliepleier?.svar } returns true
            every { detaljertBehandling.tidligereFamiliepleier?.opphoertPleieforhold } returns LocalDate.of(2023, 12, 1)
            every { detaljertBehandling.virkningstidspunkt?.dato } returns YearMonth.of(2025, 1)
            assertTrue(gjelderAktivitetspliktVarselOver12mnd(detaljertBehandling, grunnlag))

            // doedsdato
            every { detaljertBehandling.tidligereFamiliepleier?.svar } returns false
            every { avdoedGrunnlag[Opplysningstype.DOEDSDATO] } answers {
                Opplysning.Konstant(
                    id = randomUUID(),
                    kilde = Grunnlagsopplysning.Saksbehandler("", Tidspunkt.now()),
                    verdi = LocalDate.of(2023, 12, 1).toJsonNode(),
                )
            }

            assertTrue(gjelderAktivitetspliktVarselOver12mnd(detaljertBehandling, grunnlag))
        }

        @Test
        fun `skal returnere False hvis varsel under 12 mnd`() {
            val detaljertBehandling = mockk<DetaljertBehandling>()
            val grunnlag = mockk<Grunnlag>()

            val avdoedGrunnlag = mockk<Grunnlagsdata<JsonNode>>()
            every { grunnlag.hentAvdoede() } returns listOf(avdoedGrunnlag)

            // opphoertPleierforhold
            every { detaljertBehandling.tidligereFamiliepleier?.svar } returns true
            every { detaljertBehandling.tidligereFamiliepleier?.opphoertPleieforhold } returns LocalDate.of(2023, 12, 1)
            every { detaljertBehandling.virkningstidspunkt?.dato } returns YearMonth.of(2024, 1)
            assertFalse(gjelderAktivitetspliktVarselOver12mnd(detaljertBehandling, grunnlag))

            // doedsdato
            every { detaljertBehandling.tidligereFamiliepleier?.svar } returns false
            every { avdoedGrunnlag[Opplysningstype.DOEDSDATO] } answers {
                Opplysning.Konstant(
                    id = randomUUID(),
                    kilde = Grunnlagsopplysning.Saksbehandler("", Tidspunkt.now()),
                    verdi = LocalDate.of(2023, 12, 1).toJsonNode(),
                )
            }

            assertFalse(gjelderAktivitetspliktVarselOver12mnd(detaljertBehandling, grunnlag))
        }
    }
}
