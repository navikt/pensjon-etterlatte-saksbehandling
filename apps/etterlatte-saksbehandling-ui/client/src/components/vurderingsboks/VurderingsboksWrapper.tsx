import { BodyShort, Button, Heading } from '@navikt/ds-react'
import { Delete, Edit } from '@navikt/ds-icons'
import React, { ReactElement, useState } from 'react'
import styled from 'styled-components'
import Spinner from '~shared/Spinner'

interface Props {
  tittel: string
  subtittelKomponent?: ReactElement
  kommentar?: string
  defaultRediger?: boolean
  redigerbar: boolean
  slett: (onSuccess?: () => void) => void
  children: ReactElement
  lagreklikk: (onSuccess?: () => void) => void
  avbrytklikk: (onSuccess?: () => void) => void
}

export const VurderingsboksWrapper = (props: Props) => {
  const [rediger, setRediger] = useState<null | boolean>(props.defaultRediger ?? false)
  const [lagrer, setLagrer] = useState(false)

  return (
    <div>
      {!rediger && (
        <>
          <Oppsummering>
            <Heading size="small" level={'2'}>
              {props.tittel}
            </Heading>
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
              <RedigerWrapper
                onClick={async () => {
                  new Promise(() => {
                    setLagrer(true)
                    props.slett(() => setLagrer(false))
                  })
                }}
              >
                {lagrer ? (
                  <Spinner visible={true} label={''} margin={'0'} variant={'interaction'} />
                ) : (
                  <Delete aria-hidden={'true'} />
                )}
                <span className={'text'}> Slett</span>
              </RedigerWrapper>
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
              onClick={async () => {
                setLagrer(true)
                new Promise((resolve) => resolve(props.lagreklikk(() => setRediger(false)))).finally(() =>
                  setLagrer(false)
                )
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
  button {
    margin-top: 10px;
    margin-right: 10px;
  }
`

const Kommentar = styled.div`
  margin-bottom: 1.5em;
  color: var(--navds-global-color-gray-700);
`
