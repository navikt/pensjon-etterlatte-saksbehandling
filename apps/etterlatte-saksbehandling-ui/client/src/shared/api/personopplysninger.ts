import { mockdata } from '../../components/behandling/personopplysninger/MockDataPO'

export const hentPersonopplysninger = async () => {
  try {
    //TODO eller slette hvis det ikke skal hentes alene, men med saker
    const response: any = await mockdata
    return response
  } catch (e: any) {
    throw new Error('Det skjedde en feil')
  }
}
