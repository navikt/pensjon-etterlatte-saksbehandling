import { isPending, isSuccess, Result } from '~shared/api/apiUtils'
import { OppgaveDTO } from '~shared/api/oppgaver'
import Spinner from '~shared/Spinner'
import { isFailureHandler } from '~shared/api/IsFailureHandler'
import { ReactNode } from 'react'

export const OppgaveFeilWrapper = (props: {
  oppgaver: Result<Array<OppgaveDTO>>
  gosysOppgaver: Result<Array<OppgaveDTO>>
  children: ReactNode
}) => {
  const { oppgaver, gosysOppgaver, children } = props
  return (
    <>
      {isPending(oppgaver) && <Spinner visible={true} label="Henter nye oppgaver" />}
      {isFailureHandler({
        apiResult: oppgaver,
        errorMessage: 'Kunne ikke hente oppgaver',
      })}
      {isFailureHandler({
        apiResult: gosysOppgaver,
        errorMessage: 'Kunne ikke hente gosys oppgaver',
      })}
      {isSuccess(oppgaver) && <>{children}</>}
    </>
  )
}
