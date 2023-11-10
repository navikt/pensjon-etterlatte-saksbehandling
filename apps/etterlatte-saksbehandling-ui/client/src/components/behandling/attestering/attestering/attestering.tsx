import { useBehandlingRoutes } from '~components/behandling/BehandlingRoutes'
import { IBeslutning } from '../types'
import { Beslutningsvalg } from './beslutningsvalg'
import { useAppSelector } from '~store/Store'
import { Alert, BodyShort } from '@navikt/ds-react'
import { useEffect, useState } from 'react'
import { SidebarPanel } from '~shared/components/Sidebar'
import { VedtakSammendrag } from '~components/vedtak/typer'
import { useSaksbehandlerGjeldendeOppgave } from '~components/behandling/sidemeny/useSaksbehandlerGjeldendeOppgave'

type Props = {
  setBeslutning: (value: IBeslutning) => void
  beslutning: IBeslutning | undefined
  vedtak?: VedtakSammendrag
  erFattet: boolean
}

export const Attestering = ({ setBeslutning, beslutning, vedtak, erFattet }: Props) => {
  const { lastPage } = useBehandlingRoutes()
  const innloggetSaksbehandler = useAppSelector((state) => state.saksbehandlerReducer.saksbehandler)
  const saksbehandlerForGjeldendeOppgave = useSaksbehandlerGjeldendeOppgave()
  const attestantOgSaksbehandlerErSammePerson = vedtak?.saksbehandlerId === innloggetSaksbehandler.ident
  const [oppgaveErTildeltInnloggetBruker, setOppgaveErTildeltInnloggetBruker] = useState(false)

  useEffect(() => {
    setOppgaveErTildeltInnloggetBruker(saksbehandlerForGjeldendeOppgave === innloggetSaksbehandler.ident)
  }, [])

  return (
    <SidebarPanel>
      {oppgaveErTildeltInnloggetBruker ? (
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
          {saksbehandlerForGjeldendeOppgave ? (
            <BodyShort>Oppgaven er tildelt {saksbehandlerForGjeldendeOppgave}.&nbsp;</BodyShort>
          ) : (
            <BodyShort>Oppgaven er ikke tildelt noen.&nbsp;</BodyShort>
          )}
          <BodyShort spacing>
            {erFattet
              ? 'For å kunne attestere behandlingen, må oppgaven være tildelt deg.'
              : 'For å kunne fortsette behandling, må oppgaven være tildelt deg.'}
          </BodyShort>
        </Alert>
      )}
    </SidebarPanel>
  )
}
