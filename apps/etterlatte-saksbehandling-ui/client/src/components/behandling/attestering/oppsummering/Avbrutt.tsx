import { IBehandlingInfo } from '~components/behandling/sidemeny/IBehandlingInfo'
import { SidebarPanel } from '~shared/components/Sidebar'
import { Box, Detail, Heading, HStack, Label, VStack } from '@navikt/ds-react'
import { formaterBehandlingstype } from '~utils/formatering/formatering'
import React, { useEffect } from 'react'
import { SakTypeTag } from '~shared/tags/SakTypeTag'
import { UtenlandstilknytningTypeTag } from '~shared/tags/UtenlandstilknytningTypeTag'
import { hentNavnforIdent } from '~shared/api/user'
import { useApiCall } from '~shared/hooks/useApiCall'
import { hentOppgaverMedReferanse } from '~shared/api/oppgaver'
import { isSuccess, mapApiResult } from '~shared/api/apiUtils'
import Spinner from '~shared/Spinner'
import { ApiErrorAlert } from '~ErrorBoundary'
import { Oppgavestatus } from '~shared/types/oppgave'
import { formaterDato } from '~utils/formatering/dato'
import { KopierbarVerdi } from '~shared/statusbar/KopierbarVerdi'

export const Avbrutt = ({ behandlingsInfo }: { behandlingsInfo: IBehandlingInfo }) => {
  const [oppgaveForBehandling, hentOppgaveForBehandling] = useApiCall(hentOppgaverMedReferanse)
  const [saksbehandlerNavn, hentNavnForIdent] = useApiCall(hentNavnforIdent)

  useEffect(() => {
    hentOppgaveForBehandling(behandlingsInfo.behandlingId)
  }, [])

  useEffect(() => {
    if (isSuccess(oppgaveForBehandling)) {
      const avbrutteOppgaver = oppgaveForBehandling.data.filter((oppgave) => oppgave.status === Oppgavestatus.AVBRUTT)
      if (avbrutteOppgaver.length) {
        const riktigOppgave = avbrutteOppgaver[0]
        if (!riktigOppgave.saksbehandler?.navn) {
          if (riktigOppgave.saksbehandler?.ident) {
            hentNavnForIdent(riktigOppgave.saksbehandler?.ident)
          }
        }
      }
    }
  }, [oppgaveForBehandling.status])

  return (
    <SidebarPanel $border>
      <VStack gap="2">
        <div>
          <Heading size="small">{formaterBehandlingstype(behandlingsInfo.type)}</Heading>
          <Heading size="xsmall">Avbrutt</Heading>
        </div>

        <Box paddingInline="2 0">
          <HStack gap="2">
            <SakTypeTag sakType={behandlingsInfo.sakType} size="small" />
            <UtenlandstilknytningTypeTag utenlandstilknytningType={behandlingsInfo.nasjonalEllerUtland} size="small" />
          </HStack>
        </Box>
        <VStack gap="2" justify="space-between">
          {mapApiResult(
            oppgaveForBehandling,
            <Spinner label="Henter oppgave med saksbehandler" />,
            () => (
              <ApiErrorAlert>Kunne ikke hente oppgave for behandling</ApiErrorAlert>
            ),
            (oppgaver) => {
              const oppgaveravbrutt = oppgaver.filter((oppgave) => oppgave.status === Oppgavestatus.AVBRUTT)
              if (oppgaveravbrutt.length) {
                const foersteavbrutteoppgave = oppgaveravbrutt[0]
                const hentetNavn = isSuccess(saksbehandlerNavn) ? saksbehandlerNavn.data : undefined
                return (
                  <div>
                    <Label size="small">Saksbehandler</Label>
                    <Detail>
                      {hentetNavn ||
                        foersteavbrutteoppgave.saksbehandler?.navn ||
                        foersteavbrutteoppgave.saksbehandler?.ident}
                    </Detail>
                  </div>
                )
              } else {
                return <Detail>Ingen oppgave med status avbrutt funnet</Detail>
              }
            }
          )}
          <div>
            <Label size="small">Kilde</Label>
            <Detail>{behandlingsInfo.kilde}</Detail>
          </div>
        </VStack>

        <HStack gap="4" justify="space-between">
          <div>
            <Label size="small">Virkningstidspunkt</Label>
            <Detail>{behandlingsInfo.virkningsdato ? formaterDato(behandlingsInfo.virkningsdato) : 'Ikke satt'}</Detail>
          </div>
          <div>
            <Label size="small">Vedtaksdato</Label>
            <Detail>{behandlingsInfo.datoAttestert ? formaterDato(behandlingsInfo.datoAttestert) : 'Ikke satt'}</Detail>
          </div>
        </HStack>

        <HStack gap="4" align="center">
          <Label size="small">Sakid:</Label>
          <KopierbarVerdi value={behandlingsInfo.sakId.toString()} />
        </HStack>
      </VStack>
    </SidebarPanel>
  )
}
