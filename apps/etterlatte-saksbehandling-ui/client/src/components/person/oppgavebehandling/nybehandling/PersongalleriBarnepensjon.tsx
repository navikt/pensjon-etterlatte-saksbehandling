import { Button, Heading, Panel, TextField } from '@navikt/ds-react'
import { Persongalleri } from '~shared/types/Person'
import { PlusIcon, XMarkIcon } from '@navikt/aksel-icons'
import React from 'react'
import { FormWrapper } from '../styled'
import { InputList, InputRow } from './OpprettNyBehandling'

interface Props {
  persongalleri?: Persongalleri
  oppdaterPersongalleri: (persongalleri: Persongalleri) => void
}

type PersonArray = keyof Omit<Persongalleri, 'soeker' | 'innsender'>

export default function PersongalleriBarnepensjon({ persongalleri, oppdaterPersongalleri }: Props) {
  const oppdater = (field: PersonArray, fnr: string, index: number) => {
    const nyState = persongalleri ? [...(persongalleri[field] || [])] : []
    nyState[index] = fnr
    oppdaterPersongalleri({ ...persongalleri, [field]: nyState })
  }

  const leggTil = (field: PersonArray) => {
    const nyState = persongalleri ? [...(persongalleri[field] || []), ''] : ['']

    oppdaterPersongalleri({
      ...persongalleri,
      [field]: nyState,
    })
  }

  const fjern = (field: PersonArray, index: number) => {
    const nyState = persongalleri ? [...(persongalleri[field] || [])] : []
    nyState.splice(index, 1)
    oppdaterPersongalleri({ ...persongalleri, [field]: nyState })
  }

  const kanLeggeTil = (): boolean => {
    return (persongalleri?.gjenlevende?.length || 0) + (persongalleri?.avdoed?.length || 0) < 2
  }

  return (
    <FormWrapper column>
      <InputRow>
        <TextField
          label={'Søker (barnet)'}
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

      <Panel border>
        <Heading size={'small'} spacing>
          Gjenlevende forelder
        </Heading>

        <InputList>
          {persongalleri?.gjenlevende?.map((gjenlevende, index) => (
            <InputRow key={index}>
              <TextField
                label={`Gjenlevende ${persongalleri!!.gjenlevende!!.length > 1 ? index + 1 : ''}`}
                placeholder={'Fødselsnummer'}
                value={gjenlevende}
                pattern={'[0-9]{11}'}
                maxLength={11}
                onChange={(e) => oppdater('gjenlevende', e.target.value, index)}
              />
              <Button icon={<XMarkIcon />} variant={'tertiary'} onClick={() => fjern('gjenlevende', index)} />
            </InputRow>
          ))}
          <Button icon={<PlusIcon />} onClick={() => leggTil('gjenlevende')} disabled={!kanLeggeTil()}>
            Legg til gjenlevende
          </Button>
        </InputList>
      </Panel>

      <Panel border>
        <Heading size={'small'} spacing>
          Avdød forelder
        </Heading>

        <InputList>
          {persongalleri?.avdoed?.map((avdoed, index) => (
            <InputRow key={index}>
              <TextField
                label={`Avdød ${persongalleri!!.avdoed!!.length > 1 ? index + 1 : ''}`}
                placeholder={'Fødselsnummer'}
                value={avdoed}
                pattern={'[0-9]{11}'}
                maxLength={11}
                onChange={(e) => oppdater('avdoed', e.target.value, index)}
              />
              <Button icon={<XMarkIcon />} variant={'tertiary'} onClick={() => fjern('avdoed', index)} />
            </InputRow>
          ))}
          <Button icon={<PlusIcon />} onClick={() => leggTil('avdoed')} disabled={!kanLeggeTil()}>
            Legg til avdød
          </Button>
        </InputList>
      </Panel>

      <Panel border>
        <Heading size={'small'} spacing>
          Søsken
        </Heading>

        <InputList>
          {persongalleri?.soesken?.map((soesken, index) => (
            <InputRow key={index}>
              <TextField
                label={`Søsken ${persongalleri!!.soesken!!.length > 1 ? index + 1 : ''}`}
                placeholder={'Fødselsnummer'}
                value={soesken}
                pattern={'[0-9]{11}'}
                maxLength={11}
                onChange={(e) => oppdater('soesken', e.target.value, index)}
              />
              <Button icon={<XMarkIcon />} variant={'tertiary'} onClick={() => fjern('soesken', index)} />
            </InputRow>
          ))}
          <Button icon={<PlusIcon />} onClick={() => leggTil('soesken')}>
            Legg til søsken
          </Button>
        </InputList>
      </Panel>
    </FormWrapper>
  )
}
