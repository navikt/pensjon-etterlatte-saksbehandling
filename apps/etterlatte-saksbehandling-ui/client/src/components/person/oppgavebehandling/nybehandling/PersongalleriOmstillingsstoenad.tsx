import { Button, Heading, Panel, TextField } from '@navikt/ds-react'
import React from 'react'
import { PlusIcon, XMarkIcon } from '@navikt/aksel-icons'
import { Persongalleri } from '~shared/types/Person'
import { InputList, InputRow } from '~components/person/oppgavebehandling/nybehandling/OpprettNyBehandling'
import { FormWrapper } from '../styled'

interface Props {
  persongalleri?: Persongalleri
  oppdaterPersongalleri: (persongalleri: Persongalleri) => void
}

export default function PersongalleriOmstillingsstoenad({ persongalleri, oppdaterPersongalleri }: Props) {
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
    <FormWrapper column>
      <InputRow>
        <TextField
          label={'Søker (gjenlevende)'}
          placeholder={'Fødselsnummer'}
          value={persongalleri?.soeker || ''}
          pattern={'[0-9]{11}'}
          maxLength={11}
          onChange={(e) => oppdaterPersongalleri({ ...persongalleri, soeker: e.target.value })}
          description={'Automatisk valgt utifra bruker på oppgaven.'}
        />
      </InputRow>

      <InputRow>
        <TextField
          label={'Innsender'}
          placeholder={'Fødselsnummer'}
          value={persongalleri?.innsender || ''}
          pattern={'[0-9]{11}'}
          maxLength={11}
          onChange={(e) => oppdaterPersongalleri({ ...persongalleri, innsender: e.target.value })}
        />
      </InputRow>

      <InputRow>
        <TextField
          label={`Avdød`}
          placeholder={'Avdød sitt fødselsnummer'}
          value={persongalleri?.avdoed ? persongalleri?.avdoed[0] : ''}
          pattern={'[0-9]{11}'}
          maxLength={11}
          onChange={(e) => oppdaterPersongalleri({ ...persongalleri, avdoed: [e.target.value] })}
        />
      </InputRow>

      <Panel border>
        <Heading size={'small'} spacing>
          Barn
        </Heading>

        <InputList>
          {persongalleri?.soesken?.map((barn, index) => (
            <InputRow key={index}>
              <TextField
                label={`Barn ${persongalleri!!.soesken!!.length > 1 ? index + 1 : ''}`}
                placeholder={'Fødselsnummer'}
                value={barn}
                pattern={'[0-9]{11}'}
                maxLength={11}
                onChange={(e) => oppdater(e.target.value, index)}
              />
              <Button icon={<XMarkIcon />} variant={'tertiary'} onClick={() => fjern(index)} />
            </InputRow>
          ))}
          <Button icon={<PlusIcon />} onClick={leggTil}>
            Legg til barn
          </Button>
        </InputList>
      </Panel>
    </FormWrapper>
  )
}
