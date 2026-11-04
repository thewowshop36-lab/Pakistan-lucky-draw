import { Plus, Trash2 } from "lucide-react"

export type PrizeTierInput = {
  prize_name: string
  prize_description: string
  prize_amount: string
}

export function PrizeTierEditor({
  items,
  onChange,
}: {
  items: PrizeTierInput[]
  onChange: (items: PrizeTierInput[]) => void
}) {
  function updateItem(index: number, key: keyof PrizeTierInput, value: string) {
    onChange(items.map((item, itemIndex) => itemIndex === index ? { ...item, [key]: value } : item))
  }

  return (
    <div className="space-y-3 md:col-span-2">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <div>
          <label className="field-label">Prize tiers</label>
          <p className="text-xs leading-5 text-muted">Cash amounts are reserved from the draw&apos;s available prize pool.</p>
        </div>
        <button type="button" className="secondary-button min-h-9 px-3" onClick={() => onChange([...items, { prize_name: "", prize_description: "", prize_amount: "0" }])} disabled={items.length >= 20}>
          <Plus size={15} aria-hidden="true" /> Add tier
        </button>
      </div>
      {items.map((item, index) => (
        <div key={index} className="grid gap-3 rounded-2xl border border-line bg-surface-soft p-4 sm:grid-cols-[1fr_1fr_10rem_auto] sm:items-end">
          <div>
            <label className="field-label" htmlFor={`prize-name-${index}`}>Tier {index + 1} name</label>
            <input className="field-control" id={`prize-name-${index}`} maxLength={80} value={item.prize_name} onChange={(event) => updateItem(index, "prize_name", event.target.value)} required />
          </div>
          <div>
            <label className="field-label" htmlFor={`prize-description-${index}`}>Description</label>
            <input className="field-control" id={`prize-description-${index}`} maxLength={500} value={item.prize_description} onChange={(event) => updateItem(index, "prize_description", event.target.value)} />
          </div>
          <div>
            <label className="field-label" htmlFor={`prize-amount-${index}`}>Cash amount (PKR)</label>
            <input className="field-control" id={`prize-amount-${index}`} type="number" min="0" max="100000000" step="0.01" value={item.prize_amount} onChange={(event) => updateItem(index, "prize_amount", event.target.value)} required />
          </div>
          <button type="button" className="ghost-button min-h-11 px-3 text-[#8d4545]" onClick={() => onChange(items.filter((_, itemIndex) => itemIndex !== index))} disabled={items.length <= 1} aria-label={`Remove prize tier ${index + 1}`}>
            <Trash2 size={16} aria-hidden="true" />
          </button>
        </div>
      ))}
    </div>
  )
}
