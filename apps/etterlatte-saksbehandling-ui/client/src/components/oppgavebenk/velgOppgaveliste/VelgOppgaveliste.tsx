import React, { Dispatch, ReactNode, SetStateAction } from 'react'
import styled from 'styled-components'
import { Tabs } from '@navikt/ds-react'
import { InboxIcon, PersonIcon } from '@navikt/aksel-icons'

export enum OppgavelisteValg {
  OPPGAVELISTA = 'Oppgavelista',
  MIN_OPPGAVELISTE = 'MinOppgaveliste',
}

interface Props {
  oppgavelisteValg: OppgavelisteValg
  setOppgavelisteValg: Dispatch<SetStateAction<OppgavelisteValg>>
  antallOppgavelistaOppgaver: number
  antallMinOppgavelisteOppgaver: number
}

export const VelgOppgaveliste = ({
  oppgavelisteValg,
  setOppgavelisteValg,
  antallOppgavelistaOppgaver,
  antallMinOppgavelisteOppgaver,
}: Props): ReactNode => {
  return (
    <VelgOppgavelisteTabs value={oppgavelisteValg} onChange={(e) => setOppgavelisteValg(e as OppgavelisteValg)}>
      <Tabs.List>
        <Tabs.Tab
          value={OppgavelisteValg.OPPGAVELISTA}
          label={`Oppgavelisten (${antallOppgavelistaOppgaver})`}
          icon={<InboxIcon />}
        />
        <Tabs.Tab
          value={OppgavelisteValg.MIN_OPPGAVELISTE}
          label={`Min oppgaveliste (${antallMinOppgavelisteOppgaver})`}
          icon={<PersonIcon aria-hidden />}
        />
      </Tabs.List>
    </VelgOppgavelisteTabs>
  )
}

const VelgOppgavelisteTabs = styled(Tabs)`
  max-width: fit-content;
  margin-bottom: 2rem;
`
