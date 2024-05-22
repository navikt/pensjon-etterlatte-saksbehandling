import { Button } from '@navikt/ds-react'
import { EyeIcon } from '@navikt/aksel-icons'
import { OmgjoerVedtakModal } from '~components/oppgavebenk/oppgaveModal/OmgjoerVedtakModal'
import React from 'react'
import { OppgaveDTO, OppgaveKilde, Oppgavetype } from '~shared/types/oppgave'
import { useInnloggetSaksbehandler } from '~components/behandling/useInnloggetSaksbehandler'
import { AktivitetspliktInfoModal } from '~components/person/AktivitetspliktInfoModal'
import { OpprettRevurderingModal } from '~components/person/OpprettRevurderingModal'

export const HandlingerForOppgave = ({ oppgave }: { oppgave: OppgaveDTO }) => {
  const innloggetsaksbehandler = useInnloggetSaksbehandler()

  const { id, type, kilde, fnr, saksbehandler, referanse } = oppgave
  const erInnloggetSaksbehandlerOppgave = saksbehandler?.ident === innloggetsaksbehandler.ident

  if (kilde === OppgaveKilde.GENERELL_BEHANDLING) {
    switch (type) {
      case Oppgavetype.KRAVPAKKE_UTLAND:
        return (
          erInnloggetSaksbehandlerOppgave && (
            <Button size="small" as="a" href={`/generellbehandling/${referanse}`}>
              Gå til kravpakke utland
            </Button>
          )
        )
    }
  }
  if (kilde === OppgaveKilde.TILBAKEKREVING) {
    switch (type) {
      case Oppgavetype.TILBAKEKREVING:
        return (
          erInnloggetSaksbehandlerOppgave &&
          oppgave.merknad !== 'Venter på kravgrunnlag' && (
            <Button size="small" href={`/tilbakekreving/${referanse}`} as="a">
              Gå til tilbakekreving
            </Button>
          )
        )
    }
  }
  switch (type) {
    case Oppgavetype.VURDER_KONSEKVENS:
      return (
        <Button size="small" icon={<EyeIcon />} href={`/person/${fnr}?fane=HENDELSER&referanse=${referanse}`} as="a">
          Se hendelse
        </Button>
      )
    case Oppgavetype.FOERSTEGANGSBEHANDLING:
      return (
        erInnloggetSaksbehandlerOppgave && (
          <Button size="small" as="a" href={`/behandling/${referanse}`}>
            Gå til behandling
          </Button>
        )
      )
    case Oppgavetype.REVURDERING:
      return (
        <>
          {erInnloggetSaksbehandlerOppgave && referanse && (
            <Button size="small" href={`/behandling/${referanse}`} as="a">
              Gå til revurdering
            </Button>
          )}
          {erInnloggetSaksbehandlerOppgave && !referanse && (
            <OpprettRevurderingModal
              sakId={oppgave.sakId}
              sakType={oppgave.sakType}
              oppgaveId={oppgave.id}
              begrunnelse={oppgave.merknad}
            />
          )}
        </>
      )
    case Oppgavetype.KLAGE:
      return (
        erInnloggetSaksbehandlerOppgave && (
          <Button size="small" href={`/klage/${referanse}`} as="a">
            Gå til klage
          </Button>
        )
      )
    case Oppgavetype.KRAVPAKKE_UTLAND:
      return (
        erInnloggetSaksbehandlerOppgave && (
          <Button size="small" href={`/generellbehandling/${referanse}`} as="a">
            Gå til utlandssak
          </Button>
        )
      )
    case Oppgavetype.JOURNALFOERING:
      return (
        erInnloggetSaksbehandlerOppgave && (
          <Button size="small" href={`/oppgave/${oppgave.id}`} as="a">
            Gå til oppgave
          </Button>
        )
      )
    case Oppgavetype.OMGJOERING:
      return erInnloggetSaksbehandlerOppgave && <OmgjoerVedtakModal oppgave={oppgave} />
    case Oppgavetype.GJENOPPRETTING_ALDERSOVERGANG:
      return (
        erInnloggetSaksbehandlerOppgave && (
          <Button size="small" as="a" href={`/manuellbehandling/${id}`}>
            Gå til manuell opprettelse
          </Button>
        )
      )
    case Oppgavetype.AKTIVITETSPLIKT:
      return erInnloggetSaksbehandlerOppgave && <AktivitetspliktInfoModal oppgave={oppgave} />
    default:
      return null
  }
}
