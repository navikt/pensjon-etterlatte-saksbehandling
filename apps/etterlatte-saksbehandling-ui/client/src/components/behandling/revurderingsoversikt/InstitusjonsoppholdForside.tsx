import { LovtekstMedLenke } from '~components/behandling/soeknadsoversikt/soeknadoversikt/LovtekstMedLenke'
import React, { useState } from 'react'
import Insthendelser from '~components/behandling/beregningsgrunnlag/Insthendelser'
import { Radio, RadioGroup, Textarea } from '@navikt/ds-react'
import { JaNei } from '~shared/types/ISvar'
import { RadioGroupWrapper } from '~components/behandling/vilkaarsvurdering/Vurdering'
import styled from 'styled-components'
import { LeggTilVurderingButton } from '~components/behandling/soeknadsoversikt/soeknadoversikt/LeggTilVurderingButton'
import { VurderingsboksWrapper } from '~components/vurderingsboks/VurderingsboksWrapper'
import { KildeSaksbehandler } from '~shared/types/kilde'
import { useApiCall } from '~shared/hooks/useApiCall'
import { lagreInstitusjonsoppholdData } from '~shared/api/behandling'

interface InstitusjonsoppholdMedKilde {
  kanGiReduksjonAvYtelse: JaNei
  kanGiReduksjonAvYtelseBegrunnelse: string
  forventetVarighetMerEnn3Maaneder: JaNei
  forventetVarighetMerEnn3MaanederBegrunnelse: string
  kilde: KildeSaksbehandler
}

export interface Institusjonsopphold {
  kanGiReduksjonAvYtelse: JaNei
  kanGiReduksjonAvYtelseBegrunnelse: string
  forventetVarighetMerEnn3Maaneder: JaNei
  forventetVarighetMerEnn3MaanederBegrunnelse: string
}

type InstitusjonsoppholdProps = {
  behandlingId: string
  sakId: number
  behandles: boolean
  institusjonsopphold: InstitusjonsoppholdMedKilde | undefined
}

type StateError = {
  reduksjon: boolean
  reduksjonBegrunnelse: boolean
  varighet: boolean
  varighetBegrunnelse: boolean
}

