import { useKlage, useKlageRedigerbar } from '~components/klage/useKlage'
import { Detail, Heading, HStack, Label, VStack } from '@navikt/ds-react'
import { Sidebar, SidebarPanel } from '~shared/components/Sidebar'
import { KlageStatus, teksterKabalstatus, teksterKlagestatus } from '~shared/types/Klage'
import { formaterDato } from '~utils/formatering/dato'
import { DokumentlisteLiten } from '~components/person/dokumenter/DokumentlisteLiten'
import AvsluttKlage from '~components/klage/AvsluttKlage'
import React, { useEffect, useState } from 'react'
import { updateVedtakSammendrag } from '~store/reducers/VedtakReducer'
import { useApiCall } from '~shared/hooks/useApiCall'
import { hentVedtakSammendrag } from '~shared/api/vedtaksvurdering'
import { useAppDispatch } from '~store/Store'
import { mapApiResult } from '~shared/api/apiUtils'
import Spinner from '~shared/Spinner'
import { ApiErrorAlert } from '~ErrorBoundary'
import { AttesteringEllerUnderkjenning } from '~components/behandling/attestering/attestering/attesteringEllerUnderkjenning'
import { IBeslutning } from '~components/behandling/attestering/types'
import { isFailureHandler } from '~shared/api/IsFailureHandler'
import { useOppgaveUnderBehandling } from '~shared/hooks/useOppgaveUnderBehandling'
import { useInnloggetSaksbehandler } from '~components/behandling/useInnloggetSaksbehandler'
import { SakTypeTag } from '~shared/tags/SakTypeTag'
import { RedigerMottattDato } from '~components/klage/sidemeny/RedigerMottattDato'

export function KlageSidemeny() {
  const klage = useKlage()
  const dispatch = useAppDispatch()
  const [fetchVedtakStatus, fetchVedtakSammendrag] = useApiCall(hentVedtakSammendrag)
  const innloggetSaksbehandler = useInnloggetSaksbehandler()
  const [beslutning, setBeslutning] = useState<IBeslutning>()
  const kanAttestere = !!klage && innloggetSaksbehandler.kanAttestere && klage.status === KlageStatus.FATTET_VEDTAK
  const erRedigerbar = useKlageRedigerbar()

  useEffect(() => {
    if (!klage) return
    fetchVedtakSammendrag(klage.id, (vedtakSammendrag, statusCode) => {
      if (statusCode === 200) {
        dispatch(updateVedtakSammendrag(vedtakSammendrag))
      }
    })
  }, [klage?.id, klage?.status])

  if (!klage) {
    return (
      <Sidebar>
        <SidebarPanel></SidebarPanel>
      </Sidebar>
    )
  }
  const [oppgaveResult] = useOppgaveUnderBehandling({ referanse: klage.id })

  return (
    <Sidebar>
      <SidebarPanel $border>
        <Heading size="small">Klage</Heading>
        <Heading size="xsmall" spacing>
          {teksterKlagestatus[klage.status]}
        </Heading>

        {klage.kabalStatus && (
          <>
            <Heading size="small">Status Kabal</Heading>
            <Heading size="xsmall">{teksterKabalstatus[klage.kabalStatus]}</Heading>
          </>
        )}

        <VStack gap="2">
          <div>
            <SakTypeTag sakType={klage.sak.sakType} />
          </div>

          <HStack gap="4">
            <div>
              <Label size="small">Klager</Label>
              <Detail>{klage.innkommendeDokument?.innsender ?? 'Ukjent'}</Detail>
            </div>
            <div>
              <Label size="small">Klagedato</Label>
              <Detail>
                {klage.innkommendeDokument?.mottattDato
                  ? formaterDato(klage.innkommendeDokument.mottattDato)
                  : 'Ukjent'}
              </Detail>
            </div>
          </HStack>

          {erRedigerbar && <RedigerMottattDato />}
        </VStack>
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
                erFattet={klage?.status === KlageStatus.FATTET_VEDTAK}
                gyldigStegForBeslutning={true}
              />
            )
          )}
        </>
      )}
      {isFailureHandler({
        apiResult: oppgaveResult,
        errorMessage: 'Kunne ikke hente saksbehandler gjeldende oppgave. Husk å tildele oppgaven.',
      })}
      <DokumentlisteLiten fnr={klage.sak.ident} />
      <AvsluttKlage />
    </Sidebar>
  )
}
