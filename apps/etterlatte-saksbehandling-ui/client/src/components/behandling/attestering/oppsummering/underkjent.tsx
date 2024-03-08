import { Info, Overskrift, Tekst, UnderOverskrift, Wrapper } from '../styled'
import { useAppSelector } from '~store/Store'
import { formaterBehandlingstype, formaterDatoMedKlokkeslett } from '~utils/formattering'
import { IBehandlingStatus } from '~shared/types/IDetaljertBehandling'
import { IBehandlingInfo } from '~components/behandling/sidemeny/IBehandlingInfo'
import { KopierbarVerdi } from '~shared/statusbar/kopierbarVerdi'
import { mapSuccess } from '~shared/api/apiUtils'
import { SettPaaVent } from '~components/behandling/sidemeny/SettPaaVent'
import React, { useEffect } from 'react'
import { useApiCall } from '~shared/hooks/useApiCall'
import { hentOppgaveForBehandlingUnderBehandlingIkkeattestertOppgave } from '~shared/api/oppgaver'

export const Underkjent = ({ behandlingsInfo }: { behandlingsInfo: IBehandlingInfo }) => {
  const innloggetId = useAppSelector((state) => state.saksbehandlerReducer.innloggetSaksbehandler.ident)
  const underkjentSiste = behandlingsInfo.underkjentLogg?.slice(-1)[0]
  const fattetSiste = behandlingsInfo.fattetLogg?.slice(-1)[0]

  const erReturnert = behandlingsInfo.status === IBehandlingStatus.RETURNERT
  const saksbehandler = fattetSiste?.ident
  const attestant = erReturnert ? underkjentSiste?.ident : innloggetId

  const [oppgaveForBehandlingenStatus, requesthentOppgaveForBehandling] = useApiCall(
    hentOppgaveForBehandlingUnderBehandlingIkkeattestertOppgave
  )
  const hentOppgaveForBehandling = () =>
    requesthentOppgaveForBehandling({ referanse: behandlingsInfo.behandlingId, sakId: behandlingsInfo.sakId })

  useEffect(() => {
    hentOppgaveForBehandling()
  }, [])
  return (
    <Wrapper innvilget={false}>
      <Overskrift>{formaterBehandlingstype(behandlingsInfo.type)}</Overskrift>
      <UnderOverskrift innvilget={false}>Underkjent</UnderOverskrift>

      {underkjentSiste && <Tekst>{formaterDatoMedKlokkeslett(underkjentSiste.opprettet)}</Tekst>}

      <div className="flex">
        <div>
          <Info>Attestant</Info>
          <Tekst>{attestant}</Tekst>
        </div>
        <div>
          <Info>Saksbehandler</Info>
          <Tekst>{saksbehandler}</Tekst>
        </div>
      </div>

      {erReturnert && underkjentSiste && (
        <>
          <div className="info">
            <Info>Årsak til retur</Info>
            <Tekst>{underkjentSiste.valgtBegrunnelse}</Tekst>
          </div>
          <Tekst>{underkjentSiste.kommentar}</Tekst>
        </>
      )}
      <KopierbarVerdi value={behandlingsInfo.sakId.toString()} />
      {mapSuccess(oppgaveForBehandlingenStatus, (oppgave) => (
        <SettPaaVent oppgave={oppgave} redigerbar={true} refreshOppgave={hentOppgaveForBehandling} />
      ))}
    </Wrapper>
  )
}
