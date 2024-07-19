import { IBehandlingStatus, ViderefoertOpphoer } from '~shared/types/IDetaljertBehandling'
import { VurderingsboksWrapper } from '~components/vurderingsboks/VurderingsboksWrapper'
import {
  BodyShort,
  Heading,
  Label,
  MonthPicker,
  Radio,
  RadioGroup,
  UNSAFE_Combobox,
  useMonthpicker,
} from '@navikt/ds-react'
import { SoeknadsoversiktTextArea } from '../SoeknadsoversiktTextArea'
import { useAppDispatch } from '~store/Store'
import React, { useState } from 'react'
import { useApiCall } from '~shared/hooks/useApiCall'
import { oppdaterBehandlingsstatus, oppdaterViderefoertOpphoer } from '~store/reducers/BehandlingReducer'
import { lagreViderefoertOpphoer } from '~shared/api/behandling'
import { isFailureHandler } from '~shared/api/IsFailureHandler'
import { VilkaarType } from '~shared/api/vilkaarsvurdering'
import { formaterDato } from '~utils/formatering/dato'
import { addMonths } from 'date-fns'
import { UseMonthPickerOptions } from '@navikt/ds-react/esm/date/hooks/useMonthPicker'
import { JaNei, JaNeiRec } from '~shared/types/ISvar'
import { RadioGroupWrapper } from '~components/behandling/vilkaarsvurdering/Vurdering'

const VilkaarTypeTittel: Record<VilkaarType, string> = {
  [VilkaarType.Ingen]: 'Ingen',
  [VilkaarType.BP_FORMAAL_2024]: 'BP formål',
  [VilkaarType.BP_DOEDSFALL_FORELDER_2024]: 'BP dødsfall forelder',
} as const

