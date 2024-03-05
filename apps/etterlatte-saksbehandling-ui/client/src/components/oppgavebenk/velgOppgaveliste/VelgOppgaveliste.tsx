import React, { Dispatch, ReactNode, SetStateAction } from 'react'
import styled from 'styled-components'
import { Tabs } from '@navikt/ds-react'
import { InboxIcon, PersonIcon } from '@navikt/aksel-icons'

export type oppgavelisteValg = 'Oppgavelista' | 'MinOppgaveliste'

interface Props {
  oppgavelisteValg: oppgavelisteValg
  setOppgavelisteValg: Dispatch<SetStateAction<oppgavelisteValg>>
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
    <VelgOppgavelisteTabs value={oppgavelisteValg} onChange={(e) => setOppgavelisteValg(e as oppgavelisteValg)}>
      <Tabs.List>
        <Tabs.Tab value="Oppgavelista" label={`Oppgavelisten (${antallOppgavelistaOppgaver})`} icon={<InboxIcon />} />
        <Tabs.Tab
          value="MinOppgaveliste"
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
