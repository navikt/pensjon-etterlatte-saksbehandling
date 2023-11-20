import { Button, Heading, TextField } from '@navikt/ds-react'
import { InputRow } from '~components/person/journalfoeringsoppgave/nybehandling/OpprettNyBehandling'
import { useJournalfoeringOppgave } from '~components/person/journalfoeringsoppgave/useJournalfoeringOppgave'
import { useAppDispatch } from '~store/Store'
import { settNyttTema } from '~store/reducers/JournalfoeringOppgaveReducer'
import { FlexRow } from '~shared/styled'
import AvbrytBehandleJournalfoeringOppgave from '~components/person/journalfoeringsoppgave/AvbrytBehandleJournalfoeringOppgave'
import { useNavigate } from 'react-router-dom'
import { FormWrapper } from '~components/person/journalfoeringsoppgave/BehandleJournalfoeringOppgave'
import FullfoerEndreTemaModal from '~components/person/journalfoeringsoppgave/endretema/FullfoerEndreTemaModal'

export default function EndreJournalpostTema() {
  const { endreTemaRequest, journalpost, oppgave } = useJournalfoeringOppgave()

  const dispatch = useAppDispatch()
  const navigate = useNavigate()

  const tilbake = () => navigate('../', { relative: 'path' })

  if (!oppgave) return null

  return (
    <FormWrapper column>
      <Heading size="medium" spacing>
        Endre tema
      </Heading>

      <InputRow>
        <TextField
          label="Nytt tema"
          value={endreTemaRequest?.nyttTema || ''}
          pattern="[A-Z]{3}"
          maxLength={3}
          onChange={(e) =>
            dispatch(
              settNyttTema({
                ...endreTemaRequest,
                nyttTema: e.target.value.toUpperCase(),
              })
            )
          }
          description="Nytt tema journalposten skal kobles til"
        />
      </InputRow>

      <div>
        <FlexRow justify="center" $spacing>
          <Button variant="secondary" onClick={tilbake}>
            Tilbake
          </Button>

          <FullfoerEndreTemaModal oppgave={oppgave} journalpost={journalpost!!} endreTemaRequest={endreTemaRequest!!} />
        </FlexRow>
        <FlexRow justify="center">
          <AvbrytBehandleJournalfoeringOppgave />
        </FlexRow>
      </div>
    </FormWrapper>
  )
}
