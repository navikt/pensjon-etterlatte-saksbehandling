import React, { useEffect, useState } from 'react'
import { useApiCall } from '~shared/hooks/useApiCall'
import { hentGosysOppgaver, OppgaveDTO, OppgaveSaksbehandler } from '~shared/api/oppgaver'
import { isSuccess, mapApiResult } from '~shared/api/apiUtils'
import Spinner from '~shared/Spinner'
import { ApiErrorAlert } from '~ErrorBoundary'
import { Oppgaver } from '~components/oppgavebenk/oppgaver/Oppgaver'
import { Saksbehandler } from '~shared/types/saksbehandler'
import { RevurderingsaarsakerBySakstype } from '~shared/types/Revurderingaarsak'

interface Props {
  oppdaterTildeling: (oppgave: OppgaveDTO, saksbehandler: OppgaveSaksbehandler | null, versjon: number | null) => void
  saksbehandlereIEnhet: Array<Saksbehandler>
  revurderingsaarsaker: RevurderingsaarsakerBySakstype
}

export const GosysOppgaveliste = ({ oppdaterTildeling, saksbehandlereIEnhet, revurderingsaarsaker }: Props) => {
  const [gosysOppgaver, setGosysOppgaver] = useState<Array<OppgaveDTO>>([])

  const [gosysOppgaverResult, hentGosysOppgaverFetch] = useApiCall(hentGosysOppgaver)

  const hentAlleGosysOppgaver = () => {
    hentGosysOppgaverFetch({})
  }

  useEffect(() => {
    hentAlleGosysOppgaver()
  }, [])

  useEffect(() => {
    if (isSuccess(gosysOppgaverResult)) setGosysOppgaver(gosysOppgaverResult.data)
  }, [gosysOppgaverResult])

  return (
    <>
      {mapApiResult(
        gosysOppgaverResult,
        <Spinner visible={true} label="Henter nye Gosys oppgaver" />,
        (error) => (
          <ApiErrorAlert>{error.detail || 'Kunne ikke hente Gosys oppgaver'}</ApiErrorAlert>
        ),
        () => (
          <Oppgaver
            oppgaver={gosysOppgaver}
            oppdaterTildeling={oppdaterTildeling}
            saksbehandlereIEnhet={saksbehandlereIEnhet}
            revurderingsaarsaker={revurderingsaarsaker}
          />
        )
      )}
    </>
  )
}
