import { useSidetittel } from '~shared/hooks/useSidetittel'
import { useMatch } from 'react-router-dom'
import { useApiCall } from '~shared/hooks/useApiCall'
import { hentOppgave } from '~shared/api/oppgaver'
import React, { useEffect } from 'react'
import { isSuccess, mapResult } from '~shared/api/apiUtils'
import Spinner from '~shared/Spinner'
import { ApiErrorAlert } from '~ErrorBoundary'
import { OppgaveVurderingRoute } from '~components/aktivitetsplikt/OppgaveVurderingRoute'
import { Link } from '@navikt/ds-react'

export function VurderAktivitetspliktOppgave() {
  useSidetittel('Vurder aktivitetsplikt')
  const match = useMatch('/aktivitet-vurdering/:oppgaveId/*')
  const oppgaveId = match?.params.oppgaveId

  const [oppgave, fetchOppgave] = useApiCall(hentOppgave)

  useEffect(() => {
    if (!oppgaveId || (isSuccess(oppgave) && oppgave.data.id === oppgaveId)) {
      return
    }
    fetchOppgave(oppgaveId)
  }, [oppgaveId, oppgave])

  return mapResult(oppgave, {
    initial: (
      <ApiErrorAlert>
        OppgaveId mangler. Prøv å gå inn på oppgaven på nytt fra <Link href="/">oppgavelisten.</Link>
      </ApiErrorAlert>
    ),
    pending: <Spinner visible label="Henter oppgave for vurdering" />,
    success: (oppgave) => <OppgaveVurderingRoute oppgave={oppgave} />,
    error: (e) => (
      <ApiErrorAlert>{e.detail || 'Kunne ikke hente oppgave for vurderingen av aktivitetsplikt'}</ApiErrorAlert>
    ),
  })
}
