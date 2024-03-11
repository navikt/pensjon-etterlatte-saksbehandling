import React, { Dispatch, ReactNode, SetStateAction, useEffect, useState } from 'react'
import styled from 'styled-components'
import { Tabs } from '@navikt/ds-react'
import { InboxIcon, PersonIcon } from '@navikt/aksel-icons'
import { OppgavelisteValg } from '~components/oppgavebenk/velgOppgaveliste/oppgavelisteValg'
import { initalOppgavebenkStats, OppgavebenkStats } from '~components/oppgavebenk/utils/oppgaveutils'
import { hentOppgavelisteneStats } from '~shared/api/oppgaver'
import { useApiCall } from '~shared/hooks/useApiCall'

interface Props {
  oppgavelisteValg: OppgavelisteValg
  setOppgavelisteValg: Dispatch<SetStateAction<OppgavelisteValg>>
}

export const VelgOppgaveliste = ({ oppgavelisteValg, setOppgavelisteValg }: Props): ReactNode => {
  const [oppgavebenkStats, setOppgavebenkStats] = useState<OppgavebenkStats>(initalOppgavebenkStats)
  const [, hentOppgavebenkStatsFetch] = useApiCall(hentOppgavelisteneStats)

  useEffect(() => {
    hentOppgavebenkStatsFetch({}, setOppgavebenkStats)
  }, [])

  return (
    <VelgOppgavelisteTabs value={oppgavelisteValg} onChange={(e) => setOppgavelisteValg(e as OppgavelisteValg)}>
      <Tabs.List>
        <Tabs.Tab
          value={OppgavelisteValg.OPPGAVELISTA}
          label={`Oppgavelisten (${oppgavebenkStats.antallOppgavelistaOppgaver})`}
          icon={<InboxIcon />}
        />
        <Tabs.Tab
          value={OppgavelisteValg.MIN_OPPGAVELISTE}
          label={`Min oppgaveliste (${oppgavebenkStats.antallMinOppgavelisteOppgaver})`}
          icon={<PersonIcon aria-hidden />}
        />
        <Tabs.Tab value={OppgavelisteValg.GOSYS_OPPGAVER} label="Gosys-oppgaver" icon={<InboxIcon />} />
      </Tabs.List>
    </VelgOppgavelisteTabs>
  )
}

const VelgOppgavelisteTabs = styled(Tabs)`
  max-width: fit-content;
  margin-bottom: 2rem;
`
