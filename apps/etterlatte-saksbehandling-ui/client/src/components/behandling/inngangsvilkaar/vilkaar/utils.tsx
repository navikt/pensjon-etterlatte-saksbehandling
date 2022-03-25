import { IKriterie, Kriterietype, VilkaarVurderingsResultat } from '../../../../store/reducers/BehandlingReducer'

export function vilkaarErOppfylt(resultat: VilkaarVurderingsResultat) {
  switch (resultat) {
    case VilkaarVurderingsResultat.OPPFYLT:
      return 'Vilkår er oppfyllt'
    case VilkaarVurderingsResultat.IKKE_OPPFYLT:
      return 'Vilkår er ikke oppfyllt'
    case VilkaarVurderingsResultat.KAN_IKKE_VURDERE_PGA_MANGLENDE_OPPLYSNING:
      return 'Vilkår trenger avklaring'
  }
}

export interface VilkaarTittelSvar {
  tittel: String
  svar: String
}

export function mapKriterietyperTilTekst(krit: IKriterie): VilkaarTittelSvar {
  let tittel
  let svar

  if (krit.navn === Kriterietype.AVDOED_ER_FORELDER) {
    tittel = 'Avdøde er barnets forelder'
    svar = mapEnkeltSvarTilTekst(krit)
  } else if (krit.navn === Kriterietype.DOEDSFALL_ER_REGISTRERT_I_PDL) {
    tittel = 'Dødsfallet er dokumentert'
    svar = mapEnkeltSvarTilTekst(krit)
  } else if (krit.navn === Kriterietype.SOEKER_ER_UNDER_20_PAA_VIRKNINGSDATO) {
    tittel = 'Barnet er under 18 år på virkningsdato'
    svar = mapEnkeltSvarTilTekst(krit)
  } else if (krit.navn === Kriterietype.SOEKER_IKKE_ADRESSE_I_UTLANDET) {
    tittel = 'Barnet er medlem i trygden'
    if (krit.resultat === VilkaarVurderingsResultat.OPPFYLT) {
      svar = 'Ja. Barnet har bostedsadresse i Norge'
    } else if (krit.resultat === VilkaarVurderingsResultat.IKKE_OPPFYLT) {
      svar = 'Nei. Barnet har utenlandsk bostedsadresse'
    } else {
      svar = 'Avklar. Barnet har registrert utenlandsk adresse'
    }
  } else {
    tittel = ''
    svar = ''
  }

  return { tittel: tittel, svar: svar }
}

export function mapEnkeltSvarTilTekst(krit: IKriterie): String {
  switch (krit.resultat) {
    case VilkaarVurderingsResultat.OPPFYLT:
      return 'Ja'
    case VilkaarVurderingsResultat.IKKE_OPPFYLT:
      return 'Nei'
    case VilkaarVurderingsResultat.KAN_IKKE_VURDERE_PGA_MANGLENDE_OPPLYSNING:
      return 'Mangler info for vurdering'
  }
}

export function hentKildenavn(type: String): String {
  switch (type) {
    case 'pdl':
      return 'PDL'
    case 'privatperson':
      return 'Søknad'
    default:
      return 'Ukjent kilde'
  }
}

export const capitalize = (s: string) => {
  return s.charAt(0).toUpperCase() + s.slice(1).toLowerCase()
}
