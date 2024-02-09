import { useEffect, useState } from 'react'
import { Tabs } from '@navikt/ds-react'
import { InboxIcon, PersonIcon } from '@navikt/aksel-icons'
import styled from 'styled-components'
import { useAppSelector } from '~store/Store'
import { Container } from '~shared/styled'
import { Tilgangsmelding } from '~components/oppgavebenk/Tilgangsmelding'
import { Route, Routes, useNavigate } from 'react-router-dom'
import { MinOppgaveliste } from '~components/oppgavebenk/MinOppgaveliste'
import { OppgavelistaWrapper } from '~components/oppgavebenk/OppgavelistaWrapper'

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
      navigate('minoppgaveliste')
    } else {
      navigate('/')
    }
  }, [oppgaveListeValg])

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

      <Routes>
        <Route index element={<OppgavelistaWrapper />} />
        <Route key="minoppgaveliste" path="minoppgaveliste" element={<MinOppgaveliste />} />
        <Route path="*" element={<OppgavelistaWrapper />} />
      </Routes>
    </Container>
  )
}

const TabsWidth = styled(Tabs)`
  max-width: fit-content;
  margin-bottom: 2rem;
`
