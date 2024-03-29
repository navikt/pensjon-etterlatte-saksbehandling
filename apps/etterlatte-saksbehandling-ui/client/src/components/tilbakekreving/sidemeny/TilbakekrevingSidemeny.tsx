import { CollapsibleSidebar, SidebarContent, SidebarTools } from '~shared/styled'
import React, { useEffect, useState } from 'react'
import { Alert, Button, Heading, Tag } from '@navikt/ds-react'
import { ChevronLeftDoubleIcon, ChevronRightDoubleIcon } from '@navikt/aksel-icons'
import { useTilbakekreving } from '~components/tilbakekreving/useTilbakekreving'
import { SidebarPanel } from '~shared/components/Sidebar'
import { useAppDispatch, useAppSelector } from '~store/Store'
import { teksterTilbakekrevingStatus, TilbakekrevingStatus } from '~shared/types/Tilbakekreving'
import { useApiCall } from '~shared/hooks/useApiCall'
import { ApiErrorAlert } from '~ErrorBoundary'
import Spinner from '~shared/Spinner'
import { AttesteringEllerUnderkjenning } from '~components/behandling/attestering/attestering/attesteringEllerUnderkjenning'
import { IBeslutning } from '~components/behandling/attestering/types'
import { hentVedtakSammendrag } from '~shared/api/vedtaksvurdering'
import { updateVedtakSammendrag } from '~store/reducers/VedtakReducer'

import { mapApiResult, mapSuccess } from '~shared/api/apiUtils'
import { DokumentlisteLiten } from '~components/person/dokumenter/DokumentlisteLiten'
import { tagColors, TagList } from '~shared/Tags'
import { formaterSakstype } from '~utils/formattering'
import { Info, Tekst } from '~components/behandling/attestering/styled'
import {
  hentOppgaveForBehandlingUnderBehandlingIkkeattestertOppgave,
  hentSaksbehandlerForReferanseOppgaveUnderArbeid,
} from '~shared/api/oppgaver'
import { KopierbarVerdi } from '~shared/statusbar/kopierbarVerdi'
import { SettPaaVent } from '~components/behandling/sidemeny/SettPaaVent'
import {
  resetSaksbehandlerGjeldendeOppgave,
  setSaksbehandlerGjeldendeOppgave,
} from '~store/reducers/SaksbehandlerGjeldendeOppgaveForBehandlingReducer'

export function TilbakekrevingSidemeny() {
  const tilbakekreving = useTilbakekreving()
  const dispatch = useAppDispatch()
  const innloggetSaksbehandler = useAppSelector((state) => state.saksbehandlerReducer.innloggetSaksbehandler)
  const [collapsed, setCollapsed] = useState(false)

  const [fetchVedtakStatus, fetchVedtakSammendrag] = useApiCall(hentVedtakSammendrag)
  const [beslutning, setBeslutning] = useState<IBeslutning>()

  const [oppgaveForBehandlingenStatus, requesthentOppgaveForBehandling] = useApiCall(
    hentOppgaveForBehandlingUnderBehandlingIkkeattestertOppgave
  )
  const [, hentSaksbehandlerForOppgave] = useApiCall(hentSaksbehandlerForReferanseOppgaveUnderArbeid)

  const hentOppgaveForBehandling = () => {
    if (tilbakekreving) {
      requesthentOppgaveForBehandling({ referanse: tilbakekreving.id, sakId: tilbakekreving.sak.id })
    }
  }

  useEffect(() => {
    hentOppgaveForBehandling()
  }, [])

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

  useEffect(() => {
    if (!tilbakekreving) return
    hentSaksbehandlerForOppgave(
      { referanse: tilbakekreving.id, sakId: tilbakekreving.sak.id },
      (saksbehandler, statusCode) => {
        if (statusCode === 200) {
          dispatch(setSaksbehandlerGjeldendeOppgave(saksbehandler.ident))
        }
      },
      () => dispatch(resetSaksbehandlerGjeldendeOppgave())
    )
  }, [])

  return (
    <CollapsibleSidebar collapsed={collapsed}>
      <SidebarTools>
        <Button variant={collapsed ? 'primary' : 'tertiary'} onClick={() => setCollapsed((collapsed) => !collapsed)}>
          {collapsed ? <ChevronLeftDoubleIcon /> : <ChevronRightDoubleIcon />}
        </Button>
      </SidebarTools>
      <SidebarContent collapsed={collapsed}>
        <SidebarPanel border>
          <Heading size="small">Tilbakekreving</Heading>
          <Heading size="xsmall" spacing>
            {teksterTilbakekrevingStatus[tilbakekreving!!.status]}
          </Heading>
          <TagList>
            <li>
              <Tag variant={tagColors[tilbakekreving!!.sak.sakType]}>
                {formaterSakstype(tilbakekreving!!.sak.sakType)}
              </Tag>
            </li>
          </TagList>
          <div className="info">
            <Info>Saksbehandler</Info>
            {mapApiResult(
              oppgaveForBehandlingenStatus,
              <Spinner visible={true} label="Henter oppgave" />,
              () => (
                <ApiErrorAlert>Kunne ikke hente saksbehandler fra oppgave</ApiErrorAlert>
              ),
              (oppgave) =>
                !!oppgave?.saksbehandler ? (
                  <Tekst>{oppgave.saksbehandler?.navn || oppgave.saksbehandler?.ident}</Tekst>
                ) : (
                  <Alert size="small" variant="warning">
                    Ingen saksbehandler har tatt denne oppgaven
                  </Alert>
                )
            )}
          </div>
          <div>
            <Info>Sakid:</Info>
            <KopierbarVerdi value={tilbakekreving!!.sak.id.toString()} />
          </div>
          {mapSuccess(oppgaveForBehandlingenStatus, (oppgave) => {
            if (
              [
                TilbakekrevingStatus.OPPRETTET,
                TilbakekrevingStatus.UNDER_ARBEID,
                TilbakekrevingStatus.UNDERKJENT,
                TilbakekrevingStatus.FATTET_VEDTAK,
              ].includes(tilbakekreving!!.status)
            ) {
              return <SettPaaVent oppgave={oppgave} redigerbar={true} refreshOppgave={hentOppgaveForBehandling} />
            }
            return null
          })}
        </SidebarPanel>
      </SidebarContent>
      {kanAttestere && (
        <>
          {mapApiResult(
            fetchVedtakStatus,
            <Spinner label="Henter vedtaksdetaljer" visible />,
            () => (
              <ApiErrorAlert>Kunne ikke hente vedtak</ApiErrorAlert>
            ),
            (vedtak) => (
              <AttesteringEllerUnderkjenning
                setBeslutning={setBeslutning}
                beslutning={beslutning}
                vedtak={vedtak}
                erFattet={tilbakekreving?.status === TilbakekrevingStatus.FATTET_VEDTAK}
              />
            )
          )}
        </>
      )}

      {tilbakekreving?.sak.ident && <DokumentlisteLiten fnr={tilbakekreving.sak.ident} />}
    </CollapsibleSidebar>
  )
}
