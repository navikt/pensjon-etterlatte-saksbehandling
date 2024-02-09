import React, { useEffect, useState } from 'react'
import { Tabs } from '@navikt/ds-react'
import { InboxIcon, PersonIcon } from '@navikt/aksel-icons'
import styled from 'styled-components'
import { useAppSelector } from '~store/Store'
import { Container } from '~shared/styled'
import { Tilgangsmelding } from '~components/oppgavebenk/Tilgangsmelding'
import { Outlet, useLocation, useNavigate } from 'react-router-dom'

type OppgavelisteToggle = 'Oppgavelista' | 'MinOppgaveliste'

export const ToggleMinOppgaveliste = () => {
  const innloggetSaksbehandler = useAppSelector((state) => state.saksbehandlerReducer.innloggetSaksbehandler)
  if (!innloggetSaksbehandler.skriveTilgang) {
    return <Tilgangsmelding />
  }

  const [oppgaveListeValg, setOppgaveListeValg] = useState<OppgavelisteToggle>('Oppgavelista')
  const navigate = useNavigate()

  const oppgavelengde = useAppSelector((state) => state.oppgaveRedurcer)

  useEffect(() => {
    if (oppgaveListeValg === 'MinOppgaveliste') {
      navigate('/minoppgaveliste')
    } else {
      navigate('/')
    }
  }, [oppgaveListeValg])
  const location = useLocation()

  console.log('renders toggle: ', location.pathname)
  return (
    <Container>
      <TabsWidth value={oppgaveListeValg} onChange={(e) => setOppgaveListeValg(e as OppgavelisteToggle)}>
        <Tabs.List>
          <Tabs.Tab value="Oppgavelista" label={`Oppgavelisten (${oppgavelengde.hovedlista})`} icon={<InboxIcon />} />
          <Tabs.Tab
            value="MinOppgaveliste"
            label={`Min oppgaveliste (${oppgavelengde.minliste})`}
            icon={<PersonIcon aria-hidden />}
          />
        </Tabs.List>
      </TabsWidth>
      <Outlet />
    </Container>
  )
}

const TabsWidth = styled(Tabs)`
  max-width: fit-content;
  margin-bottom: 2rem;
`
