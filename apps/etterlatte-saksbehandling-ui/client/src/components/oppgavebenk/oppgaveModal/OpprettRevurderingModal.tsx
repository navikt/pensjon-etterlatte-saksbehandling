import { OppgaveDTO } from '~shared/api/oppgaver'
import React, { useEffect } from 'react'
import { useApiCall } from '~shared/hooks/useApiCall'
import { isInitial, mapApiResult } from '~shared/api/apiUtils'
import { hentStoettedeRevurderinger } from '~shared/api/revurdering'
import Spinner from '~shared/Spinner'
import { ApiErrorAlert } from '~ErrorBoundary'
import { OpprettNyRevurdering } from '~components/person/OpprettNyRevurdering'

export function OpprettRevurderingModal(props: { oppgave: OppgaveDTO }) {
  const { oppgave } = props
  const [muligeRevurderingAarsakerStatus, hentMuligeRevurderingeraarsaker] = useApiCall(hentStoettedeRevurderinger)

  useEffect(() => {
    if (!oppgave.referanse && isInitial(muligeRevurderingAarsakerStatus)) {
      hentMuligeRevurderingeraarsaker({ sakType: oppgave.sakType })
    }
  }, [oppgave.referanse])

  return mapApiResult(
    muligeRevurderingAarsakerStatus,
    <Spinner visible label="Laster revurderingsårsaker ..." />,
    () => <ApiErrorAlert>En feil skjedde under kallet for å hente støttede revurderinger</ApiErrorAlert>,
    (muligeRevurderingAarsakerStatus) => (
      <OpprettNyRevurdering
        revurderinger={muligeRevurderingAarsakerStatus}
        sakId={oppgave.sakId}
        oppgaveId={oppgave.id}
        begrunnelse={oppgave.merknad}
        litenKnapp={true}
      />
    )
  )
}
