import { VilkaarsvurderingResultat, VurderingsResultat } from '~shared/api/vilkaarsvurdering'
import { ISvar } from '~shared/types/ISvar'
import { KildeType } from '~shared/types/kilde'

export const svarTilTotalResultat = (svar: ISvar) => {
  switch (svar) {
    case ISvar.JA:
      return VilkaarsvurderingResultat.OPPFYLT
    case ISvar.NEI:
      return VilkaarsvurderingResultat.IKKE_OPPFYLT
    case ISvar.IKKE_VURDERT:
      throw Error('IKKE_VURDERT er ikke et gyldig valg for VilkaarsvurderingResultat')
  }
}

export function hentKildenavn(type?: KildeType): string {
  switch (type) {
    case KildeType.pdl:
      return 'PDL'
    case KildeType.privatperson:
      return 'Søknad'
    case KildeType.a_ordningen:
      return 'A-ordningen'
    case KildeType.aa_registeret:
      return 'AA-registeret'
    case KildeType.vilkaarskomponenten:
      return 'Vilkårskomponenten'
    case KildeType.saksbehandler:
      return 'Saksbehandler'
    default:
      return 'Ukjent kilde'
  }
}

export function formaterVurderingsResultat(vurderingsResultat?: VurderingsResultat | null): string {
  switch (vurderingsResultat) {
    case VurderingsResultat.OPPFYLT:
      return 'Ja'
    case VurderingsResultat.IKKE_OPPFYLT:
      return 'Nei'
    case VurderingsResultat.IKKE_VURDERT:
      return 'Ikke vurdert'
    default:
      return 'Ukjent'
  }
}
