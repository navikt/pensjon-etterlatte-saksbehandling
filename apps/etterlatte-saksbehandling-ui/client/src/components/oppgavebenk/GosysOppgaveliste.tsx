import React, { useEffect, useState } from 'react'
import { mapResult } from '~shared/api/apiUtils'
import Spinner from '~shared/Spinner'
import { ApiErrorAlert } from '~ErrorBoundary'
import { Saksbehandler } from '~shared/types/saksbehandler'
import { EnhetFilterKeys, GosysFilter, GosysOppgaveValg } from '~components/oppgavebenk/filtreringAvOppgaver/typer'
import { ToggleGroup, VStack } from '@navikt/ds-react'
import { Tilgangsmelding } from '~components/oppgavebenk/components/Tilgangsmelding'
import { useOppgaveBenkState, useOppgavebenkStateDispatcher } from '~components/oppgavebenk/state/OppgavebenkContext'
import { useApiCall } from '~shared/hooks/useApiCall'
import { useInnloggetSaksbehandler } from '~components/behandling/useInnloggetSaksbehandler'
import { GosysOppgaver } from '~components/oppgavebenk/gosys/GosysOppgaver'
import { hentGosysOppgaver } from '~shared/api/gosys'
import { GosysFilterRad } from './filtreringAvOppgaver/GosysFilterRad'
import { GosysOppgave } from '~shared/types/Gosys'
import { formaterEnumTilLesbarString } from '~utils/formatering/formatering'
import {
  hentGosysOppgaverFilterFraLocalStorage,
  leggGosysOppgaverFilterILocalStorage,
} from '~components/oppgavebenk/filtreringAvOppgaver/filterLocalStorage'

interface Props {
  saksbehandlereIEnhet: Array<Saksbehandler>
}

export const sorterOppgaverEtterOpprettetGosys = (oppgaver: GosysOppgave[]) => {
  return oppgaver.sort((a, b) => new Date(b.opprettet).getTime() - new Date(a.opprettet).getTime())
}

export const GosysOppgaveliste = ({ saksbehandlereIEnhet }: Props) => {
  const innloggetSaksbehandler = useInnloggetSaksbehandler()

  if (!innloggetSaksbehandler.skriveEnheter.length) {
    return <Tilgangsmelding />
  }

  const harBareEnLokalSaksbehandlendeEnhet = (enheter: string[]): string[] => {
    const lokaleEnheter = ['E4815', 'E4808', 'E4817']
    return enheter.filter((enhet) => {
      return lokaleEnheter.includes(`E${enhet}`)
    })
  }

  const setLokalEnhetSomStandardOmIngenLocalStorage = (): GosysFilter => {
    const gosysFilter = hentGosysOppgaverFilterFraLocalStorage()
    if (!gosysFilter.enhetFilter) {
      const enheterForSaksbehandler = innloggetSaksbehandler.enheter
      const lokaleEnheterForSaksbehandler = harBareEnLokalSaksbehandlendeEnhet(enheterForSaksbehandler)
      if (lokaleEnheterForSaksbehandler.length == 1) {
        return { ...gosysFilter, enhetFilter: lokaleEnheterForSaksbehandler[0] as EnhetFilterKeys }
      }
      return { ...gosysFilter }
    }
    return gosysFilter
  }

  const [filter, setFilter] = useState<GosysFilter>(setLokalEnhetSomStandardOmIngenLocalStorage())
  const [fnrFilter, setFnrFilter] = useState<string>()

  const oppgavebenkState = useOppgaveBenkState()
  const dispatcher = useOppgavebenkStateDispatcher()

  const [gosysOppgaverResult, hentGosysOppgaverFetch] = useApiCall(hentGosysOppgaver)
  const lagGosysFilterBasertPaaOppgaveValg = (oppgaveValg: GosysOppgaveValg): GosysFilter => {
    switch (oppgaveValg) {
      case GosysOppgaveValg.ALLE_OPPGAVER:
        return { ...filter, saksbehandlerFilter: undefined, harTildelingFilter: undefined }
      case GosysOppgaveValg.MINE_OPPGAVER:
        return { ...filter, saksbehandlerFilter: innloggetSaksbehandler.ident, harTildelingFilter: undefined }
      case GosysOppgaveValg.IKKE_TILDELTE:
        return { ...filter, saksbehandlerFilter: undefined, harTildelingFilter: false }
    }
  }

  const hentOppgaver = () => {
    hentGosysOppgaverFetch(filter, (oppgaver) => {
      dispatcher.setGosysOppgavelisteOppgaver(sorterOppgaverEtterOpprettetGosys(oppgaver))
    })
  }

  useEffect(() => {
    leggGosysOppgaverFilterILocalStorage(filter)
    hentOppgaver()
  }, [filter])

  return (
    <VStack gap="space-4">
      <ToggleGroup
        defaultValue={GosysOppgaveValg.ALLE_OPPGAVER}
        onChange={(e) => setFilter(lagGosysFilterBasertPaaOppgaveValg(e as GosysOppgaveValg))}
        size="small"
      >
        <ToggleGroup.Item value={GosysOppgaveValg.ALLE_OPPGAVER}>
          {formaterEnumTilLesbarString(GosysOppgaveValg.ALLE_OPPGAVER)}
        </ToggleGroup.Item>
        <ToggleGroup.Item value={GosysOppgaveValg.MINE_OPPGAVER}>
          {formaterEnumTilLesbarString(GosysOppgaveValg.MINE_OPPGAVER)}
        </ToggleGroup.Item>
        <ToggleGroup.Item value={GosysOppgaveValg.IKKE_TILDELTE}>
          {formaterEnumTilLesbarString(GosysOppgaveValg.IKKE_TILDELTE)}
        </ToggleGroup.Item>
      </ToggleGroup>

      <GosysFilterRad
        hentAlleOppgaver={hentOppgaver}
        filter={filter}
        setFilter={setFilter}
        filterFoedselsnummer={setFnrFilter}
      />

      {mapResult(gosysOppgaverResult, {
        pending: <Spinner label="Henter Gosys-oppgaver" />,
        error: (error) => <ApiErrorAlert>{error.detail || 'Kunne ikke hente Gosys-oppgaver'}</ApiErrorAlert>,
        success: () => (
          <GosysOppgaver
            oppgaver={oppgavebenkState.gosysOppgavelisteOppgaver}
            saksbehandlereIEnhet={saksbehandlereIEnhet}
            fnrFilter={fnrFilter}
          />
        ),
      })}
    </VStack>
  )
}
