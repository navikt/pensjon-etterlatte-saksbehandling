import { Sidebar, SidebarPanel } from '~shared/components/Sidebar'
import { Generellbehandling, KravpakkeUtland, Status } from '~shared/types/Generellbehandling'
import { AttesteringMedUnderkjenning } from '~components/generellbehandling/AttesteringMedUnderkjenning'
import React, { useEffect, useState } from 'react'
import { useAppSelector } from '~store/Store'
import Spinner from '~shared/Spinner'
import { AttestertVisning } from '~components/generellbehandling/AttestertVisning'
import { Alert, BodyShort } from '@navikt/ds-react'
import { ReturnertVisning } from '~components/generellbehandling/ReturnertVisning'

import { isPending } from '~shared/api/apiUtils'
import { isFailureHandler } from '~shared/api/IsFailureHandler'
import { useSelectorSaksbehandlerGjeldendeOppgaveBehandling } from '~store/selectors/useSelectorSaksbehandlerGjeldendeOppgaveBehandling'
import { useSaksbehandlerPaaOppgaveUnderArbeidForReferanse } from '~shared/hooks/useSaksbehandlerPaaOppgaveUnderArbeidForReferanse'

export const GenerellbehandlingSidemeny = (props: {
  utlandsBehandling: Generellbehandling & { innhold: KravpakkeUtland | null }
}) => {
  const { utlandsBehandling } = props
  const [saksbehandlerForOppgaveStatus] = useSaksbehandlerPaaOppgaveUnderArbeidForReferanse({
    referanse: utlandsBehandling.id,
    sakId: utlandsBehandling.sakId,
  })

  const innloggetSaksbehandler = useAppSelector((state) => state.saksbehandlerReducer.innloggetSaksbehandler)
  const [oppgaveErTildeltInnloggetBruker, setOppgaveErTildeltInnloggetBruker] = useState(false)

  const saksbehandlerForGjeldendeOppgave = useSelectorSaksbehandlerGjeldendeOppgaveBehandling()

  useEffect(() => {
    setOppgaveErTildeltInnloggetBruker(innloggetSaksbehandler.ident === saksbehandlerForGjeldendeOppgave)
  }, [saksbehandlerForGjeldendeOppgave, innloggetSaksbehandler])

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
          apiResult: saksbehandlerForOppgaveStatus,
          errorMessage: 'Vi fant ingen saksbehandler for den tilknyttede oppgaven. Husk å tildele oppgaven.',
        })}
        {isPending(saksbehandlerForOppgaveStatus) && <Spinner visible={true} label="Henter saksbehandler or oppgave" />}
        {saksbehandlerForGjeldendeOppgave ? (
          <Alert variant={oppgaveErTildeltInnloggetBruker ? 'success' : 'info'} style={{ marginBottom: '2rem' }}>
            <BodyShort>{`Oppgaven for kravpakken er tildelt ${
              oppgaveErTildeltInnloggetBruker ? 'deg' : saksbehandlerForGjeldendeOppgave
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