export const ViderefoereOpphoerVurdering = ({
  virkningstidspunkt,
  viderefoertOpphoer,
  redigerbar,
  setVurdert,
  behandlingId,
}: {
  viderefoertOpphoer: ViderefoertOpphoer | null
  virkningstidspunkt: Date | null
  redigerbar: boolean
  setVurdert: (visVurderingKnapp: boolean) => void
  behandlingId: string
}) => {
  const dispatch = useAppDispatch()

  const [skalViderefoere, setSkalViderefoere] = useState<JaNei | undefined>(viderefoertOpphoer?.skalViderefoere)
  const [opphoerstidspunkt, setOpphoerstidspunkt] = useState<Date | null>(
    viderefoertOpphoer ? new Date(viderefoertOpphoer.dato) : null
  )
  const [vilkaar, setVilkaar] = useState<VilkaarType | undefined>(viderefoertOpphoer?.vilkaar)
  const [vilkaarError, setVilkaarError] = useState<string>('')
  const [begrunnelse, setBegrunnelse] = useState<string>(viderefoertOpphoer?.begrunnelse || '')
  const [setViderefoertOpphoerStatus, setViderefoertOpphoer, resetToInitial] = useApiCall(lagreViderefoertOpphoer)
  const [kravdato] = useState<string | undefined>()

  const valider = () => {
    if (!skalViderefoere) {
      return ''
    }
    if (!vilkaar) {
      return 'Du må velge et vilkår som ikke lenger blir oppfylt'
    }
    if (!opphoerstidspunkt) {
      return 'Du må velge opphørstidspunkt'
    }
    return ''
  }

  const lagre = (onSuccess?: () => void) => {
    setVilkaarError(valider())

    if (vilkaar !== undefined)
      return setViderefoertOpphoer(
        { skalViderefoere, behandlingId, begrunnelse, vilkaar, kravdato, opphoerstidspunkt },
        (viderefoertOpphoer) => {
          dispatch(oppdaterViderefoertOpphoer(viderefoertOpphoer))
          dispatch(oppdaterBehandlingsstatus(IBehandlingStatus.OPPRETTET))
          onSuccess?.()
        }
      )
  }

  const reset = (onSuccess?: () => void) => {
    resetToInitial()
    setSkalViderefoere(viderefoertOpphoer?.skalViderefoere)
    setVilkaar(viderefoertOpphoer?.vilkaar)
    setOpphoerstidspunkt(viderefoertOpphoer?.dato ? new Date(viderefoertOpphoer.dato) : null)
    setVilkaarError('')
    setBegrunnelse(viderefoertOpphoer?.begrunnelse || '')
    setVurdert(viderefoertOpphoer !== null)
    onSuccess?.()
  }

  const { monthpickerProps, inputProps } = useMonthpicker({
    fromDate: virkningstidspunkt ?? opphoerstidspunkt,
    toDate: addMonths(new Date(), 6),
    onMonthChange: (date: Date) => setOpphoerstidspunkt(date),
    inputFormat: 'dd.MM.yyyy',
    onValidate: (val) => {
      if (val.isBefore || val.isAfter) setVilkaarError('Opphørstidspunkt er ikke gyldig')
      else setVilkaarError('')
    },
    defaultSelected: opphoerstidspunkt ?? undefined,
  } as UseMonthPickerOptions)

  const options = Object.keys(VilkaarType).map((k) => k)

  return (
    <VurderingsboksWrapper
      tittel="Skal opphøret umiddelbart videreføres?"
      subtittelKomponent={
        <>
          {viderefoertOpphoer?.skalViderefoere !== undefined && (
            <Label as="p" size="small" style={{ marginBottom: '32px' }}>
              {JaNeiRec[viderefoertOpphoer.skalViderefoere]}
            </Label>
          )}
          {viderefoertOpphoer?.skalViderefoere && (
            <>
              <div>
                <Heading size="xsmall">Opphørstidspunkt</Heading>
                <BodyShort spacing>
                  {viderefoertOpphoer?.dato ? formaterDato(viderefoertOpphoer.dato) : 'Ikke fastsatt'}
                </BodyShort>
              </div>
              <Heading size="xsmall">Vilkår som ikke lenger er oppfylt</Heading>
              {viderefoertOpphoer?.vilkaar ? (
                <Label as="p" size="small" style={{ marginBottom: '32px' }}>
                  {VilkaarTypeTittel[viderefoertOpphoer.vilkaar]}
                </Label>
              ) : (
                <Label as="p" size="small" style={{ marginBottom: '32px' }}>
                  Ikke vurdert
                </Label>
              )}
            </>
          )}
        </>
      }
      redigerbar={redigerbar}
      vurdering={
        viderefoertOpphoer?.kilde
          ? {
              saksbehandler: viderefoertOpphoer?.kilde.ident,
              tidspunkt: new Date(viderefoertOpphoer?.kilde.tidspunkt),
            }
          : undefined
      }
      lagreklikk={lagre}
      avbrytklikk={reset}
      kommentar={viderefoertOpphoer?.begrunnelse}
      defaultRediger={viderefoertOpphoer === null}
    >
      <>
        <div>
          <Heading level="3" size="small">
            Er dette et videreført opphør?
          </Heading>
          <RadioGroupWrapper>
            <RadioGroup
              legend=""
              size="small"
              className="radioGroup"
              onChange={(event) => {
                const jaNeiElement = JaNei[event as JaNei]
                setSkalViderefoere(jaNeiElement)
                setVilkaarError('')
                if (jaNeiElement === JaNei.NEI) {
                  setOpphoerstidspunkt(null)
                  setVilkaar(VilkaarType.Ingen)
                }
              }}
              value={skalViderefoere || ''}
              error={vilkaarError ? vilkaarError : false}
            >
              <div className="flex">
                <Radio value={JaNei.JA}>Ja</Radio>
                <Radio value={JaNei.NEI}>Nei</Radio>
              </div>
            </RadioGroup>
          </RadioGroupWrapper>
        </div>
        <MonthPicker {...monthpickerProps}>
          <MonthPicker.Input label="Opphørstidspunkt" {...inputProps} />
        </MonthPicker>
        <UNSAFE_Combobox
          label="Velg vilkåret som gjør at saken opphører"
          options={options}
          onToggleSelected={(option) => {
            setVilkaar(VilkaarType[option as VilkaarType])
            setVilkaarError('')
          }}
          selectedOptions={!!vilkaar ? [vilkaar!] : []}
          isLoading={false}
          error={vilkaarError ? vilkaarError : false}
        />
        <SoeknadsoversiktTextArea
          label="Begrunnelse"
          placeholder="Valgfritt"
          value={begrunnelse}
          onChange={(e) => {
            const oppdatertBegrunnelse = e.target.value
            setBegrunnelse(oppdatertBegrunnelse)
          }}
        />
        {isFailureHandler({
          apiResult: setViderefoertOpphoerStatus,
          errorMessage: 'Kunne ikke lagre opphørsdato',
        })}
      </>
    </VurderingsboksWrapper>
  )
}
