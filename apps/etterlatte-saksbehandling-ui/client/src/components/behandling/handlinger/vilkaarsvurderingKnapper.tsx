import { handlinger } from './typer'
import { Button } from '@navikt/ds-react'
import { VurderingsResultat } from '../../../store/reducers/BehandlingReducer'
import { useBehandlingRoutes } from '../BehandlingRoutes'

export const VilkaarsVurderingKnapper = ({ vilkaarResultat }: { vilkaarResultat: VurderingsResultat | undefined }) => {
  const { next } = useBehandlingRoutes()

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
              <Button variant="primary" size="medium" className="button" onClick={() => console.log('Sette på vent')}>
                {handlinger.VILKAARSVURDERING.VENT.navn}
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
