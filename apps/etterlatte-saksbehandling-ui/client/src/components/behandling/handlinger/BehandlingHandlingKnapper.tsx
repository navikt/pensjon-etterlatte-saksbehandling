import { useMatch } from 'react-router-dom'
import { AppContext } from '../../../store/AppContext'
import { useContext } from 'react'
import { Button } from '@navikt/ds-react'
import { useBehandlingRoutes } from '../BehandlingRoutes'
import { BeregningModal } from './sendTilAttesteringModal'
import { AvbrytBehandling } from './avbryt'
import styled from 'styled-components'
import { handlinger } from './typer'
import { VilkaarsVurderingKnapper } from './vilkaarsvurderingKnapper'
import { GyldighetVurderingsResultat } from '../../../store/reducers/BehandlingReducer'

export const BehandlingHandlingKnapper = () => {
  const ctx = useContext(AppContext)
  const { next, back, lastPage, firstPage } = useBehandlingRoutes()
  const section = useMatch('/behandling/:behandlingId/:section')
  const soeknadGyldigFremsatt =
    ctx.state.behandlingReducer.gyldighetsprøving.resultat === GyldighetVurderingsResultat.OPPFYLT

  const NesteKnapp = () => {
    switch (section?.params.section) {
      case 'soeknadsoversikt':
        return (
          <Button variant="primary" size="medium" className="button" onClick={next} disabled={!soeknadGyldigFremsatt}>
            {handlinger.START.navn}
          </Button>
        )
      case 'inngangsvilkaar':
        return <VilkaarsVurderingKnapper nextPage={next} />
      case 'beregne':
        return <BeregningModal nextPage={next} />
      default:
        return (
          <Button variant="primary" size="medium" className="button" onClick={next} disabled={lastPage}>
            {handlinger.NESTE.navn}
          </Button>
        )
    }
  }

  return (
    <KnapperWrapper>
      <div>
        {!firstPage && (
          <Button variant="secondary" size="medium" className="button" onClick={back}>
            {handlinger.TILBAKE.navn}
          </Button>
        )}
        {NesteKnapp()}
      </div>
      <AvbrytBehandling />
    </KnapperWrapper>
  )
}

export const KnapperWrapper = styled.div`
  margin: 3em 0em 2em 0em;
  text-align: center;

  .button {
    padding-left: 2em;
    padding-right: 2em;
    min-width: 200px;
    margin: 0em 1em 0em 1em;
  }
  .textButton {
    margin-top: 1em;
    text-decoration: none;
    cursor: pointer;
    font-weight: bold;
    &:hover {
      text-decoration: underline;
    }
  }
`
