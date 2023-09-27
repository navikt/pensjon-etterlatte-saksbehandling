import { BodyShort, Button, Heading, Radio, RadioGroup, Textarea } from '@navikt/ds-react'
import { useEffect, useState } from 'react'
import styled from 'styled-components'
import { BeregningsMetode, BeregningsMetodeBeregningsgrunnlag } from '~shared/types/Beregning'
import { IBehandlingReducer } from '~store/reducers/BehandlingReducer'
import { hentBehandlesFraStatus } from '../felles/utils'

type BeregningsgrunnlagMetodeProps = {
  behandling: IBehandlingReducer
  onSubmit: (data: BeregningsMetodeBeregningsgrunnlag) => void
}

const BeregningsgrunnlagMetode = (props: BeregningsgrunnlagMetodeProps) => {
  const { behandling, onSubmit } = props

  const [beregningsMetodeBeregningsgrunnlag, setBeregningsMetodeBeregningsgrunnlag] =
    useState<BeregningsMetodeBeregningsgrunnlag>()

  const behandles = hentBehandlesFraStatus(behandling?.status)

  useEffect(() => {
    setBeregningsMetodeBeregningsgrunnlag(
      behandling.beregningsGrunnlag?.beregningsMetode ?? {
        beregningsMetode: BeregningsMetode.NASJONAL,
        begrunnelse: '',
      }
    )
  }, [])

  const beskrivelseFor = (metode: BeregningsMetode) => {
    switch (metode) {
      case BeregningsMetode.BEST:
        return 'Den som gir høyest verdi av nasjonal/prorata (EØS/avtale-land, der rettighet er oppfylt etter nasjonale regler)'
      case BeregningsMetode.NASJONAL:
        return 'Nasjonal (nasjonal sak)'
      case BeregningsMetode.PRORATA:
        return 'Prorata (EØS/avtale-land, der rettighet er oppfylt ved sammenlegging)'
    }
  }

  return (
    beregningsMetodeBeregningsgrunnlag && (
      <BeregningsgrunnlagMetodeWrapper>
        <Heading size="medium" level="2">
          Trygdetid brukt i beregningen
        </Heading>

        {behandles && (
          <>
            <RadioGroup
              legend=""
              size="small"
              className="radioGroup"
              value={beregningsMetodeBeregningsgrunnlag.beregningsMetode}
              onChange={(value) =>
                setBeregningsMetodeBeregningsgrunnlag({
                  ...beregningsMetodeBeregningsgrunnlag,
                  beregningsMetode: value,
                })
              }
            >
              <Radio value={BeregningsMetode.NASJONAL}>{beskrivelseFor(BeregningsMetode.NASJONAL)}</Radio>
              <Radio value={BeregningsMetode.PRORATA}>{beskrivelseFor(BeregningsMetode.PRORATA)}</Radio>
              <Radio value={BeregningsMetode.BEST}>{beskrivelseFor(BeregningsMetode.BEST)}</Radio>
            </RadioGroup>

            <Begrunnelse
              value={beregningsMetodeBeregningsgrunnlag.begrunnelse ?? ''}
              onChange={(e) =>
                setBeregningsMetodeBeregningsgrunnlag({
                  ...beregningsMetodeBeregningsgrunnlag,
                  begrunnelse: e.target.value,
                })
              }
            />

            <Button type="submit" size="small" onClick={() => onSubmit(beregningsMetodeBeregningsgrunnlag)}>
              Lagre
            </Button>
          </>
        )}
        {!behandles && (
          <>
            <BodyShort>{beskrivelseFor(beregningsMetodeBeregningsgrunnlag.beregningsMetode)}</BodyShort>

            {beregningsMetodeBeregningsgrunnlag.begrunnelse &&
              beregningsMetodeBeregningsgrunnlag.begrunnelse.length > 0 && (
                <>
                  <Heading size="small" level="3">
                    Begrunnelse
                  </Heading>

                  <BodyShort>{beregningsMetodeBeregningsgrunnlag.begrunnelse ?? ''}</BodyShort>
                </>
              )}
          </>
        )}
      </BeregningsgrunnlagMetodeWrapper>
    )
  )
}

const BeregningsgrunnlagMetodeWrapper = styled.div`
  padding: 1em 4em;
  max-width: 70em;
  margin-bottom: 1rem;
`

const Begrunnelse = styled(Textarea).attrs({
  label: 'Begrunnelse',
  hideLabel: false,
  placeholder: 'Valgfritt',
  minRows: 3,
  autoComplete: 'off',
})`
  margin-bottom: 10px;
  margin-top: 10px;
  width: 250px;
`

export default BeregningsgrunnlagMetode
