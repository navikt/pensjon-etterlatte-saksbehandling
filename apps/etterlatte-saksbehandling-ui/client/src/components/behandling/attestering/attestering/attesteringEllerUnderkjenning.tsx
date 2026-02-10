import { IBeslutning } from '../types'
import { Beslutningsvalg } from './beslutningsvalg'
import { Alert, BodyShort, VStack } from '@navikt/ds-react'
import { useEffect, useState } from 'react'
import { SidebarPanel } from '~shared/components/Sidebar'
import { VedtakSammendrag } from '~components/vedtak/typer'
import { useSelectorOppgaveUnderBehandling } from '~store/selectors/useSelectorOppgaveUnderBehandling'
import { useInnloggetSaksbehandler } from '~components/behandling/useInnloggetSaksbehandler'
import { Oppgavestatus } from '~shared/types/oppgave'

type Props = {
  setBeslutning: (value: IBeslutning) => void
  beslutning: IBeslutning | undefined
  vedtak?: VedtakSammendrag
  erFattet: boolean
  gyldigStegForBeslutning: boolean
}

export const AttesteringEllerUnderkjenning = ({
  setBeslutning,
  beslutning,
  vedtak,
  erFattet,
  gyldigStegForBeslutning,
}: Props) => {
  const oppgave = useSelectorOppgaveUnderBehandling()
  const innloggetSaksbehandler = useInnloggetSaksbehandler()

  const attestantOgSaksbehandlerErSammePerson = vedtak?.behandlendeSaksbehandler === innloggetSaksbehandler.ident
  const [oppgaveErTildeltInnloggetBruker, setOppgaveErTildeltInnloggetBruker] = useState(false)

  useEffect(() => {
    setOppgaveErTildeltInnloggetBruker(oppgave?.saksbehandler?.ident === innloggetSaksbehandler.ident)
  }, [oppgave?.saksbehandler, innloggetSaksbehandler.ident, beslutning])

  return (
    <SidebarPanel>
      {oppgaveErTildeltInnloggetBruker ? (
        <VStack gap="space-4">
          {oppgave?.status === Oppgavestatus.PAA_VENT ? (
            <Alert variant="warning" size="small" inline>
              Kan ikke attestere en oppgave som står på vent
            </Alert>
          ) : (
            <Alert variant="info" size="small">
              Kontroller opplysninger og faglige vurderinger gjort under behandling.
            </Alert>
          )}

          {gyldigStegForBeslutning ? (
            <Beslutningsvalg
              beslutning={beslutning}
              setBeslutning={setBeslutning}
              disabled={attestantOgSaksbehandlerErSammePerson || oppgave?.status === Oppgavestatus.PAA_VENT}
            />
          ) : (
            <BodyShort size="small" spacing>
              <i>Se gjennom alle steg før du tar en beslutning.</i>
            </BodyShort>
          )}

          {attestantOgSaksbehandlerErSammePerson && (
            <Alert variant="warning">Du kan ikke attestere en sak som du har saksbehandlet</Alert>
          )}
        </VStack>
      ) : (
        <Alert variant="warning">
          {oppgave?.saksbehandler ? (
            <BodyShort>
              Oppgaven er tildelt {oppgave?.saksbehandler?.navn || oppgave?.saksbehandler?.ident}.&nbsp;
            </BodyShort>
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
