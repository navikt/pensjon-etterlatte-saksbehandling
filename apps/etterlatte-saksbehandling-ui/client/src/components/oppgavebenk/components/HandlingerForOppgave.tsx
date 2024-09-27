import { Button } from '@navikt/ds-react'
import { EyeIcon } from '@navikt/aksel-icons'
import { OmgjoerVedtakModal } from '~components/oppgavebenk/oppgaveModal/OmgjoerVedtakModal'
import React from 'react'
import { OppgaveDTO, OppgaveKilde, Oppgavestatus, Oppgavetype } from '~shared/types/oppgave'
import { useInnloggetSaksbehandler } from '~components/behandling/useInnloggetSaksbehandler'
import { AktivitetspliktInfoModal } from '~components/oppgavebenk/oppgaveModal/AktivitetspliktInfoModal'
import { OpprettRevurderingModal } from '~components/person/OpprettRevurderingModal'
import { AktivitetspliktRevurderingModal } from '~components/oppgavebenk/oppgaveModal/AktivitetspliktRevurderingModal'
import { GeneriskOppgaveModal } from '~components/oppgavebenk/oppgaveModal/GeneriskOppgaveModal'
import { PersonButtonLink } from '~components/person/lenker/PersonButtonLink'
import { PersonOversiktFane } from '~components/person/Person'
import { AktivitetspliktInfo6MndVarigUnntakModal } from '~components/oppgavebenk/oppgaveModal/AktivitetspliktInfo6MndVarigUnntakModal'
import { BrevOppgaveModal } from '~components/oppgavebenk/oppgaveModal/BrevOppgaveModal'
import { TilleggsinformasjonOppgaveModal } from '~components/oppgavebenk/oppgaveModal/TilleggsinformasjonOppgaveModal'

export const HandlingerForOppgave = ({
  oppgave,
  oppdaterStatus,
}: {
  oppgave: OppgaveDTO
  oppdaterStatus: (oppgaveId: string, status: Oppgavestatus) => void
}) => {
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
        <PersonButtonLink
          size="small"
          icon={<EyeIcon />}
          fnr={fnr || '-'}
          fane={PersonOversiktFane.HENDELSER}
          queryParams={{ referanse: referanse || '-' }}
          disabled={!fnr}
        >
          Se hendelse
        </PersonButtonLink>
      )
    case Oppgavetype.MANGLER_SOEKNAD:
      return <GeneriskOppgaveModal heading="Mangler søknad" oppdaterStatus={oppdaterStatus} oppgave={oppgave} />
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
    case Oppgavetype.TILLEGGSINFORMASJON:
      return (
        erInnloggetSaksbehandlerOppgave && (
          <TilleggsinformasjonOppgaveModal oppgave={oppgave} oppdaterStatus={oppdaterStatus} />
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
      return (
        erInnloggetSaksbehandlerOppgave && (
          <AktivitetspliktInfoModal oppgave={oppgave} oppdaterStatus={oppdaterStatus} />
        )
      )
    case Oppgavetype.AKTIVITETSPLIKT_REVURDERING:
      return (
        erInnloggetSaksbehandlerOppgave && (
          <AktivitetspliktRevurderingModal oppgave={oppgave} oppdaterStatus={oppdaterStatus} />
        )
      )
    case Oppgavetype.GENERELL_OPPGAVE:
      return <GeneriskOppgaveModal heading="Generell oppgave" oppgave={oppgave} oppdaterStatus={oppdaterStatus} />
    case Oppgavetype.MANUELL_UTSENDING_BREV:
      return <BrevOppgaveModal oppgave={oppgave} oppdaterStatus={oppdaterStatus} />
    case Oppgavetype.AKTIVITETSPLIKT_INFORMASJON_VARIG_UNNTAK:
      return (
        erInnloggetSaksbehandlerOppgave && (
          <AktivitetspliktInfo6MndVarigUnntakModal oppgave={oppgave} oppdaterStatus={oppdaterStatus} />
        )
      )
    default:
      return null
  }
}
