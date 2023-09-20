package no.nav.etterlatte.libs.common.gyldigSoeknad.gyldighetsgrunnlag

import no.nav.etterlatte.libs.common.person.VergemaalEllerFremtidsfullmakt

data class IngenAnnenVergeEnnForelderGrunnlag(
    var vergemaalEllerFremtidsfullmakt: List<VergemaalEllerFremtidsfullmakt>?,
)
