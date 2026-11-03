const SEP = ' + '

const parts = (name: string) =>
  name
    .split('+')
    .map((p) => p.trim())
    .filter(Boolean)

/** Группы, уже вписанные в название (без учёта регистра): для подсветки кнопок-подсказок. */
export const groupsIn = (name: string): Set<string> => new Set(parts(name).map((p) => p.toLowerCase()))

/** Нажатие на подсказку: добавляет группу через « + » или убирает, если она уже есть. Остальной текст сохраняется. */
export function toggleGroup(name: string, group: string): string {
  const list = parts(name)
  const i = list.findIndex((p) => p.toLowerCase() === group.toLowerCase())
  if (i >= 0) list.splice(i, 1)
  else list.push(group)
  return list.join(SEP)
}
