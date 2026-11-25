import { useState } from 'react'
import { Textarea, VStack } from '@navikt/ds-react'
import { IBehandlingReducer } from '~store/reducers/BehandlingReducer'
import { ResultatForlengetOms } from './ResultatForlengetOms'
import { ForlengetOmsVurdering } from '~shared/api/forlengetOms'

export const ForlengetOmsIngenAktivitet = ({
  behandling,
  redigerbar,
  vurdering,
  lagrer,
  onLagrerChange,
  onLagret,
}: {
  behandling: IBehandlingReducer
  redigerbar: boolean
  vurdering?: ForlengetOmsVurdering
  lagrer: boolean
  onLagrerChange: (lagrer: boolean) => void
  onLagret: (vurdering: ForlengetOmsVurdering) => void
}) => {
  const [begrunnelse, setBegrunnelse] = useState(
    vurdering?.begrunnelse ?? behandling.vilkaarsvurdering?.resultat?.kommentar ?? ''
  )
  const [visValideringsfeil, setVisValideringsfeil] = useState(false)
  const sporsmaalRedigerbare = redigerbar && !lagrer && !behandling.vilkaarsvurdering?.resultat

  const validerAktivitet = () => {
    setVisValideringsfeil(true)
    return !!begrunnelse.trim()
  }

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
            error={visValideringsfeil && !begrunnelse.trim() ? 'Oppgi en begrunnelse.' : undefined}
          />
        </VStack>
      </VStack>
      <ResultatForlengetOms
        behandling={behandling}
        redigerbar={redigerbar}
        vurdering={vurdering}
        onLagrerChange={onLagrerChange}
        onLagret={onLagret}
        aktivitet={{ type: 'NEI' }}
        innvilgelseFeil="Forlenget OMS kan bare avslås når bruker verken tar utdanning eller deltar i tiltak."
        resultatBegrunnelse={{ tekst: begrunnelse, onChange: setBegrunnelse }}
        validerAktivitet={validerAktivitet}
      />
    </>
  )
}
