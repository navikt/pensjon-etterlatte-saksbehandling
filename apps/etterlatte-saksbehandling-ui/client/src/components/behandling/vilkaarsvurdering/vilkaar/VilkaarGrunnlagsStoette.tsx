import { Vilkaar } from '~shared/api/vilkaarsvurdering'
import { AlderBarn } from './AlderBarn'
import styled from 'styled-components'

export const VilkaarGrunnlagsStoette = ({ vilkaar }: { vilkaar: Vilkaar }) => {
  const finnGrunnlag = (vilkaar: Vilkaar) => {
    switch (vilkaar.hovedvilkaar.type) {
      case 'BP_ALDER_BARN':
        return <AlderBarn grunnlag={vilkaar.grunnlag} />
      default:
        return <></>
    }
  }

  return <VilkaarGrunnlagsStoetteWrapper>{finnGrunnlag(vilkaar)}</VilkaarGrunnlagsStoetteWrapper>
}

const VilkaarGrunnlagsStoetteWrapper = styled.div`
  display: flex;
  gap: 1em;
  flex-wrap: wrap;
`
