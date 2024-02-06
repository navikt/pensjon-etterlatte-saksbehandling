import { OppgaveDTO } from '~shared/api/oppgaver'
import { Button } from '@navikt/ds-react'
import { EyeIcon } from '@navikt/aksel-icons'
import { useAppSelector } from '~store/Store'
import { GosysOppgaveModal } from '~components/oppgavebenk/oppgavemodal/GosysOppgaveModal'

export const HandlingerForOppgave = ({ oppgave }: { oppgave: OppgaveDTO }) => {
  const innloggetsaksbehandler = useAppSelector((state) => state.saksbehandlerReducer.innloggetSaksbehandler)
  const { type, kilde, fnr, saksbehandler, referanse, merknad } = oppgave
  const pesysid = (merknad: String | null) => (merknad ? merknad.split('=')[1] : '')

  const erInnloggetSaksbehandlerOppgave = saksbehandler ? saksbehandler === innloggetsaksbehandler.ident : false
  if (kilde === 'GENERELL_BEHANDLING') {
    switch (type) {
      case 'UNDERKJENT':
        return (
          <>
            {erInnloggetSaksbehandlerOppgave && (
              <Button size="small" as="a" href={`/generellbehandling/${referanse}`}>
                Gå til kravpakke utland
              </Button>
            )}
          </>
        )
      case 'KRAVPAKKE_UTLAND':
        return (
          <>
            {erInnloggetSaksbehandlerOppgave && (
              <Button size="small" as="a" href={`/generellbehandling/${referanse}`}>
                Gå til kravpakke utland
              </Button>
            )}
          </>
        )
      case 'ATTESTERING':
        return (
          <>
            {erInnloggetSaksbehandlerOppgave && (
              <Button size="small" as="a" href={`/generellbehandling/${referanse}`}>
                Gå til attestering av generell behandling
              </Button>
            )}
          </>
        )
    }
  }
  if (kilde === 'TILBAKEKREVING') {
    switch (type) {
      case 'ATTESTERING':
      case 'UNDERKJENT':
      case 'TILBAKEKREVING':
        return (
          erInnloggetSaksbehandlerOppgave && (
            <Button size="small" href={`/tilbakekreving/${referanse}`} as="a">
              Gå til tilbakekreving
            </Button>
          )
        )
    }
  }
  switch (type) {
    case 'VURDER_KONSEKVENS':
      return (
        <>
          <Button size="small" icon={<EyeIcon />} href={`/person/${fnr}`} as="a">
            Se hendelse
          </Button>
        </>
      )
    case 'UNDERKJENT':
    case 'FOERSTEGANGSBEHANDLING':
      return (
        <>
          {erInnloggetSaksbehandlerOppgave && (
            <Button
              size="small"
              as="a"
              href={
                referanse === 'GJENNOPPRETTELSE_OPPGAVE'
                  ? `/manuellbehandling/${fnr}/${pesysid(merknad)}`
                  : `/behandling/${referanse}`
              }
            >
              {referanse === 'GJENNOPPRETTELSE_OPPGAVE' ? 'Gå til manuell opprettelse' : 'Gå til behandling'}
            </Button>
          )}
        </>
      )
    case 'REVURDERING':
      return (
        <>
          {erInnloggetSaksbehandlerOppgave && (
            <Button size="small" href={`/behandling/${referanse}`} as="a">
              Gå til revurdering
            </Button>
          )}
        </>
      )
    case 'MANUELT_OPPHOER':
      return (
        <>
          {erInnloggetSaksbehandlerOppgave && (
            <Button size="small" href={`/behandling/${referanse}`} as="a">
              Gå til opphør
            </Button>
          )}
        </>
      )
    case 'ATTESTERING':
      return (
        <>
          {erInnloggetSaksbehandlerOppgave && (
            <Button size="small" href={`/behandling/${referanse}`} as="a">
              Gå til attestering
            </Button>
          )}
        </>
      )
    case 'GOSYS':
      return <GosysOppgaveModal oppgave={oppgave} />
    case 'KLAGE':
      return erInnloggetSaksbehandlerOppgave ? (
        <Button size="small" href={`/klage/${referanse}`} as="a">
          Gå til klage
        </Button>
      ) : null
    case 'KRAVPAKKE_UTLAND':
      return erInnloggetSaksbehandlerOppgave ? (
        <Button size="small" href={`/generellbehandling/${referanse}`} as="a">
          Gå til utlandssak
        </Button>
      ) : null
    case 'JOURNALFOERING':
      return (
        erInnloggetSaksbehandlerOppgave && (
          <Button size="small" href={`/oppgave/${oppgave.id}`} as="a">
            Gå til oppgave
          </Button>
        )
      )
    default:
      return null
  }
}
