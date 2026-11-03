import { describe, expect, it } from 'vitest'
import { groupsIn, toggleGroup } from '../lib/workoutName'

describe('название тренировки из подсказок', () => {
  it('добавляет группы через « + »', () => {
    expect(toggleGroup('', 'Спина')).toBe('Спина')
    expect(toggleGroup('Спина', 'Грудь')).toBe('Спина + Грудь')
  })
  it('повторное нажатие убирает группу, независимо от регистра', () => {
    expect(toggleGroup('Спина + Грудь', 'спина')).toBe('Грудь')
    expect(toggleGroup('Грудь', 'Грудь')).toBe('')
  })
  it('сохраняет то, что пользователь вписал сам', () => {
    expect(toggleGroup('Тяжёлая спина+грудь', 'Руки')).toBe('Тяжёлая спина + грудь + Руки')
  })
  it('определяет, какие группы уже в названии', () => {
    expect(groupsIn('Спина+ грудь ')).toEqual(new Set(['спина', 'грудь']))
    expect(groupsIn('')).toEqual(new Set())
  })
})
