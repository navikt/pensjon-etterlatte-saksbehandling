import { mockDataPersoner } from '../../components/behandling/personopplysninger/MockDataPO'

export const hentPersonerMedRelasjon = async () => {
  try {
    //TODO eller slette hvis det ikke skal hentes alene, men med saker
    const response: any = await mockDataPersoner
    return response
  } catch (e: any) {
    throw new Error('Det skjedde en feil')
  }
}
