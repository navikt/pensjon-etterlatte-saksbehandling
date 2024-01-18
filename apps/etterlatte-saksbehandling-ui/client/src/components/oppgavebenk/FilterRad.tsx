import { Button, Select, TextField, UNSAFE_Combobox } from '@navikt/ds-react'
import React, { ReactNode, useEffect, useState } from 'react'
import {
  ENHETFILTER,
  EnhetFilterKeys,
  Filter,
  FRISTFILTER,
  FristFilterKeys,
  initialFilter,
  OPPGAVEKILDEFILTER,
  OppgaveKildeFilterKeys,
  oppgavetypefilter,
  OppgavetypeFilterKeys,
  SAKSBEHANDLERFILTER,
  YTELSEFILTER,
  YtelseFilterKeys,
} from '~components/oppgavebenk/Oppgavelistafiltre'
import { useFeatureEnabledMedDefault } from '~shared/hooks/useFeatureToggle'
import { FlexRow } from '~shared/styled'
import { OppgaveDTO } from '~shared/api/oppgaver'
import { FEATURE_TOGGLE_KAN_BRUKE_KLAGE } from '~components/person/KlageListe'
import { VelgOppgavestatuser } from '~components/oppgavebenk/VelgOppgavestatuser'

interface Props {
  hentOppgaver: () => void
  filter: Filter
  setFilter: (filter: Filter) => void
  alleOppgaver: OppgaveDTO[]
}

export const FilterRad = ({ hentOppgaver, filter, setFilter, alleOppgaver }: Props): ReactNode => {
  const saksbehandlere = new Set(
    alleOppgaver.map((oppgave) => oppgave.saksbehandler).filter((s): s is Exclude<typeof s, null> => s !== null)
  )
  const [saksbehandlerFilterLokal, setSaksbehandlerFilterLokal] = useState<string>(filter.saksbehandlerFilter)
  const kanBrukeKlage = useFeatureEnabledMedDefault(FEATURE_TOGGLE_KAN_BRUKE_KLAGE, false)

  const [oppgavestatuserValgt, setOppgavestatuserValgt] = useState<Array<string>>(['NY', 'UNDER_BEHANDLING'])

  useEffect(() => {
    setFilter({ ...filter, oppgavestatusFilter: oppgavestatuserValgt })
  }, [oppgavestatuserValgt])

  return (
    <>
      <FlexRow $spacing align="start">
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
        <VelgOppgavestatuser
          oppgavestatuserValgt={oppgavestatuserValgt}
          setOppgavestatuserValgt={setOppgavestatuserValgt}
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
      </FlexRow>

      <FlexRow $spacing>
        <Button onClick={hentOppgaver}>Hent</Button>
        <Button variant="secondary" onClick={() => setFilter(initialFilter())}>
          Tilbakestill alle filtre
        </Button>
      </FlexRow>
    </>
  )
}
