import { differenceInYears } from 'date-fns'
import {
  GyldigFramsattType,
  IGyldighetproving,
  IVilkaarsproving,
  VilkaarsType,
  VurderingsResultat,
} from '../../../store/reducers/BehandlingReducer'

export const hentAlderVedDoedsdato = (foedselsdato: string, doedsdato: string): string => {
  return Math.floor(differenceInYears(new Date(doedsdato), new Date(foedselsdato))).toString()
}

export function mapGyldighetstyperTilTekst(gyldig: IGyldighetproving | undefined): string {
  if (gyldig?.navn === GyldigFramsattType.INNSENDER_ER_FORELDER) {
    switch (gyldig.resultat) {
      case VurderingsResultat.OPPFYLT:
        return ''
      case VurderingsResultat.IKKE_OPPFYLT:
        return 'Innsender er ikke gjenlevende forelder. '
      case VurderingsResultat.KAN_IKKE_VURDERE_PGA_MANGLENDE_OPPLYSNING:
        return 'Mangler info om innsender er gjenlevende forelder. '
    }
  } else if (gyldig?.navn === GyldigFramsattType.HAR_FORELDREANSVAR_FOR_BARNET) {
    switch (gyldig.resultat) {
      case VurderingsResultat.OPPFYLT:
        return ''
      case VurderingsResultat.IKKE_OPPFYLT:
        return 'Innsender har ikke forelderansvar. '
      case VurderingsResultat.KAN_IKKE_VURDERE_PGA_MANGLENDE_OPPLYSNING:
        return 'Mangler info om innsender har forelderansvar. '
    }
  } else {
    return ''
  }
}

export function hentGyldigBostedTekst(gyldig: IVilkaarsproving): String | undefined {
  let svar
  if (gyldig.navn === VilkaarsType.SAMME_ADRESSE) {
    if (gyldig.resultat === VurderingsResultat.IKKE_OPPFYLT) {
      svar = 'Barn og gjenlevende forelder bor ikke på samme adresse'
    } else if (gyldig.resultat === VurderingsResultat.KAN_IKKE_VURDERE_PGA_MANGLENDE_OPPLYSNING) {
      svar = 'Mangler info'
    }
  } else {
    svar = undefined
  }
  return svar
}
