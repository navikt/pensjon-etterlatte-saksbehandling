import { differenceInYears } from 'date-fns'
import { GyldigFramsattType, IGyldighetproving, VurderingsResultat } from '../../../store/reducers/BehandlingReducer'

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

export function hentSammeAdresseTekst(sammeAdresse: VurderingsResultat | undefined): string {
  switch (sammeAdresse) {
    case VurderingsResultat.OPPFYLT:
      return 'Gjenlevende og barnet bor på samme adresse. '
    case VurderingsResultat.IKKE_OPPFYLT:
      return 'Barnet bor ikke på samme adresse som gjenlevende forelder. '
    default:
      return 'Mangler info. '
  }
}

export function hentBarnUtlandsadresseTekst(utland: VurderingsResultat | undefined): string {
  switch (utland) {
    case VurderingsResultat.IKKE_OPPFYLT:
      return 'Barnet har oppgitt utlandsadresse i søknaden. '
    default:
      return ''
  }
}

export function hentSammeAdresseSomAvdoedTekst(sammeAdresse: VurderingsResultat | undefined): string {
  switch (sammeAdresse) {
    case VurderingsResultat.OPPFYLT:
      return 'Barnet bor på samme adresse som avdøde. '
    case VurderingsResultat.IKKE_OPPFYLT:
      return 'Barnet bor ikke på samme adresse som avdøde. '
    default:
      return ''
  }
}

export function hentKommerBarnetTilgodeVurderingsTekst(
  sammeAdresse: VurderingsResultat | undefined,
  barnIngenUtland: VurderingsResultat | undefined,
  sammeAdresseAvdoed: VurderingsResultat | undefined
): String | undefined {
  let svar

  if (sammeAdresse === VurderingsResultat.IKKE_OPPFYLT) {
    svar =
      'Ulik adresse for barn og gjenlevende forelder. Kontakt forelder og evt statsforvalter for å avklare hvorfor barnet ikke bor med vedkommende. '
  } else if (
    sammeAdresse === VurderingsResultat.KAN_IKKE_VURDERE_PGA_MANGLENDE_OPPLYSNING ||
    sammeAdresse == undefined
  ) {
    svar = 'Mangler info. Kontakt forelder for å avklare hvem barnet bor med. '
  } else {
    if (barnIngenUtland === VurderingsResultat.IKKE_OPPFYLT) {
      svar =
        'Barn har oppgitt at de bor på utenlandsadresse i søknaden. Kontakt forelder for å avklare hvor barnet bor.'
    } else if (sammeAdresseAvdoed === VurderingsResultat.OPPFYLT) {
      svar = 'Barn bor på avdødes adresse. Kontakt forelder for å avklare hvorfor barnet ikke bor med vedkommende. '
    } else {
      svar = ''
    }
  }
  return svar
}
