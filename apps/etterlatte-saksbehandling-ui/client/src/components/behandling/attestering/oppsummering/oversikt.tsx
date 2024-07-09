import { IBehandlingStatus, UtlandstilknytningType } from '~shared/types/IDetaljertBehandling'
import { formaterBehandlingstype } from '~utils/formatering/formatering'
import { formaterDatoMedKlokkeslett, formaterDato } from '~utils/formatering/dato'
import { IBehandlingInfo } from '~components/behandling/sidemeny/IBehandlingInfo'
import { Alert, Box, Detail, Heading, HStack, Label, VStack } from '@navikt/ds-react'
import { SidebarPanel } from '~shared/components/Sidebar'
import React, { useEffect } from 'react'
import { KopierbarVerdi } from '~shared/statusbar/KopierbarVerdi'
import { EessiPensjonLenke } from '~components/behandling/soeknadsoversikt/bosattUtland/EessiPensjonLenke'
import { SettPaaVent } from '~components/behandling/sidemeny/SettPaaVent'
import { useSelectorOppgaveUnderBehandling } from '~store/selectors/useSelectorOppgaveUnderBehandling'
import { SakTypeTag } from '~shared/tags/SakTypeTag'
import { UtenlandstilknytningTypeTag } from '~shared/tags/UtenlandstilknytningTypeTag'
import { hentNavnforIdent } from '~shared/api/user'
import { useApiCall } from '~shared/hooks/useApiCall'
import { mapApiResult } from '~shared/api/apiUtils'
import Spinner from '~shared/Spinner'
import { ApiErrorAlert } from '~ErrorBoundary'

export const Oversikt = ({
  behandlingsInfo,
  behandlendeSaksbehandler,
}: {
  behandlingsInfo: IBehandlingInfo
  behandlendeSaksbehandler?: string
}) => {
  const kommentarFraAttestant = behandlingsInfo.attestertLogg?.slice(-1)[0]?.kommentar
  const oppgave = useSelectorOppgaveUnderBehandling()

  const [res, hentNavnForIdent] = useApiCall(hentNavnforIdent)
  useEffect(() => {
    if (behandlingsInfo.status == IBehandlingStatus.FATTET_VEDTAK && behandlendeSaksbehandler) {
      console.log('henter')
      hentNavnForIdent(behandlendeSaksbehandler)
    }
  }, [behandlendeSaksbehandler, behandlingsInfo.status])

  console.log(res.status)

  const hentStatus = () => {
    switch (behandlingsInfo.status) {
      case IBehandlingStatus.FATTET_VEDTAK:
        return 'Attestering'
      case IBehandlingStatus.TIL_SAMORDNING:
        return 'Til samordning'
      case IBehandlingStatus.SAMORDNET:
        return 'Samordnet'
      case IBehandlingStatus.IVERKSATT:
        return 'Iverksatt'
      case IBehandlingStatus.AVSLAG:
        return 'Avslag'
      case IBehandlingStatus.AVBRUTT:
        return 'Avbrutt'
      case IBehandlingStatus.OPPRETTET:
      case IBehandlingStatus.VILKAARSVURDERT:
      case IBehandlingStatus.BEREGNET:
      case IBehandlingStatus.ATTESTERT:
      case IBehandlingStatus.RETURNERT:
        return 'Under behandling'
    }
  }

  return (
    <SidebarPanel $border>
      <VStack gap="2">
        <div>
          <Heading size="small">
            {formaterBehandlingstype(behandlingsInfo.type)}

            {(behandlingsInfo.nasjonalEllerUtland === UtlandstilknytningType.UTLANDSTILSNITT ||
              behandlingsInfo.nasjonalEllerUtland === UtlandstilknytningType.BOSATT_UTLAND) && (
              <EessiPensjonLenke
                sakId={behandlingsInfo.sakId}
                behandlingId={behandlingsInfo.behandlingId}
                sakType={behandlingsInfo.sakType}
              />
            )}
          </Heading>

          <Heading size="xsmall">{hentStatus()}</Heading>

          {behandlingsInfo.datoFattet && <Detail>{formaterDatoMedKlokkeslett(behandlingsInfo.datoFattet)}</Detail>}
        </div>

        <Box paddingInline="2 0">
          <HStack gap="2">
            <SakTypeTag sakType={behandlingsInfo.sakType} size="small" />
            <UtenlandstilknytningTypeTag utenlandstilknytningType={behandlingsInfo.nasjonalEllerUtland} size="small" />
          </HStack>
        </Box>

        <VStack gap="4" justify="space-between">
          {behandlingsInfo.status == IBehandlingStatus.FATTET_VEDTAK && behandlendeSaksbehandler ? (
            <>
              {mapApiResult(
                res,
                <Spinner visible={true} label="Henter saksbehandler" />,
                () => (
                  <ApiErrorAlert>Kunne ikke hent saksbehandlende saksbehandler</ApiErrorAlert>
                ),
                (saksbehandlernavn) => {
                  return (
                    <>
                      <HStack gap="4" justify="space-between">
                        <div>
                          <Label size="small">Attestant</Label>
                          {!!oppgave?.saksbehandler ? (
                            <Detail>{oppgave.saksbehandler?.navn || oppgave.saksbehandler?.ident}</Detail>
                          ) : (
                            <Alert size="small" variant="warning">
                              Oppgaven er ikke tildelt
                            </Alert>
                          )}
                        </div>
                        <div>
                          <Label size="small">Saksbehandler</Label>
                          <Detail>{saksbehandlernavn || behandlendeSaksbehandler}</Detail>
                        </div>
                      </HStack>
                    </>
                  )
                }
              )}
            </>
          ) : (
            <div>
              <Label size="small">Saksbehandler</Label>
              {!!oppgave?.saksbehandler ? (
                <Detail>{oppgave.saksbehandler?.navn || oppgave.saksbehandler?.ident}</Detail>
              ) : (
                <Alert size="small" variant="warning">
                  Oppgaven er ikke tildelt
                </Alert>
              )}
            </div>
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

        {kommentarFraAttestant && (
          <HStack gap="4" justify="space-between">
            <Label size="small">Kommentar fra attestant</Label>
            <Detail>{kommentarFraAttestant}</Detail>
          </HStack>
        )}

        <HStack gap="4" align="center">
          <Label size="small">Sakid:</Label>
          <KopierbarVerdi value={behandlingsInfo.sakId.toString()} />
        </HStack>

        <SettPaaVent oppgave={oppgave} />
      </VStack>
    </SidebarPanel>
  )
}
