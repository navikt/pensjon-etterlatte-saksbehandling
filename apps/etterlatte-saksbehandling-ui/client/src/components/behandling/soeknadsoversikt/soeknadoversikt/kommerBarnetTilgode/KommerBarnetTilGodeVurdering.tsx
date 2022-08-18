import {
  ISvar,
  IVilkaarResultat,
  KriterieOpplysningsType,
  Kriterietype,
  VilkaarsType,
  VurderingsResultat,
} from '../../../../../store/reducers/BehandlingReducer'
import { GyldighetIcon } from '../../../../../shared/icons/gyldigIcon'
import { VurderingsTitle, Undertekst, VurderingsContainer } from '../../styled'
import { CaseworkerfilledIcon } from '../../../../../shared/icons/caseworkerfilledIcon'
import styled from 'styled-components'
import { useState } from 'react'
import { EndreVurdering } from './EndreVurdering'
import { hentKriterierMedOpplysning } from '../../../felles/utils'
import { formatterStringDato } from '../../../../../utils/formattering'

export const KommerBarnetTilGodeVurdering = ({
  kommerSoekerTilgodeVurdering,
  automatiskTekst,
}: {
  kommerSoekerTilgodeVurdering: IVilkaarResultat
  automatiskTekst: string
}) => {
  const [redigeringsModus, setRedigeringsModus] = useState(false)
  const vilkaar = kommerSoekerTilgodeVurdering?.vilkaar
  const saksbehandlerVurdering = vilkaar.find((vilkaar) => vilkaar.navn === VilkaarsType.SAKSBEHANDLER_RESULTAT)
  const saksbehandlerKriterie = hentKriterierMedOpplysning(
    saksbehandlerVurdering,
    Kriterietype.SAKSBEHANDLER_RESULTAT,
    KriterieOpplysningsType.SAKSBEHANDLER_RESULTAT
  )

  const saksbehandlerOpplysning: SaksbehandlerOpplysning = saksbehandlerKriterie?.opplysning

  interface SaksbehandlerOpplysning {
    svar: ISvar
    kommentar: string
  }

  const harSaksbehandlerOpplysning = saksbehandlerKriterie !== undefined

  const tittel =
    kommerSoekerTilgodeVurdering.resultat !== VurderingsResultat.OPPFYLT
      ? 'Ikke sannsynlig pensjonen kommer barnet til gode'
      : 'Sannsynlig pensjonen kommer barnet til gode'

  const typeVurdering = saksbehandlerVurdering ? 'Vurdert av ' + saksbehandlerKriterie?.kilde.ident : 'Automatisk '
  const redigerTekst = saksbehandlerVurdering ? 'Rediger vurdering' : 'Legg til vurdering'

  return (
    <VurderingsContainer>
      <div>
        {kommerSoekerTilgodeVurdering.resultat && (
          <GyldighetIcon status={kommerSoekerTilgodeVurdering.resultat} large={true} />
        )}
      </div>
      {redigeringsModus ? (
        <EndreVurdering setRedigeringsModusFalse={() => setRedigeringsModus(false)} />
      ) : (
        <div>
          <VurderingsTitle>{tittel}</VurderingsTitle>
          <Undertekst gray={true}>
            {typeVurdering} {formatterStringDato(kommerSoekerTilgodeVurdering.vurdertDato)}
          </Undertekst>
          {harSaksbehandlerOpplysning ? (
            <div>
              <Undertekst gray={false}>
                Boforholdet er avklart og sannsynliggjort at pensjonen kommer barnet til gode?
              </Undertekst>
              <div>{saksbehandlerOpplysning.svar}</div>
              <BegrunnelseWrapper>
                <div style={{ fontWeight: 'bold' }}>Begrunnelse</div>
                <div>{saksbehandlerOpplysning.kommentar}</div>
              </BegrunnelseWrapper>
            </div>
          ) : (
            <Undertekst gray={false}>{automatiskTekst}</Undertekst>
          )}
          <RedigerWrapper onClick={() => setRedigeringsModus(true)}>
            <CaseworkerfilledIcon /> <span className={'text'}> {redigerTekst}</span>
          </RedigerWrapper>
        </div>
      )}
    </VurderingsContainer>
  )
}

const RedigerWrapper = styled.div`
  display: inline-flex;
  cursor: pointer;
  color: #0056b4;
  margin-top: 10px;

  .text {
    margin-left: 0.3em;
  }

  &:hover {
    text-decoration-line: underline;
  }
`

export const RadioGroupWrapper = styled.div`
  margin-top: 0.5em;
  margin-bottom: 1em;

  .flex {
    display: flex;
    gap: 20px;
  }
`
const BegrunnelseWrapper = styled.div`
  background-color: ${'#EFECF4'};
  color: ;
  padding: 0.1em 0.5em;
  border-radius: 4px;
  margin-right: 0.9em;
`
