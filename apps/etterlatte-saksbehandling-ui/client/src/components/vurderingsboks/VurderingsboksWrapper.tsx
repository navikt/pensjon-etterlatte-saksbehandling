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
  vurdering?: Vurdering
  automatiskVurdertDato?: Date
  slett?: (onSuccess?: () => void) => void
  children?: ReactElement
  lagreklikk?: (onSuccess?: () => void) => void
  avbrytklikk?: (onSuccess?: () => void) => void
}

export const VurderingsboksWrapper = (props: Props) => {
  const [rediger, setRediger] = useState<boolean>((props.defaultRediger && props.redigerbar) ?? false)
  const [lagrer, setLagrer] = useState(false)

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
            {props.automatiskVurdertDato && (
              <VilkaarVurdertInformasjon>
                <Detail>Automatisk</Detail>
                <Detail>{formaterDatoMedTidspunkt(props.automatiskVurdertDato)}</Detail>
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
                  onClick={async () => {
                    new Promise(() => {
                      setLagrer(true)
                      props.slett?.(() => setLagrer(false))
                    })
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
            {props.lagreklikk && (
              <Button
                loading={lagrer}
                variant={'primary'}
                size={'small'}
                onClick={async () => {
                  setLagrer(true)
                  new Promise((resolve) => resolve(props.lagreklikk!!(() => setRediger(false)))).finally(() =>
                    setLagrer(false)
                  )
                }}
              >
                Lagre
              </Button>
            )}
            {props.avbrytklikk && (
              <Button variant={'secondary'} size={'small'} onClick={() => props.avbrytklikk!!(() => setRediger(false))}>
                Avbryt
              </Button>
            )}
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
    font-size: 1rem;
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
