import React, { Dispatch, ReactNode, SetStateAction } from 'react'
import styled from 'styled-components'
import { Tabs } from '@navikt/ds-react'
import { InboxIcon, PersonIcon } from '@navikt/aksel-icons'
import { OppgavelisteValg } from '~components/oppgavebenk/velgOppgaveliste/oppgavelisteValg'
import { OppgavelisteneStats } from '~components/oppgavebenk/utils/oppgaveutils'

interface Props {
  oppgavelisteValg: OppgavelisteValg
  setOppgavelisteValg: Dispatch<SetStateAction<OppgavelisteValg>>
  oppgavelisteneStats: OppgavelisteneStats
}

export const VelgOppgaveliste = ({ oppgavelisteValg, setOppgavelisteValg, oppgavelisteneStats }: Props): ReactNode => {
  return (
    <VelgOppgavelisteTabs value={oppgavelisteValg} onChange={(e) => setOppgavelisteValg(e as OppgavelisteValg)}>
      <Tabs.List>
        <Tabs.Tab
          value={OppgavelisteValg.OPPGAVELISTA}
          label={`Oppgavelisten (${oppgavelisteneStats.antallOppgavelistaOppgaver})`}
          icon={<InboxIcon />}
        />
        <Tabs.Tab
          value={OppgavelisteValg.MIN_OPPGAVELISTE}
          label={`Min oppgaveliste (${oppgavelisteneStats.antallMinOppgavelisteOppgaver})`}
          icon={<PersonIcon aria-hidden />}
        />
        <Tabs.Tab
          value={OppgavelisteValg.GOSYS_OPPGAVER}
          label={`Gosys-oppgaver (${oppgavelisteneStats.antallGosysOppgaver})`}
          icon={<InboxIcon />}
        />
      </Tabs.List>
    </VelgOppgavelisteTabs>
  )
}

const VelgOppgavelisteTabs = styled(Tabs)`
  max-width: fit-content;
  margin-bottom: 2rem;
`
