import { mapApiResult, Result } from '~shared/api/apiUtils'
import { OppgaveDTO } from '~shared/api/oppgaver'
import Spinner from '~shared/Spinner'
import { isFailureHandler } from '~shared/api/IsFailureHandler'
import { ReactNode } from 'react'
import { ApiErrorAlert } from '~ErrorBoundary'

export const OppgaveFeilWrapper = (props: {
  oppgaver: Result<Array<OppgaveDTO>>
  gosysOppgaver: Result<Array<OppgaveDTO>>
  children: ReactNode
}) => {
  const { oppgaver, gosysOppgaver, children } = props
  return (
    <>
      {mapApiResult(
        oppgaver,
        <Spinner visible={true} label="Henter nye oppgaver" />,
        (error) => (
          <ApiErrorAlert>{error.detail || 'Kunne ikke hente oppgaver'}</ApiErrorAlert>
        ),
        () => (
          <>{children}</>
        )
      )}
      {isFailureHandler({
        apiResult: gosysOppgaver,
        errorMessage: 'Kunne ikke hente gosys oppgaver',
      })}
    </>
  )
}
