import { ISvar } from '../../../store/reducers/BehandlingReducer'
import { VurderingsResultat } from '../../../shared/api/vilkaarsvurdering'

export const svarTilResultat = (svar: ISvar) => {
  switch (svar) {
    case ISvar.JA:
      return VurderingsResultat.OPPFYLT
    case ISvar.NEI:
      return VurderingsResultat.IKKE_OPPFYLT
    case ISvar.IKKE_VURDERT:
      return VurderingsResultat.IKKE_VURDERT
  }
}
