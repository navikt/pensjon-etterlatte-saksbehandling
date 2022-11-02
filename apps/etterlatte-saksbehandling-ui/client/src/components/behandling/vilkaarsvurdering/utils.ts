import { ISvar, KildeType } from '../../../store/reducers/BehandlingReducer'
import { VilkaarsvurderingResultat } from '../../../shared/api/vilkaarsvurdering'

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


export enum RequestStatus {
    notStarted,
    isloading,
    error,
    ok,
}
