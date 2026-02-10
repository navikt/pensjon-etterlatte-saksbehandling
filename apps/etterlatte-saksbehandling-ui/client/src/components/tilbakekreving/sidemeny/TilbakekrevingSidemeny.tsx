import React, { useEffect, useState } from 'react'
import { Alert, Box, Detail, Heading, Label, Link, VStack } from '@navikt/ds-react'
import { useTilbakekreving } from '~components/tilbakekreving/useTilbakekreving'
import { Sidebar, SidebarPanel } from '~shared/components/Sidebar'
import { useAppDispatch } from '~store/Store'
import { teksterTilbakekrevingStatus, TilbakekrevingStatus } from '~shared/types/Tilbakekreving'
import { useApiCall } from '~shared/hooks/useApiCall'
import { ApiErrorAlert } from '~ErrorBoundary'
import Spinner from '~shared/Spinner'
import { AttesteringEllerUnderkjenning } from '~components/behandling/attestering/attestering/attesteringEllerUnderkjenning'
import { IBeslutning } from '~components/behandling/attestering/types'
import { hentVedtakSammendrag } from '~shared/api/vedtaksvurdering'
import { updateVedtakSammendrag } from '~store/reducers/VedtakReducer'
import { mapApiResult } from '~shared/api/apiUtils'
import { DokumentlisteLiten } from '~components/person/dokumenter/DokumentlisteLiten'
import { KopierbarVerdi } from '~shared/statusbar/KopierbarVerdi'
import { SettPaaVent } from '~components/behandling/sidemeny/SettPaaVent'
import { useAttesterbarBehandlingOppgave } from '~shared/hooks/useAttesterbarBehandlingOppgave'
import { useInnloggetSaksbehandler } from '~components/behandling/useInnloggetSaksbehandler'
import { useSelectorOppgaveUnderBehandling } from '~store/selectors/useSelectorOppgaveUnderBehandling'
import { SakTypeTag } from '~shared/tags/SakTypeTag'
import { lenkeTilTilbakekrevingMedId } from '~components/person/sakOgBehandling/TilbakekrevingListe'

export function TilbakekrevingSidemeny() {
  const dispatch = useAppDispatch()

  const tilbakekreving = useTilbakekreving()
  const innloggetSaksbehandler = useInnloggetSaksbehandler()
  const oppgave = useSelectorOppgaveUnderBehandling()

  const [beslutning, setBeslutning] = useState<IBeslutning>()

  const [fetchVedtakStatus, fetchVedtakSammendrag] = useApiCall(hentVedtakSammendrag)

  const kanAttestere =
    !!tilbakekreving &&
    innloggetSaksbehandler.kanAttestere &&
    tilbakekreving?.status === TilbakekrevingStatus.FATTET_VEDTAK

  useEffect(() => {
    if (!tilbakekreving?.id) return
    fetchVedtakSammendrag(tilbakekreving.id, (vedtakSammendrag, statusCode) => {
      if (statusCode === 200) {
        dispatch(updateVedtakSammendrag(vedtakSammendrag))
      }
    })
  }, [tilbakekreving?.id])

  if (!tilbakekreving) {
    return (
      <Sidebar>
        <SidebarPanel></SidebarPanel>
      </Sidebar>
    )
  }

  const [oppgaveResult] = useAttesterbarBehandlingOppgave({ referanse: tilbakekreving.id })

  return (
    <Sidebar>
      <SidebarPanel $border>
        <Heading size="small">Tilbakekreving</Heading>
        <Heading size="xsmall" spacing>
          {teksterTilbakekrevingStatus[tilbakekreving!!.status]}
        </Heading>
        <div>
          <SakTypeTag sakType={tilbakekreving.sak.sakType} />
        </div>
        <Box marginBlock="space-4">
          <Label size="small">Saksbehandler</Label>
          {mapApiResult(
            oppgaveResult,
            <Spinner label="Henter oppgave" />,
            () => (
              <ApiErrorAlert>Kunne ikke hente saksbehandler fra oppgave</ApiErrorAlert>
            ),
            (oppgave) =>
              !!oppgave?.saksbehandler ? (
                <Detail>{oppgave.saksbehandler?.navn || oppgave.saksbehandler?.ident}</Detail>
              ) : (
                <Alert size="small" variant="warning">
                  Ingen saksbehandler har tatt denne oppgaven
                </Alert>
              )
          )}
        </Box>
        <div>
          <Label size="small">Sakid:</Label>
          <KopierbarVerdi value={tilbakekreving!!.sak.id.toString()} />
        </div>

        {tilbakekreving.omgjoeringAvId && (
          <VStack gap="space-2" marginBlock="space-4">
            <Label size="medium">Tilbakekrevingen er en omgjøring</Label>

            <Link href={lenkeTilTilbakekrevingMedId(tilbakekreving.omgjoeringAvId)}>Åpne forrige tilbakekreving</Link>
          </VStack>
        )}

        <SettPaaVent oppgave={oppgave} />
      </SidebarPanel>

      {kanAttestere && (
        <>
          {mapApiResult(
            fetchVedtakStatus,
            <Spinner label="Henter vedtaksdetaljer" />,
            () => (
              <ApiErrorAlert>Kunne ikke hente vedtak</ApiErrorAlert>
            ),
            (vedtak) => (
              <AttesteringEllerUnderkjenning
                setBeslutning={setBeslutning}
                beslutning={beslutning}
                vedtak={vedtak}
                erFattet={tilbakekreving?.status === TilbakekrevingStatus.FATTET_VEDTAK}
                gyldigStegForBeslutning={true} // TODO lage en måte å sjekke at vi er på riktig steg her (https://jira.adeo.no/browse/EY-4780)
              />
            )
          )}
        </>
      )}

      {tilbakekreving?.sak.ident && <DokumentlisteLiten fnr={tilbakekreving.sak.ident} />}
    </Sidebar>
  )
}
