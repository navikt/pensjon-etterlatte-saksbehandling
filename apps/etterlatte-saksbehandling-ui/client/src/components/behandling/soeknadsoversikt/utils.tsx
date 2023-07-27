import { StatusIconProps } from '~shared/icons/statusIcon'
import { JaNei } from '~shared/types/ISvar'
import { VurderingsResultat } from '~shared/types/VurderingsResultat'
import { KildePdl } from '~shared/types/kilde'
import { formaterStringDato } from '~utils/formattering'

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
          'Innsender er ikke gjenlevende forelder eller har foreldreansvar. Kontakt statsforvalter for å avklare mandatet for vergermålet. '
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

export const svarTilVurderingsstatus = (svar: JaNei) => {
  switch (svar) {
    case JaNei.JA:
      return VurderingsResultat.OPPFYLT
    case JaNei.NEI:
      return VurderingsResultat.IKKE_OPPFYLT
  }
}

export const svarTilStatusIcon = (svar: JaNei | undefined): StatusIconProps => {
  switch (svar) {
    case JaNei.JA:
      return 'success'
    case JaNei.NEI:
      return 'error'
    default:
      return 'warning'
  }
}

export const formaterKildePdl = (kilde?: KildePdl) => {
  return kilde ? kilde.navn.toUpperCase() + ': ' + formaterStringDato(kilde.tidspunktForInnhenting) : undefined
}
