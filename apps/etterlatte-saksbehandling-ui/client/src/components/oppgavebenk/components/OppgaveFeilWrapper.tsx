import { mapApiResult, Result } from '~shared/api/apiUtils'
import { OppgaveDTO } from '~shared/api/oppgaver'
import Spinner from '~shared/Spinner'
import { ReactNode } from 'react'
import { ApiErrorAlert } from '~ErrorBoundary'

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
