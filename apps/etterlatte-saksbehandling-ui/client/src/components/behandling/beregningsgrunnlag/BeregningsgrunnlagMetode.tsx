import { BodyShort, Heading, Radio, RadioGroup, Textarea } from '@navikt/ds-react'
import { useEffect, useState } from 'react'
import styled from 'styled-components'
import { BeregningsMetode, BeregningsMetodeBeregningsgrunnlag } from '~shared/types/Beregning'

type BeregningsgrunnlagMetodeProps = {
  behandles: boolean
  grunnlag: BeregningsMetodeBeregningsgrunnlag | null
  onUpdate: (data: BeregningsMetodeBeregningsgrunnlag) => void
}

const BeregningsgrunnlagMetode = (props: BeregningsgrunnlagMetodeProps) => {
  const { behandles, grunnlag, onUpdate } = props

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

  const [begrunnelse, setBegrunnelse] = useState<string>('')

  useEffect(() => {
    setBegrunnelse(grunnlag?.begrunnelse ?? '')
  }, [])

  return (
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
            value={grunnlag?.beregningsMetode}
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

          <Begrunnelse
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
        </>
      )}
      {!behandles && grunnlag && (
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
