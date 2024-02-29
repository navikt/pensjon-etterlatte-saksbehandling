import { Button, Select, TextField, UNSAFE_Combobox } from '@navikt/ds-react'
import React, { ReactNode, useState } from 'react'
import {
  ENHETFILTER,
  EnhetFilterKeys,
  Filter,
  FRISTFILTER,
  FristFilterKeys,
  initialFilter,
  oppgavetypefilter,
  OppgavetypeFilterKeys,
  SAKSBEHANDLERFILTER,
  YTELSEFILTER,
  YtelseFilterKeys,
} from '~components/oppgavebenk/oppgaveFiltrering/oppgavelistafiltre'
import { useFeatureEnabledMedDefault } from '~shared/hooks/useFeatureToggle'
import { FlexRow } from '~shared/styled'
import { FEATURE_TOGGLE_KAN_BRUKE_KLAGE } from '~components/person/KlageListe'
import { VelgOppgavestatuser } from '~components/oppgavebenk/oppgaveFiltrering/VelgOppgavestatuser'
import { Saksbehandler } from '~shared/types/saksbehandler'

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
  const [saksbehandlerFilterLokal, setSaksbehandlerFilterLokal] = useState<string>(filter.saksbehandlerFilter)
  const kanBrukeKlage = useFeatureEnabledMedDefault(FEATURE_TOGGLE_KAN_BRUKE_KLAGE, false)

  const saksbehandlerFilterValues = (): string[] => {
    return Object.entries(SAKSBEHANDLERFILTER).map(([, beskrivelse]) => beskrivelse)
  }

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
          options={saksbehandlerFilterValues().concat(
            Array.from(saksbehandlereIEnhet.map((behandler) => behandler.navn))
          )}
          onChange={(e) => {
            setSaksbehandlerFilterLokal(e?.target.value ? e?.target.value : '')
          }}
          onToggleSelected={(option, isSelected) => {
            if (isSelected) {
              if (saksbehandlerFilterValues().find((val) => val === option)) {
                setFilter({ ...filter, saksbehandlerFilter: option })
              } else {
                const selectedSaksbehandler: Saksbehandler = saksbehandlereIEnhet.find(
                  (behandler) => behandler.navn === option
                ) || { navn: '', ident: '' }
                setFilter({ ...filter, saksbehandlerFilter: selectedSaksbehandler?.ident })
              }
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
