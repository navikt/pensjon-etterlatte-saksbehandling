import { mapApiResult, Result } from '~shared/api/apiUtils'
import Spinner from '~shared/Spinner'
import { ReactNode } from 'react'
import { ApiErrorAlert } from '~ErrorBoundary'
import { OppgaveDTO } from '~shared/types/oppgave'

export const OppgaveFeilWrapper = (props: { oppgaver: Result<Array<OppgaveDTO>>; children: ReactNode }) => {
  const { oppgaver, children } = props
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
    </>
  )
}
