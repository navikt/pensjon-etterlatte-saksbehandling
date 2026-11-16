import { useContext } from 'react'
import { Alert, Button, VStack } from '@navikt/ds-react'
import { BehandlingRouteContext } from '~components/behandling/BehandlingRoutes'
import { BehandlingHandlingKnapper } from '~components/behandling/handlinger/BehandlingHandlingKnapper'
import { NesteOgTilbake } from '~components/behandling/handlinger/NesteOgTilbake'
import { IVilkaarsvurdering } from '~shared/api/vilkaarsvurdering'

export const ForlengetOmsVilkaarKnapper = ({
  vilkaarsvurdering,
  redigerbar,
}: {
  vilkaarsvurdering: IVilkaarsvurdering
  redigerbar: boolean
}) => {
  const { next } = useContext(BehandlingRouteContext)
  const alleVilkaarErVurdert = vilkaarsvurdering.vilkaar.every((vilkaar) => !!vilkaar.vurdering)

  return (
    <VStack paddingInline="space-64 space-32" gap="space-16">
      <Alert variant="info">
        Utfallet for forlenget OMS velges på aktivitetssiden. For å endre vilkårene etter at et utfall er valgt, åpne
        aktivitetssiden og velg «Rediger vurdering».
      </Alert>
      {redigerbar ? (
        <BehandlingHandlingKnapper>
          <Button onClick={next} disabled={!alleVilkaarErVurdert}>
            Neste
          </Button>
        </BehandlingHandlingKnapper>
      ) : (
        <NesteOgTilbake />
      )}
    </VStack>
  )
}
