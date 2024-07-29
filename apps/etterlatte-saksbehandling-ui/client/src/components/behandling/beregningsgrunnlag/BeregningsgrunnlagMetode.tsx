import { BodyShort, Box, Heading, Radio, RadioGroup, Textarea } from '@navikt/ds-react'
import { useEffect, useState } from 'react'
import styled from 'styled-components'
import { BeregningsMetode, BeregningsMetodeBeregningsgrunnlag } from '~shared/types/Beregning'

type BeregningsgrunnlagMetodeProps = {
  redigerbar: boolean
  grunnlag: BeregningsMetodeBeregningsgrunnlag | null
  onUpdate: (data: BeregningsMetodeBeregningsgrunnlag) => void
}

const BeregningsgrunnlagMetode = (props: BeregningsgrunnlagMetodeProps) => {
  const { redigerbar, grunnlag, onUpdate } = props

  const beskrivelseFor = (metode: BeregningsMetode | null) => {
    switch (metode) {
      case BeregningsMetode.BEST:
        return 'Den som gir høyest verdi av nasjonal/prorata (EØS/avtale-land, der rettighet er oppfylt etter nasjonale regler)'
      case BeregningsMetode.NASJONAL:
        return 'Nasjonal beregning (folketrygdberegning)'
      case BeregningsMetode.PRORATA:
        return 'Prorata (EØS/avtale-land, der rettighet er oppfylt ved sammenlegging)'
      default:
        return ''
    }
  }

  const [begrunnelse, setBegrunnelse] = useState<string>('')

  useEffect(() => {
    setBegrunnelse(grunnlag?.begrunnelse ?? '')
  }, [])

  return (
    <BeregningsgrunnlagMetodeWrapper>
      <Heading size="medium" level="2">
        Trygdetid brukt i beregningen
      </Heading>

      {redigerbar && (
        <>
          <RadioGroup
            legend=""
            size="small"
            className="radioGroup"
            value={grunnlag?.beregningsMetode ?? ''}
            onChange={(value) =>
              onUpdate({
                beregningsMetode: value,
                begrunnelse: grunnlag?.begrunnelse,
              })
            }
          >
            <Radio value={BeregningsMetode.NASJONAL}>{beskrivelseFor(BeregningsMetode.NASJONAL)}</Radio>
            <Radio value={BeregningsMetode.PRORATA}>{beskrivelseFor(BeregningsMetode.PRORATA)}</Radio>
            <Radio value={BeregningsMetode.BEST}>{beskrivelseFor(BeregningsMetode.BEST)}</Radio>
          </RadioGroup>

          <Box width="15rem">
            <Textarea
              label="Begrunnelse"
              placeholder="Valgfritt"
              minRows={3}
              autoComplete="off"
              disabled={grunnlag === undefined}
              value={begrunnelse}
              onChange={(e) => setBegrunnelse(e.target.value)}
              onBlur={() =>
                onUpdate({
                  beregningsMetode: grunnlag!!.beregningsMetode,
                  begrunnelse: begrunnelse,
                })
              }
            />
          </Box>
        </>
      )}
      {!redigerbar && grunnlag && (
        <>
          <BodyShort>{beskrivelseFor(grunnlag.beregningsMetode)}</BodyShort>

          {grunnlag.begrunnelse && grunnlag.begrunnelse.length > 0 && (
            <>
              <Heading size="small" level="3">
                Begrunnelse
              </Heading>

              <BodyShort>{grunnlag.begrunnelse}</BodyShort>
            </>
          )}
        </>
      )}
    </BeregningsgrunnlagMetodeWrapper>
  )
}

const BeregningsgrunnlagMetodeWrapper = styled.div`
  max-width: 70em;
  margin-bottom: 1rem;
`

export default BeregningsgrunnlagMetode
