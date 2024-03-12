package no.nav.etterlatte.trygdetid

import no.nav.etterlatte.libs.common.Vedtaksloesning
import no.nav.etterlatte.libs.common.behandling.BehandlingStatus
import no.nav.etterlatte.libs.common.behandling.BehandlingType
import no.nav.etterlatte.libs.common.behandling.DetaljertBehandling
import no.nav.etterlatte.libs.common.behandling.JaNei
import no.nav.etterlatte.libs.common.behandling.Prosesstype
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.common.toJsonNode
import no.nav.etterlatte.libs.common.trygdetid.DetaljertBeregnetTrygdetidResultat
import no.nav.etterlatte.libs.common.trygdetid.avtale.Trygdeavtale
import no.nav.etterlatte.libs.testdata.grunnlag.GrunnlagTestData
import no.nav.etterlatte.token.Saksbehandler
import java.time.LocalDate
import java.time.Period
import java.util.UUID
import java.util.UUID.randomUUID

val saksbehandler = Saksbehandler("token", "ident", null)

private val pdlKilde: Grunnlagsopplysning.Pdl = Grunnlagsopplysning.Pdl(Tidspunkt.now(), null, "opplysningsId1")

private val regelKilde: Grunnlagsopplysning.RegelKilde = Grunnlagsopplysning.RegelKilde("regel", Tidspunkt.now(), "1")

fun behandling(
    behandlingId: UUID = randomUUID(),
    sakId: Long = 1,
    behandlingStatus: BehandlingStatus = BehandlingStatus.VILKAARSVURDERT,
) = DetaljertBehandling(
    id = behandlingId,
    sak = sakId,
    sakType = SakType.BARNEPENSJON,
    soeker = "",
    status = behandlingStatus,
    behandlingType = BehandlingType.FØRSTEGANGSBEHANDLING,
    virkningstidspunkt = null,
    boddEllerArbeidetUtlandet = null,
    utlandstilknytning = null,
    revurderingsaarsak = null,
    revurderingInfo = null,
    prosesstype = Prosesstype.AUTOMATISK,
    kilde = Vedtaksloesning.GJENNY,
)

fun trygdetid(
    behandlingId: UUID = randomUUID(),
    sakId: Long = 1,
    ident: String = GrunnlagTestData().avdoede.first().foedselsnummer.value,
    beregnetTrygdetid: DetaljertBeregnetTrygdetid? = null,
    trygdetidGrunnlag: List<TrygdetidGrunnlag> = emptyList(),
    opplysninger: List<Opplysningsgrunnlag> = standardOpplysningsgrunnlag(),
    yrkesskade: Boolean = false,
) = Trygdetid(
    id = randomUUID(),
    sakId = sakId,
    behandlingId = behandlingId,
    trygdetidGrunnlag = trygdetidGrunnlag,
    opplysninger = opplysninger,
    beregnetTrygdetid = beregnetTrygdetid,
    ident = ident,
    yrkesskade = yrkesskade,
)

fun standardOpplysningsgrunnlag(): List<Opplysningsgrunnlag> {
    val foedselsdato = LocalDate.of(2000, 1, 1)
    val doedsdato = LocalDate.of(2020, 1, 1)
    val seksten = LocalDate.of(2016, 1, 1)
    val seksti = LocalDate.of(2066, 1, 1)

    return opplysningsgrunnlag(foedselsdato, doedsdato, seksten, seksti)
}

private fun opplysningsgrunnlag(
    foedselsdato: LocalDate,
    doedsdato: LocalDate,
    seksten: LocalDate,
    seksti: LocalDate,
): List<Opplysningsgrunnlag> {
    return listOf(
        Opplysningsgrunnlag.ny(TrygdetidOpplysningType.FOEDSELSDATO, pdlKilde, foedselsdato),
        Opplysningsgrunnlag.ny(TrygdetidOpplysningType.DOEDSDATO, pdlKilde, doedsdato),
        Opplysningsgrunnlag.ny(TrygdetidOpplysningType.FYLT_16, regelKilde, seksten),
        Opplysningsgrunnlag.ny(TrygdetidOpplysningType.FYLLER_66, regelKilde, seksti),
    )
}

fun trygdetidGrunnlag(
    beregnetTrygdetidGrunnlag: BeregnetTrygdetidGrunnlag? = beregnetTrygdetidGrunnlag(),
    periode: TrygdetidPeriode =
        TrygdetidPeriode(
            fra = LocalDate.of(2023, 1, 1),
            til = LocalDate.of(2023, 2, 1),
        ),
    begrunnelse: String? = null,
    poengInnAar: Boolean = false,
    poengUtAar: Boolean = false,
    prorata: Boolean = false,
    trygdetidType: TrygdetidType = TrygdetidType.FAKTISK,
) = TrygdetidGrunnlag(
    id = randomUUID(),
    type = trygdetidType,
    bosted = LandNormalisert.NORGE.isoCode,
    periode = periode,
    beregnetTrygdetid = beregnetTrygdetidGrunnlag,
    kilde = Grunnlagsopplysning.Saksbehandler(ident = "Z123", tidspunkt = Tidspunkt.now()),
    begrunnelse = begrunnelse,
    poengUtAar = poengUtAar,
    poengInnAar = poengInnAar,
    prorata = prorata,
)

fun trygdeavtale(
    behandlingId: UUID,
    avtaleKode: String,
    avtaleDatoKode: String? = null,
    avtaleKriteriaKode: String? = null,
    personKrets: JaNei? = null,
    arbInntekt1G: JaNei? = null,
    arbInntekt1GKommentar: String? = null,
    beregArt50: JaNei? = null,
    beregArt50Kommentar: String? = null,
    nordiskTrygdeAvtale: JaNei? = null,
    nordiskTrygdeAvtaleKommentar: String? = null,
) = Trygdeavtale(
    id = randomUUID(),
    behandlingId = behandlingId,
    avtaleKode = avtaleKode,
    avtaleDatoKode = avtaleDatoKode,
    avtaleKriteriaKode = avtaleKriteriaKode,
    personKrets = null,
    arbInntekt1G = null,
    arbInntekt1GKommentar = null,
    beregArt50 = null,
    beregArt50Kommentar = null,
    nordiskTrygdeAvtale = null,
    nordiskTrygdeAvtaleKommentar = null,
    kilde = Grunnlagsopplysning.Saksbehandler(ident = "Z123", tidspunkt = Tidspunkt.now()),
)

fun beregnetTrygdetid(
    total: Int = 0,
    tidspunkt: Tidspunkt = Tidspunkt.now(),
    yrkesskade: Boolean = false,
) = DetaljertBeregnetTrygdetid(
    resultat =
        DetaljertBeregnetTrygdetidResultat(
            faktiskTrygdetidNorge = null,
            faktiskTrygdetidTeoretisk = null,
            fremtidigTrygdetidNorge = null,
            fremtidigTrygdetidTeoretisk = null,
            samletTrygdetidNorge = total,
            samletTrygdetidTeoretisk = null,
            prorataBroek = null,
            overstyrt = false,
            yrkesskade = yrkesskade,
        ),
    tidspunkt = tidspunkt,
    regelResultat = "".toJsonNode(),
)

fun beregnetTrygdetidGrunnlag(verdi: Period = Period.parse("P2Y2D")) =
    BeregnetTrygdetidGrunnlag(
        verdi = verdi,
        tidspunkt = Tidspunkt.now(),
        regelResultat = "".toJsonNode(),
    )
