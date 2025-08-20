package no.nav.etterlatte.brev.varselbrev

import no.nav.etterlatte.libs.common.behandling.DetaljertBehandling
import no.nav.etterlatte.libs.common.feilhaandtering.InternfeilException
import no.nav.etterlatte.libs.common.grunnlag.Grunnlag
import no.nav.etterlatte.libs.common.grunnlag.hentDoedsdato
import java.time.YearMonth

// For å skille mellom 6 mnd varsel og over 12 mnd varsel
fun gjelderAktivitetspliktVarselOver12mnd(
    detaljertBehandling: DetaljertBehandling,
    grunnlag: Grunnlag,
): Boolean {
    val erTidligereFamiliepleier = detaljertBehandling.tidligereFamiliepleier?.svar == true

    val doedsdatoEllerOpphoertPleieforhold =
        if (erTidligereFamiliepleier) {
            detaljertBehandling.tidligereFamiliepleier!!.opphoertPleieforhold!!
        } else {
            grunnlag
                .hentAvdoede()
                .singleOrNull()
                ?.hentDoedsdato()
                ?.verdi
        }

    if (doedsdatoEllerOpphoertPleieforhold == null) {
        throw InternfeilException("Opphørt pleieforhold eller dødsdato mangler")
    }

    val virk = detaljertBehandling.virkningstidspunkt?.dato
    return virk!!.isAfter(YearMonth.from(doedsdatoEllerOpphoertPleieforhold.plusMonths(12)))
}
