import { handlinger } from './typer'
import { Button } from '@navikt/ds-react'
import { VurderingsResultat } from '../../../store/reducers/BehandlingReducer'
import { useBehandlingRoutes } from '../BehandlingRoutes'
import { useState } from 'react'

export const VilkaarsVurderingKnapper = ({ vilkaarResultat }: { vilkaarResultat: VurderingsResultat | undefined }) => {
  const { next } = useBehandlingRoutes()
  const [ventNanv, setVentNavn] = useState<string>(handlinger.VILKAARSVURDERING.VENT.navn)

  function toggleVentNavn() {
    const navn =
      ventNanv === handlinger.VILKAARSVURDERING.VENT.navn
        ? handlinger.VILKAARSVURDERING.GJENNOPPTA.navn
        : handlinger.VILKAARSVURDERING.VENT.navn
    setVentNavn(navn)
  }

  return (
    <>
      {(() => {
        switch (vilkaarResultat) {
          case VurderingsResultat.IKKE_OPPFYLT:
            return (
              //TODO - Avslag
              <Button variant="primary" size="medium" className="button" onClick={() => console.log('Avslag')}>
                {handlinger.VILKAARSVURDERING.AVSLAG.navn}
              </Button>
            )
          case VurderingsResultat.KAN_IKKE_VURDERE_PGA_MANGLENDE_OPPLYSNING:
            return (
              //TODO - oppdatere sak med Status "satt på vent" og Handlinger "Gjenoppta saken" i oppgavebenken
              <Button variant="primary" size="medium" className="button" onClick={() => toggleVentNavn()}>
                {ventNanv}
              </Button>
            )
          case VurderingsResultat.OPPFYLT:
            return (
              <Button variant="primary" size="medium" className="button" onClick={next}>
                {handlinger.VILKAARSVURDERING.BEREGNE.navn}
              </Button>
            )
        }
      })()}
    </>
  )
}
