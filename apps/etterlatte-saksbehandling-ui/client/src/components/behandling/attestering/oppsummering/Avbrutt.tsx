import { IBehandlingInfo } from '~components/behandling/sidemeny/IBehandlingInfo'
import { SidebarPanel } from '~shared/components/Sidebar'
import { Alert, BodyShort, Box, Detail, Heading, HStack, Label, VStack } from '@navikt/ds-react'
import { formaterBehandlingstype } from '~utils/formatering/formatering'
import React, { useEffect, useState } from 'react'
import { SakTypeTag } from '~shared/tags/SakTypeTag'
import { UtenlandstilknytningTypeTag } from '~shared/tags/UtenlandstilknytningTypeTag'
import { hentNavnforIdent } from '~shared/api/user'
import { useApiCall } from '~shared/hooks/useApiCall'
import { hentOppgaverMedReferanse } from '~shared/api/oppgaver'
import { isSuccess, mapResult } from '~shared/api/apiUtils'
import Spinner from '~shared/Spinner'
import { ApiErrorAlert } from '~ErrorBoundary'
import { OppgaveDTO, Oppgavestatus } from '~shared/types/oppgave'
import { formaterDato } from '~utils/formatering/dato'
import { KopierbarVerdi } from '~shared/statusbar/KopierbarVerdi'

const finnAvbrutteOppgaver = (oppgaver: OppgaveDTO[]) => {
  return oppgaver.filter((oppgave) => oppgave.status === Oppgavestatus.AVBRUTT)
}

export const Avbrutt = ({ behandlingsInfo }: { behandlingsInfo: IBehandlingInfo }) => {
  const [oppgaveForBehandling, hentOppgaveForBehandling] = useApiCall(hentOppgaverMedReferanse)
  const [saksbehandlerNavn, hentNavnForIdent] = useApiCall(hentNavnforIdent)

  const [manglerIdent, setManglerIdent] = useState(false)

  useEffect(() => {
    hentOppgaveForBehandling(behandlingsInfo.behandlingId)
  }, [])

  const finnNavnFraAvbruttOppgave = (riktigOppgave: OppgaveDTO) => {
    if (!riktigOppgave.saksbehandler?.navn) {
      if (riktigOppgave.saksbehandler?.ident) {
        hentNavnForIdent(riktigOppgave.saksbehandler?.ident)
      } else {
        setManglerIdent(true)
      }
    }
  }

  useEffect(() => {
    if (isSuccess(oppgaveForBehandling)) {
      const avbrutteOppgaver = finnAvbrutteOppgaver(oppgaveForBehandling.data)
      if (avbrutteOppgaver.length) {
        const riktigOppgave = avbrutteOppgaver[0]
        finnNavnFraAvbruttOppgave(riktigOppgave)
      }
    }
  }, [oppgaveForBehandling.status])

  return (
    <SidebarPanel $border>
      <VStack gap="space-2">
        <div>
          <Heading size="small">{formaterBehandlingstype(behandlingsInfo.type)}</Heading>
          <Heading size="xsmall">Avbrutt</Heading>
        </div>

        <Box paddingInline="space-2 space-0">
          <HStack gap="space-2">
            <SakTypeTag sakType={behandlingsInfo.sakType} size="small" />
            <UtenlandstilknytningTypeTag utenlandstilknytningType={behandlingsInfo.nasjonalEllerUtland} size="small" />
          </HStack>
        </Box>
        <VStack gap="space-2" justify="space-between">
          {mapResult(oppgaveForBehandling, {
            pending: <Spinner label="Henter oppgave med saksbehandler" />,
            error: () => <ApiErrorAlert>Kunne ikke hente oppgave for behandling</ApiErrorAlert>,
            success: (oppgaver) => {
              const oppgaverAvbrutt = finnAvbrutteOppgaver(oppgaver)
              if (oppgaverAvbrutt.length) {
                const riktigOppgave = oppgaverAvbrutt[0]
                const hentetNavn = isSuccess(saksbehandlerNavn) ? saksbehandlerNavn.data : undefined
                return (
                  <div>
                    <Label size="small">Saksbehandler</Label>
                    {manglerIdent ? (
                      <Alert size="small" variant="warning">
                        Mangler saksbehandlerident på oppgave
                      </Alert>
                    ) : (
                      <BodyShort size="small">
                        {hentetNavn || riktigOppgave.saksbehandler?.navn || riktigOppgave.saksbehandler?.ident}
                      </BodyShort>
                    )}
                  </div>
                )
              } else {
                return <BodyShort size="small">Ingen oppgave med status avbrutt funnet</BodyShort>
              }
            },
          })}
          <div>
            <Label size="small">Kilde</Label>
            <BodyShort size="small">{behandlingsInfo.kilde}</BodyShort>
          </div>
        </VStack>

        <HStack gap="space-4" justify="space-between">
          <div>
            <Label size="small">Virkningstidspunkt</Label>
            <Detail>{behandlingsInfo.virkningsdato ? formaterDato(behandlingsInfo.virkningsdato) : 'Ikke satt'}</Detail>
          </div>
        </HStack>

        <HStack gap="space-4" align="center">
          <Label size="small">Sakid:</Label>
          <KopierbarVerdi value={behandlingsInfo.sakId.toString()} />
        </HStack>
      </VStack>
    </SidebarPanel>
  )
}
