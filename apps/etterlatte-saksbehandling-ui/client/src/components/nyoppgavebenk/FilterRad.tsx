import { Button, Select, TextField } from '@navikt/ds-react'
import React from 'react'
import styled from 'styled-components'
import {
  ENHETFILTER,
  EnhetFilterKeys,
  Filter,
  FRISTFILTER,
  FristFilterKeys,
  initialFilter,
  OPPGAVEKILDEFILTER,
  OppgaveKildeFilterKeys,
  OPPGAVESTATUSFILTER,
  OppgavestatusFilterKeys,
  oppgavetypefilter,
  OppgavetypeFilterKeys,
  SAKSBEHANDLERFILTER,
  SaksbehandlerFilterKeys,
  YTELSEFILTER,
  YtelseFilterKeys,
} from '~components/nyoppgavebenk/Oppgavelistafiltre'
import { useFeatureEnabledMedDefault } from '~shared/hooks/useFeatureToggle'
import { FEATURE_TOGGLE_KAN_BRUKE_KLAGE } from '~components/person/OpprettKlage'

const FilterFlex = styled.div`
  display: flex;
  gap: 2rem;
`

const ButtonWrapper = styled.div`
  display: flex;
  justify-content: flex-start;
  margin: 2rem 2rem 2rem 0rem;
  max-width: 20rem;
  button:first-child {
    margin-right: 1rem;
  }
`

export const FilterRad = (props: { hentOppgaver: () => void; filter: Filter; setFilter: (filter: Filter) => void }) => {
  const { hentOppgaver, filter, setFilter } = props
  const kanBrukeKlage = useFeatureEnabledMedDefault(FEATURE_TOGGLE_KAN_BRUKE_KLAGE)
  return (
    <>
      <FilterFlex>
        <TextField
          label="Fødselsnummer"
          value={filter.fnrFilter}
          onChange={(e) => setFilter({ ...filter, fnrFilter: e.target.value })}
          placeholder={'Søk'}
          autoComplete="off"
        />
        <Select
          label="Frist"
          value={filter.fristFilter}
          onChange={(e) =>
            setFilter({
              ...filter,
              fristFilter: e.target.value as FristFilterKeys,
            })
          }
        >
          {Object.entries(FRISTFILTER).map(([key, fristBeskrivelse]) => (
            <option key={key} value={key}>
              {fristBeskrivelse}
            </option>
          ))}
        </Select>
        <Select
          label="Saksbehandler"
          value={filter.saksbehandlerFilter}
          onChange={(e) => setFilter({ ...filter, saksbehandlerFilter: e.target.value as SaksbehandlerFilterKeys })}
        >
          {Object.entries(SAKSBEHANDLERFILTER).map(([key, beskrivelse]) => (
            <option key={key} value={key}>
              {beskrivelse}
            </option>
          ))}
        </Select>

        <Select
          label="Enhet"
          value={filter.enhetsFilter}
          onChange={(e) => setFilter({ ...filter, enhetsFilter: e.target.value as EnhetFilterKeys })}
        >
          {Object.entries(ENHETFILTER).map(([enhetsnummer, enhetBeskrivelse]) => (
            <option key={enhetsnummer} value={enhetsnummer}>
              {enhetBeskrivelse}
            </option>
          ))}
        </Select>
        <Select
          label="Ytelse"
          value={filter.ytelseFilter}
          onChange={(e) => setFilter({ ...filter, ytelseFilter: e.target.value as YtelseFilterKeys })}
        >
          {Object.entries(YTELSEFILTER).map(([saktype, saktypetekst]) => (
            <option key={saktype} value={saktype}>
              {saktypetekst}
            </option>
          ))}
        </Select>
        <Select
          label="Oppgavestatus"
          value={filter.oppgavestatusFilter}
          onChange={(e) => setFilter({ ...filter, oppgavestatusFilter: e.target.value as OppgavestatusFilterKeys })}
        >
          {Object.entries(OPPGAVESTATUSFILTER).map(([status, statusbeskrivelse]) => (
            <option key={status} value={status}>
              {statusbeskrivelse}
            </option>
          ))}
        </Select>
        <Select
          label="Oppgavetype"
          value={filter.oppgavetypeFilter}
          onChange={(e) => setFilter({ ...filter, oppgavetypeFilter: e.target.value as OppgavetypeFilterKeys })}
        >
          {oppgavetypefilter(kanBrukeKlage).map(([type, typebeskrivelse]) => (
            <option key={type} value={type}>
              {typebeskrivelse}
            </option>
          ))}
        </Select>
        <Select
          label="Kilde"
          value={filter.oppgavekildeFilter}
          onChange={(e) => setFilter({ ...filter, oppgavekildeFilter: e.target.value as OppgaveKildeFilterKeys })}
        >
          {Object.entries(OPPGAVEKILDEFILTER).map(([type, typebeskrivelse]) => (
            <option key={type} value={type}>
              {typebeskrivelse}
            </option>
          ))}
        </Select>
      </FilterFlex>
      <ButtonWrapper>
        <Button onClick={hentOppgaver}>Hent</Button>
        <Button variant="secondary" onClick={() => setFilter(initialFilter())}>
          Tilbakestill alle filtre
        </Button>
      </ButtonWrapper>
    </>
  )
}
