import { Button, Select, TextField } from '@navikt/ds-react'
import React, { ReactNode, useEffect, useState } from 'react'
import {
  ENHETFILTER,
  EnhetFilterKeys,
  Filter,
  FRISTFILTER,
  FristFilterKeys,
  initialFilter,
  oppgavetypefilter,
  OppgavetypeFilterKeys,
  YTELSEFILTER,
  YtelseFilterKeys,
} from '~components/oppgavebenk/oppgaveFiltrering/oppgavelistafiltre'
import { useFeatureEnabledMedDefault } from '~shared/hooks/useFeatureToggle'
import { FlexRow } from '~shared/styled'
import { FEATURE_TOGGLE_KAN_BRUKE_KLAGE } from '~components/person/KlageListe'
import { VelgOppgavestatuser } from '~components/oppgavebenk/oppgaveFiltrering/VelgOppgavestatuser'
import { Saksbehandler } from '~shared/types/saksbehandler'
import { FiltrerPaaSaksbehandler } from '~components/oppgavebenk/oppgaveFiltrering/FiltrerPaaSaksbehandler'

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
  const [fnr, setFnr] = useState<string>(filter.fnrFilter)

  const kanBrukeKlage = useFeatureEnabledMedDefault(FEATURE_TOGGLE_KAN_BRUKE_KLAGE, false)

  useEffect(() => {
    const delay = setTimeout(() => setFilter({ ...filter, sakidFilter: sakId || '', fnrFilter: fnr || '' }), 500)
    return () => clearTimeout(delay)
  }, [sakId, fnr])

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
          value={fnr}
          onChange={(e) => setFnr(e.target.value?.replace(/[^0-9+]/, ''))}
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
        <VelgOppgavestatuser
          value={filter.oppgavestatusFilter}
          onChange={(oppgavestatusFilter) => {
            hentOppgaverStatus(oppgavestatusFilter)
            setFilter({ ...filter, oppgavestatusFilter })
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
