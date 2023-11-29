const controlDigits1 = [3, 7, 6, 1, 8, 9, 4, 5, 2]
const controlDigits2 = [5, 4, 3, 2, 7, 6, 5, 4, 3, 2]

const mod = (arr: number[], value: string): number => {
  const sum = arr.map((v, i) => v * Number(value[i])).reduce((prevNum, currNum) => prevNum + currNum)

  const result = 11 - (sum % 11)

  if (result === 11) return 0
  else return result
}

const validerKontrollsiffer = (value: string): boolean => {
  const ks1 = Number(value[9])

  const c1 = mod(controlDigits1, value)
  if (c1 === 10 || c1 !== ks1) {
    return false
  }

  const c2 = mod(controlDigits2, value)

  return !(c2 === 10 || c2 !== Number(value[10]))
}

export const fnrErGyldig = (input: string | undefined): boolean => {
  const value = input?.trim()

  if (!/^\d{11}$/.test(value ?? ''))
    // hvis fnr ikke består av 11 siffer
    return false
  else if (/^\d{6}0{5}$/.test(value!!))
    // hvis siste 5 siffer i fnr er 00000
    return false
  else return validerKontrollsiffer(value!!)
}
