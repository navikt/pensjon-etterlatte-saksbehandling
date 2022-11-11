import { Vilkaar } from '~shared/api/vilkaarsvurdering'
import { AlderBarn } from './AlderBarn'

export const VilkaarGrunnlagsStoette = ({ vilkaar }: { vilkaar: Vilkaar }) => {
  const finnGrunnlag = (vilkaar: Vilkaar) => {
    switch (vilkaar.hovedvilkaar.type) {
      case 'ALDER_BARN':
        return <AlderBarn grunnlag={vilkaar.grunnlag} />
      default:
        return <></>
    }
  }

  return finnGrunnlag(vilkaar)
}
