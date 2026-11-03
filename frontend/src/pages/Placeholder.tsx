export default function Placeholder({ title, hint }: { title: string; hint: string }) {
  return (
    <>
      <h1 className="text-3xl font-bold tracking-tight">{title}</h1>
      <p className="mt-6 py-12 text-center text-mute">{hint}</p>
    </>
  )
}
