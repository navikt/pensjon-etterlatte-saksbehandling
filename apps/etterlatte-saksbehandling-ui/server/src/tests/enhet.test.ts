import { expect } from 'chai'
import { lagEnhetFraString } from '../utils/enhet'

describe('Enhet test', () => {
  it('Should create Enhet object successfully', () => {
    const enhetString = '0001 NAV Etterlatte'
    const enhet = lagEnhetFraString(enhetString)
    expect(enhet).to.have.property('enhetId', '0001')
    expect(enhet).to.have.property('navn', 'NAV Etterlatte')
  })
})
