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
    case Kriterietype.SOEKER_IKKE_OPPGITT_ADRESSE_I_UTLANDET_I_SOEKNAD:
      return 'Barnet har ikke oppgitt utenlandsadresse i søknaden'
    case Kriterietype.SOEKER_IKKE_BOSTEDADRESSE_I_UTLANDET:
      return 'Barnet har ikke bostedsadresse i utlandet'
    case Kriterietype.SOEKER_IKKE_OPPHOLDADRESSE_I_UTLANDET:
      return 'Barnet har ikke oppholdsadresse i utlandet'
    case Kriterietype.SOEKER_IKKE_KONTAKTADRESSE_I_UTLANDET:
      return 'Barnet har ikke kontaktadresse i utlandet'
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
    case Kriterietype.SOEKER_IKKE_OPPGITT_ADRESSE_I_UTLANDET_I_SOEKNAD:
      return krit.resultat === VilkaarVurderingsResultat.OPPFYLT
        ? 'Ingen adresse i utlandet'
        : 'Har oppgitt adresse i utlandet'
    case Kriterietype.SOEKER_IKKE_BOSTEDADRESSE_I_UTLANDET:
      return krit.resultat === VilkaarVurderingsResultat.OPPFYLT
        ? 'Har ikke bostedsadresse i utlandet'
        : 'Har bostedsadresse i utlandet'
    case Kriterietype.SOEKER_IKKE_OPPHOLDADRESSE_I_UTLANDET:
      return krit.resultat === VilkaarVurderingsResultat.OPPFYLT
        ? 'Har ikke oppholdssadresse i utlandet'
        : 'Har oppholdssadresse i utlandet'
    case Kriterietype.SOEKER_IKKE_KONTAKTADRESSE_I_UTLANDET:
      return krit.resultat === VilkaarVurderingsResultat.OPPFYLT
        ? 'Har ikke kontaktadresse i utlandet'
        : 'Har kontaktadresse i utlandet'
  }
}
