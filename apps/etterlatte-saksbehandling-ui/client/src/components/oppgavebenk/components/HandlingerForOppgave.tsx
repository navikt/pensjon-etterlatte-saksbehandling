import { Button, HStack } from '@navikt/ds-react'
import { EyeIcon } from '@navikt/aksel-icons'
import { OmgjoerVedtakModal } from '~components/oppgavebenk/oppgaveModal/OmgjoerVedtakModal'
import React from 'react'
import { erOppgaveRedigerbar, OppgaveDTO, OppgaveKilde, Oppgavestatus, Oppgavetype } from '~shared/types/oppgave'
import { useInnloggetSaksbehandler } from '~components/behandling/useInnloggetSaksbehandler'
import { OpprettRevurderingModal } from '~components/person/OpprettRevurderingModal'
import { AktivitetspliktRevurderingModal } from '~components/oppgavebenk/oppgaveModal/aktivitetsplikt/AktivitetspliktRevurderingModal'
import { GeneriskOppgaveModal } from '~components/oppgavebenk/oppgaveModal/GeneriskOppgaveModal'
import { PersonButtonLink } from '~components/person/lenker/PersonButtonLink'
import { PersonOversiktFane } from '~components/person/Person'
import { AktivitetspliktInfo6MndVarigUnntakModal } from '~components/oppgavebenk/oppgaveModal/aktivitetsplikt/AktivitetspliktInfo6MndVarigUnntakModal'
import { BrevOppgaveModal } from '~components/oppgavebenk/oppgaveModal/BrevOppgaveModal'
import { TilleggsinformasjonOppgaveModal } from '~components/oppgavebenk/oppgaveModal/TilleggsinformasjonOppgaveModal'
import { AktivitetspliktSteg } from '~components/aktivitetsplikt/stegmeny/AktivitetspliktStegmeny'
import { useNavigate } from 'react-router-dom'
import { useApiCall } from '~shared/hooks/useApiCall'
import { opprettManuellInntektsjustering as opprettManuellInntektsjusteringApi } from '~shared/api/revurdering'
import Spinner from '~shared/Spinner'
import { mapResult } from '~shared/api/apiUtils'
import { ApiErrorAlert } from '~ErrorBoundary'
import { InntektsopplysningModal } from '~components/oppgavebenk/oppgaveModal/InntektsopplysningModal'
import { OppfoelgingAvOppgaveModal } from '~components/oppgavebenk/oppgaveModal/oppfoelgingsOppgave/OppfoelgingsOppgaveModal'
import { EtteroppgjoerOpprettRevurderingModal } from '~components/oppgavebenk/oppgaveModal/EtteroppgjoerOpprettRevurderingModal'
import { OpprettEtteroppgjoerForbehandlingModal } from '~components/oppgavebenk/oppgaveModal/OpprettEtteroppgjoerForbehandlingModal'
import { KlageBehandleSvarFraKa } from '~components/oppgavebenk/oppgaveModal/KlageBehandleSvarFraKa'
import { omgjoerEtteroppgjoerRevurdering as omgjoerEtteroppgjoerRevurderingApi } from '~shared/api/etteroppgjoer'
import { IDetaljertBehandling } from '~shared/types/IDetaljertBehandling'

