import { useContext } from 'react'
import { AppContext } from '../../../store/AppContext'
import { handlinger } from './typer'
import { Button } from '@navikt/ds-react'

export const VilkaarsVurderingKnapper = ({ nextPage }: { nextPage: () => void }) => {
  const ctx = useContext(AppContext)
  const vilkaarResultat = ctx.state.behandlingReducer.vilkårsprøving.resultat

  //TODO - hvis ikke oppfylt eller settes på vent, hva skal skje med sidemenyen? skal man ikke kunne trykke videre i den?

  return (
    <>
      {(() => {
        switch (vilkaarResultat) {
          case 'IKKE_OPPFYLT':
            return (
              //TODO - Avslag
              <Button variant="primary" size="medium" className="button" onClick={() => console.log('Avslag')}>
                {handlinger.VILKAARSVURDERING.AVSLAG.navn}
              </Button>
            )
          case 'KAN_IKKE_VURDERE_PGA_MANGLENDE_OPPLYSNING':
            return (
              //TODO - oppdatere sak med Status "satt på vent" og Handlinger "Gjenoppta saken" i oppgavebenken
              <Button variant="primary" size="medium" className="button" onClick={() => console.log('Sette på vent')}>
                {handlinger.VILKAARSVURDERING.VENT.navn}
              </Button>
            )
          case 'OPPFYLT':
            return (
              <Button variant="primary" size="medium" className="button" onClick={nextPage}>
                {handlinger.VILKAARSVURDERING.BEREGNE.navn}
              </Button>
            )
        }
      })()}
    </>
  )
}
