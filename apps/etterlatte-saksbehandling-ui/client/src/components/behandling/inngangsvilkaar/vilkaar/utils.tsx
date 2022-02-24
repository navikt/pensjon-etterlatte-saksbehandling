import { IKriterie, Kriterietype, VilkaarVurderingsResultat } from '../../../../store/reducers/BehandlingReducer'

export function vilkaarErOppfylt(resultat: VilkaarVurderingsResultat) {
  return (
    <div>
      Vilkår er{' '}
      {resultat === VilkaarVurderingsResultat.KAN_IKKE_VURDERE_PGA_MANGLENDE_OPPLYSNING ? (
        <> ikke oppfyllt</>
      ) : (
        <> oppfyllt</>
      )}
    </div>
  )
}

export function mapKriterieTypeTilTekst(navn: Kriterietype): String {
  switch (navn) {
    case Kriterietype.AVDOED_ER_FORELDER:
      return 'Avdøde er barnets forelder'
    case Kriterietype.DOEDSFALL_ER_REGISTRERT_I_PDL:
      return 'Dødsfallet er registrert i PDL'
    case Kriterietype.SOEKER_ER_UNDER_20_PAA_VIRKNINGSDATO:
      return 'Barnet er under 20 år på virkningsdato'
  }
}

export function mapKriterieTilSvar(krit: IKriterie): String {
  if (krit.resultat === VilkaarVurderingsResultat.KAN_IKKE_VURDERE_PGA_MANGLENDE_OPPLYSNING) {
    return 'Mangler info for vurdering'
  }

  switch (krit.navn) {
    case Kriterietype.AVDOED_ER_FORELDER:
      return krit.resultat === VilkaarVurderingsResultat.OPPFYLT ? 'Ja' : 'Nei'
    case Kriterietype.DOEDSFALL_ER_REGISTRERT_I_PDL:
      return krit.resultat === VilkaarVurderingsResultat.OPPFYLT ? 'Ja' : 'Nei'
    case Kriterietype.SOEKER_ER_UNDER_20_PAA_VIRKNINGSDATO:
      return krit.resultat === VilkaarVurderingsResultat.OPPFYLT ? 'Ja' : 'Nei'
  }
}
