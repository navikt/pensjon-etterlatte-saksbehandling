import styled from 'styled-components'
import { IVilkaarResultat, IVilkaarsproving, VurderingsResultat } from '../../../../../store/reducers/BehandlingReducer'
import { format } from 'date-fns'
import { GyldighetIcon } from '../../../../../shared/icons/gyldigIcon'
import { hentGyldigBostedTekst } from '../../utils'
import { Title, Undertekst } from '../gyldigFramsattSoeknad/styled'

export const KommerBarnetTilGodeVurdering = ({
  gjenlevendeOgSoekerLikAdresse,
  kommerSoekerTilgodeVurdering,
}: {
  gjenlevendeOgSoekerLikAdresse: IVilkaarsproving | undefined
  kommerSoekerTilgodeVurdering: IVilkaarResultat
}) => {
  const hentTekst = (): any => {
    return gjenlevendeOgSoekerLikAdresse && hentGyldigBostedTekst(gjenlevendeOgSoekerLikAdresse)
  }

  const tittel =
    kommerSoekerTilgodeVurdering.resultat !== VurderingsResultat.OPPFYLT
      ? 'Ikke sannsynlig Pensjon kommer barnet til gode'
      : 'Sannsynlig pensjonen kommer barnet til gode'

  return (
    <Wrapper>
      <div>
        {kommerSoekerTilgodeVurdering.resultat && (
          <GyldighetIcon status={kommerSoekerTilgodeVurdering.resultat} large={true} />
        )}
      </div>
      <div>
        <Title>{tittel}</Title>
        <Undertekst gray={true}>
          Automatisk {format(new Date(kommerSoekerTilgodeVurdering.vurdertDato), 'dd.MM.yyyy')}
        </Undertekst>
        {gjenlevendeOgSoekerLikAdresse?.resultat !== VurderingsResultat.OPPFYLT && (
          <Undertekst gray={false}>{hentTekst()}</Undertekst>
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
