import { IKommerBarnetTilgode, JaNei, JaNeiRec } from '../../../../../store/reducers/BehandlingReducer'
import { GyldighetIcon } from '../../../../../shared/icons/gyldigIcon'
import { Undertekst, VurderingsContainer, VurderingsTitle } from '../../styled'
import { CaseworkerfilledIcon } from '../../../../../shared/icons/caseworkerfilledIcon'
import styled from 'styled-components'
import { useState } from 'react'
import { EndreVurdering } from './EndreVurdering'
import { formaterStringDato } from '../../../../../utils/formattering'
import { svarTilVurderingsstatus } from '../../utils'

export const KommerBarnetTilGodeVurdering = ({
  kommerBarnetTilgode,
}: {
  kommerBarnetTilgode: IKommerBarnetTilgode | null
}) => {
  const [redigeringsModus, setRedigeringsModus] = useState(false)

  const tittel =
    kommerBarnetTilgode === null
      ? 'Ikke vurdert'
      : kommerBarnetTilgode?.svar === JaNei.JA
      ? 'Sannsynlig pensjonen kommer barnet til gode'
      : kommerBarnetTilgode?.svar === JaNei.NEI
      ? 'Ikke sannsynlig pensjonen kommer barnet til gode'
      : 'Usikkert om pensjonen kommer barnet til gode'

  return (
    <VurderingsContainer>
      <div>
        {kommerBarnetTilgode?.svar && (
          <GyldighetIcon status={svarTilVurderingsstatus(kommerBarnetTilgode.svar)} large={true} />
        )}
      </div>
      {redigeringsModus ? (
        <EndreVurdering
          kommerBarnetTilgode={kommerBarnetTilgode}
          setRedigeringsModusFalse={() => setRedigeringsModus(false)}
        />
      ) : (
        <div>
          <VurderingsTitle>{tittel}</VurderingsTitle>
          <Undertekst gray={true}>
            {kommerBarnetTilgode?.kilde?.tidspunkt
              ? formaterStringDato(kommerBarnetTilgode.kilde.tidspunkt)
              : 'Ikke vurdert'}
          </Undertekst>
          <Undertekst gray={false}>
            Boforholdet er avklart og sannsynliggjort at pensjonen kommer barnet til gode?
          </Undertekst>
          <div>{kommerBarnetTilgode?.svar ? JaNeiRec[kommerBarnetTilgode?.svar] : '-'}</div>
          <BegrunnelseWrapper>
            <div style={{ fontWeight: 'bold' }}>Begrunnelse</div>
            <div>{kommerBarnetTilgode?.begrunnelse ?? ''}</div>
          </BegrunnelseWrapper>
          <RedigerWrapper onClick={() => setRedigeringsModus(true)}>
            <CaseworkerfilledIcon /> <span className={'text'}> Rediger vurdering</span>
          </RedigerWrapper>
        </div>
      )}
    </VurderingsContainer>
  )
}

// TODO ai: Trekk ut
export const RedigerWrapper = styled.div`
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
  padding: 0.1em 0.5em;
  border-radius: 4px;
  margin-right: 0.9em;
`
