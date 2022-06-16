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

export function hentKommerBarnetTilgodeTekst(
  sammeAdresse: VurderingsResultat | undefined,
  barnIngenUtland: VurderingsResultat | undefined,
  sammeAdresseAvdoed: VurderingsResultat | undefined
): string | undefined {
  let svar

  if (sammeAdresse === VurderingsResultat.IKKE_OPPFYLT) {
    if (barnIngenUtland === VurderingsResultat.IKKE_OPPFYLT) {
      if (sammeAdresseAvdoed === VurderingsResultat.IKKE_OPPFYLT) {
        svar =
          'Barnet bor ikke på samme adresse som gjenlevende forelder eller avdøde, og barnet har oppgitt adresse i utlandet.'
      } else if (sammeAdresseAvdoed === VurderingsResultat.OPPFYLT) {
        svar =
          'Barnet bor ikke på samme adresse som gjenlevende, men har samme adresse som avdøde og har oppgitt adresse i utlandet.'
      }
    } else if (barnIngenUtland === VurderingsResultat.OPPFYLT)
      if (sammeAdresseAvdoed === VurderingsResultat.IKKE_OPPFYLT) {
        svar = 'Barnet bor ikke på samme adresse som gjenlevende forelder eller avdøde.'
      } else if (sammeAdresseAvdoed === VurderingsResultat.OPPFYLT) {
        svar = 'Barnet bor ikke på samme adresse som gjenlevende, men har samme adresse som avdøde.'
      }
  } else if (sammeAdresse === VurderingsResultat.OPPFYLT) {
    if (barnIngenUtland === VurderingsResultat.IKKE_OPPFYLT) {
      svar = 'Barnet bor på samme adresse som gjenlevende forelder, men har oppgitt adresse i utlandet.'
    } else {
      svar = 'Barn bor på samme adresse som gjenlevende forelder.'
    }
  } else if (
    sammeAdresse === VurderingsResultat.KAN_IKKE_VURDERE_PGA_MANGLENDE_OPPLYSNING ||
    sammeAdresse == undefined
  ) {
    svar = 'Mangler info.'
  }

  return svar
}

export function hentKommerBarnetTilgodeVurderingsTekst(
  sammeAdresse: VurderingsResultat | undefined,
  barnIngenUtland: VurderingsResultat | undefined,
  sammeAdresseAvdoed: VurderingsResultat | undefined
): String | undefined {
  let svar

  if (sammeAdresse === VurderingsResultat.IKKE_OPPFYLT) {
    if (barnIngenUtland === VurderingsResultat.IKKE_OPPFYLT) {
      svar =
        'Ulik adresse for barn og gjenlevende forelder, og barn har oppgitt at de bor på utenlandsadresse i søknaden. Kontakt forelder for å avklare hvor barnet bor.'
    } else if (sammeAdresseAvdoed === VurderingsResultat.OPPFYLT) {
      svar = 'Barn bor på avdødes adresse. Kontakt forelder for å avklare hvorfor barnet ikke bor med vedkommende. '
    } else {
      svar =
        'Ulik adresse for barn og gjenlevende forelder. Kontakt forelder og evt statsforvalter for å avklare hvorfor barnet ikke bor med vedkommende. '
    }
  } else if (
    sammeAdresse === VurderingsResultat.KAN_IKKE_VURDERE_PGA_MANGLENDE_OPPLYSNING ||
    sammeAdresse == undefined
  ) {
    svar = 'Mangler info. Kontakt forelder for å avklare hvem barnet bor med. '
  } else {
    if (barnIngenUtland === VurderingsResultat.IKKE_OPPFYLT) {
      svar =
        'Barn har oppgitt at de bor på utenlandsadresse i søknaden. Kontakt forelder for å avklare hvor barnet bor.'
    }
  }
  return svar
}
