package trygdetid

import no.nav.etterlatte.libs.common.behandling.BehandlingType
import no.nav.etterlatte.libs.common.behandling.DetaljertBehandling
import no.nav.etterlatte.libs.common.behandling.Prosesstype
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import no.nav.etterlatte.libs.common.toJsonNode
import no.nav.etterlatte.libs.common.trygdetid.DetaljertBeregnetTrygdetidResultat
import no.nav.etterlatte.libs.common.trygdetid.avtale.Trygdeavtale
import no.nav.etterlatte.token.Saksbehandler
import no.nav.etterlatte.token.Systembruker
import no.nav.etterlatte.trygdetid.BeregnetTrygdetidGrunnlag
import no.nav.etterlatte.trygdetid.DetaljertBeregnetTrygdetid
import no.nav.etterlatte.trygdetid.LandNormalisert
import no.nav.etterlatte.trygdetid.Opplysningsgrunnlag
import no.nav.etterlatte.trygdetid.Trygdetid
import no.nav.etterlatte.trygdetid.TrygdetidGrunnlag
import no.nav.etterlatte.trygdetid.TrygdetidPeriode
import no.nav.etterlatte.trygdetid.TrygdetidType
import java.time.LocalDate
import java.time.Period
import java.util.UUID
import java.util.UUID.randomUUID

val saksbehandler = Saksbehandler("token", "ident", null)

val pesysBruker = Systembruker("", "")

fun behandling(
    behandlingId: UUID = randomUUID(),
    sakId: Long = 1,
) = DetaljertBehandling(
    id = behandlingId,
    sak = sakId,
    sakType = SakType.BARNEPENSJON,
    soeker = "",
    behandlingType = BehandlingType.FØRSTEGANGSBEHANDLING,
    virkningstidspunkt = null,
    boddEllerArbeidetUtlandet = null,
    revurderingsaarsak = null,
    revurderingInfo = null,
    prosesstype = Prosesstype.AUTOMATISK,
)

fun trygdetid(
    behandlingId: UUID = randomUUID(),
    sakId: Long = 1,
    beregnetTrygdetid: DetaljertBeregnetTrygdetid? = null,
    trygdetidGrunnlag: List<TrygdetidGrunnlag> = emptyList(),
    opplysninger: List<Opplysningsgrunnlag> = emptyList(),
) = Trygdetid(
    id = randomUUID(),
    sakId = sakId,
    behandlingId = behandlingId,
    trygdetidGrunnlag = trygdetidGrunnlag,
    opplysninger = opplysninger,
    beregnetTrygdetid = beregnetTrygdetid,
)

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
) = TrygdetidGrunnlag(
    id = randomUUID(),
    type = TrygdetidType.FAKTISK,
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
) = Trygdeavtale(
    id = randomUUID(),
    behandlingId = behandlingId,
    avtaleKode = avtaleKode,
    avtaleDatoKode = avtaleDatoKode,
    avtaleKriteriaKode = avtaleKriteriaKode,
    kilde = Grunnlagsopplysning.Saksbehandler(ident = "Z123", tidspunkt = Tidspunkt.now()),
)

fun beregnetTrygdetid(
    total: Int = 0,
    tidspunkt: Tidspunkt = Tidspunkt.now(),
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

fun beregnetYrkesskadeTrygdetid() =
    DetaljertBeregnetTrygdetid(
        resultat = DetaljertBeregnetTrygdetidResultat.fraSamletTrygdetidNorge(40),
        tidspunkt = Tidspunkt.now(),
        regelResultat =
            """
            {
                "verdi": {
                    "faktiskTrygdetidNorge": null,
                    "faktiskTrygdetidTeoretisk": null,
                    "fremtidigTrygdetidNorge": null,
                    "fremtidigTrygdetidTeoretisk": null,
                    "samletTrygdetidNorge": null,
                    "samletTrygdetidTeoretisk": null,
                    "prorataBroek": [0, 0] 
                },
                "regel": {
                    "gjelderFra": "1900-01-01",
                    "beskrivelse": "Yrkesskade fører altid til 40 år",
                    "regelReferanse": { "id": "REGEL-YRKESSKADE-TRYGDETID", "versjon": "1" }
                },
                "noder": [
                    {
                        "verdi": 40,
                        "regel": {
                        "gjelderFra": "1900-01-01",
                        "beskrivelse": "Full trygdetidsopptjening er 40 år",
                        "regelReferanse": {
                        "id": "REGEL-TOTAL-TRYGDETID-MAKS-ANTALL-ÅR",
                        "versjon": "1"
                    }
                    },
                        "noder": [],
                        "opprettet": "2023-06-30T13:22:15.799453Z"
                    }
                ],
                "opprettet": "2023-06-30T13:22:15.799509Z"
            }
            """.trimIndent().toJsonNode(),
    )
