import { Label } from '@navikt/ds-react'
import { DetailWrapper, WarningIconWrapper } from '../../styled'
import { VurderingsResultat, IGyldighetproving } from '../../../../../store/reducers/BehandlingReducer'
import { WarningIcon } from '../../../../../shared/icons/warningIcon'

export const Innsender = ({ innsenderErForelder }: { innsenderErForelder: IGyldighetproving | undefined }) => {
  const navn = innsenderErForelder?.basertPaaOpplysninger?.innsender?.navn

  return (
    <DetailWrapper>
      {innsenderErForelder?.resultat === VurderingsResultat.OPPFYLT && (
        <div>
          <Label size="small">Innsender</Label>
          {navn}
          <div>(gjenlevende forelder)</div>
        </div>
      )}

      {innsenderErForelder?.resultat === VurderingsResultat.IKKE_OPPFYLT && (
        <div>
          <Label size="small" className="labelWrapperWithIcon">
            <WarningIconWrapper>
              <WarningIcon />
            </WarningIconWrapper>
            Innsender
          </Label>
          <span>{navn}</span>
          <div className="warningText"> Ikke gjenlevende forelder</div>
        </div>
      )}

      {innsenderErForelder?.resultat === VurderingsResultat.KAN_IKKE_VURDERE_PGA_MANGLENDE_OPPLYSNING && (
        <div>
          <Label size="small" className="labelWrapperWithIcon">
            <WarningIconWrapper>
              <WarningIcon />
            </WarningIconWrapper>
            Innsender
          </Label>
          <span className="warningText">Mangler info</span>
        </div>
      )}
    </DetailWrapper>
  )
}
