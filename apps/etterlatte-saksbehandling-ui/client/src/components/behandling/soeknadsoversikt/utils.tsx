import { differenceInYears } from 'date-fns'
import { VurderingsResultat } from '../../../store/reducers/BehandlingReducer'

export const hentAlderVedDoedsdato = (foedselsdato: string, doedsdato: string): string => {
  return Math.floor(differenceInYears(new Date(doedsdato), new Date(foedselsdato))).toString()
}

export function hentGyldighetsTekst(
  innsenderErForelder: VurderingsResultat | undefined,
  innsenderHarForeldreansvar: VurderingsResultat | undefined,
  ingenAnnenVergeEnnForelder: VurderingsResultat | undefined
): string | undefined {
  let svar

  if (innsenderErForelder === VurderingsResultat.IKKE_OPPFYLT) {
    if (innsenderHarForeldreansvar === VurderingsResultat.IKKE_OPPFYLT) {
      if (ingenAnnenVergeEnnForelder === VurderingsResultat.IKKE_OPPFYLT) {
        svar =
          'Innsender er ikke gjenlevende forelder eller har foreldreansvar. En annen verge enn forelder er registrert. Send orientering om at søknaden ikke blir behandlet.'
      } else if (ingenAnnenVergeEnnForelder === VurderingsResultat.OPPFYLT) {
        svar =
          'Innsender er ikke gjenlevende forelder eller har foreldreansvar. Kontakt statsforvalter for å for å avklare mandatet for vergermålet. '
      }
    } else if (innsenderHarForeldreansvar === VurderingsResultat.OPPFYLT) {
      if (ingenAnnenVergeEnnForelder === VurderingsResultat.IKKE_OPPFYLT) {
        svar =
          'Innsender er ikke gjenlevende forelder, men har foreldreansvar. En annen verge enn forelder er registrert. Kontakt statsforvalter for å avklare mandatet for vergermålet. '
      } else if (ingenAnnenVergeEnnForelder === VurderingsResultat.OPPFYLT) {
        svar =
          'Innsender er ikke gjenlevende forelder, men har foreldreansvar. Dette må avklares før du kan starte vilkårsvurderingen.'
      }
    } else if (innsenderHarForeldreansvar === VurderingsResultat.KAN_IKKE_VURDERE_PGA_MANGLENDE_OPPLYSNING ||
      innsenderHarForeldreansvar === undefined) {
      svar =
        'Innsender er ikke gjenlevende forelder, og mangler info om innsender har forelderansvar. Dette må avklares før du kan starte vilkårsvurderingen.'
    }
  } else if (innsenderErForelder === VurderingsResultat.OPPFYLT) {
    if (innsenderHarForeldreansvar === VurderingsResultat.IKKE_OPPFYLT) {
      if (ingenAnnenVergeEnnForelder === VurderingsResultat.IKKE_OPPFYLT) {
        svar =
          'Innsender har ikke foreldreansvar og en annen verge enn forelder er registrert. Send orientering om at søknaden ikke blir behandlet.'
      } else if (ingenAnnenVergeEnnForelder === VurderingsResultat.OPPFYLT) {
        svar =
          'Innsender har ikke foreldreansvar. Kontakt statsforvalter for å for å avklare mandatet for vergermålet. '
      }
    } else if (innsenderHarForeldreansvar === VurderingsResultat.OPPFYLT) {
      if (ingenAnnenVergeEnnForelder === VurderingsResultat.IKKE_OPPFYLT) {
        svar =
          'En annen verge enn forelder er registrert. Kontakt statsforvalter for å avklare mandatet for vergermålet. '
      } else if (ingenAnnenVergeEnnForelder === VurderingsResultat.OPPFYLT) {
        svar = ''
      }
    } else if (innsenderHarForeldreansvar === VurderingsResultat.KAN_IKKE_VURDERE_PGA_MANGLENDE_OPPLYSNING ||
      innsenderHarForeldreansvar === undefined) {
      svar = 'Mangler info om innsender har forelderansvar. Dette må avklares før du kan starte vilkårsvurderingen.'
    }
  } else if (innsenderErForelder === VurderingsResultat.KAN_IKKE_VURDERE_PGA_MANGLENDE_OPPLYSNING ||
    innsenderErForelder === undefined) {
    if (innsenderHarForeldreansvar === VurderingsResultat.OPPFYLT) {
      svar =
        'Mangler info om innsender er gjenlevende forelder. Dette må avklares før du kan starte vilkårsvurderingen.'
    } else if (innsenderHarForeldreansvar === VurderingsResultat.IKKE_OPPFYLT) {
      svar =
        'Mangler info om innsender er gjenlevende forelder og innsender har ikke forelderansvar. Dette må avklares før du kan starte vilkårsvurderingen.'
    } else if (innsenderHarForeldreansvar === VurderingsResultat.KAN_IKKE_VURDERE_PGA_MANGLENDE_OPPLYSNING ||
      innsenderHarForeldreansvar === undefined) {
      svar = 'Mangler info om innsender er gjenlevende forelder eller har foreldreansvar. '
    }
  }
  return svar
}

