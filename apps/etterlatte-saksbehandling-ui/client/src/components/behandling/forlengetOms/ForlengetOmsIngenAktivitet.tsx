import { useState } from 'react'
import { Heading, Textarea, VStack } from '@navikt/ds-react'
import { IBehandlingReducer } from '~store/reducers/BehandlingReducer'
import { ResultatForlengetOms } from './ResultatForlengetOms'

export const ForlengetOmsIngenAktivitet = ({
  behandling,
  redigerbar,
}: {
  behandling: IBehandlingReducer
  redigerbar: boolean
}) => {
  const [begrunnelse, setBegrunnelse] = useState(behandling.vilkaarsvurdering?.resultat?.kommentar ?? '')
  const [visValideringsfeil, setVisValideringsfeil] = useState(false)
  const sporsmaalRedigerbare = redigerbar && !behandling.vilkaarsvurdering?.resultat

  const validerAktivitet = () => {
    setVisValideringsfeil(true)
    return !!begrunnelse.trim()
  }

  return (
    <>
      <VStack paddingInline="space-64 space-32" paddingBlock="space-24 space-32">
        <VStack gap="space-16" maxWidth="30rem">
          <Heading size="medium" level="2">
            Begrunnelse
          </Heading>
          <Textarea
            label="Begrunnelse"
            hideLabel
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
        resultatBegrunnelse={{ tekst: begrunnelse, onChange: setBegrunnelse }}
        validerAktivitet={validerAktivitet}
        kunAvslag
      />
    </>
  )
}
