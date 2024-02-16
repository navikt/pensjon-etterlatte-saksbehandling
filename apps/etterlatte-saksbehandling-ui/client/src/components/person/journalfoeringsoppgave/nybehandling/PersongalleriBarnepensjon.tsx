import { BodyShort, Button, Heading, Panel, TextField } from '@navikt/ds-react'
import { Persongalleri } from '~shared/types/Person'
import { PlusIcon, XMarkIcon } from '@navikt/aksel-icons'
import React, { useEffect } from 'react'
import { InputList, InputRow } from './OpprettNyBehandling'
import { useJournalfoeringOppgave } from '~components/person/journalfoeringsoppgave/useJournalfoeringOppgave'
import { useAppDispatch } from '~store/Store'
import { settNyBehandlingRequest } from '~store/reducers/JournalfoeringOppgaveReducer'
import { Control } from 'react-hook-form'
import { ControlledTextField } from '~components/person/journalfoeringsoppgave/nybehandling/ControlledTextField'

type PersonArray = keyof Omit<Persongalleri, 'soeker' | 'innsender'>

export default function PersongalleriBarnepensjon({
  erManuellMigrering = false,
  fnrFraOppgave = undefined,
  control,
}: {
  erManuellMigrering?: boolean
  fnrFraOppgave?: string | undefined
  control: Control
}) {
  const { nyBehandlingRequest } = useJournalfoeringOppgave()
  const dispatch = useAppDispatch()

  const persongalleri = nyBehandlingRequest?.persongalleri

  const oppdaterPersongalleri = (persongalleri: Persongalleri) => {
    dispatch(settNyBehandlingRequest({ ...nyBehandlingRequest, persongalleri }))
  }

  useEffect(() => {
    if (fnrFraOppgave) {
      oppdaterPersongalleri({ ...persongalleri, soeker: fnrFraOppgave })
    }
  }, [fnrFraOppgave])

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

  const validerFnrValgfri = (fnr: string): string | undefined => {
    if (fnr && !new RegExp(/[0-9]{11}/).test(fnr)) {
      return 'Fødselsnummer er på ugyldig format'
    }
    return undefined
  }

  const validateFnrObligatorisk = (fnr: string): string | undefined => {
    if (!fnr) {
      return 'Fødselsnummer må være satt'
    } else if (!new RegExp(/[0-9]{11}/).test(fnr)) {
      return 'Fødselsnummer er på ugyldig format'
    }
    return undefined
  }

  return (
    <>
      <InputRow>
        <ControlledTextField
          name="persongalleri.soeker"
          control={control}
          validate={validateFnrObligatorisk}
          label="Søker (barnet)"
          description={
            erManuellMigrering ? 'Oppgi søker sitt fødselsnummer' : 'Fødselsnummeret er automatisk hentet fra oppgaven'
          }
          readOnly={!erManuellMigrering}
        />
      </InputRow>

      {/*<InputRow>*/}
      {/*  <TextField*/}
      {/*    label="Søker (barnet)"*/}
      {/*    value={fnrFraOppgave || persongalleri?.soeker || ''}*/}
      {/*    pattern="[0-9]{11}"*/}
      {/*    maxLength={11}*/}
      {/*    onChange={(e) => oppdaterPersongalleri({ ...persongalleri, soeker: e.target.value })}*/}
      {/*    description={*/}
      {/*      erManuellMigrering ? 'Oppgi søker sitt fødselsnummer' : 'Fødselsnummeret er automatisk hentet fra oppgaven'*/}
      {/*    }*/}
      {/*    readOnly={!erManuellMigrering}*/}
      {/*  />*/}
      {/*</InputRow>*/}

      <InputRow>
        <ControlledTextField
          name="persongalleri.innsender"
          control={control}
          validate={validerFnrValgfri}
          label="Innsender"
          description="Oppgi innsenderen sitt fødselsnummer (dersom det er tilgjengelig)"
        />
      </InputRow>

      {/*<InputRow>*/}
      {/*  <TextField*/}
      {/*    label="Innsender"*/}
      {/*    description="Oppgi innsenderen sitt fødselsnummer (dersom det er tilgjengelig)"*/}
      {/*    value={persongalleri?.innsender || ''}*/}
      {/*    pattern="[0-9]{11}"*/}
      {/*    maxLength={11}*/}
      {/*    onChange={(e) => oppdaterPersongalleri({ ...persongalleri, innsender: e.target.value })}*/}
      {/*  />*/}
      {/*</InputRow>*/}

      <Panel border>
        <Heading size="small" spacing>
          Gjenlevende forelder
          <BodyShort textColor="subtle">Legg til gjenlevende hvis tilgjengelig</BodyShort>
        </Heading>

        {/* TODO: bruke useFieldArray fra RHF*/}
        <InputList>
          {persongalleri?.gjenlevende?.map((gjenlevende, index) => (
            <InputRow key={index}>
              <TextField
                label="Gjenlevende forelder"
                value={gjenlevende}
                pattern="[0-9]{11}"
                maxLength={11}
                onChange={(e) => oppdater('gjenlevende', e.target.value, index)}
                description="Oppgi fødselsnummer"
              />
              <Button icon={<XMarkIcon />} variant="tertiary" onClick={() => fjern('gjenlevende', index)} />
            </InputRow>
          ))}
          <Button icon={<PlusIcon />} onClick={() => leggTil('gjenlevende')} disabled={!kanLeggeTil()}>
            Legg til gjenlevende
          </Button>
        </InputList>
      </Panel>

      <Panel border>
        <Heading size="small" spacing>
          Avdød forelder
          <BodyShort textColor="subtle">Legg til avdød hvis tilgjengelig</BodyShort>
        </Heading>

        <InputList>
          {persongalleri?.avdoed?.map((avdoed, index) => (
            <InputRow key={index}>
              <TextField
                label="Avdød forelder"
                value={avdoed}
                pattern="[0-9]{11}"
                maxLength={11}
                onChange={(e) => oppdater('avdoed', e.target.value, index)}
                description="Oppgi fødselsnummer"
              />
              <Button icon={<XMarkIcon />} variant="tertiary" onClick={() => fjern('avdoed', index)} />
            </InputRow>
          ))}
          <Button icon={<PlusIcon />} onClick={() => leggTil('avdoed')} disabled={!kanLeggeTil()}>
            Legg til avdød
          </Button>
        </InputList>
      </Panel>

      <Panel border>
        <Heading size="small" spacing>
          Søsken
          <BodyShort textColor="subtle">Legg til barn hvis tilgjengelig</BodyShort>
        </Heading>

        <InputList>
          {persongalleri?.soesken?.map((soesken, index) => (
            <InputRow key={index}>
              <TextField
                label={`Søsken ${persongalleri!!.soesken!!.length > 1 ? index + 1 : ''}`}
                value={soesken}
                pattern="[0-9]{11}"
                maxLength={11}
                onChange={(e) => oppdater('soesken', e.target.value, index)}
                description="Oppgi fødselsnummer"
              />
              <Button icon={<XMarkIcon />} variant="tertiary" onClick={() => fjern('soesken', index)} />
            </InputRow>
          ))}
          <Button icon={<PlusIcon />} onClick={() => leggTil('soesken')}>
            Legg til søsken
          </Button>
        </InputList>
      </Panel>
    </>
  )
}