export function hentKommerBarnetTilgodeTekst(
  sammeAdresse: VurderingsResultat | undefined,
  barnIngenUtland: VurderingsResultat | undefined,
  sammeAdresseAvdoed: VurderingsResultat | undefined,
  saksbehandlerVurdering: VurderingsResultat | undefined,
): string {
  let svar
  if (saksbehandlerVurdering === VurderingsResultat.OPPFYLT) {
    svar = 'Saksbehandler har vurdert at det er sannsynlig at pensjonen kommer barnet til gode.'
  } else if (saksbehandlerVurdering === VurderingsResultat.IKKE_OPPFYLT) {
    svar = 'Saksbehandler har vurdert at det ikke er sannsynlig at pensjonen kommer barnet til gode.'
  } else if (sammeAdresse === VurderingsResultat.IKKE_OPPFYLT) {
    if (barnIngenUtland === VurderingsResultat.IKKE_OPPFYLT) {
      if (sammeAdresseAvdoed === VurderingsResultat.IKKE_OPPFYLT) {
        svar =
          'Barnet bor ikke på samme adresse som gjenlevende forelder eller avdøde, og barnet har oppgitt adresse i utlandet.'
      } else if (sammeAdresseAvdoed === VurderingsResultat.OPPFYLT) {
        svar =
          'Barnet bor ikke på samme adresse som gjenlevende, men har samme adresse som avdøde og har oppgitt adresse i utlandet.'
      }
    } else if (barnIngenUtland === VurderingsResultat.OPPFYLT) if (sammeAdresseAvdoed ===
      VurderingsResultat.IKKE_OPPFYLT) {
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
  } else if (sammeAdresse === VurderingsResultat.KAN_IKKE_VURDERE_PGA_MANGLENDE_OPPLYSNING || sammeAdresse ==
    undefined) {
    svar = 'Mangler info.'
  }

  return svar ? svar : ''
}

export function hentKommerBarnetTilgodeVurderingsTekst(
  sammeAdresse: VurderingsResultat | undefined,
  barnIngenUtland: VurderingsResultat | undefined,
  sammeAdresseAvdoed: VurderingsResultat | undefined,
): string {
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
  } else if (sammeAdresse === VurderingsResultat.KAN_IKKE_VURDERE_PGA_MANGLENDE_OPPLYSNING || sammeAdresse ==
    undefined) {
    svar = 'Mangler info. Kontakt forelder for å avklare hvem barnet bor med. '
  } else {
    if (barnIngenUtland === VurderingsResultat.IKKE_OPPFYLT) {
      svar =
        'Barn har oppgitt at de bor på utenlandsadresse i søknaden. Kontakt forelder for å avklare hvor barnet bor.'
    }
  }
  return svar ? svar : ''
}
