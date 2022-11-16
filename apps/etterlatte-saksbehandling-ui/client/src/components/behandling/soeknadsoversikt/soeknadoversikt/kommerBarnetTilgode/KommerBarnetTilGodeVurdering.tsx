import { IKommerBarnetTilgode, JaNei, JaNeiRec, VurderingsResultat } from '~store/reducers/BehandlingReducer'
import { GyldighetIcon } from '~shared/icons/gyldigIcon'
import { Undertekst, VurderingsContainer, VurderingsTitle } from '../../styled'
import styled from 'styled-components'
import { useState } from 'react'
import { EndreVurdering } from './EndreVurdering'
import { formaterStringDato } from '~utils/formattering'
import { svarTilVurderingsstatus } from '../../utils'
import { Edit } from '@navikt/ds-icons'

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
        <GyldighetIcon
          large
          status={
            kommerBarnetTilgode?.svar
              ? svarTilVurderingsstatus(kommerBarnetTilgode.svar)
              : VurderingsResultat.KAN_IKKE_VURDERE_PGA_MANGLENDE_OPPLYSNING
          }
        />
      </div>
      {redigeringsModus ? (
        <EndreVurdering
          kommerBarnetTilgode={kommerBarnetTilgode}
          setRedigeringsModusFalse={() => setRedigeringsModus(false)}
        />
      ) : (
        <div>
          <VurderingsTitle>{tittel}</VurderingsTitle>
          {kommerBarnetTilgode?.kilde?.ident && (
            <Undertekst gray={true}>{`Endret manuelt av ${kommerBarnetTilgode.kilde.ident}`}</Undertekst>
          )}
          <Undertekst gray={true}>
            {kommerBarnetTilgode?.kilde?.tidspunkt
              ? `Sist endret ${formaterStringDato(kommerBarnetTilgode.kilde.tidspunkt)}`
              : 'Ikke vurdert'}
          </Undertekst>
          <Undertekst gray={false}>
            Boforholdet er avklart og sannsynliggjort at pensjonen kommer barnet til gode?
          </Undertekst>
          {kommerBarnetTilgode?.svar && <div>{JaNeiRec[kommerBarnetTilgode.svar]}</div>}
          {kommerBarnetTilgode?.begrunnelse && (
            <BegrunnelseWrapper>
              <Bold>Begrunnelse</Bold>
              <div>{kommerBarnetTilgode.begrunnelse}</div>
            </BegrunnelseWrapper>
          )}
          <RedigerWrapper onClick={() => setRedigeringsModus(true)}>
            <Edit /> Rediger
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

const Bold = styled.div`
  font-weight: bold;
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
  margin: 0.9em 0.9em 0.9em 0;
`
