import { IDetaljertBehandling, IUtlandstilknytning } from '~shared/types/IDetaljertBehandling'
import { LovtekstMedLenke } from '../LovtekstMedLenke'
import { Informasjon, Vurdering } from '../styled'
import { UtlandstilknytningVurdering } from './UtlandstilknytningVurdering'

const statusIkon = (utlandstilknytning: IUtlandstilknytning | null) => {
  if (utlandstilknytning === null) {
    return 'warning'
  }
  return 'success'
}

export const Utlandstilknytning = ({
  behandling,
  redigerbar,
}: {
  behandling: IDetaljertBehandling
  redigerbar: boolean
}) => {
  return (
    <LovtekstMedLenke tittel="Utlandstilknytning" hjemler={[]} status={statusIkon(behandling.utlandstilknytning)}>
      <Informasjon>
        Svar for om saken skal behandles som følge av utlandstilknytning basert på om avdøde har bodd/arbeidet i
        EØS/avtale-land eller ikke, og om gjenlevende bor i Norge eller utlandet. Om søker bor i utlandet er det en
        bosatt utland-sak, om avdøde har bodd/arbeidet i EØS/avtale-land og gjenlevende bor i Norge er det en
        utlandstilsnitt-sak. I andre tilfeller er det en nasjonal sak.
      </Informasjon>
      <Vurdering>
        <UtlandstilknytningVurdering
          utlandstilknytning={behandling.utlandstilknytning}
          redigerbar={redigerbar}
          behandlingId={behandling.id}
        />
      </Vurdering>
    </LovtekstMedLenke>
  )
}
