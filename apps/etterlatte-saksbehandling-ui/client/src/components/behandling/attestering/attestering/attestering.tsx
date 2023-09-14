import styled from 'styled-components'
import { useBehandlingRoutes } from '~components/behandling/BehandlingRoutes'
import { IBeslutning } from '../types'
import { Beslutningsvalg } from './beslutningsvalg'
import { useAppSelector } from '~store/Store'
import { Alert } from '@navikt/ds-react'
import { isFailure, isPending, isSuccess, useApiCall } from '~shared/hooks/useApiCall'
import { hentOppgaveForBehandlingUnderBehandling } from '~shared/api/oppgaverny'
import { useEffect, useState } from 'react'
import Spinner from '~shared/Spinner'
import { ApiErrorAlert } from '~ErrorBoundary'
import { IBehandlingReducer } from '~store/reducers/BehandlingReducer'

type Props = {
  setBeslutning: (value: IBeslutning) => void
  beslutning: IBeslutning | undefined
  behandling: IBehandlingReducer
}

export const Attestering = ({ setBeslutning, beslutning, behandling }: Props) => {
  const { lastPage } = useBehandlingRoutes()

  const innloggetSaksbehandler = useAppSelector((state) => state.saksbehandlerReducer.saksbehandler)

  const attestantOgSaksbehandlerErSammePerson = behandling.vedtak?.saksbehandlerId === innloggetSaksbehandler.ident
  const [saksbehandlerPaaOppgave, setSaksbehandlerPaaOppgave] = useState<string | null>(null)

  const [oppgaveForBehandlingStatus, requestHentUnderbehandlingSaksbehandler] = useApiCall(
    hentOppgaveForBehandlingUnderBehandling
  )

  useEffect(() => {
    requestHentUnderbehandlingSaksbehandler({ behandlingId: behandling.id }, (saksbehandler, statusCode) => {
      if (statusCode === 200) {
        setSaksbehandlerPaaOppgave(saksbehandler)
      }
    })
  }, [])

  const innloggetBrukerErSammeSomPaaOppgave = saksbehandlerPaaOppgave === innloggetSaksbehandler.ident

  return (
    <AttesteringWrapper>
      <div className="info">
        <Overskrift>Kontroller opplysninger og faglige vurderinger gjort under behandling.</Overskrift>
      </div>

      {isPending(oppgaveForBehandlingStatus) && <Spinner visible label={'Henter oppgave'} />}
      {isFailure(oppgaveForBehandlingStatus) && <ApiErrorAlert>Kunne ikke hente oppgave for behandling</ApiErrorAlert>}
      {isSuccess(oppgaveForBehandlingStatus) && (
        <>
          {innloggetBrukerErSammeSomPaaOppgave ? (
            <TextWrapper>
              Beslutning
              {lastPage ? (
                <Beslutningsvalg
                  beslutning={beslutning}
                  setBeslutning={setBeslutning}
                  behandling={behandling}
                  disabled={attestantOgSaksbehandlerErSammePerson}
                />
              ) : (
                <Tekst>Se gjennom alle steg før du tar en beslutning.</Tekst>
              )}
              {attestantOgSaksbehandlerErSammePerson && (
                <Alert variant={'warning'}>Du kan ikke attestere en sak som du har saksbehandlet</Alert>
              )}
            </TextWrapper>
          ) : (
            <Alert variant={'warning'}>
              Oppgaven må være tildelt deg for å kunne godkjenne eller underkjenne behandlingen
            </Alert>
          )}
        </>
      )}
    </AttesteringWrapper>
  )
}

const AttesteringWrapper = styled.div`
  margin: 1em;

  .info {
    margin-top: 1em;
    margin-bottom: 1em;
    padding: 1em;
  }
`

const TextWrapper = styled.div`
  font-size: 18px;
  font-weight: 600;
  margin: 1em;
`

const Overskrift = styled.div`
  font-weight: 600;
  font-size: 16px;
  line-height: 22px;
  color: #3e3832;
`

const Tekst = styled.div`
  font-size: 18px;
  font-weight: 400;
  color: #3e3832;
  margin-top: 6px;
`
