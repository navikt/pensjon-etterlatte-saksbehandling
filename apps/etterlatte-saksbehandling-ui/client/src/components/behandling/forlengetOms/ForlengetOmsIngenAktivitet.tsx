import { useState } from 'react'
import { Textarea, VStack } from '@navikt/ds-react'
import { IBehandlingReducer } from '~store/reducers/BehandlingReducer'
import { ResultatForlengetOms } from './ResultatForlengetOms'
import { ForlengetOmsVurdering, LagreForlengetOmsVurdering } from '~shared/api/forlengetOms'

export const ForlengetOmsIngenAktivitet = ({
  behandling,
  redigerbar,
  vurdering,
  lagrer,
  onLagre,
}: {
  behandling: IBehandlingReducer
  redigerbar: boolean
  vurdering?: ForlengetOmsVurdering
  lagrer: boolean
  onLagre: (request: LagreForlengetOmsVurdering) => void
}) => {
  const [begrunnelse, setBegrunnelse] = useState(
    vurdering?.begrunnelse ?? behandling.vilkaarsvurdering?.resultat?.kommentar ?? ''
  )
  const sporsmaalRedigerbare = redigerbar && !lagrer && !behandling.vilkaarsvurdering?.resultat

  return (
    <>
      <VStack paddingInline="space-64 space-32" paddingBlock="space-24 space-32">
        <VStack gap="space-16" maxWidth="30rem">
          <Textarea
            label="Begrunnelse"
            value={begrunnelse}
            onChange={(event) => setBegrunnelse(event.target.value)}
            minRows={6}
            readOnly={!sporsmaalRedigerbare}
          />
        </VStack>
      </VStack>
      <ResultatForlengetOms
        behandling={behandling}
        redigerbar={redigerbar}
        vurdering={vurdering}
        lagrer={lagrer}
        onLagre={onLagre}
        aktivitet={{ type: 'NEI' }}
        innvilgelseFeil="Forlenget OMS kan bare avslås når bruker verken tar utdanning eller deltar i tiltak."
        begrunnelse={begrunnelse}
      />
    </>
  )
}
