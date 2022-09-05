import { IKriterie, Kriterietype, VurderingsResultat } from '../../../../store/reducers/BehandlingReducer'

export function vilkaarErOppfylt(resultat: VurderingsResultat) {
  switch (resultat) {
    case VurderingsResultat.OPPFYLT:
      return 'Vilkår er oppfyllt'
    case VurderingsResultat.IKKE_OPPFYLT:
      return 'Vilkår er ikke oppfyllt'
    case VurderingsResultat.KAN_IKKE_VURDERE_PGA_MANGLENDE_OPPLYSNING:
      return 'Vilkår trenger avklaring'
  }
}

export interface VilkaarTittelSvar {
  tittel: string
  svar: string
}

export function mapKriterietyperTilTekst(krit: IKriterie): VilkaarTittelSvar {
  switch (krit.navn) {
    case Kriterietype.AVDOED_ER_FORELDER:
      return { tittel: 'Avdøde er barnets forelder', svar: mapEnkeltSvarTilTekst(krit) }
    case Kriterietype.DOEDSFALL_ER_REGISTRERT_I_PDL:
      return { tittel: 'Dødsfallet er dokumentert', svar: mapEnkeltSvarTilTekst(krit) }
    case Kriterietype.SOEKER_ER_I_LIVE:
      return { tittel: 'Søker var i live på virkningsdato', svar: mapEnkeltSvarTilTekst(krit) }

    case Kriterietype.SOEKER_ER_UNDER_20_PAA_VIRKNINGSDATO:
      return { tittel: 'Barnet er under 18 år på virkningsdato', svar: mapEnkeltSvarTilTekst(krit) }

    case Kriterietype.SOEKER_IKKE_ADRESSE_I_UTLANDET: {
      const tittel = 'Barnet er medlem i trygden'
      if (krit.resultat === VurderingsResultat.OPPFYLT) return { tittel, svar: 'Ja. Barnet har bostedsadresse i Norge' }
      if (krit.resultat === VurderingsResultat.IKKE_OPPFYLT)
        return { tittel, svar: 'Nei. Barnet har utenlandsk bostedsadresse' }

      return { tittel, svar: 'Avklar. Barnet har registrert utenlandsk adresse' }
    }
    default: {
      return { tittel: '', svar: '' }
    }
  }
}

export function mapEnkeltSvarTilTekst(krit: IKriterie): string {
  switch (krit.resultat) {
    case VurderingsResultat.OPPFYLT:
      return 'Ja'
    case VurderingsResultat.IKKE_OPPFYLT:
      return 'Nei'
    case VurderingsResultat.KAN_IKKE_VURDERE_PGA_MANGLENDE_OPPLYSNING:
      return 'Mangler info for vurdering'
  }
}

export function hentKildenavn(type?: string): string {
  switch (type) {
    case 'pdl':
      return 'PDL'
    case 'privatperson':
      return 'Søknad'
    case 'automatisk':
      return 'Automatisk'
    default:
      return 'Ukjent kilde'
  }
}

export const capitalize = (s?: string) => {
  if (!s) return ''
  return s.charAt(0).toUpperCase() + s.slice(1).toLowerCase()
}
