import { StegMenyWrapper } from '~components/behandling/StegMeny/stegmeny'
import { NavLink } from 'react-router-dom'
import { Separator } from '~components/behandling/StegMeny/NavLenke'

export default function OppgaveStegmeny() {
  return (
    <StegMenyWrapper>
      <li>
        <NavLink to={'kontroll'}>Oppgavekontroll</NavLink>
      </li>
      <Separator aria-hidden={'true'} />
      <li>
        <NavLink to={'nybehandling'}>Ny behandling</NavLink>
      </li>
      <Separator aria-hidden={'true'} />
      <li>
        <NavLink to={'oppsummering'}>Oppsummering</NavLink>
      </li>
    </StegMenyWrapper>
  )
}
