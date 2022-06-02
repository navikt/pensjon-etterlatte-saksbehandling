import { add, differenceInYears, startOfMonth } from 'date-fns'
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

//TODO: Flyttes til sjekk i backend
export const hentVirkningstidspunkt = (doedsdato: string, mottattDato: string): string => {
  if (sjekkDodsfallMerEnn3AarSiden(doedsdato, mottattDato)) {
    /*
                TODO se mere på utregning av virkningstidspunkt. for gjenlevende er det 3 måneder(?) og barnepensjon 1,
                men er det mere enn 3 år siden dødsfall, går man bare 3 år tilbake. Denne burde kanskje regnes ut i backend og ligge
                lagret på behandlingen, og så må saksbehandler ha mulighet til å endre den hvis det er behov, på vilkårssiden der den vises.
                */
    return startOfMonth(add(new Date(doedsdato), { years: 3 })).toString()
  }
  return startOfMonth(add(new Date(doedsdato), { months: 1 })).toString()
}

export const sjekkDodsfallMerEnn3AarSiden = (doedsdato: string, mottattDato: string): boolean => {
  return differenceInYears(new Date(mottattDato), new Date(doedsdato)) > 3
}

export function mapGyldighetstyperTilTekst(gyldig: IGyldighetproving): String | undefined {
  let svar

  if (gyldig.resultat === VurderingsResultat.KAN_IKKE_VURDERE_PGA_MANGLENDE_OPPLYSNING) {
    svar = 'Mangler info'
  } else if (gyldig.resultat === VurderingsResultat.OPPFYLT) {
    svar = undefined
  } else if (gyldig.navn === GyldigFramsattType.HAR_FORELDREANSVAR_FOR_BARNET) {
    svar = 'Nei, innsender har ikke forelderansvar. Dette må avklares før du kan starte vilkårsvurderingen.'
  } else if (gyldig.navn === GyldigFramsattType.INNSENDER_ER_FORELDER) {
    svar = 'Nei, innsender er ikke gjenlevende forelder. Dette må avklares før du kan starte vilkårsvurderingen.'
  }

  return svar
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
