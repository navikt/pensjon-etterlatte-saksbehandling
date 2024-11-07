import { useSidetittel } from '~shared/hooks/useSidetittel'
import { useMatch } from 'react-router-dom'
import { useApiCall } from '~shared/hooks/useApiCall'
import React, { useEffect } from 'react'
import { isSuccess, mapResult } from '~shared/api/apiUtils'
import Spinner from '~shared/Spinner'
import { ApiErrorAlert } from '~ErrorBoundary'
import { OppgaveVurderingRoute } from '~components/aktivitetsplikt/OppgaveVurderingRoute'
import { Link } from '@navikt/ds-react'
import { hentAktivitetspliktOppgaveVurdering } from '~shared/api/aktivitetsplikt'
import { useDispatch } from 'react-redux'
import { setStartdata, useAktivitetspliktOppgaveVurderingState } from '~store/reducers/Aktivitetsplikt12mnd'

export function VurderAktivitetspliktOppgave() {
  useSidetittel('Vurder aktivitetsplikt')
  const match = useMatch('/aktivitet-vurdering/:oppgaveId/*')
  const oppgaveId = match?.params.oppgaveId

  const [oppgaveStatus, fetchOppgave] = useApiCall(hentAktivitetspliktOppgaveVurdering)
  const dispatch = useDispatch()
  const data = useAktivitetspliktOppgaveVurderingState()
  useEffect(() => {
    if (!oppgaveId || (isSuccess(oppgaveStatus) && oppgaveStatus.data.oppgave.id === oppgaveId)) {
      return
    }
    fetchOppgave({ oppgaveId })
  }, [oppgaveId])

  useEffect(() => {
    if (isSuccess(oppgaveStatus)) {
      dispatch(setStartdata(oppgaveStatus.data))
    }
  }, [oppgaveId, oppgaveStatus])
  const dataErSatt = !!data.oppgave

  return mapResult(oppgaveStatus, {
    initial: (
      <ApiErrorAlert>
        OppgaveId mangler. Prøv å gå inn på oppgaven på nytt fra <Link href="/">oppgavelisten.</Link>
      </ApiErrorAlert>
    ),
    pending: <Spinner visible label="Henter oppgave for vurdering" />,
    success: (oppgave) => {
      return (
        <>
          {dataErSatt && (
            <OppgaveVurderingRoute
              vurderingOgOppgave={oppgave}
              fetchOppgave={() => fetchOppgave({ oppgaveId: oppgave.oppgave.id })}
            />
          )}
        </>
      )
    },
    error: (e) => (
      <ApiErrorAlert>{e.detail || 'Kunne ikke hente oppgave for vurderingen av aktivitetsplikt'}</ApiErrorAlert>
    ),
  })
}
