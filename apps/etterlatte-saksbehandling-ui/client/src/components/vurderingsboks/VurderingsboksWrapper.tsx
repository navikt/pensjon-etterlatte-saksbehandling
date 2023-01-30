import { BodyShort, Button, Detail, Heading } from '@navikt/ds-react'
import { Delete, Edit } from '@navikt/ds-icons'
import React, { ReactElement, useState } from 'react'
import styled from 'styled-components'
import Spinner from '~shared/Spinner'
import { VilkaarVurdertInformasjon } from '~components/behandling/vilkaarsvurdering/Vurdering'
import { formaterDatoMedTidspunkt } from '~utils/formattering'

interface Vurdering {
  saksbehandler: string
  tidspunkt: Date
}

interface Props {
  tittel: string
  subtittelKomponent?: ReactElement
  kommentar?: string
  defaultRediger?: boolean
  redigerbar: boolean
  vurdering: Vurdering | null
  slett?: (onSuccess?: () => void) => void
  children: ReactElement
  lagreklikk: (onSuccess?: () => void) => void
  avbrytklikk: (onSuccess?: () => void) => void
}

export const VurderingsboksWrapper = (props: Props) => {
  const [rediger, setRediger] = useState<boolean>(props.defaultRediger ?? false)
  const [lagrer, setLagrer] = useState(false)

  const reset = () => {
    setRediger(false)
    setLagrer(false)
  }

  return (
    <div>
      {!rediger && (
        <>
          <Oppsummering>
            <Heading size="small" level={'2'}>
              {props.tittel}
            </Heading>
            {props.vurdering && (
              <VilkaarVurdertInformasjon>
                <Detail>Manuelt av {props.vurdering.saksbehandler}</Detail>
                <Detail>
                  Sist endret {props.vurdering.tidspunkt ? formaterDatoMedTidspunkt(props.vurdering.tidspunkt) : '-'}
                </Detail>
              </VilkaarVurdertInformasjon>
            )}
            {props.subtittelKomponent ?? <></>}
            {props.kommentar && (
              <Kommentar>
                <Heading size="xsmall" level={'3'}>
                  Begrunnelse
                </Heading>
                <BodyShort size="small">{props.kommentar}</BodyShort>
              </Kommentar>
            )}
          </Oppsummering>

          {props.redigerbar && (
            <>
              <RedigerWrapper onClick={() => setRediger(true)}>
                <Edit aria-hidden={'true'} />
                <span className={'text'}> Rediger</span>
              </RedigerWrapper>
              {props.slett && (
                <RedigerWrapper
                  onClick={() => {
                    new Promise((resolve) => {
                      setLagrer(true)
                      resolve(props.slett?.(() => setLagrer(false)))
                    }).catch(reset)
                  }}
                >
                  {lagrer ? (
                    <Spinner visible label={''} margin={'0'} variant={'interaction'} />
                  ) : (
                    <Delete aria-hidden={'true'} />
                  )}
                  <span className={'text'}> Slett</span>
                </RedigerWrapper>
              )}
            </>
          )}
        </>
      )}
      {rediger && (
        <>
          {props.children}
          <VurderingKnapper>
            <Button
              loading={lagrer}
              variant={'primary'}
              size={'small'}
              onClick={() => {
                setLagrer(true)
                new Promise((resolve) => resolve(props.lagreklikk(reset))).catch(reset)
              }}
            >
              Lagre
            </Button>
            <Button variant={'secondary'} size={'small'} onClick={() => props.avbrytklikk(() => setRediger(false))}>
              Avbryt
            </Button>
          </VurderingKnapper>
        </>
      )}
    </div>
  )
}

const RedigerWrapper = styled.div`
  display: inline-flex;
  float: left;
  cursor: pointer;
  color: #0067c5;
  margin-right: 10px;

  .text {
    margin-left: 0.3em;
    font-size: 0.7em;
    font-weight: normal;
  }

  &:hover {
    text-decoration-line: underline;
  }
`

const Oppsummering = styled.div`
  font-size: 0.7em;

  p {
    font-weight: normal;
  }
`

const VurderingKnapper = styled.div`
  display: flex;
  button {
    margin-top: 10px;
    margin-right: 10px;
  }
`

const Kommentar = styled.div`
  margin-bottom: 1.5em;
  color: var(--navds-global-color-gray-700);
`
