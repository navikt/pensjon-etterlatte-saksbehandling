import { mockdata } from '../../components/oppgavebenken/OppgaveKolonner'

export const hentOppgaver = async () => {
  try {
    const response: any = await mockdata
    return response
  } catch (e: any) {
    throw new Error('Det skjedde en feil')
  }
}
