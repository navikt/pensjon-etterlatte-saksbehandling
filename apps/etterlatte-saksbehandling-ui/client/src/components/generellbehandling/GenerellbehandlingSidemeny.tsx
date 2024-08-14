import { Sidebar, SidebarPanel } from '~shared/components/Sidebar'
import { Generellbehandling, KravpakkeUtland, Status } from '~shared/types/Generellbehandling'
import { AttesteringMedUnderkjenning } from '~components/generellbehandling/AttesteringMedUnderkjenning'
import React, { useEffect, useState } from 'react'
import Spinner from '~shared/Spinner'
import { AttestertVisning } from '~components/generellbehandling/AttestertVisning'
import { Alert, BodyShort } from '@navikt/ds-react'
import { ReturnertVisning } from '~components/generellbehandling/ReturnertVisning'

import { isPending } from '~shared/api/apiUtils'
import { isFailureHandler } from '~shared/api/IsFailureHandler'
import { useSelectorOppgaveUnderBehandling } from '~store/selectors/useSelectorOppgaveUnderBehandling'
import { useOppgaveUnderBehandling } from '~shared/hooks/useOppgaveUnderBehandling'
import { useInnloggetSaksbehandler } from '~components/behandling/useInnloggetSaksbehandler'

export const GenerellbehandlingSidemeny = (props: {
  utlandsBehandling: Generellbehandling & { innhold: KravpakkeUtland | null }
}) => {
  const { utlandsBehandling } = props
  const [oppgaveResult] = useOppgaveUnderBehandling({ referanse: utlandsBehandling.id })

  const innloggetSaksbehandler = useInnloggetSaksbehandler()
  const [oppgaveErTildeltInnloggetBruker, setOppgaveErTildeltInnloggetBruker] = useState(false)

  const oppgave = useSelectorOppgaveUnderBehandling()

  useEffect(() => {
    setOppgaveErTildeltInnloggetBruker(innloggetSaksbehandler.ident === oppgave?.saksbehandler?.ident)
  }, [oppgave?.saksbehandler, innloggetSaksbehandler])

  const genererSidemeny = () => {
    switch (utlandsBehandling.status) {
      case Status.RETURNERT:
        return <ReturnertVisning utlandsBehandling={utlandsBehandling} />
      case Status.OPPRETTET:
        return null
      case Status.FATTET:
        return (
          <AttesteringMedUnderkjenning
            utlandsBehandling={utlandsBehandling}
            oppgaveErTildeltInnloggetBruker={oppgaveErTildeltInnloggetBruker}
          />
        )
      case Status.ATTESTERT:
        return <AttestertVisning utlandsBehandling={utlandsBehandling} />
      case Status.AVBRUTT:
        return null
    }
  }
  return (
    <Sidebar>
      <SidebarPanel>
        {isFailureHandler({
          apiResult: oppgaveResult,
          errorMessage: 'Vi fant ingen saksbehandler for den tilknyttede oppgaven. Husk å tildele oppgaven.',
        })}
        <Spinner visible={isPending(oppgaveResult)} label="Henter saksbehandler or oppgave" />
        {oppgave?.saksbehandler ? (
          <Alert variant={oppgaveErTildeltInnloggetBruker ? 'success' : 'info'} style={{ marginBottom: '2rem' }}>
            <BodyShort>{`Oppgaven for kravpakken er tildelt ${
              oppgaveErTildeltInnloggetBruker ? 'deg' : oppgave?.saksbehandler?.navn
            }`}</BodyShort>
          </Alert>
        ) : (
          <Alert variant="warning" style={{ marginTop: '2rem' }}>
            <BodyShort>Oppgaven er ikke tildelt noen.&nbsp;</BodyShort>
          </Alert>
        )}
        {genererSidemeny()}
      </SidebarPanel>
    </Sidebar>
  )
}
