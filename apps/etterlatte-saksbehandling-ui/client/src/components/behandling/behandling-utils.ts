import { IKriterie, IVilkaaropplysing, Kriterietype, OpplysningsType } from '../../store/reducers/BehandlingReducer'


/*
TODO: type opp return type
*/

export const hentKriterier = (vilkaar: any, kriterieType: Kriterietype, opplysningsType: OpplysningsType) => {
  try {
    return vilkaar.kriterier
      .find((krit: IKriterie) => krit.navn === kriterieType)
      .basertPaaOpplysninger.find((opplysning: IVilkaaropplysing) => opplysning.opplysningsType === opplysningsType)
  } catch (e: any) {
    console.error(e)
  }
}
