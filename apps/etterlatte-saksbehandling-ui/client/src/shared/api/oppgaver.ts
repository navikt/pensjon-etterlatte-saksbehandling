import { mockdata } from '../../components/oppgavebenken/OppgaveUtils'

export const hentOppgaver = async () => {
  try {
    const response: any = await mockdata
    return response
  } catch (e: any) {
    throw new Error('Det skjedde en feil')
  }
}
