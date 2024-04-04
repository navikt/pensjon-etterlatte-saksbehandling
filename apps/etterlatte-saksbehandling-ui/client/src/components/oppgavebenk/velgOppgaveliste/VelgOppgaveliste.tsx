import React, { Dispatch, ReactNode, SetStateAction, useEffect } from 'react'
import styled from 'styled-components'
import { Loader, Tabs } from '@navikt/ds-react'
import { InboxIcon, PersonIcon } from '@navikt/aksel-icons'
import { OppgavelisteValg } from '~components/oppgavebenk/velgOppgaveliste/oppgavelisteValg'
import { useOppgaveBenkState, useOppgavebenkStateDispatcher } from '~components/oppgavebenk/state/OppgavebenkContext'
import { useApiCall } from '~shared/hooks/useApiCall'
import { hentOppgavebenkStats } from '~shared/api/oppgaver'
import { mapResult } from '~shared/api/apiUtils'

interface Props {
  oppgavelisteValg: OppgavelisteValg
  setOppgavelisteValg: Dispatch<SetStateAction<OppgavelisteValg>>
}

export const VelgOppgaveliste = ({ oppgavelisteValg, setOppgavelisteValg }: Props): ReactNode => {
  const dispatcher = useOppgavebenkStateDispatcher()
  const oppgaveBenkState = useOppgaveBenkState()

  const [oppgavebenkStatsResult, oppgavebenkStatsFetch] = useApiCall(hentOppgavebenkStats)

  useEffect(() => {
    if (!!oppgaveBenkState.oppgpavebenkStats.antallOppgavelistaOppgaver) {
      oppgavebenkStatsFetch({}, (result) => dispatcher.setOppgavebenkStats(result))
    }
  }, [oppgaveBenkState.oppgpavebenkStats.antallOppgavelistaOppgaver])

  return (
    <VelgOppgavelisteTabs value={oppgavelisteValg} onChange={(e) => setOppgavelisteValg(e as OppgavelisteValg)}>
      <Tabs.List>
        <Tabs.Tab
          value={OppgavelisteValg.OPPGAVELISTA}
          label={
            <>
              Oppgavelisten{' '}
              {mapResult(oppgavebenkStatsResult, {
                pending: <Loader />,
                success: ({ antallOppgavelistaOppgaver }) => <>{`(${antallOppgavelistaOppgaver})`}</>,
              })}
            </>
          }
          icon={<InboxIcon aria-hidden />}
        />
        <Tabs.Tab
          value={OppgavelisteValg.MIN_OPPGAVELISTE}
          label={
            <>
              Min oppgaveliste{' '}
              {mapResult(oppgavebenkStatsResult, {
                pending: <Loader />,
                success: ({ antallMinOppgavelisteOppgaver }) => <>{`(${antallMinOppgavelisteOppgaver})`}</>,
              })}
            </>
          }
          icon={<PersonIcon aria-hidden />}
        />
        <Tabs.Tab value={OppgavelisteValg.GOSYS_OPPGAVER} label="Gosys-oppgaver" icon={<InboxIcon aria-hidden />} />
      </Tabs.List>
    </VelgOppgavelisteTabs>
  )
}

const VelgOppgavelisteTabs = styled(Tabs)`
  max-width: fit-content;
  margin-bottom: 2rem;
`
