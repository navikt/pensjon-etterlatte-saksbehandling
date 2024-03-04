import { Button, Select, TextField } from '@navikt/ds-react'
import React, { ReactNode, useEffect, useState } from 'react'
import { initialFilter, oppgavetypefilter } from '~components/oppgavebenk/filtreringAvOppgaver/filtrerOppgaver'
import { useFeatureEnabledMedDefault } from '~shared/hooks/useFeatureToggle'
import { FlexRow } from '~shared/styled'
import { FEATURE_TOGGLE_KAN_BRUKE_KLAGE } from '~components/person/KlageListe'
import { Saksbehandler } from '~shared/types/saksbehandler'
import { FiltrerPaaSaksbehandler } from '~components/oppgavebenk/filtreringAvOppgaver/FiltrerPaaSaksbehandler'
import {
  ENHETFILTER,
  EnhetFilterKeys,
  Filter,
  FRISTFILTER,
  FristFilterKeys,
  OPPGAVESTATUSFILTER,
  YTELSEFILTER,
  YtelseFilterKeys,
} from '~components/oppgavebenk/filtreringAvOppgaver/typer'
import { MultiSelectFilter } from '~components/oppgavebenk/filtreringAvOppgaver/MultiSelectFilter'

interface Props {
  hentAlleOppgaver: () => void
  hentOppgaverStatus: (oppgavestatusFilter: Array<string>) => void
  filter: Filter
  setFilter: (filter: Filter) => void
  saksbehandlereIEnhet: Array<Saksbehandler>
}

export const FilterRad = ({
  hentAlleOppgaver,
  hentOppgaverStatus,
  filter,
  setFilter,
  saksbehandlereIEnhet,
}: Props): ReactNode => {
  const [sakId, setSakId] = useState<string>(filter.sakidFilter)
  const kanBrukeKlage = useFeatureEnabledMedDefault(FEATURE_TOGGLE_KAN_BRUKE_KLAGE, false)

  useEffect(() => {
    const delay = setTimeout(() => setFilter({ ...filter, sakidFilter: sakId || '' }), 500)
    return () => clearTimeout(delay)
  }, [sakId])

  return (
    <>
      <FlexRow $spacing align="start">
        <TextField
          label="Sak ID"
          width="1rem"
          value={sakId}
          onChange={(e) => setSakId(e.target.value?.replace(/[^0-9+]/, ''))}
          type="tel"
          min={0}
          placeholder="Søk"
        />
        <TextField
          label="Fødselsnummer"
          value={filter.fnrFilter}
          onChange={(e) => setFilter({ ...filter, fnrFilter: e.target.value })}
          placeholder="Søk"
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
          {Object.entries(FRISTFILTER)
            .filter((o) => o)
            .map(([key, fristBeskrivelse]) => (
              <option key={key} value={key}>
                {fristBeskrivelse}
              </option>
            ))}
        </Select>

        {/* TODO: FÅ DENNE TIL Å FUNKE IGJEN */}
        <FiltrerPaaSaksbehandler saksbehandlereIEnhet={saksbehandlereIEnhet} filter={filter} setFilter={setFilter} />

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

        <MultiSelectFilter
          label="Oppgavestatus"
          options={Object.entries(OPPGAVESTATUSFILTER).map(([, beskrivelse]) => beskrivelse)}
          value={filter.oppgavestatusFilter}
          onChange={(statuser) => {
            hentOppgaverStatus(statuser)
            setFilter({ ...filter, oppgavestatusFilter: statuser })
          }}
        />

        <MultiSelectFilter
          label="Oppgavetype"
          options={oppgavetypefilter(kanBrukeKlage).map(([, beskrivelse]) => beskrivelse)}
          value={filter.oppgavetypeFilter}
          onChange={(statuser) => setFilter({ ...filter, oppgavetypeFilter: statuser })}
        />
      </FlexRow>

      <FlexRow $spacing>
        <Button onClick={hentAlleOppgaver}>Hent</Button>
        <Button
          variant="secondary"
          onClick={() => {
            setFilter(initialFilter())
            hentAlleOppgaver()
          }}
        >
          Tilbakestill alle filtre
        </Button>
      </FlexRow>
    </>
  )
}