const defaultErrorStatus = {
  reduksjon: false,
  reduksjonBegrunnelse: false,
  varighet: false,
  varighetBegrunnelse: false,
}
const InstitusjonsoppholdForside = (props: InstitusjonsoppholdProps) => {
  const { sakId, behandles, behandlingId, institusjonsopphold } = props
  const [vurdert, setVurdert] = useState(institusjonsopphold !== undefined)
  const [svarReduksjon, setSvarReduksjon] = useState<JaNei | undefined>(undefined)
  const [begrunnelseReduksjon, setBegrunnelseReduksjon] = useState<string>('')
  const [svarForventetLengerEnnTreMaaneder, setSvarForventetLengerEnnTreMaaneder] = useState<JaNei | undefined>(
    undefined
  )
  const [begrunnelseForventetLengerEnnTreMaaender, setBegrunnelseForventetLengerEnnTreMaaender] = useState<string>('')

  const [errors, setErrors] = useState<StateError>(defaultErrorStatus)

  const [, lagreInstitusjonsopphold] = useApiCall(lagreInstitusjonsoppholdData)
  const lagreSvarInstitusjonsopphold = () => {
    let tmpErrors = { ...errors }
    if (begrunnelseReduksjon.trim().length === 0) {
      tmpErrors = { ...tmpErrors, reduksjonBegrunnelse: true }
    }
    if (svarReduksjon === undefined) {
      tmpErrors = { ...tmpErrors, reduksjon: true }
    }
    if (svarForventetLengerEnnTreMaaneder === undefined) {
      tmpErrors = { ...tmpErrors, varighet: true }
    }
    if (begrunnelseForventetLengerEnnTreMaaender.trim().length === 0) {
      tmpErrors = { ...tmpErrors, varighetBegrunnelse: true }
    }
    if (Object.values(tmpErrors).every((e) => !e)) {
      lagreInstitusjonsopphold({
        behandlingId: behandlingId,
        institusjonsopphold: {
          kanGiReduksjonAvYtelse: svarReduksjon as JaNei,
          kanGiReduksjonAvYtelseBegrunnelse: begrunnelseReduksjon,
          forventetVarighetMerEnn3Maaneder: svarForventetLengerEnnTreMaaneder as JaNei,
          forventetVarighetMerEnn3MaanederBegrunnelse: begrunnelseForventetLengerEnnTreMaaender,
        },
      })
    } else {
      setErrors(tmpErrors)
    }
  }
  return (
    <>
      <LovtekstMedLenke
        tittel={'Institusjonsopphold'}
        hjemler={[
          {
            tittel: '§ 18-8.Barnepensjon under opphold i institusjon',
            lenke: 'https://lovdata.no/dokument/NL/lov/1997-02-28-19/KAPITTEL_6-6#%C2%A718-8',
          },
        ]}
        status={institusjonsopphold ? 'success' : 'warning'}
      >
        <>
          <MarginInnhold>
            Barnepensjonen skal reduseres under opphold i en institusjon med fri kost og losji under statlig ansvar
            eller tilsvarende institusjon i utlandet. Regelen gjelder ikke ved opphold i somatiske sykehusavdelinger.
            Oppholdet må vare i tre måneder i tillegg til innleggelsesmåneden for at barnepensjonen skal bli redusert.
            Dersom barnet har faste og nødvendige utgifter til bolig, kan arbeids- og velferdsetaten bestemme at
            barnepensjonen ikke skal reduseres eller reduseres mindre enn hovedregelen sier.
          </MarginInnhold>
        </>
        <VurderingsContainerWrapper>
          {!vurdert ? (
            <LeggTilVurderingButton onClick={() => setVurdert(true)}>Legg til vurdering</LeggTilVurderingButton>
          ) : (
            <VurderingsboksWrapper
              tittel={''}
              vurdering={
                institusjonsopphold
                  ? {
                      saksbehandler: institusjonsopphold.kilde.ident,
                      tidspunkt: new Date(institusjonsopphold.kilde.tidspunkt),
                    }
                  : undefined
              }
              redigerbar={behandles}
              lagreklikk={lagreSvarInstitusjonsopphold}
              avbrytklikk={() => {
                setVurdert(false)
                setErrors(defaultErrorStatus)
              }}
              defaultRediger={true}
            >
              <>
                <p>Er dette en institusjon som kan gi reduksjon av ytelsen?</p>
                <RadioGroupWrapper>
                  <RadioGroup
                    legend=""
                    size="small"
                    className="radioGroup"
                    onChange={(event) => {
                      setErrors({ ...errors, reduksjon: false })
                      setSvarReduksjon(JaNei[event as JaNei])
                    }}
                    value={svarReduksjon || ''}
                    error={errors.reduksjon}
                  >
                    <div className="flex">
                      <Radio value={JaNei.JA}>Ja</Radio>
                      <Radio value={JaNei.NEI}>Nei</Radio>
                    </div>
                  </RadioGroup>
                </RadioGroupWrapper>
                <Textarea
                  value={begrunnelseReduksjon}
                  onChange={(e) => {
                    setErrors({ ...errors, reduksjonBegrunnelse: false })
                    setBegrunnelseReduksjon(e.target.value)
                  }}
                  placeholder="Begrunnelse reduksjon"
                  error={errors.reduksjonBegrunnelse && 'Begrunnelse kan ikke være tom'}
                  label=""
                />
                <p>Er oppholdet forventet å vare lenger enn innleggelsesmåned + tre måneder?</p>
                <RadioGroupWrapper>
                  <RadioGroup
                    legend=""
                    size="small"
                    className="radioGroup"
                    onChange={(event) => {
                      setErrors({ ...errors, varighet: false })
                      setSvarForventetLengerEnnTreMaaneder(JaNei[event as JaNei])
                    }}
                    value={svarForventetLengerEnnTreMaaneder || ''}
                    error={errors.varighet}
                  >
                    <div className="flex">
                      <Radio value={JaNei.JA}>Ja</Radio>
                      <Radio value={JaNei.NEI}>Nei</Radio>
                    </div>
                  </RadioGroup>
                </RadioGroupWrapper>
                <Textarea
                  value={begrunnelseForventetLengerEnnTreMaaender}
                  onChange={(e) => {
                    setErrors({ ...errors, varighetBegrunnelse: false })
                    setBegrunnelseForventetLengerEnnTreMaaender(e.target.value)
                  }}
                  placeholder="Begrunnelse varighet"
                  error={errors.varighetBegrunnelse && 'Begrunnelse kan ikke være tom'}
                  label=""
                />
              </>
            </VurderingsboksWrapper>
          )}
        </VurderingsContainerWrapper>
      </LovtekstMedLenke>
      <Insthendelser sakid={sakId} />
    </>
  )
}

export default InstitusjonsoppholdForside

const MarginInnhold = styled('p')({
  marginRight: '2em',
})

const VurderingsContainer = styled.div`
  display: flex;
  flex-direction: column;
  border-left: 4px solid #e5e5e5;
  min-width: 300px;
`

const VurderingsContainerWrapper = styled(VurderingsContainer)`
  padding-left: 20px;
  width: 10em;
`
