package no.nav.etterlatte.brev

import no.nav.etterlatte.brev.behandling.Beregningsperiode
import no.nav.etterlatte.libs.common.IntBroek
import no.nav.etterlatte.libs.common.beregning.BeregningsMetode
import no.nav.etterlatte.libs.testdata.grunnlag.AVDOED_FOEDSELSNUMMER
import no.nav.pensjon.brevbaker.api.model.Kroner
import java.time.LocalDate

fun beregningsperiode(
    datoFOM: LocalDate = LocalDate.now(),
    datoTOM: LocalDate? = null,
    grunnbeloep: Kroner = Kroner(12345),
    antallBarn: Int = 0,
    utbetaltBeloep: Kroner = Kroner(100000),
    trygdetid: Int = 40,
    trygdetidForIdent: String? = AVDOED_FOEDSELSNUMMER.value,
    prorataBroek: IntBroek? = null,
    institusjon: Boolean = false,
    beregningsMetodeAnvendt: BeregningsMetode = BeregningsMetode.NASJONAL,
    beregningsMetodeFraGrunnlag: BeregningsMetode = BeregningsMetode.BEST,
    avdoedeForeldre: List<String?>? = null,
): Beregningsperiode =
    Beregningsperiode(
        datoFOM = datoFOM,
        datoTOM = datoTOM,
        grunnbeloep = grunnbeloep,
        antallBarn = antallBarn,
        utbetaltBeloep = utbetaltBeloep,
        trygdetid = trygdetid,
        trygdetidForIdent = trygdetidForIdent,
        prorataBroek = prorataBroek,
        institusjon = institusjon,
        beregningsMetodeAnvendt = beregningsMetodeAnvendt,
        beregningsMetodeFraGrunnlag = beregningsMetodeFraGrunnlag,
        avdoedeForeldre = avdoedeForeldre,
    )
