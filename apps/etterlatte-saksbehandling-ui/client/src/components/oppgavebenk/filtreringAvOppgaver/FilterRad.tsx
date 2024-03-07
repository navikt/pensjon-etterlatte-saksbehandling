import { Button, Select, TextField } from '@navikt/ds-react'
import React, { ReactNode, useEffect, useState } from 'react'
import {
  initialFilter,
  minOppgavelisteFiltre,
  oppgavetypefilter,
} from '~components/oppgavebenk/filtreringAvOppgaver/filtrerOppgaver'
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
import { ArrowCirclepathIcon, ArrowUndoIcon } from '@navikt/aksel-icons'
import { OppgavelisteValg } from '~components/oppgavebenk/velgOppgaveliste/oppgavelisteValg'

interface Props {
  hentAlleOppgaver: () => void
  hentOppgaverStatus: (oppgavestatusFilter: Array<string>) => void
  filter: Filter
  setFilter: (filter: Filter) => void
  saksbehandlereIEnhet: Array<Saksbehandler>
  oppgavelisteValg?: OppgavelisteValg
}

export const FilterRad = ({
  hentAlleOppgaver,
  hentOppgaverStatus,
  filter,
  setFilter,
  saksbehandlereIEnhet,
  oppgavelisteValg,
}: Props): ReactNode => {
  const [sakEllerFnr, setSakEllerFnr] = useState<string>(filter.sakEllerFnrFilter)

  const kanBrukeKlage = useFeatureEnabledMedDefault(FEATURE_TOGGLE_KAN_BRUKE_KLAGE, false)

  useEffect(() => {
    const delay = setTimeout(() => setFilter({ ...filter, sakEllerFnrFilter: sakEllerFnr }), 500)
    return () => clearTimeout(delay)
  }, [sakEllerFnr])

  return (
    <>
      <FlexRow $spacing align="start">
        <TextField
          label="Sakid / Fnr."
          width="1rem"
          value={sakEllerFnr}
          onChange={(e) => setSakEllerFnr(e.target.value?.replace(/[^0-9+]/, ''))}
          type="tel"
          min={0}
          placeholder="Søk"
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
        {oppgavelisteValg === OppgavelisteValg.OPPGAVELISTA && (
          <>
            <FiltrerPaaSaksbehandler
              saksbehandlereIEnhet={saksbehandlereIEnhet}
              filter={filter}
              setFilter={setFilter}
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
          </>
        )}

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

        {oppgavelisteValg !== OppgavelisteValg.GOSYS_OPPGAVER && (
          <>
            <MultiSelectFilter
              label="Oppgavestatus"
              options={Object.entries(OPPGAVESTATUSFILTER).map(([, beskrivelse]) => beskrivelse)}
              values={filter.oppgavestatusFilter}
              onChange={(statuser) => {
                hentOppgaverStatus(statuser)
                setFilter({ ...filter, oppgavestatusFilter: statuser })
              }}
            />

            <MultiSelectFilter
              label="Oppgavetype"
              options={oppgavetypefilter(kanBrukeKlage).map(([, beskrivelse]) => beskrivelse)}
              values={filter.oppgavetypeFilter}
              onChange={(statuser) => setFilter({ ...filter, oppgavetypeFilter: statuser })}
            />
          </>
        )}
      </FlexRow>

      <FlexRow $spacing>
        <Button onClick={hentAlleOppgaver} size="small" icon={<ArrowCirclepathIcon />} iconPosition="right">
          Hent oppgaver
        </Button>
        <Button
          variant="secondary"
          onClick={() => {
            setFilter(oppgavelisteValg === OppgavelisteValg.OPPGAVELISTA ? initialFilter() : minOppgavelisteFiltre())
            hentAlleOppgaver()
          }}
          size="small"
          icon={<ArrowUndoIcon />}
          iconPosition="right"
        >
          Tilbakestill filtre
        </Button>
      </FlexRow>
    </>
  )
}
