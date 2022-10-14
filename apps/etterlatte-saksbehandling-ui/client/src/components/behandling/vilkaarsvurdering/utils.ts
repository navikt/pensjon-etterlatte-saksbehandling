import { ISvar } from '../../../store/reducers/BehandlingReducer'
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
