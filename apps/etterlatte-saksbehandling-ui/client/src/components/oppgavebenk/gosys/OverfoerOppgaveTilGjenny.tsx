import { useApiCall } from '~shared/hooks/useApiCall'
import { hentSakForPerson } from '~shared/api/sak'
import { isPending, mapResult } from '~shared/api/apiUtils'
import { Alert, Button, Checkbox, HStack, Select } from '@navikt/ds-react'
import { GosysActionToggle } from '~components/oppgavebenk/oppgaveModal/GosysOppgaveModal'
import React, { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { GosysBrukerType, GosysOppgave, GosysTema, sakTypeFraTema } from '~shared/types/Gosys'
import { flyttTilGjenny } from '~shared/api/gosys'
import { SakType } from '~shared/types/sak'
import { formaterSakstype } from '~utils/formatering/formatering'
import { useInnloggetSaksbehandler } from '~components/behandling/useInnloggetSaksbehandler'
import { GOSYS_TEMA_FILTER } from '~components/oppgavebenk/filtreringAvOppgaver/typer'
import { ApiErrorAlert } from '~ErrorBoundary'
import { ClickEvent, trackClick } from '~utils/analytics'

export const OverfoerOppgaveTilGjenny = ({
  oppgave,
  setToggle,
}: {
  oppgave: GosysOppgave
  setToggle: (toggle: GosysActionToggle) => void
}) => {
  const navigate = useNavigate()
  const innloggetSaksbehandler = useInnloggetSaksbehandler()

  const [skalOppretteSak, setSkalOppretteSak] = useState(false)
  const [sakType, setSakType] = useState<SakType | undefined>(sakTypeFraTema(oppgave.tema))

  const [sakStatus, hentSak] = useApiCall(hentSakForPerson)
  const [flyttOppgaveResult, flyttOppgaveTilGjenny] = useApiCall(flyttTilGjenny)

  const konverterTilGjennyoppgave = () => {
    if (!oppgave.bruker?.ident) throw Error('Kan ikke opprette sak i Gjenny uten fødselsnummer')

    trackClick(ClickEvent.FLYTT_GOSYS_OPPGAVE)

    hentSak({ fnr: oppgave.bruker.ident, type: sakType!!, opprettHvisIkkeFinnes: skalOppretteSak }, (sak) => {
      if (!sak) return

      flyttOppgaveTilGjenny({ oppgaveId: oppgave.id, sakId: sak.id, enhetsnr: sak.enhet })
    })
  }

  const loading = isPending(sakStatus) || isPending(flyttOppgaveResult)

  if (oppgave.saksbehandler?.ident !== innloggetSaksbehandler.ident)
    return <Alert variant="warning">Oppgaven er ikke tildelt deg!</Alert>

  if (oppgave.bruker?.type !== GosysBrukerType.PERSON || !oppgave.bruker?.ident) {
    return <Alert variant="warning">Kan ikke konvertere oppgave med ugyldig bruker</Alert>
  }

  return (
    <>
      <Alert variant="info">
        Er du sikker på at du vil flytte oppgaven til Gjenny?
        <br />
        Gosys-oppgaven vil bli markert som feilregistrert og erstattet med en ny oppgave i Gjenny.
      </Alert>

      <br />

      {(!oppgave.tema || oppgave.tema === GosysTema.PEN) && (
        <>
          <Alert variant="warning" inline>
            Kan ikke automatisk velge saktype fra tema {[GOSYS_TEMA_FILTER[oppgave.tema]]}!
          </Alert>
          <br />
          <Select label="Velg saktype" onChange={(e) => setSakType(e.target.value as SakType)}>
            <option value="">Velg ...</option>
            <option value={SakType.OMSTILLINGSSTOENAD}>{formaterSakstype(SakType.OMSTILLINGSSTOENAD)}</option>
            <option value={SakType.BARNEPENSJON}>{formaterSakstype(SakType.BARNEPENSJON)}</option>
          </Select>
        </>
      )}

      {mapResult(sakStatus, {
        success: (sak) =>
          sak ? (
            <Alert variant="success" size="small">
              Sak funnet
            </Alert>
          ) : (
            <Alert variant="warning">
              Kan ikke overføre oppgave siden brukeren mangler sak i Gjenny.
              <br />
              Vil du opprette en {formaterSakstype(sakType!!)}-sak på brukeren ({oppgave.bruker?.ident})?
              <br />
              <br />
              <Checkbox value={skalOppretteSak} onChange={(e) => setSkalOppretteSak(e.target.checked)}>
                Ja, opprett sak
              </Checkbox>
            </Alert>
          ),
        error: (error) => (
          <Alert variant="error" size="small">
            {error.detail || 'Ukjent feil oppsto'}
          </Alert>
        ),
      })}

      <br />

      {mapResult(flyttOppgaveResult, {
        initial: (
          <HStack gap="space-4" justify="end">
            <Button variant="secondary" onClick={() => setToggle({ ferdigstill: false })} disabled={loading}>
              Nei, avbryt
            </Button>
            <Button onClick={konverterTilGjennyoppgave} loading={loading} disabled={!sakType}>
              Ja, overfør
            </Button>
          </HStack>
        ),
        success: (oppgave) => (
          <HStack gap="space-4" justify="end">
            <Button variant="tertiary" onClick={() => window.location.reload()}>
              Avslutt
            </Button>
            <Button onClick={() => navigate(`/oppgave/${oppgave.id}`)}>Gå til oppgaven</Button>
          </HStack>
        ),
        error: (error) => <ApiErrorAlert>{error.detail || 'Ukjent feil oppsto ved flytting av oppgave'}</ApiErrorAlert>,
      })}
    </>
  )
}
