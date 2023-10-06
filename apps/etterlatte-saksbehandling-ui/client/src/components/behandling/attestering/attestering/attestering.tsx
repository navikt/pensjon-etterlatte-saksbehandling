import { useBehandlingRoutes } from '~components/behandling/BehandlingRoutes'
import { IBeslutning } from '../types'
import { Beslutningsvalg } from './beslutningsvalg'
import { useAppSelector } from '~store/Store'
import { Alert, BodyShort } from '@navikt/ds-react'
import { isFailure, isPending, isSuccess, useApiCall } from '~shared/hooks/useApiCall'
import { hentSaksbehandlerForOppgaveUnderArbeid } from '~shared/api/oppgaver'
import { useEffect, useState } from 'react'
import Spinner from '~shared/Spinner'
import { ApiErrorAlert } from '~ErrorBoundary'
import { SidebarPanel } from '~shared/components/Sidebar'
import { VedtakSammendrag } from '~components/vedtak/typer'

type Props = {
  setBeslutning: (value: IBeslutning) => void
  beslutning: IBeslutning | undefined
  behandlingId: string
  vedtak?: VedtakSammendrag
  erFattet: boolean
}

export const Attestering = ({ setBeslutning, beslutning, behandlingId, vedtak, erFattet }: Props) => {
  const { lastPage } = useBehandlingRoutes()
  const innloggetSaksbehandler = useAppSelector((state) => state.saksbehandlerReducer.saksbehandler)
  const [saksbehandlerForOppgave, hentSaksbehandlerForOppgave] = useApiCall(hentSaksbehandlerForOppgaveUnderArbeid)
  const attestantOgSaksbehandlerErSammePerson = vedtak?.saksbehandlerId === innloggetSaksbehandler.ident
  const [oppgaveErTildeltInnloggetBruker, setOppgaveErTildeltInnloggetBruker] = useState(false)

  useEffect(() => {
    hentSaksbehandlerForOppgave({ behandlingId }, (saksbehandler) => {
      setOppgaveErTildeltInnloggetBruker(saksbehandler === innloggetSaksbehandler.ident)
    })
  }, [])

  return (
    <SidebarPanel>
      {isPending(saksbehandlerForOppgave) && <Spinner visible label="Henter oppgave" />}
      {isFailure(saksbehandlerForOppgave) && <ApiErrorAlert>Kunne ikke hente oppgave for behandling</ApiErrorAlert>}
      {isSuccess(saksbehandlerForOppgave) &&
        (oppgaveErTildeltInnloggetBruker ? (
          <>
            <Alert variant="info" size="small">
              Kontroller opplysninger og faglige vurderinger gjort under behandling.
            </Alert>
            <br />

            {lastPage ? (
              <Beslutningsvalg
                beslutning={beslutning}
                setBeslutning={setBeslutning}
                disabled={attestantOgSaksbehandlerErSammePerson}
              />
            ) : (
              <BodyShort size="small" spacing>
                <i>Se gjennom alle steg før du tar en beslutning.</i>
              </BodyShort>
            )}

            {attestantOgSaksbehandlerErSammePerson && (
              <Alert variant="warning">Du kan ikke attestere en sak som du har saksbehandlet</Alert>
            )}
          </>
        ) : (
          <Alert variant="warning">
            {saksbehandlerForOppgave.data ? (
              <BodyShort>Oppgaven er tildelt {saksbehandlerForOppgave.data}.&nbsp;</BodyShort>
            ) : (
              <BodyShort>Oppgaven er ikke tildelt noen.&nbsp;</BodyShort>
            )}
            <BodyShort spacing>
              {erFattet
                ? 'For å kunne attestere behandlingen, må oppgaven være tildelt deg.'
                : 'For å kunne fortsette behandling, må oppgaven være tildelt deg.'}
            </BodyShort>
          </Alert>
        ))}
    </SidebarPanel>
  )
}
