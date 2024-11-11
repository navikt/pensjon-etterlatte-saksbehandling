import { BodyShort, Button, Detail, Heading } from '@navikt/ds-react'
import { PencilIcon, TrashIcon } from '@navikt/aksel-icons'
import React, { ReactElement, useEffect, useState } from 'react'
import styled from 'styled-components'
import Spinner from '~shared/Spinner'
import { VilkaarVurdertInformasjon } from '~components/behandling/vilkaarsvurdering/Vurdering'
import { formaterDatoMedTidspunkt } from '~utils/formatering/dato'

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
  overstyrRediger?: boolean
  setOverstyrRediger?: (rediger: boolean) => void
  visAvbryt?: boolean
}

export const VurderingsboksWrapper = ({
  automatiskVurdertDato,
  avbrytklikk,
  children,
  defaultRediger,
  kommentar,
  lagreklikk,
  overstyrRediger,
  redigerbar,
  setOverstyrRediger,
  slett,
  subtittelKomponent,
  tittel,
  visAvbryt = true,
  vurdering,
}: Props) => {
  const [rediger, setRediger] = useState<boolean>((defaultRediger && redigerbar) ?? false)
  const [lagrer, setLagrer] = useState(false)

  useEffect(() => {
    if (overstyrRediger !== undefined) {
      setRediger(overstyrRediger)
    }
  }, [overstyrRediger])

  return (
    <div>
      {!rediger && (
        <>
          <Oppsummering>
            <Heading size="small" level="2">
              {tittel}
            </Heading>
            {vurdering && (
              <VilkaarVurdertInformasjon>
                <Detail>Manuelt av {vurdering.saksbehandler}</Detail>
                <Detail>Sist endret {vurdering.tidspunkt ? formaterDatoMedTidspunkt(vurdering.tidspunkt) : '-'}</Detail>
              </VilkaarVurdertInformasjon>
            )}
            {automatiskVurdertDato && (
              <VilkaarVurdertInformasjon>
                <Detail>Automatisk</Detail>
                <Detail>{formaterDatoMedTidspunkt(automatiskVurdertDato)}</Detail>
              </VilkaarVurdertInformasjon>
            )}
            {subtittelKomponent ?? <></>}
            {kommentar && (
              <Kommentar>
                <Heading size="xsmall" level="3">
                  Begrunnelse
                </Heading>

                <VurderingsKommentar>{kommentar}</VurderingsKommentar>
              </Kommentar>
            )}
          </Oppsummering>

          {redigerbar && (
            <>
              <RedigerWrapper
                onClick={() => {
                  setRediger(true)
                  if (setOverstyrRediger) setOverstyrRediger(true)
                }}
              >
                <PencilIcon aria-hidden="true" />
                <span className="text"> Rediger</span>
              </RedigerWrapper>
              {slett && (
                <RedigerWrapper
                  onClick={async () => {
                    new Promise(() => {
                      setLagrer(true)
                      slett?.(() => setLagrer(false))
                    })
                  }}
                >
                  {lagrer ? <Spinner label="" margin="0" variant="interaction" /> : <TrashIcon aria-hidden="true" />}
                  <span className="text"> Slett</span>
                </RedigerWrapper>
              )}
            </>
          )}
        </>
      )}
      {rediger && (
        <>
          {children}
          <VurderingKnapper>
            {lagreklikk && (
              <Button
                loading={lagrer}
                variant="primary"
                size="small"
                onClick={async () => {
                  setLagrer(true)
                  new Promise((resolve) => resolve(lagreklikk!!(() => setRediger(false)))).finally(() =>
                    setLagrer(false)
                  )
                }}
              >
                Lagre
              </Button>
            )}
            {avbrytklikk && visAvbryt && (
              <Button variant="secondary" size="small" onClick={() => avbrytklikk!!(() => setRediger(false))}>
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

const VurderingsKommentar = styled(BodyShort)`
  white-space: pre-wrap;
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
  color: var(--a-gray-700);
`
