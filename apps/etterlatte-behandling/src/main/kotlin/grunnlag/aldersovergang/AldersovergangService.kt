package no.nav.etterlatte.grunnlag.aldersovergang

import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.feilhaandtering.UgyldigForespoerselException
import no.nav.etterlatte.libs.common.grunnlag.opplysningstyper.Opplysningstype
import no.nav.etterlatte.libs.common.person.PersonRolle
import no.nav.etterlatte.libs.common.sak.SakId
import no.nav.etterlatte.libs.ktor.token.BrukerTokenInfo
import java.time.LocalDate
import java.time.YearMonth

interface IAldersovergangService {
    suspend fun aldersovergangMaaned(
        sakId: SakId,
        sakType: SakType,
        brukerTokenInfo: BrukerTokenInfo,
    ): YearMonth?

    suspend fun hentSoekereFoedtIEnGittMaaned(
        maaned: YearMonth,
        brukerTokenInfo: BrukerTokenInfo,
    ): List<String>

    suspend fun hentSakerHvorDoedsfallForekomIGittMaaned(
        behandlingsmaaned: YearMonth,
        brukerTokenInfo: BrukerTokenInfo,
    ): List<String>

    fun hentAlder(
        sakId: SakId,
        rolle: PersonRolle,
        paaDato: LocalDate,
    ): Alder?
}

class AldersovergangService(
    private val dao: AldersovergangDao,
) : IAldersovergangService {
    override suspend fun aldersovergangMaaned(
        sakId: SakId,
        sakType: SakType,
        brukerTokenInfo: BrukerTokenInfo,
    ): YearMonth? {
        val foedselsdato = dao.hentFoedselsdato(sakId, Opplysningstype.SOEKER_PDL_V1)
        return when (sakType) {
            SakType.BARNEPENSJON -> throw UgyldigForespoerselException(
                "SAKTYPE_IKKE_STØTTET",
                "Uthenting av måned for opphør grunnet aldersovergang støttes ikke for barnepensjon",
            )
            // Mottaker av Omstillingstønad opphører måned etter fylt 67 (§ 22-12 sjette ledd)
            SakType.OMSTILLINGSSTOENAD -> foedselsdato?.let { YearMonth.from(it.plusYears(67).plusMonths(1)) }
        }
    }

    override suspend fun hentSoekereFoedtIEnGittMaaned(
        maaned: YearMonth,
        brukerTokenInfo: BrukerTokenInfo,
    ) = dao.hentSoekereFoedtIEnGittMaaned(maaned)

    override suspend fun hentSakerHvorDoedsfallForekomIGittMaaned(
        behandlingsmaaned: YearMonth,
        brukerTokenInfo: BrukerTokenInfo,
    ) = dao.hentSakerHvorDoedsfallForekomIGittMaaned(behandlingsmaaned)

    override fun hentAlder(
        sakId: SakId,
        rolle: PersonRolle,
        paaDato: LocalDate,
    ): Alder? =
        when (rolle) {
            PersonRolle.BARN -> dao.hentFoedselsdato(sakId, Opplysningstype.SOEKER_PDL_V1)?.until(paaDato)?.years
            else -> throw NotImplementedError("Ikke støtta å finne alder per nå for rolle $rolle")
        }
}

typealias Alder = Int
