import { Button, Select, TextField, UNSAFE_Combobox } from '@navikt/ds-react'
import React, { ReactNode, useEffect, useState } from 'react'
import { Filter, initialFilter, oppgavetypefilter } from '~components/oppgavebenk/oppgaveFiltrering/oppgavelistafiltre'
import { useFeatureEnabledMedDefault } from '~shared/hooks/useFeatureToggle'
import { FlexRow } from '~shared/styled'
import { FEATURE_TOGGLE_KAN_BRUKE_KLAGE } from '~components/person/KlageListe'
import { Saksbehandler } from '~shared/types/saksbehandler'
import { OppgaveDTO } from '~shared/api/oppgaver'
import { MultiSelectFilter } from '~components/oppgavebenk/oppgaveFiltrering/MultiSelectFilter'
import {
  ENHETFILTER,
  EnhetFilterKeys,
  FRISTFILTER,
  FristFilterKeys,
  OPPGAVESTATUSFILTER,
  OppgavetypeFilterKeys,
  SAKSBEHANDLERFILTER,
  YTELSEFILTER,
  YtelseFilterKeys,
} from '~components/oppgavebenk/oppgaveFiltrering/filterTyper'

interface Props {
  hentAlleOppgaver: () => void
  hentOppgaverStatus: (oppgavestatusFilter: Array<string>) => void
  filter: Filter
  setFilter: (filter: Filter) => void
  alleOppgaver: Array<OppgaveDTO>
  saksbehandlereIEnhet: Array<Saksbehandler>
}

export const FilterRad = ({
  hentAlleOppgaver,
  hentOppgaverStatus,
  filter,
  setFilter,
  alleOppgaver,
}: Props): ReactNode => {
  const [sakId, setSakId] = useState<string>(filter.sakidFilter)

  const saksbehandlere: Set<string> = new Set(
    alleOppgaver.map((oppgave) => oppgave.saksbehandler?.ident || '').filter((ident) => !!ident)
  )
  const [saksbehandlerFilterLokal, setSaksbehandlerFilterLokal] = useState<string>(filter.saksbehandlerFilter)
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

        {/* TODO: Burde være en liste over navn, ikke identer. Burde også KUN vise de som tilhører enhet */}
        <UNSAFE_Combobox
          label="Saksbehandler"
          value={saksbehandlerFilterLokal}
          options={Object.entries(SAKSBEHANDLERFILTER)
            .map(([, beskrivelse]) => beskrivelse)
            .concat(Array.from(saksbehandlere))}
          onChange={(e) => {
            setSaksbehandlerFilterLokal(e?.target.value ? e?.target.value : '')
          }}
          onToggleSelected={(option, isSelected) => {
            if (isSelected) {
              setFilter({ ...filter, saksbehandlerFilter: option })
            }
          }}
        />

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
          onChange={(options) => {
            hentOppgaverStatus(options)
            setFilter({ ...filter, oppgavestatusFilter: options })
          }}
        />

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
