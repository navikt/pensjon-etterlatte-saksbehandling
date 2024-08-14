import { IBehandlingStatus, ViderefoertOpphoer } from '~shared/types/IDetaljertBehandling'
import { VurderingsboksWrapper } from '~components/vurderingsboks/VurderingsboksWrapper'
import {
  BodyShort,
  Box,
  Heading,
  Label,
  MonthPicker,
  Radio,
  RadioGroup,
  Stack,
  UNSAFE_Combobox,
  useMonthpicker,
  VStack,
} from '@navikt/ds-react'
import { SoeknadsoversiktTextArea } from '../SoeknadsoversiktTextArea'
import { useAppDispatch } from '~store/Store'
import React, { useEffect, useState } from 'react'
import { useApiCall } from '~shared/hooks/useApiCall'
import {
  oppdaterBehandlingsstatus,
  oppdaterViderefoertOpphoer,
  resetViderefoertOpphoer,
} from '~store/reducers/BehandlingReducer'
import { lagreViderefoertOpphoer, slettViderefoertOpphoer } from '~shared/api/behandling'
import { isFailureHandler } from '~shared/api/IsFailureHandler'
import { hentVilkaartyper, Vilkaartyper } from '~shared/api/vilkaarsvurdering'
import { formaterDato } from '~utils/formatering/dato'
import { addMonths } from 'date-fns'
import { UseMonthPickerOptions } from '@navikt/ds-react/esm/date/hooks/useMonthPicker'
import { JaNei, JaNeiRec } from '~shared/types/ISvar'
import { isSuccess, mapResult } from '~shared/api/apiUtils'
import Spinner from '~shared/Spinner'
import { ApiErrorAlert, ApiWarningAlert } from '~ErrorBoundary'

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
  const [vilkaar, setVilkaar] = useState<string | undefined>(viderefoertOpphoer?.vilkaar)
  const [vilkaarError, setVilkaarError] = useState<string>('')
  const [begrunnelse, setBegrunnelse] = useState<string>(viderefoertOpphoer?.begrunnelse || '')
  const [setViderefoertOpphoerStatus, setViderefoertOpphoer, resetToInitial] = useApiCall(lagreViderefoertOpphoer)
  const [kravdato] = useState<string | undefined>()
  const [slettViderefoertOpphoerResult, slettViderefoertOpphoerCall, resetSlettToInitial] =
    useApiCall(slettViderefoertOpphoer)

  const [vilkaartyperResult, hentVilkaartyperRequest, resetHentVilkaarToInitial] = useApiCall(hentVilkaartyper)

  useEffect(() => {
    if (virkningstidspunkt != null) {
      hentVilkaartyperRequest(behandlingId)
    } else {
      resetHentVilkaarToInitial()
    }
  }, [behandlingId, virkningstidspunkt])

  const valider = () => {
    if (!skalViderefoere || skalViderefoere == JaNei.NEI) {
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

    if (skalViderefoere !== undefined && !vilkaarError && isSuccess(vilkaartyperResult)) {
      const vilkaartype = vilkaar ? finnVilkaartypeFraTittel(vilkaartyperResult.data, vilkaar)?.name || '' : undefined
      return setViderefoertOpphoer(
        { skalViderefoere, behandlingId, begrunnelse, vilkaar: vilkaartype, kravdato, opphoerstidspunkt },
        (viderefoertOpphoer) => {
          dispatch(oppdaterViderefoertOpphoer(viderefoertOpphoer))
          dispatch(oppdaterBehandlingsstatus(IBehandlingStatus.OPPRETTET))
          onSuccess?.()
        }
      )
    }
  }

  const reset = (onSuccess?: () => void) => {
    resetToInitial()
    resetSlettToInitial()
    resetHentVilkaarToInitial()
    setSkalViderefoere(viderefoertOpphoer?.skalViderefoere)
    setVilkaar(viderefoertOpphoer?.vilkaar)
    setOpphoerstidspunkt(viderefoertOpphoer?.dato ? new Date(viderefoertOpphoer.dato) : null)
    setVilkaarError('')
    setBegrunnelse(viderefoertOpphoer?.begrunnelse || '')
    setVurdert(viderefoertOpphoer !== null)
    onSuccess?.()
  }

  const finnVilkaartypeFraTittel = (vilkaar: Vilkaartyper, tittel: string) => {
    return vilkaar.typer.find((p) => p.tittel === tittel)
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

  function haandterViderefoertOpphoer(event: JaNei) {
    const jaNeiElement = JaNei[event as JaNei]
    setSkalViderefoere(jaNeiElement)
    setVilkaarError('')
    if (jaNeiElement === JaNei.NEI) {
      setOpphoerstidspunkt(null)
      setVilkaar(undefined)
    }
  }

  const slettOpphoer = (onSuccess?: () => void) =>
    slettViderefoertOpphoerCall({ behandlingId: behandlingId }, () => {
      onSuccess?.()
      dispatch(resetViderefoertOpphoer())
      setVurdert(false)
    })

  return (
    <VurderingsboksWrapper
      tittel="Skal opphøret umiddelbart videreføres?"
      subtittelKomponent={
        <Box paddingBlock="0 8">
          {viderefoertOpphoer?.skalViderefoere !== undefined && (
            <Label as="p" size="small">
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
                <Label as="p" size="small">
                  {isSuccess(vilkaartyperResult) &&
                    vilkaartyperResult.data.typer.find((n) => n.name == viderefoertOpphoer.vilkaar)?.tittel}
                </Label>
              ) : (
                <Label as="p" size="small">
                  Ikke vurdert
                </Label>
              )}
            </>
          )}
        </Box>
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
      slett={(callback) => slettOpphoer(callback)}
    >
      <VStack gap="2">
        <div>
          <Heading level="3" size="small">
            Er det nødvendig å fastsette til og med-dato?
          </Heading>
          <RadioGroup
            legend=""
            size="small"
            className="radioGroup"
            onChange={(event) => {
              haandterViderefoertOpphoer(event)
            }}
            value={skalViderefoere || ''}
            error={vilkaarError ? vilkaarError : false}
          >
            <Stack gap="0 6" direction={{ xs: 'column', sm: 'row' }} wrap={false}>
              <Radio value={JaNei.JA}>Ja</Radio>
              <Radio value={JaNei.NEI}>Nei</Radio>
            </Stack>
          </RadioGroup>
        </div>
        <MonthPicker {...monthpickerProps}>
          <MonthPicker.Input label="Opphørstidspunkt" {...inputProps} />
        </MonthPicker>
        {mapResult(vilkaartyperResult, {
          initial: <ApiWarningAlert>Du må sette virkningstidspunkt først</ApiWarningAlert>,
          pending: <Spinner label="Laster vilkårstyper" />,
          error: () => <ApiErrorAlert>Kunne ikke laste vilkårstyper</ApiErrorAlert>,
          success: (typer) => (
            <UNSAFE_Combobox
              label="Velg vilkåret som gjør at saken opphører"
              options={typer.typer.map((i) => i.tittel)}
              onToggleSelected={(option) => {
                setVilkaar(option)
                setVilkaarError('')
              }}
              selectedOptions={!!vilkaar ? [vilkaar!] : []}
              error={vilkaarError ? vilkaarError : false}
            />
          ),
        })}
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
        {isFailureHandler({
          apiResult: slettViderefoertOpphoerResult,
          errorMessage: 'Kunne ikke slette opphørsdato',
        })}
      </VStack>
    </VurderingsboksWrapper>
  )
}
