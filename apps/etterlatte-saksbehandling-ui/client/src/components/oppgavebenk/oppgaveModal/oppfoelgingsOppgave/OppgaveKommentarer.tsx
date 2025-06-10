import { OppgaveKommentar } from '~shared/types/oppgave'
import { BodyShort, Detail } from '@navikt/ds-react'
import { EndringElement, EndringListe } from '~components/behandling/sidemeny/OppgaveEndring'

const kommentarer: Array<OppgaveKommentar> = [
  {
    oppgaveId: '1',
    kommentar: 'Æ har ringt sverre',
    ident: 'K12345',
    navn: 'Ludvigsen knut',
    tidspunkt: '12.12.24',
  },
  {
    oppgaveId: '2',
    kommentar: 'Æ har ringt sverre, igjen',
    ident: 'K12345',
    navn: 'Ludvigsen knut',
    tidspunkt: '13.12.24',
  },
].reverse()

export const OppgaveKommentarer = () => {
  return (
    <EndringListe>
      {kommentarer.map((kommentar, index) => (
        <EndringElement key={index}>
          <BodyShort size="small">{kommentar.kommentar}</BodyShort>
          <Detail>utført av {kommentar.navn}</Detail>
          <Detail>{kommentar.tidspunkt}</Detail>
        </EndringElement>
      ))}
    </EndringListe>
  )
}
