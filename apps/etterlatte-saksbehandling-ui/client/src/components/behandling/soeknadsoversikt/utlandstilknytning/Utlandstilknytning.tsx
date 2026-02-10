import { IDetaljertBehandling, IUtlandstilknytning } from '~shared/types/IDetaljertBehandling'
import { SoeknadVurdering } from '../SoeknadVurdering'
import { UtlandstilknytningVurdering } from './UtlandstilknytningVurdering'
import { Box } from '@navikt/ds-react'

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
    <SoeknadVurdering tittel="Utlandstilknytning" hjemler={[]} status={statusIkon(behandling.utlandstilknytning)}>
      <Box marginBlock="space-2" marginInline="space-0" maxWidth="41rem">
        Svar for om saken skal behandles som følge av utlandstilknytning basert på om avdøde har bodd/arbeidet i
        EØS/avtale-land eller ikke, og om gjenlevende bor i Norge eller utlandet. Om søker bor i utlandet er det en
        bosatt utland-sak, om avdøde har bodd/arbeidet i EØS/avtale-land og gjenlevende bor i Norge er det en
        utlandstilsnitt-sak. I andre tilfeller er det en nasjonal sak.
      </Box>
      <Box
        paddingInline="space-2 space-0"
        minWidth="18.75rem"
        width="10rem"
        borderWidth="0 0 0 2"
        borderColor="neutral-subtle"
      >
        <UtlandstilknytningVurdering
          utlandstilknytning={behandling.utlandstilknytning}
          redigerbar={redigerbar}
          behandlingId={behandling.id}
        />
      </Box>
    </SoeknadVurdering>
  )
}
