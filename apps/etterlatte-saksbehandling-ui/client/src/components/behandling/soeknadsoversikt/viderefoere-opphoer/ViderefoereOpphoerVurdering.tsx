import { IBehandlingStatus, IDetaljertBehandling, ViderefoertOpphoer } from '~shared/types/IDetaljertBehandling'
import { VurderingsboksWrapper } from '~components/vurderingsboks/VurderingsboksWrapper'
import { BodyShort, Heading, Label, MonthPicker, UNSAFE_Combobox, useMonthpicker } from '@navikt/ds-react'
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

const VilkaarTypeTittel: Record<VilkaarType, string> = {
  [VilkaarType.BP_FORMAAL_2024]: 'BP formål',
  [VilkaarType.BP_DOEDSFALL_FORELDER_2024]: 'BP dødsfall forelder',
} as const

export const ViderefoereOpphoerVurdering = ({
  behandling,
  viderefoertOpphoer,
  redigerbar,
  setVurdert,
  behandlingId,
}: {
  behandling: IDetaljertBehandling
  viderefoertOpphoer: ViderefoertOpphoer | null
  redigerbar: boolean
  setVurdert: (visVurderingKnapp: boolean) => void
  behandlingId: string
}) => {
  const dispatch = useAppDispatch()

  const [opphoerstidspunkt, setOpphoerstidspunkt] = useState<Date | null>(
    behandling.viderefoertOpphoer ? new Date(behandling.viderefoertOpphoer.dato) : null
  )
  const [vilkaar, setVilkaar] = useState<VilkaarType | undefined>(viderefoertOpphoer?.vilkaar)
  const [vilkaarError, setVilkaarError] = useState<string>('')
  const [begrunnelse, setBegrunnelse] = useState<string>(viderefoertOpphoer?.begrunnelse || '')
  const [setViderefoertOpphoerStatus, setViderefoertOpphoer, resetToInitial] = useApiCall(lagreViderefoertOpphoer)
  const [kravdato] = useState<string | undefined>()

  const valider = () => {
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
        { behandlingId, begrunnelse, vilkaar, kravdato, opphoerstidspunkt },
        (viderefoertOpphoer) => {
          dispatch(oppdaterViderefoertOpphoer(viderefoertOpphoer))
          dispatch(oppdaterBehandlingsstatus(IBehandlingStatus.OPPRETTET))
          onSuccess?.()
        }
      )
  }

  const reset = (onSuccess?: () => void) => {
    resetToInitial()
    setVilkaar(viderefoertOpphoer?.vilkaar)
    setVilkaarError('')
    setBegrunnelse(viderefoertOpphoer?.begrunnelse || '')
    setVurdert(viderefoertOpphoer !== null)
    onSuccess?.()
  }

  const { monthpickerProps, inputProps } = useMonthpicker({
    fromDate: behandling.virkningstidspunkt ?? opphoerstidspunkt,
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
      tittel="Hvilket vilkår?"
      subtittelKomponent={
        <>
          <div>
            <Heading size="xsmall">Opphørstidspunkt</Heading>
            <BodyShort spacing>
              {viderefoertOpphoer?.dato ? formaterDato(viderefoertOpphoer.dato) : 'Ikke fastsatt'}
            </BodyShort>
          </div>
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
        <Heading level="3" size="small">
          Er dette et videreført opphør?
        </Heading>
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
