import { JaNeiVetikke, VurderingsResultat } from '../../../store/reducers/BehandlingReducer'

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
    } else if (
      innsenderHarForeldreansvar === VurderingsResultat.KAN_IKKE_VURDERE_PGA_MANGLENDE_OPPLYSNING ||
      innsenderHarForeldreansvar === undefined
    ) {
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
    } else if (
      innsenderHarForeldreansvar === VurderingsResultat.KAN_IKKE_VURDERE_PGA_MANGLENDE_OPPLYSNING ||
      innsenderHarForeldreansvar === undefined
    ) {
      svar = 'Mangler info om innsender har forelderansvar. Dette må avklares før du kan starte vilkårsvurderingen.'
    }
  } else if (
    innsenderErForelder === VurderingsResultat.KAN_IKKE_VURDERE_PGA_MANGLENDE_OPPLYSNING ||
    innsenderErForelder === undefined
  ) {
    if (innsenderHarForeldreansvar === VurderingsResultat.OPPFYLT) {
      svar =
        'Mangler info om innsender er gjenlevende forelder. Dette må avklares før du kan starte vilkårsvurderingen.'
    } else if (innsenderHarForeldreansvar === VurderingsResultat.IKKE_OPPFYLT) {
      svar =
        'Mangler info om innsender er gjenlevende forelder og innsender har ikke forelderansvar. Dette må avklares før du kan starte vilkårsvurderingen.'
    } else if (
      innsenderHarForeldreansvar === VurderingsResultat.KAN_IKKE_VURDERE_PGA_MANGLENDE_OPPLYSNING ||
      innsenderHarForeldreansvar === undefined
    ) {
      svar = 'Mangler info om innsender er gjenlevende forelder eller har foreldreansvar. '
    }
  }
  return svar
}

export const svarTilVurderingsstatus = (svar: JaNeiVetikke) => {
  switch (svar) {
    case JaNeiVetikke.JA:
      return VurderingsResultat.OPPFYLT
    case JaNeiVetikke.NEI:
      return VurderingsResultat.IKKE_OPPFYLT
    case JaNeiVetikke.VET_IKKE:
      return VurderingsResultat.KAN_IKKE_VURDERE_PGA_MANGLENDE_OPPLYSNING
  }
}
