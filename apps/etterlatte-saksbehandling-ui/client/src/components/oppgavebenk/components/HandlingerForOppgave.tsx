import { OppgaveDTO } from '~shared/api/oppgaver'
import { Button } from '@navikt/ds-react'
import { EyeIcon } from '@navikt/aksel-icons'
import { useAppSelector } from '~store/Store'
import { GosysOppgaveModal } from '~components/oppgavebenk/oppgaveModal/GosysOppgaveModal'
import { OmgjoerVedtakModal } from '~components/oppgavebenk/oppgaveModal/OmgjoerVedtakModal'

export const HandlingerForOppgave = ({ oppgave }: { oppgave: OppgaveDTO }) => {
  const innloggetsaksbehandler = useAppSelector((state) => state.saksbehandlerReducer.innloggetSaksbehandler)

  const { id, type, kilde, fnr, saksbehandler, referanse } = oppgave
  const erInnloggetSaksbehandlerOppgave = saksbehandler?.ident === innloggetsaksbehandler.ident

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
            <Button size="small" as="a" href={`/behandling/${referanse}`}>
              Gå til behandling
            </Button>
          )}
        </>
      )
    case 'REVURDERING':
      return (
        <>
          {erInnloggetSaksbehandlerOppgave && referanse && (
            <Button size="small" href={`/behandling/${referanse}`} as="a">
              Gå til revurdering
            </Button>
          )}
          {erInnloggetSaksbehandlerOppgave && !referanse && (
            <Button size="small" href={`/person/${fnr}`} as="a">
              Gå til sak
            </Button>
          )}
        </>
      )
    case 'MANUELT_OPPHOER':
      return (
        <>
          {erInnloggetSaksbehandlerOppgave && referanse && (
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
    case 'OMGJOERING':
      return erInnloggetSaksbehandlerOppgave && <OmgjoerVedtakModal oppgave={oppgave} />
    case 'GJENOPPRETTING_ALDERSOVERGANG':
      return (
        <>
          {erInnloggetSaksbehandlerOppgave && (
            <Button size="small" as="a" href={`/manuellbehandling/${id}`}>
              Gå til manuell opprettelse
            </Button>
          )}
        </>
      )
    default:
      return null
  }
}
