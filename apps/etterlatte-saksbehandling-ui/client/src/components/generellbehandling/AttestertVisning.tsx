import { Info, Overskrift, Tekst, Wrapper } from '~components/behandling/attestering/styled'
import { Generellbehandling, KravpakkeUtland } from '~shared/types/Generellbehandling'
import { genbehandlingTypeTilLesbartNavn } from '~components/person/behandlingsslistemappere'
import { useEffect } from 'react'
import { mapApiResult, useApiCall } from '~shared/hooks/useApiCall'
import { hentFerdigstiltAtteseringsoppgaveForReferanse } from '~shared/api/oppgaver'
import Spinner from '~shared/Spinner'
import { ApiErrorAlert } from '~ErrorBoundary'

export const AttestertVisning = (props: {
  utlandsBehandling: Generellbehandling & { innhold: KravpakkeUtland | null }
}) => {
  const { utlandsBehandling } = props

  const [hentoppgaveStatus, hentoppgave] = useApiCall(hentFerdigstiltAtteseringsoppgaveForReferanse)
  useEffect(() => {
    hentoppgave({ referanse: utlandsBehandling.id, sakId: utlandsBehandling.sakId })
  }, [])

  //TODO: burde ha med Attestert dato og saksbehandler senere
  return (
    <Wrapper innvilget={true}>
      <Overskrift>{genbehandlingTypeTilLesbartNavn(utlandsBehandling.type)}</Overskrift>
      <div className="flex">
        <Info>Attestant</Info>
        {mapApiResult(
          hentoppgaveStatus,
          <Spinner visible={true} label="Henter attestant" />,
          () => (
            <ApiErrorAlert>Vi klarte ikke å hente attestant</ApiErrorAlert>
          ),
          (saksbehandler) => (
            <Tekst>{saksbehandler}</Tekst>
          )
        )}
      </div>
    </Wrapper>
  )
}
