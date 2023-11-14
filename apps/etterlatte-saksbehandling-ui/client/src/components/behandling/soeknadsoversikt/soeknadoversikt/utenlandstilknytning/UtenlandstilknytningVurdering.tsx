import { IBehandlingStatus, IUtenlandstilknytning, UtenlandstilknytningType } from '~shared/types/IDetaljertBehandling'
import { VurderingsboksWrapper } from '~components/vurderingsboks/VurderingsboksWrapper'
import { BodyShort, Label, Radio, RadioGroup } from '@navikt/ds-react'
import { RadioGroupWrapper } from '~components/behandling/vilkaarsvurdering/Vurdering'
import { Undertekst, VurderingsTitle } from '../../styled'
import { SoeknadsoversiktTextArea } from '../SoeknadsoversiktTextArea'
import { useAppDispatch } from '~store/Store'
import { useState } from 'react'
import { isFailure, useApiCall } from '~shared/hooks/useApiCall'
import { oppdaterBehandlingsstatus, oppdaterUtenlandstilknytning } from '~store/reducers/BehandlingReducer'
import { ApiErrorAlert } from '~ErrorBoundary'
import { lagreUtenlandstilknytning } from '~shared/api/sak'

const UtenlandstilknytningTypeTittel: Record<UtenlandstilknytningType, string> = {
  [UtenlandstilknytningType.NASJONAL]: 'Nasjonal',
  [UtenlandstilknytningType.UTLANDSTILSNITT]: 'Utlandstilsnitt-bosatt Norge',
  [UtenlandstilknytningType.BOSATT_UTLAND]: 'Bosatt utland',
} as const

export const UtenlandstilknytningVurdering = ({
  utenlandstilknytning,
  redigerbar,
  setVurdert,
  sakId,
}: {
  utenlandstilknytning: IUtenlandstilknytning | null
  redigerbar: boolean
  setVurdert: (visVurderingKnapp: boolean) => void
  sakId: number
}) => {
  const dispatch = useAppDispatch()

  const [svar, setSvar] = useState<UtenlandstilknytningType | undefined>(utenlandstilknytning?.type)
  const [radioError, setRadioError] = useState<string>('')
  const [begrunnelse, setBegrunnelse] = useState<string>(utenlandstilknytning?.begrunnelse || '')
  const [setUtenlandstilknytningStatus, setUtenlandstilknytning, resetToInitial] = useApiCall(lagreUtenlandstilknytning)

  const lagre = (onSuccess?: () => void) => {
    !svar ? setRadioError('Du må velge et svar') : setRadioError('')

    if (svar !== undefined)
      return setUtenlandstilknytning({ sakId, begrunnelse, svar }, (utenlandstilknyningstype) => {
        dispatch(oppdaterUtenlandstilknytning(utenlandstilknyningstype))
        dispatch(oppdaterBehandlingsstatus(IBehandlingStatus.OPPRETTET)) //Denne er her bare fordi denne ligger i søknadsoversikten, den burde ligget i saksoversikten etc eller tidligere i flyten
        onSuccess?.()
      })
  }

  const reset = (onSuccess?: () => void) => {
    resetToInitial()
    setSvar(utenlandstilknytning?.type)
    setRadioError('')
    setBegrunnelse(utenlandstilknytning?.begrunnelse || '')
    setVurdert(utenlandstilknytning !== null)
    onSuccess?.()
  }

  return (
    <VurderingsboksWrapper
      tittel=""
      subtittelKomponent={
        <>
          <BodyShort spacing>Hvilken type sak er dette?</BodyShort>
          {utenlandstilknytning?.type ? (
            <Label as="p" size="small" style={{ marginBottom: '32px' }}>
              {UtenlandstilknytningTypeTittel[utenlandstilknytning.type]}
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
        utenlandstilknytning?.kilde
          ? {
              saksbehandler: utenlandstilknytning?.kilde.ident,
              tidspunkt: new Date(utenlandstilknytning?.kilde.tidspunkt),
            }
          : undefined
      }
      lagreklikk={lagre}
      avbrytklikk={reset}
      kommentar={utenlandstilknytning?.begrunnelse}
      defaultRediger={utenlandstilknytning === null}
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
              setSvar(UtenlandstilknytningType[event as UtenlandstilknytningType])
              setRadioError('')
            }}
            value={svar || ''}
            error={radioError ? radioError : false}
          >
            <Radio value={UtenlandstilknytningType.NASJONAL}>Nasjonal</Radio>
            <Radio value={UtenlandstilknytningType.UTLANDSTILSNITT}>Utlandstilsnitt - bosatt Norge</Radio>
            <Radio value={UtenlandstilknytningType.BOSATT_UTLAND}>Bosatt Utland</Radio>
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
        {isFailure(setUtenlandstilknytningStatus) && <ApiErrorAlert>Kunne ikke lagre utlandstilknytning</ApiErrorAlert>}
      </>
    </VurderingsboksWrapper>
  )
}
