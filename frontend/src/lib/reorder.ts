export interface Slot {
  id: number
  orderIndex: number
}

/**
 * В БД уникальный индекс (workout_id, order_index), а бэкенд меняет по одной записи за запрос.
 * Поэтому порядок меняется в две фазы: сначала сдвигаемые записи «паркуются» на свободные номера
 * выше текущего максимума, затем встают на итоговые 1..n. Внутри каждой фазы запросы можно слать
 * параллельно: номера в фазе не пересекаются ни друг с другом, ни с неподвижными записями.
 */
export function planReorder(current: Slot[], orderedIds: number[]) {
  const known = new Set(current.map((c) => c.id))
  const order = [...new Set(orderedIds)].filter((id) => known.has(id))
  // записи, которых нет в новом порядке (добавлены параллельно), остаются в конце
  for (const c of [...current].sort((a, b) => a.orderIndex - b.orderIndex)) {
    if (!order.includes(c.id)) order.push(c.id)
  }
  const now = new Map(current.map((c) => [c.id, c.orderIndex]))
  const max = current.reduce((m, c) => Math.max(m, c.orderIndex), 0)
  const moves = order.map((id, i) => ({ id, to: i + 1 })).filter((m) => now.get(m.id) !== m.to)
  return {
    park: moves.map((m, k) => ({ id: m.id, orderIndex: max + 1 + k })),
    place: moves.map((m) => ({ id: m.id, orderIndex: m.to })),
  }
}
