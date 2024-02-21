import styled from 'styled-components'
import { IBehandlingStatus, UtlandstilknytningType } from '~shared/types/IDetaljertBehandling'
import {
  formaterBehandlingstype,
  formaterDatoMedKlokkeslett,
  formaterEnumTilLesbarString,
  formaterSakstype,
  formaterStringDato,
} from '~utils/formattering'
import { IBehandlingInfo } from '~components/behandling/sidemeny/IBehandlingInfo'
import { Alert, BodyShort, Button, Heading, Tag, TextField } from '@navikt/ds-react'
import { tagColors, TagList } from '~shared/Tags'
import { SidebarPanel } from '~shared/components/Sidebar'
import React, { useEffect, useState } from 'react'
import { useApiCall } from '~shared/hooks/useApiCall'
import {
  erOppgaveRedigerbar,
  hentOppgaveForBehandlingUnderBehandlingIkkeattestertOppgave,
  OppgaveDTO,
  settOppgavePaaVentApi,
  SettPaaVentRequest,
} from '~shared/api/oppgaver'
import Spinner from '~shared/Spinner'
import { ApiErrorAlert } from '~ErrorBoundary'
import { KopierbarVerdi } from '~shared/statusbar/kopierbarVerdi'
import { isInitial, isPending, isSuccess, mapApiResult } from '~shared/api/apiUtils'
import { FlexRow } from '~shared/styled'
import { EessiPensjonLenke } from '~components/behandling/soeknadsoversikt/bosattUtland/EessiPensjonLenke'
import { FristHandlinger } from '~components/oppgavebenk/frist/FristHandlinger'
import { oppdaterFrist } from '~components/oppgavebenk/utils/oppgaveutils'

