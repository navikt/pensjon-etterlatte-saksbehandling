import styled from 'styled-components'
import { VurderingsResultat, IVilkaarsproving, IVilkaarResultat } from '../../../../../store/reducers/BehandlingReducer'
import { format } from 'date-fns'
import { GyldighetIcon } from '../../../../../shared/icons/gyldigIcon'
import { hentGyldigBostedTekst } from '../../utils'
import { Title, Undertekst } from '../gyldigFramsattSoeknad/styled'

export const BarnetTilGode = ({
  sammeAdresse,
  kommerSoekerTilgodeVurdering,
}: {
  sammeAdresse: IVilkaarsproving | undefined
  kommerSoekerTilgodeVurdering: IVilkaarResultat
}) => {
  const sjekkInfo = (): any => {
    return kommerSoekerTilgodeVurdering.resultat && sammeAdresse && hentGyldigBostedTekst(sammeAdresse)
  }

  return (
    <Wrapper>
      {sammeAdresse && (
        <div>
          {sammeAdresse?.resultat && <GyldighetIcon status={kommerSoekerTilgodeVurdering.resultat!!} large={true} />}
        </div>
      )}
      <div>
        <Title>Pensjon kommer barnet til gode</Title>
        <Undertekst gray={true}>
          Automatisk {format(new Date(kommerSoekerTilgodeVurdering.vurdertDato), 'dd.MM.yyyy')}
        </Undertekst>
        {sammeAdresse && sammeAdresse?.resultat !== VurderingsResultat.OPPFYLT && (
          <Undertekst gray={false}>{sjekkInfo()}</Undertekst>
        )}
      </div>
    </Wrapper>
  )
}

export const Wrapper = styled.div`
  display: flex;
  border-left: 4px solid #e5e5e5;
  height: fit-content;
  width: 350px;
`
