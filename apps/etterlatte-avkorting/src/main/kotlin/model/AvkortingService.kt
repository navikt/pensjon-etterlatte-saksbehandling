package model

import no.nav.etterlatte.libs.common.avkorting.AvkortingsResultat
import no.nav.etterlatte.libs.common.avkorting.AvkortingsResultatType
import no.nav.etterlatte.libs.common.avkorting.Avkortingsperiode
import no.nav.etterlatte.libs.common.avkorting.Avkortingstyper
import no.nav.etterlatte.libs.common.avkorting.Endringskode
import no.nav.etterlatte.libs.common.beregning.BeregningsResultat
import no.nav.etterlatte.libs.common.beregning.Beregningsperiode
import org.slf4j.LoggerFactory
import java.time.LocalDateTime
import java.util.*

class AvkortingService {
    private val logger = LoggerFactory.getLogger(AvkortingService::class.java)

    fun avkortingsResultat(beregningsResultat: BeregningsResultat): AvkortingsResultat {
        logger.info("Leverer en fake beregning")
        return AvkortingsResultat(
            // TODO se på hvordan denne initieres
            id = UUID.randomUUID(),
            type = beregningsResultat.type,
            endringskode = Endringskode.NY,
            resultat = AvkortingsResultatType.BEREGNET,
            beregningsperioder = avkortPerioder(beregningsResultat.beregningsperioder),
            beregnetDato = LocalDateTime.now()
        )
    }

    // TODO implementere avkortning (må ha inn inntekt)
    private fun beregnAvkorting(belop: Int): Int {
        return belop
    }

    private fun avkortPerioder(beregningsperioder: List<Beregningsperiode>): List<Avkortingsperiode> {
        var i = 1
        var avkortingsperioder: List<Avkortingsperiode> = emptyList()
        for (periode in beregningsperioder) {
            avkortingsperioder = avkortingsperioder + Avkortingsperiode(
                avkortingsId = i++.toString(),
                type = Avkortingstyper.INNTEKT,
                datoFOM = periode.datoFOM,
                datoTOM = periode.datoTOM,
                belop = beregnAvkorting(periode.grunnbelopMnd)
            )
        }
        return avkortingsperioder
    }
}