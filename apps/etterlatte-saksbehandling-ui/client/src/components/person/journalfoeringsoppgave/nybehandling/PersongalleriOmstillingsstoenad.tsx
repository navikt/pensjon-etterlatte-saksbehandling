import { Button, Heading, Panel, TextField } from '@navikt/ds-react'
import { PlusIcon, XMarkIcon } from '@navikt/aksel-icons'
import { Persongalleri } from '~shared/types/Person'
import { InputList, InputRow } from '~components/person/journalfoeringsoppgave/nybehandling/OpprettNyBehandling'
import { settNyBehandlingRequest } from '~store/reducers/JournalfoeringOppgaveReducer'
import { useAppDispatch } from '~store/Store'
import { useJournalfoeringOppgave } from '~components/person/journalfoeringsoppgave/useJournalfoeringOppgave'

export default function PersongalleriOmstillingsstoenad() {
  const { nyBehandlingRequest } = useJournalfoeringOppgave()
  const dispatch = useAppDispatch()

  const persongalleri = nyBehandlingRequest?.persongalleri

  const oppdaterPersongalleri = (persongalleri: Persongalleri) => {
    dispatch(settNyBehandlingRequest({ ...nyBehandlingRequest, persongalleri }))
  }

  const oppdater = (fnr: string, index: number) => {
    const nyState = persongalleri ? [...(persongalleri?.soesken || [])] : []
    nyState[index] = fnr
    oppdaterPersongalleri({ ...persongalleri, soesken: nyState })
  }

  const leggTil = () => {
    const nyState = persongalleri ? [...(persongalleri?.soesken || []), ''] : ['']

    oppdaterPersongalleri({
      ...persongalleri,
      soesken: nyState,
    })
  }

  const fjern = (index: number) => {
    const nyState = persongalleri ? [...(persongalleri?.soesken || [])] : []
    nyState.splice(index, 1)
    oppdaterPersongalleri({ ...persongalleri, soesken: nyState })
  }

  return (
    <>
      <InputRow>
        <TextField
          label="Søker (gjenlevende)"
          value={persongalleri?.soeker || ''}
          pattern="[0-9]{11}"
          maxLength={11}
          onChange={(e) => oppdaterPersongalleri({ ...persongalleri, soeker: e.target.value })}
          description="Fødselsnummeret er automatisk hentet fra oppgaven"
          readOnly
        />
      </InputRow>

      <InputRow>
        <TextField
          label="Innsender"
          description="Oppgi innsenderen sitt fødselsnummer dersom det er tilgjengelig"
          value={persongalleri?.innsender || ''}
          pattern="[0-9]{11}"
          maxLength={11}
          onChange={(e) => oppdaterPersongalleri({ ...persongalleri, innsender: e.target.value })}
        />
      </InputRow>

      <InputRow>
        <TextField
          label="Avdød"
          description="Oppgi fødselsnummer"
          value={persongalleri?.avdoed ? persongalleri?.avdoed[0] : ''}
          pattern="[0-9]{11}"
          maxLength={11}
          onChange={(e) => oppdaterPersongalleri({ ...persongalleri, avdoed: [e.target.value] })}
        />
      </InputRow>

      <Panel border>
        <Heading size="small" spacing>
          Barn
        </Heading>

        <InputList>
          {persongalleri?.soesken?.map((barn, index) => (
            <InputRow key={index}>
              <TextField
                label={`Barn ${persongalleri!!.soesken!!.length > 1 ? index + 1 : ''}`}
                description="Oppgi fødselsnummer"
                value={barn}
                pattern="[0-9]{11}"
                maxLength={11}
                onChange={(e) => oppdater(e.target.value, index)}
              />
              <Button icon={<XMarkIcon />} variant="tertiary" onClick={() => fjern(index)} />
            </InputRow>
          ))}
          <Button icon={<PlusIcon />} onClick={leggTil}>
            Legg til barn
          </Button>
        </InputList>
      </Panel>
    </>
  )
}
