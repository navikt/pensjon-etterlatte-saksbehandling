import { BodyShort, Button, Heading } from '@navikt/ds-react'
import { Delete, Edit } from '@navikt/ds-icons'
import React, { ReactElement, useState } from 'react'
import styled from 'styled-components'

interface Props {
  tittel: string
  subtittelKomponent?: ReactElement
  oppfyltUnntaksvilkaarstittel?: string
  kommentar?: string
  defaultRediger?: boolean
  redigerbar: boolean
  slettVurdering: () => void
  children: ReactElement
  lagreklikk: (onSuccess?: () => void) => Promise<any>
  avbrytklikk: (onSuccess?: () => void) => void
}

export const VurderingsboksWrapper = (props: Props) => {
  const [rediger, setRediger] = useState<null | boolean>(props.defaultRediger ?? false)
  const [lagrer, setLagrer] = useState(false)

  return (
    <div>
      {!rediger && (
        <>
          <KildeVilkaar>
            <Heading size="small" level={'2'}>
              {props.tittel}
            </Heading>
            {props.subtittelKomponent ?? <></>}
            {props.oppfyltUnntaksvilkaarstittel && (
              <VilkaarVurdertInformasjon>
                <Heading size="xsmall" level={'3'}>
                  Unntak er oppfylt
                </Heading>
                <BodyShort size="small">{props.oppfyltUnntaksvilkaarstittel}</BodyShort>
              </VilkaarVurdertInformasjon>
            )}
            {props.kommentar && (
              <VilkaarVurdertInformasjon>
                <Heading size="xsmall" level={'3'}>
                  Begrunnelse
                </Heading>
                <BodyShort size="small">{props.kommentar}</BodyShort>
              </VilkaarVurdertInformasjon>
            )}
          </KildeVilkaar>

          {props.redigerbar && (
            <>
              <RedigerWrapper onClick={() => setRediger(true)}>
                <Edit aria-hidden={'true'} />
                <span className={'text'}> Rediger</span>
              </RedigerWrapper>
              <RedigerWrapper onClick={props.slettVurdering}>
                <Delete aria-hidden={'true'} />
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
              onClick={() => {
                setLagrer(true)
                props.lagreklikk(() => setRediger(false)).finally(() => setLagrer(false))
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

const KildeVilkaar = styled.div`
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

const VilkaarVurdertInformasjon = styled.div`
  margin-bottom: 1.5em;
  color: var(--navds-global-color-gray-700);
`
