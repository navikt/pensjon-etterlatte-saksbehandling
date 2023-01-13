import { GyldighetIcon } from '~shared/icons/gyldigIcon'
import { Undertekst, VurderingsContainer, VurderingsTitle } from '../../styled'
import styled from 'styled-components'
import { useState } from 'react'
import { EndreVurdering } from './EndreVurdering'
import { formaterStringDato, formaterStringTidspunkt } from '~utils/formattering'
import { svarTilVurderingsstatus } from '../../utils'
import { Edit } from '@navikt/ds-icons'
import { IKommerBarnetTilgode } from '~shared/types/IDetaljertBehandling'
import { JaNei, JaNeiRec } from '~shared/types/ISvar'
import { VurderingsResultat } from '~shared/types/VurderingsResultat'

export const KommerBarnetTilGodeVurdering = ({
  kommerBarnetTilgode,
  redigerbar,
}: {
  kommerBarnetTilgode: IKommerBarnetTilgode | null
  redigerbar: boolean
}) => {
  const [redigeringsModus, setRedigeringsModus] = useState(false)

  const tittel =
    kommerBarnetTilgode?.svar === JaNei.JA
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
          <VurderingsTitle title={tittel} />
          {kommerBarnetTilgode?.kilde ? (
            <>
              <Undertekst $gray>{`Manuelt av ${kommerBarnetTilgode.kilde.ident}`}</Undertekst>
              <Undertekst $gray spacing>{`Sist endret ${formaterStringDato(
                kommerBarnetTilgode.kilde.tidspunkt
              )} kl.${formaterStringTidspunkt(kommerBarnetTilgode.kilde.tidspunkt)}`}</Undertekst>
            </>
          ) : (
            <Undertekst $gray spacing>
              Ikke vurdert
            </Undertekst>
          )}
          <Undertekst spacing>
            Boforholdet er avklart og sannsynliggjort at pensjonen kommer barnet til gode?
          </Undertekst>
          {kommerBarnetTilgode?.svar && <div>{JaNeiRec[kommerBarnetTilgode.svar]}</div>}
          {kommerBarnetTilgode?.begrunnelse && (
            <BegrunnelseWrapper>
              <Bold>Begrunnelse</Bold>
              <div>{kommerBarnetTilgode.begrunnelse}</div>
            </BegrunnelseWrapper>
          )}
          {redigerbar && (
            <RedigerWrapper onClick={() => setRedigeringsModus(true)}>
              <Edit /> Rediger
            </RedigerWrapper>
          )}
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
