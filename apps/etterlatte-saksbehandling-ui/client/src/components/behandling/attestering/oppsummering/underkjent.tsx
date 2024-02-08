import { Info, Overskrift, Tekst, UnderOverskrift, Wrapper } from '../styled'
import { useAppSelector } from '~store/Store'
import { formaterBehandlingstype, formaterDatoMedKlokkeslett } from '~utils/formattering'
import { IBehandlingStatus } from '~shared/types/IDetaljertBehandling'
import { IBehandlingInfo } from '~components/behandling/sidemeny/IBehandlingInfo'
import { KopierbarVerdi } from '~shared/statusbar/kopierbarVerdi'

export const Underkjent = ({ behandlingsInfo }: { behandlingsInfo: IBehandlingInfo }) => {
  const innloggetId = useAppSelector((state) => state.saksbehandlerReducer.innloggetSaksbehandler.ident)
  const underkjentSiste = behandlingsInfo.underkjentLogg?.slice(-1)[0]
  const fattetSiste = behandlingsInfo.fattetLogg?.slice(-1)[0]

  const erReturnert = behandlingsInfo.status === IBehandlingStatus.RETURNERT
  const saksbehandler = fattetSiste?.ident
  const attestant = erReturnert ? underkjentSiste?.ident : innloggetId

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
    </Wrapper>
  )
}