export const Oversikt = ({ behandlingsInfo }: { behandlingsInfo: IBehandlingInfo }) => {
  const kommentarFraAttestant = behandlingsInfo.attestertLogg?.slice(-1)[0]?.kommentar
  const [oppgavenTilBehandlingen, setOppgave] = useState<OppgaveDTO | null>(null)

  const [oppgaveForBehandlingenStatus, requesthentOppgaveForBehandlingEkte] = useApiCall(
    hentOppgaveForBehandlingUnderBehandlingIkkeattestertOppgave
  )
  const [lagreSettPaaVentStatus, requestSettPaaVent] = useApiCall(settOppgavePaaVentApi)
  const [settPaaVent, setVisPaaVent] = useState(false)
  const [merknad, setMerknad] = useState<string>('')
  const [minOppgavelisteOppgaver, setMinOppgavelisteOppgaver] = useState<Array<OppgaveDTO>>([])

  useEffect(() => {
    requesthentOppgaveForBehandlingEkte(
      { referanse: behandlingsInfo.behandlingId, sakId: behandlingsInfo.sakId },
      (oppgave) => {
        setOppgave(oppgave)
        setMerknad(oppgave.merknad || '')
      }
    )
  }, [lagreSettPaaVentStatus])

  const lagreVent = (data: SettPaaVentRequest) => {
    if (!oppgavenTilBehandlingen) throw new Error('Mangler oppgave')
    requestSettPaaVent({
      oppgaveId: oppgavenTilBehandlingen.id,
      settPaaVentRequest: data,
    })
    setVisPaaVent(false)
  }
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

  if (isInitial(oppgaveForBehandlingenStatus) || isPending(oppgaveForBehandlingenStatus)) {
    return <Spinner visible={true} label="Henter oppgave" />
  }
  if (isPending(lagreSettPaaVentStatus)) {
    return <Spinner visible={true} label="Lagrer oppgave" />
  }

  return (
    <SidebarPanel border>
      <Heading size="small">
        {formaterBehandlingstype(behandlingsInfo.type)}
        {behandlingsInfo.nasjonalEllerUtland !== UtlandstilknytningType.NASJONAL && (
          <EessiPensjonLenke
            sakId={behandlingsInfo.sakId}
            behandlingId={behandlingsInfo.behandlingId}
            sakType={behandlingsInfo.sakType}
          />
        )}
      </Heading>

      <Heading size="xsmall" spacing>
        {hentStatus()}
      </Heading>

      {behandlingsInfo.datoFattet && <Tekst>{formaterDatoMedKlokkeslett(behandlingsInfo.datoFattet)}</Tekst>}

      <TagList>
        <li>
          <Tag variant={tagColors[behandlingsInfo.sakType]} size="small">
            {formaterSakstype(behandlingsInfo.sakType)}
          </Tag>
        </li>
        <li>
          {behandlingsInfo.nasjonalEllerUtland ? (
            <Tag variant={tagColors[behandlingsInfo.nasjonalEllerUtland]} size="small">
              {formaterEnumTilLesbarString(behandlingsInfo.nasjonalEllerUtland)}
            </Tag>
          ) : (
            <BodyShort>Du må velge en tilknytning</BodyShort>
          )}
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
          () =>
            oppgavenTilBehandlingen ? (
              <Tekst>{oppgavenTilBehandlingen.saksbehandlerNavn || oppgavenTilBehandlingen.saksbehandlerIdent}</Tekst>
            ) : (
              <Alert size="small" variant="warning">
                Ingen saksbehandler har tatt denne oppgaven
              </Alert>
            )
        )}
      </div>
      <div className="flex">
        <div>
          <Info>Virkningstidspunkt</Info>
          <Tekst>
            {behandlingsInfo.virkningsdato ? formaterStringDato(behandlingsInfo.virkningsdato) : 'Ikke satt'}
          </Tekst>
        </div>
        <div>
          <Info>Vedtaksdato</Info>
          <Tekst>
            {behandlingsInfo.datoAttestert ? formaterStringDato(behandlingsInfo.datoAttestert) : 'Ikke satt'}
          </Tekst>
        </div>
      </div>
      {kommentarFraAttestant && (
        <div className="info">
          <Info>Kommentar fra attestant</Info>
          <Tekst>{kommentarFraAttestant}</Tekst>
        </div>
      )}
      <FlexRow align="center" $spacing={true}>
        <Info>Sakid:</Info>
        <KopierbarVerdi value={behandlingsInfo.sakId.toString()} />
      </FlexRow>

      {settPaaVent && (
        <>
          <TextField
            label="Merknad for venting"
            size="medium"
            type="text"
            value={merknad}
            onChange={(e) => setMerknad(e.target.value)}
          />
        </>
      )}

      {settPaaVent &&
        isSuccess(oppgaveForBehandlingenStatus) &&
        oppgavenTilBehandlingen &&
        oppgavenTilBehandlingen.status !== 'PAA_VENT' && (
          <>
            <FlexRow align="center">
              <b>Frist</b>
            </FlexRow>
            <FlexRow align="center">
              <FristHandlinger
                orginalFrist={oppgavenTilBehandlingen.frist}
                oppgaveId={oppgavenTilBehandlingen.id}
                oppdaterFrist={(id: string, nyfrist: string, versjon: number | null) =>
                  oppdaterFrist(setMinOppgavelisteOppgaver, minOppgavelisteOppgaver, id, nyfrist, versjon)
                }
                erRedigerbar={erOppgaveRedigerbar(oppgavenTilBehandlingen.status)}
                oppgaveVersjon={oppgavenTilBehandlingen.versjon}
                type={oppgavenTilBehandlingen.type}
              />
            </FlexRow>
          </>
        )}

      {settPaaVent &&
        isSuccess(oppgaveForBehandlingenStatus) &&
        oppgavenTilBehandlingen &&
        oppgavenTilBehandlingen.status !== 'PAA_VENT' && (
          <>
            <FlexRow>
              <Button
                variant="primary"
                onClick={() =>
                  lagreVent({
                    merknad: merknad,
                    versjon: null,
                    status: oppgavenTilBehandlingen.status,
                  } as SettPaaVentRequest)
                }
              >
                Bekreft Vent
              </Button>
            </FlexRow>
            <FlexRow>
              <Button variant="tertiary" onClick={() => setVisPaaVent(false)}>
                Avbryt
              </Button>
            </FlexRow>
          </>
        )}
      {settPaaVent &&
        isSuccess(oppgaveForBehandlingenStatus) &&
        oppgavenTilBehandlingen &&
        oppgavenTilBehandlingen.status == 'PAA_VENT' && (
          <>
            <FlexRow>
              <Button
                variant="primary"
                onClick={() =>
                  lagreVent({
                    merknad: merknad,
                    versjon: null,
                    status: oppgavenTilBehandlingen.status,
                  } as SettPaaVentRequest)
                }
              >
                Bekreft fjerning av Vent
              </Button>
            </FlexRow>
            <FlexRow>
              <Button variant="tertiary" onClick={() => setVisPaaVent(false)}>
                Avbryt
              </Button>
            </FlexRow>
          </>
        )}
      {!settPaaVent && oppgavenTilBehandlingen?.status !== 'PAA_VENT' && (
        <Button variant="primary" onClick={() => setVisPaaVent(true)}>
          Sett på vent
        </Button>
      )}
      {!settPaaVent && oppgavenTilBehandlingen?.status === 'PAA_VENT' && (
        <>
          <Alert variant="warning">
            <b>Oppgaven er satt på vent.</b> <br></br>
            <b>Merknad: </b>
            {oppgavenTilBehandlingen.merknad}
            <br></br>
            <b>Ny frist: </b>
            {formaterStringDato(oppgavenTilBehandlingen.frist)}
          </Alert>
          <Button variant="primary" onClick={() => setVisPaaVent(true)}>
            Ta av vent
          </Button>
        </>
      )}
    </SidebarPanel>
  )
}

const Info = styled.div`
  font-size: 14px;
  font-weight: 600;
`

const Tekst = styled.div`
  font-size: 14px;
  font-weight: 600;
  color: #595959;
`
