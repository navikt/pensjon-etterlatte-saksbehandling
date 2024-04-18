import { feilregistrerGosysOppgave, opprettOppgave } from '~shared/api/oppgaver'
import { useApiCall } from '~shared/hooks/useApiCall'
import { hentSakForPerson } from '~shared/api/sak'
import { isInitial, isPending, isSuccess, mapResult, mapSuccess } from '~shared/api/apiUtils'
import { Alert, Button, Checkbox } from '@navikt/ds-react'
import { FlexRow } from '~shared/styled'
import { GosysActionToggle } from '~components/oppgavebenk/oppgaveModal/GosysOppgaveModal'
import React, { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { formaterSakstype } from '~utils/formattering'
import { ResultAlert } from '~shared/alerts/ResultAlert'
import { OppgaveDTO, OppgaveKilde, Oppgavetype } from '~shared/types/oppgave'
import { useInnloggetSaksbehandler } from '~components/behandling/useInnloggetSaksbehandler'

export const OverfoerOppgaveTilGjenny = ({
  oppgave,
  setToggle,
}: {
  oppgave: OppgaveDTO
  setToggle: (toggle: GosysActionToggle) => void
}) => {
  const navigate = useNavigate()
  const innloggetSaksbehandler = useInnloggetSaksbehandler()

  const [skalOppretteSak, setSkalOppretteSak] = useState(false)

  const [opprettOppgaveStatus, apiOpprettOppgave] = useApiCall(opprettOppgave)
  const [feilregistrerStatus, feilregistrerOppgave] = useApiCall(feilregistrerGosysOppgave)
  const [sakStatus, hentSak] = useApiCall(hentSakForPerson)

  const konverterTilGjennyoppgave = () => {
    if (!oppgave.saksbehandler?.ident) {
      throw Error('Kan ikke konvertere oppgave som ikke er tildelt')
    }

    hentSak({ fnr: oppgave.fnr!!, type: oppgave.sakType, opprettHvisIkkeFinnes: skalOppretteSak }, (sak) => {
      apiOpprettOppgave(
        {
          sakId: sak.id,
          request: {
            oppgaveType: Oppgavetype.JOURNALFOERING,
            referanse: oppgave.journalpostId!!,
            merknad: oppgave.beskrivelse || 'Journalføringsoppgave konvertert fra Gosys',
            oppgaveKilde: OppgaveKilde.SAKSBEHANDLER,
            saksbehandler: innloggetSaksbehandler.ident,
          },
        },
        () => {
          feilregistrerOppgave({
            oppgaveId: oppgave.id,
            versjon: oppgave.versjon!!,
            beskrivelse: 'Oppgave ble flyttet til Gjenny',
          })
        }
      )
    })
  }

  const loading = isPending(sakStatus) || isPending(opprettOppgaveStatus) || isPending(feilregistrerStatus)

  return (
    <>
      <Alert variant="info">
        Er du sikker på at du vil flytte oppgaven til Gjenny?
        <br />
        Gosys-oppgaven vil bli markert som feilregistrert og erstattet med en ny oppgave i Gjenny.
      </Alert>

      <br />

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
              Vil du opprette en {formaterSakstype(oppgave.sakType)}-sak på brukeren ({oppgave.fnr})?
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
      <ResultAlert result={opprettOppgaveStatus} success="Opprettet ny oppgave" />
      <ResultAlert result={feilregistrerStatus} success="Gosys-oppgave markert som feilregistrert" />

      <br />

      {isSuccess(opprettOppgaveStatus) && (
        <FlexRow justify="right">
          <Button variant="tertiary" onClick={() => window.location.reload()}>
            Avslutt
          </Button>
          {mapSuccess(opprettOppgaveStatus, (oppgave) => (
            <Button onClick={() => navigate(`/oppgave/${oppgave.id}`)}>Gå til oppgaven</Button>
          ))}
        </FlexRow>
      )}

      {isInitial(opprettOppgaveStatus) && (
        <FlexRow justify="right">
          <Button variant="secondary" onClick={() => setToggle({ ferdigstill: false })} disabled={loading}>
            Nei, avbryt
          </Button>
          <Button onClick={konverterTilGjennyoppgave} loading={loading}>
            Ja, overfør
          </Button>
        </FlexRow>
      )}
    </>
  )
}