export const HandlingerForOppgave = ({
  oppgave,
  oppdaterStatus,
  oppdaterMerknad,
}: {
  oppgave: OppgaveDTO
  oppdaterStatus: (oppgaveId: string, status: Oppgavestatus) => void
  oppdaterMerknad: (oppgaveId: string, merknad: string) => void
}) => {
  const navigate = useNavigate()
  const innloggetsaksbehandler = useInnloggetSaksbehandler()

  const [opprettManuellRevurderingStatus, opprettManuellInntektsjustering] = useApiCall(
    opprettManuellInntektsjusteringApi
  )

  const [, omgjoerEtteroppgjoerRevurdering] = useApiCall(omgjoerEtteroppgjoerRevurderingApi)

  const { id, type, kilde, fnr, saksbehandler, referanse } = oppgave
  const erInnloggetSaksbehandlerOppgave = saksbehandler?.ident === innloggetsaksbehandler.ident

  const erISakOversikt = () => {
    return window.location.href.includes('/person')
  }

  const opprettInntektsjusteringRevurdering = () => {
    opprettManuellInntektsjustering(
      {
        sakId: oppgave.sakId,
        oppgaveId: oppgave.id,
      },
      (revurdering: string) => navigate(`/behandling/${revurdering}/`)
    )
  }

  const omgjoerEoRevurdering = () => {
    omgjoerEtteroppgjoerRevurdering(
      {
        behandlingId: oppgave.referanse!!,
      },
      (revurdering: IDetaljertBehandling) => {
        navigate(`/behandling/${revurdering.id}/`)
      }
    )
  }

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
          icon={<EyeIcon aria-hidden />}
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
            <HStack gap="4">
              <Button size="small" href={`/behandling/${referanse}`} as="a">
                Gå til revurdering
              </Button>

              {oppgave.status === Oppgavestatus.AVBRUTT && (
                <Button size="small" onClick={omgjoerEoRevurdering}>
                  Omgjør
                </Button>
              )}
            </HStack>
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
    case Oppgavetype.ETTEROPPGJOER:
      return (
        <>
          {erInnloggetSaksbehandlerOppgave && referanse && (
            <Button size="small" as="a" href={`/etteroppgjoer/${referanse}`}>
              Gå til etteroppgjør
            </Button>
          )}

          {erInnloggetSaksbehandlerOppgave && !referanse && (
            <OpprettEtteroppgjoerForbehandlingModal oppgave={oppgave} oppdaterStatus={oppdaterStatus} />
          )}
        </>
      )

    case Oppgavetype.ETTEROPPGJOER_OPPRETT_REVURDERING:
      return (
        erInnloggetSaksbehandlerOppgave && (
          <EtteroppgjoerOpprettRevurderingModal oppgave={oppgave} oppdaterStatus={oppdaterStatus} />
        )
      )

    case Oppgavetype.KLAGE:
      return (
        erInnloggetSaksbehandlerOppgave && (
          <Button size="small" href={`/klage/${referanse}`} as="a">
            Gå til klage
          </Button>
        )
      )

    case Oppgavetype.KLAGE_SVAR_KABAL:
      return (
        erInnloggetSaksbehandlerOppgave && <KlageBehandleSvarFraKa oppgave={oppgave} oppdaterStatus={oppdaterStatus} />
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
          <TilleggsinformasjonOppgaveModal
            oppgave={oppgave}
            oppdaterStatus={oppdaterStatus}
            oppdaterMerknad={oppdaterMerknad}
          />
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
    case Oppgavetype.AKTIVITETSPLIKT_12MND:
    case Oppgavetype.AKTIVITETSPLIKT:
      return (
        <Button size="small" as="a" href={`/aktivitet-vurdering/${id}/${AktivitetspliktSteg.VURDERING}`}>
          Gå til vurdering
        </Button>
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
    case Oppgavetype.INNTEKTSOPPLYSNING:
      return <InntektsopplysningModal oppgave={oppgave} oppdaterStatus={oppdaterStatus} />
    case Oppgavetype.AARLIG_INNTEKTSJUSTERING:
      return mapResult(opprettManuellRevurderingStatus, {
        initial: (
          <Button size="small" onClick={opprettInntektsjusteringRevurdering}>
            Opprett revurdering
          </Button>
        ),
        pending: <Spinner label="Oppretter ..." margin="0" />,
        error: (error) => <ApiErrorAlert>{error?.detail ?? 'Ukjent feil'}</ApiErrorAlert>,
      })
    case Oppgavetype.OPPFOELGING:
      return <OppfoelgingAvOppgaveModal oppgave={oppgave} oppdaterStatus={oppdaterStatus} />
    case Oppgavetype.MELDT_INN_ENDRING:
      if (!erISakOversikt()) {
        return (
          <PersonButtonLink
            size="small"
            icon={<EyeIcon aria-hidden />}
            fnr={fnr || '-'}
            fane={PersonOversiktFane.SAKER}
            queryParams={{ referanse: referanse || '-' }}
            disabled={!fnr}
          >
            Se sak
          </PersonButtonLink>
        )
      } else {
        return (
          erOppgaveRedigerbar(oppgave.status) && (
            <Button size="small" as="a" href={`/meldt-inn-endring/${oppgave.id}`}>
              Gå til oppgave
            </Button>
          )
        )
      }

    default:
      return null
  }
}
