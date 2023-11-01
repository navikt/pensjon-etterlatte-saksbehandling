import { IBehandlingStatus, IUtenlandstilsnitt, INasjonalitetsType } from '~shared/types/IDetaljertBehandling'
import { VurderingsboksWrapper } from '~components/vurderingsboks/VurderingsboksWrapper'
import { BodyShort, Label, Radio, RadioGroup } from '@navikt/ds-react'
import { RadioGroupWrapper } from '~components/behandling/vilkaarsvurdering/Vurdering'
import { Undertekst, VurderingsTitle } from '../../styled'
import { SoeknadsoversiktTextArea } from '../SoeknadsoversiktTextArea'
import { useAppDispatch } from '~store/Store'
import { useState } from 'react'
import { isFailure, useApiCall } from '~shared/hooks/useApiCall'
import { lagreUtenlandstilsnitt } from '~shared/api/behandling'
import { oppdaterBehandlingsstatus, oppdaterUtenlandstilsnitt } from '~store/reducers/BehandlingReducer'
import { ApiErrorAlert } from '~ErrorBoundary'

const UtenlandstilsnittTypeTittel: Record<INasjonalitetsType, string> = {
  [INasjonalitetsType.NASJONAL]: 'Nasjonal',
  [INasjonalitetsType.UTLANDSTILSNITT]: 'Utlandstilsnitt',
  [INasjonalitetsType.BOSATT_UTLAND]: 'Bosatt utland',
} as const

export const UtenlandstilsnittVurdering = ({
  utenlandstilsnitt,
  redigerbar,
  setVurdert,
  behandlingId,
}: {
  utenlandstilsnitt: IUtenlandstilsnitt | undefined
  redigerbar: boolean
  setVurdert: (visVurderingKnapp: boolean) => void
  behandlingId: string
}) => {
  const dispatch = useAppDispatch()

  const [svar, setSvar] = useState<INasjonalitetsType | undefined>(utenlandstilsnitt?.type)
  const [radioError, setRadioError] = useState<string>('')
  const [begrunnelse, setBegrunnelse] = useState<string>(utenlandstilsnitt?.begrunnelse || '')
  const [setUtenlandstilsnittStatus, setUtenlandstilsnitt, resetToInitial] = useApiCall(lagreUtenlandstilsnitt)

  const lagre = (onSuccess?: () => void) => {
    !svar ? setRadioError('Du må velge et svar') : setRadioError('')

    if (svar !== undefined)
      return setUtenlandstilsnitt({ behandlingId, begrunnelse, svar }, (response) => {
        dispatch(oppdaterUtenlandstilsnitt(response))
        dispatch(oppdaterBehandlingsstatus(IBehandlingStatus.OPPRETTET))
        onSuccess?.()
      })
  }

  const reset = (onSuccess?: () => void) => {
    resetToInitial()
    setSvar(utenlandstilsnitt?.type)
    setRadioError('')
    setBegrunnelse(utenlandstilsnitt?.begrunnelse || '')
    setVurdert(utenlandstilsnitt !== null)
    onSuccess?.()
  }

  return (
    <VurderingsboksWrapper
      tittel=""
      subtittelKomponent={
        <>
          <BodyShort spacing>Hvilken type sak er dette?</BodyShort>
          {utenlandstilsnitt?.type ? (
            <Label as="p" size="small" style={{ marginBottom: '32px' }}>
              {UtenlandstilsnittTypeTittel[utenlandstilsnitt.type]}
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
        utenlandstilsnitt?.kilde
          ? {
              saksbehandler: utenlandstilsnitt?.kilde.ident,
              tidspunkt: new Date(utenlandstilsnitt?.kilde.tidspunkt),
            }
          : undefined
      }
      lagreklikk={lagre}
      avbrytklikk={reset}
      kommentar={utenlandstilsnitt?.begrunnelse}
      defaultRediger={utenlandstilsnitt === null}
    >
      <>
        <VurderingsTitle title="Utlandstilknytning" />
        <Undertekst $gray={false}>Hvilken type sak er dette?</Undertekst>
        <RadioGroupWrapper>
          <RadioGroup
            legend=""
            size="small"
            className="radioGroup"
            onChange={(event) => {
              setSvar(INasjonalitetsType[event as INasjonalitetsType])
              setRadioError('')
            }}
            value={svar || ''}
            error={radioError ? radioError : false}
          >
            <Radio value={INasjonalitetsType.NASJONAL}>Nasjonal</Radio>
            <Radio value={INasjonalitetsType.UTLANDSTILSNITT}>Utlandstilsnitt</Radio>
            <Radio value={INasjonalitetsType.BOSATT_UTLAND}>Bosatt Utland</Radio>
          </RadioGroup>
        </RadioGroupWrapper>
        <SoeknadsoversiktTextArea
          value={begrunnelse}
          onChange={(e) => {
            const oppdatertBegrunnelse = e.target.value
            setBegrunnelse(oppdatertBegrunnelse)
          }}
          placeholder="Valgfritt"
        />
        {isFailure(setUtenlandstilsnittStatus) && <ApiErrorAlert>Kunne ikke lagre utlandstilknytning</ApiErrorAlert>}
      </>
    </VurderingsboksWrapper>
  )
}
