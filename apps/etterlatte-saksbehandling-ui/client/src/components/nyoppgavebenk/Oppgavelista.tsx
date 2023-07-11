import { isPending, isSuccess, useApiCall } from '~shared/hooks/useApiCall'
import { hentNyeOppgaver, OppgaveDTOny } from '~shared/api/oppgaverny'
import { useEffect, useState } from 'react'
import Spinner from '~shared/Spinner'

export const Oppgavelista = () => {
  const [oppgaver, hentOppgaver] = useApiCall(hentNyeOppgaver)
  const [hentedeOppgaver, setHentedeOppgaver] = useState<ReadonlyArray<OppgaveDTOny> | null>()
  useEffect(() => {
    hentOppgaver(
      {},
      (oppgaver) => {
        setHentedeOppgaver(oppgaver.oppgaver)
      },
      () => {
        console.error('kunne ikke hente oppgaver')
      }
    )
  }, [])
  return (
    <div>
      {isPending(oppgaver) && <Spinner visible={true} label={'henter nye oppgaver'} />}
      {isSuccess(oppgaver) && <>hentet antall oppgaver: {hentedeOppgaver?.length}</>}
    </div>
  )
}
