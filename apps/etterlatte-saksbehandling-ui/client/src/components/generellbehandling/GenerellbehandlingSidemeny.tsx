import { Sidebar } from '~shared/components/Sidebar'
import { Generellbehandling, KravpakkeUtland, Status } from '~shared/types/Generellbehandling'
import { AttesteringMedUnderkjenning } from '~components/generellbehandling/AttesteringMedUnderkjenning'
import { isFailure, isPending, useApiCall } from '~shared/hooks/useApiCall'
import { hentSaksbehandlerForReferanseOppgaveUnderArbeid } from '~shared/api/oppgaver'
import { useEffect, useState } from 'react'
import { ApiErrorAlert } from '~ErrorBoundary'
import { useAppSelector } from '~store/Store'
import Spinner from '~shared/Spinner'
import { AttestertVisning } from '~components/generellbehandling/AttestertVisning'

export const GenerellbehandlingSidemeny = (props: {
  utlandsBehandling: Generellbehandling & { innhold: KravpakkeUtland | null }
}) => {
  const { utlandsBehandling } = props
  const [saksbehandlerForOppgaveStatus, hentSaksbehandlerForOppgaveUnderArbeid] = useApiCall(
    hentSaksbehandlerForReferanseOppgaveUnderArbeid
  )

  const innloggetSaksbehandler = useAppSelector((state) => state.saksbehandlerReducer.innloggetSaksbehandler)
  const [saksbehandlerForGjeldendeOppgave, setGeldendeSaksbehandler] = useState<string | null>(null)
  const [oppgaveErTildeltInnloggetBruker, setOppgaveErTildeltInnloggetBruker] = useState(false)

  useEffect(() => {
    hentSaksbehandlerForOppgaveUnderArbeid(
      { referanse: utlandsBehandling.id, sakId: utlandsBehandling.sakId },
      (saksbehandler) => setGeldendeSaksbehandler(saksbehandler),
      () => setGeldendeSaksbehandler(null)
    )
  }, [utlandsBehandling.id])

  useEffect(() => {
    setOppgaveErTildeltInnloggetBruker(innloggetSaksbehandler.ident === saksbehandlerForGjeldendeOppgave)
  }, [saksbehandlerForGjeldendeOppgave, innloggetSaksbehandler])

  const genererSidemeny = () => {
    switch (utlandsBehandling.status) {
      case Status.OPPRETTET:
        return null
      case Status.FATTET:
        return (
          <AttesteringMedUnderkjenning
            utlandsBehandling={utlandsBehandling}
            oppgaveErTildeltInnloggetBruker={oppgaveErTildeltInnloggetBruker}
            saksbehandlerForGjeldendeOppgave={saksbehandlerForGjeldendeOppgave}
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
      {isFailure(saksbehandlerForOppgaveStatus) && (
        <ApiErrorAlert>Vi fant ingen saksbehadler for den tilknyttede oppgaven. Husk å tildele oppgaven.</ApiErrorAlert>
      )}
      {isPending(saksbehandlerForOppgaveStatus) && <Spinner visible={true} label="Henter saksbehandler or oppgave" />}
      {genererSidemeny()}
    </Sidebar>
  )
}
